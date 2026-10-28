"""Vision extraction proxy.

Forwards receipt images to an OpenAI-compatible chat endpoint (llama.cpp
``llama-server``, Ollama, or a hosted API) and returns the structured
receipt JSON defined in docs/api-contract.md.
"""

import base64
import json
import os
import re

import httpx

VISION_BASE_URL = os.getenv("VISION_BASE_URL", "http://schleppx.local:1235").rstrip("/")
VISION_MODEL = os.getenv("VISION_MODEL", "qwen2.5-vl-3b-instruct")
VISION_API_KEY = os.getenv("VISION_API_KEY", "")
VISION_TIMEOUT = float(os.getenv("VISION_TIMEOUT", "180"))

SYSTEM_PROMPT = """You are an expert at extracting structured data from grocery receipts (especially European stores like Aldi, Lidl, Netto).

Extract the following in valid JSON:
- store: {name, branch, address?}
- date: ISO8601 datetime
- receiptNumber
- items: [{rawText, name, quantity, unitPrice, totalPrice, category, taxCode}]
- totals: {subtotal, tax, total, currency}
- paymentMethod

Rules:
- Normalize product names to English/common names where possible
- Infer categories (produce, dairy, meat, bakery, frozen, drinks, household, etc.)
- Handle weight-based items (e.g., 1.234 kg @ 1.99€/kg)
- Handle discounts/promos
- Use null for values that are not visible on the receipt
- Return ONLY the JSON object. No markdown, no code fences, no commentary."""


class VisionError(Exception):
    """Raised when the upstream call or output parsing fails."""


def _normalize(data: dict) -> dict:
    """Map the model's JSON onto the api-contract.md response shape."""
    store = data.get("store") or {}
    if isinstance(store, str):
        store = {"name": store}

    items = []
    for it in data.get("items") or []:
        if not isinstance(it, dict):
            continue
        try:
            quantity = float(it.get("quantity") or 1.0)
            unit_price = float(it.get("unitPrice") or 0.0)
        except (TypeError, ValueError):
            quantity, unit_price = 1.0, 0.0
        try:
            total_price = float(it.get("totalPrice") or quantity * unit_price)
        except (TypeError, ValueError):
            total_price = quantity * unit_price
        items.append(
            {
                "rawText": str(it.get("rawText") or ""),
                "name": str(it.get("name") or "").strip(),
                "quantity": quantity,
                "unitPrice": unit_price,
                "totalPrice": total_price,
                "category": str(it.get("category") or ""),
                "taxCode": str(it.get("taxCode") or ""),
            }
        )

    totals = data.get("totals") or {}

    def opt(value):
        try:
            return float(value) if value is not None else None
        except (TypeError, ValueError):
            return None

    return {
        "store": {"name": str(store.get("name") or ""), "branch": str(store.get("branch") or "")},
        "date": data.get("date"),
        "receiptNumber": data.get("receiptNumber"),
        "items": items,
        "totals": {
            "subtotal": opt(totals.get("subtotal")),
            "tax": opt(totals.get("tax")),
            "total": opt(totals.get("total")),
            "currency": str(totals.get("currency") or "EUR"),
        },
        "paymentMethod": data.get("paymentMethod"),
    }


def _parse_model_output(text: str) -> dict:
    text = text.strip()
    fence = re.search(r"```(?:json)?\s*(.*?)```", text, re.DOTALL)
    if fence:
        text = fence.group(1).strip()
    # Fall back to the outermost JSON object if the model wrapped prose around it.
    if not text.startswith("{"):
        start, end = text.find("{"), text.rfind("}")
        if start == -1 or end <= start:
            raise VisionError(f"no JSON object in model output: {text[:200]!r}")
        text = text[start : end + 1]
    try:
        data = json.loads(text)
    except json.JSONDecodeError as e:
        raise VisionError(f"invalid JSON from model: {e}") from e
    if not isinstance(data, dict):
        raise VisionError("model output is not a JSON object")
    return _normalize(data)


async def extract_receipt(image_bytes: bytes, content_type: str, store_hint: str | None) -> dict:
    """Call the upstream vision model and return the normalized receipt dict."""
    data_url = f"data:{content_type};base64,{base64.b64encode(image_bytes).decode()}"
    text = SYSTEM_PROMPT
    if store_hint:
        text += f"\n\nThe store is probably: {store_hint}"

    headers = {"Content-Type": "application/json"}
    if VISION_API_KEY:
        headers["Authorization"] = f"Bearer {VISION_API_KEY}"

    payload = {
        "model": VISION_MODEL,
        "temperature": 0.0,
        "max_tokens": 4096,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": text},
                    {"type": "image_url", "image_url": {"url": data_url}},
                ],
            },
        ],
    }

    try:
        async with httpx.AsyncClient(timeout=VISION_TIMEOUT) as client:
            response = await client.post(
                f"{VISION_BASE_URL}/v1/chat/completions", json=payload, headers=headers
            )
    except httpx.HTTPError as e:
        raise VisionError(f"vision service unreachable at {VISION_BASE_URL}: {e}") from e

    if response.status_code != 200:
        raise VisionError(f"vision service returned HTTP {response.status_code}: {response.text[:300]}")

    try:
        body = response.json()
        content = body["choices"][0]["message"]["content"]
        if isinstance(content, list):  # some servers return content parts
            content = "".join(part.get("text", "") for part in content if isinstance(part, dict))
    except (ValueError, KeyError, IndexError) as e:
        raise VisionError(f"unexpected vision service response: {response.text[:300]}") from e

    return _parse_model_output(content)
