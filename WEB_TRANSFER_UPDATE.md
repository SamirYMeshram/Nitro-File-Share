# NitroDrop Native - Web Transfer Update

This ZIP is based on the user-provided project, not on the older generated ZIPs.

## Added

- `webtransfer/` native Kotlin local HTTP server module.
- Web Transfer route and Compose screen.
- Home screen button: **Web Transfer to PC**.
- Phone → PC browser download using `GET /api/download/{id}`.
- HTTP Range support for resumable browser/download-manager downloads.
- PC → phone upload using raw binary chunk `PUT /api/upload`.
- 16 MB browser upload chunk size in the served webpage.
- Live speed/progress/ETA/stability metrics calculated from real transferred bytes.
- Session-token URL protection.
- Wi-Fi high-performance lock while the Web Transfer server is running.
- Uploads are written as `.nitro_part` first and saved to `Downloads/NitroDrop` after completion.

## How to test

1. Install the app on your phone.
2. Connect phone and PC to the same Wi-Fi.
3. Open NitroDrop → **Web Transfer to PC**.
4. Optional: select phone files to make them downloadable from the PC.
5. Tap **Start server**.
6. Copy/open the displayed URL on your PC browser.
7. Download phone files or upload PC files from the browser page.

## Notes

- This is a local Wi-Fi feature. It does not use cloud storage or any backend.
- The feature uses port `8989` so it does not collide with the existing phone-to-phone TCP transfer port `8988`.
- Use trusted Wi-Fi only. Stop the server after finishing transfer.
