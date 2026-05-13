# Web Transfer Reliability Patch

## Root causes fixed

1. **Stale share list**
   - The old `LocalHttpServer` constructor accepted `List<SelectedFile>` and kept that fixed list for the entire server lifetime.
   - Files selected after server start were only reflected in UI state, not in server state.
   - Fixed by adding `WebSharedFileRegistry`, a synchronized live registry queried by the server on every `/api/files` and `/d/{id}` request.

2. **Multiple-download crash / server failure risk**
   - The old download handler used index-based file IDs and had broad error handling that tried to write a JSON 500 even after download headers/body may already have started.
   - Browser cancellations/retries could throw socket I/O exceptions and poison the visible server state.
   - Fixed by using stable internal IDs, opening a fresh `InputStream` per request, catching download I/O separately, and keeping the server running after cancelled/failed downloads.

3. **Long URL token**
   - The old public URL exposed `?token=<long random token>`.
   - Fixed by displaying only `http://<phone-ip>:8989/` when the fixed port is available.
   - If 8989 is busy, the server falls back to an available port and updates the displayed URL.

4. **No one-time password gate**
   - The old page required the token in the URL.
   - Fixed with a random 3-digit password per server session and a local `nd_session` cookie after successful authentication.

5. **Web page did not live-refresh files**
   - The old HTML embedded the file list at page generation time.
   - Fixed by polling `/api/files` every 2 seconds and providing a Refresh button.

## Corrected route design

- `GET /` — password page if not authenticated; web transfer page if authenticated.
- `POST /auth` — validates the 3-digit password and sets `nd_session` cookie.
- `GET /api/files` — live shared-file list from `WebSharedFileRegistry`.
- `GET /d/{id}` — download by stable internal ID.
- `PUT /u` — raw chunk upload from browser.
- `GET /api/upload/status` — resumable upload checkpoint.
- Legacy compatibility remains for `/api/download/{index}` and `/api/upload`.

## Changed files

- `webtransfer/WebSharedFile.kt`
- `webtransfer/WebSharedFileRegistry.kt`
- `webtransfer/WebTransferSecurity.kt`
- `webtransfer/WebTransferStats.kt`
- `webtransfer/WebTransferManager.kt`
- `webtransfer/LocalHttpServer.kt`
- `webtransfer/WebPageAssets.kt`
- `webtransfer/HttpResponseWriter.kt`
- `viewmodel/WebTransferViewModel.kt`
- `ui/screens/WebTransferScreen.kt`

## Manual verification

1. Open Web Transfer.
2. Start server with zero files selected.
3. Open the short address on laptop: `http://PHONE_IP:8989/`.
4. Enter the 3-digit password shown in the Android app.
5. Add files on Android while the laptop page remains open.
6. Confirm files appear automatically within 2 seconds, or press Refresh.
7. Download file 1.
8. Download file 2.
9. Download the same file repeatedly.
10. Start two downloads close together to verify basic concurrent download behavior.
11. Cancel a browser download and confirm Android app/server stays open.
12. Upload files from laptop to phone.
13. Stop and restart server; confirm a new 3-digit password is generated and the old browser session is no longer valid.
