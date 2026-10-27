# Receipt Ledger App

Android app to scan receipts (bons) from Aldi, Lidl, etc., extract line items via Vision/OCR, and maintain a household ledger with sync capabilities.

## Features
- Camera scanning of receipts
- OCR/Vision model integration (on-device or remote)
- Parse receipt data to structured JSON
- Local storage with Room/SQLite
- Sync extracted data to backend database
- Category/tagging and household ledger view
- Multi-store support

## Architecture
- Android (Kotlin, Jetpack Compose)
- CameraX for image capture
- ML Kit / on-device OCR or remote Vision API
- Room DB for local storage
- Retrofit for API calls
- DataStore for preferences
- Clean Architecture (UI, Domain, Data)

## Project Structure
See directory layout below.

## Setup
TBD

## API Contract
Receipts sent as images to vision service, returns structured JSON:
```json
{
  "store": "Aldi",
  "date": "2026-10-27",
  "total": 12.34,
  "currency": "EUR",
  "items": [
    {"name": "...", "quantity": 1, "price": 1.23, "category": "..."}
  ]
}
```
