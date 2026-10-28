# Receipt Ledger Backend

FastAPI backend for receipt vision extraction and sync.

## Endpoints
- `POST /api/vision/extract` - Extract receipt data from image
- `POST /api/sync/receipts` - Sync receipts from devices
- `GET /api/health` - Health check

## Vision upstream

`POST /api/vision/extract` proxies to an OpenAI-compatible chat endpoint.

| Env var | Default | Description |
|---|---|---|
| `VISION_BASE_URL` | `http://schleppx.local:1235` | Base URL of the vision server (llama.cpp `llama-server`, Ollama, …) |
| `VISION_MODEL` | `qwen2.5-vl-3b-instruct` | Model name passed to the upstream (llama.cpp ignores it) |
| `VISION_API_KEY` | *(empty)* | Bearer token, if the upstream requires auth |
| `VISION_TIMEOUT` | `180` | Upstream request timeout in seconds |

Example upstream (llama.cpp, Qwen2.5-VL 3B):

```bash
llama-server -hf unsloth/Qwen2.5-VL-3B-Instruct-GGUF:Q4_K_M -c 8192 --port 1235
```

The extraction prompt lives in `app/vision.py` (`SYSTEM_PROMPT`), sourced from
`ml/prompts/receipt-extraction.md`.

## Run locally
```bash
pip install -r requirements.txt
uvicorn app.main:app --reload
```
