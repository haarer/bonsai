# Receipt Ledger Backend

FastAPI backend for receipt vision extraction and sync.

## Endpoints
- `POST /api/vision/extract` - Extract receipt data from image
- `POST /api/sync/receipts` - Sync receipts from devices
- `GET /api/health` - Health check

## Run locally
```bash
pip install -r requirements.txt
uvicorn app.main:app --reload
```
