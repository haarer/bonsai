from fastapi import FastAPI

app = FastAPI(title="Receipt Ledger API", version="0.1.0")

@app.get("/api/health")
def health():
    return {"status": "ok"}
