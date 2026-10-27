# API Contract

## Vision Extraction Endpoint
`POST /api/vision/extract`

Request (multipart):
- `image`: file (jpg/png)
- `store_hint` (optional): "aldi", "lidl", etc.
- `locale` (optional): "de-DE"

Response:
```json
{
  "success": true,
  "data": {
    "store": {"name": "Aldi", "branch": "..."},
    "date": "2026-10-27T10:30:00Z",
    "receiptNumber": "...",
    "items": [
      {
        "rawText": "BANANEN 1.00",
        "name": "Bananas",
        "quantity": 1.0,
        "unitPrice": 0.99,
        "totalPrice": 0.99,
        "category": "produce",
        "taxCode": "A"
      }
    ],
    "totals": {
      "subtotal": 10.00,
      "tax": 1.90,
      "total": 11.90,
      "currency": "EUR"
    },
    "rawOcr": "..."
  },
  "confidence": 0.95
}
```

## Sync Endpoint
`POST /api/sync/receipts`

Body: array of Receipt objects with local IDs, client timestamps.
