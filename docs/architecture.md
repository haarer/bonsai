# Architecture

## Modules
- `app` - Android application
- `backend` - Optional sync server (API + DB)
- `ml` - Vision/OCR processing logic/docs

## Data Flow
1. User captures receipt photo via CameraX
2. Image sent to Vision model (local ML Kit/TensorFlow Lite or remote API)
3. Structured JSON extracted (store, date, items, totals)
4. Validated/edited in app
5. Saved to local Room DB
6. Synced to remote DB when online

## Key Components
- **UI**: Jetpack Compose screens (Scan, Review, Ledger, Settings)
- **Data**: Room entities, DAOs, Repository
- **Network**: Retrofit + OkHttp for remote vision/sync
- **ML**: Interface for VisionProvider (Local/Remote)
- **Sync**: WorkManager for background sync
