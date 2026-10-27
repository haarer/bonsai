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

### Android app
Build with Android Studio (or `./gradlew :app:assembleDebug`).
Requires JDK 17 (Gradle 8.10.2, AGP 8.5.2), Android SDK 35, min SDK 26.
`local.properties` with `sdk.dir` (or `ANDROID_HOME`) must point at the SDK.
No local secrets needed.

### Backend
`docker/docker-compose.yml` starts the FastAPI service (port 8000) with Postgres 16.
The app's Settings screen holds the backend base URL
(default `http://10.0.2.2:8000` for the emulator).

## Current state
- App: camera scan (CameraX + ImageCapture), receipt review with manual item editing,
  local Room storage, ledger list, settings (server URL via DataStore).
  Navigation: Scan / Review / Ledger / Settings with a bottom bar.
- Backend: `/api/health` only. `/api/vision/extract` and `/api/sync/receipts`
  (see `docs/api-contract.md`) are not implemented yet.

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
