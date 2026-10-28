from fastapi import FastAPI, File, Form, HTTPException, UploadFile

from .vision import VisionError, extract_receipt

app = FastAPI(title="Receipt Ledger API", version="0.1.0")


@app.get("/api/health")
def health():
    return {"status": "ok"}


@app.post("/api/vision/extract")
async def vision_extract(
    image: UploadFile = File(...),
    store_hint: str | None = Form(None),
    locale: str | None = Form(None),
):
    data = await image.read()
    if not data:
        raise HTTPException(status_code=400, detail="empty image")
    try:
        result = await extract_receipt(data, image.content_type or "image/jpeg", store_hint)
    except VisionError as e:
        raise HTTPException(status_code=502, detail=str(e)) from e
    return {"success": True, "data": result, "confidence": None}
