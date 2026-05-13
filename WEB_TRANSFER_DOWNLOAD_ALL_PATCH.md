# Web Transfer Download-All Patch

This update adds browser-side and server-side downloaded-state handling for Web Transfer.

## Added

- `Download all missing` button in the browser page.
- Downloaded files are visually marked with a `✓ Downloaded` badge next to the file name.
- Manually downloaded files can still be downloaded again.
- `Download all missing` skips files marked downloaded so repeated clicks do not create duplicate downloads.
- Download marks are tracked in two layers:
  - Server session state after a full `/d/{id}` stream completes.
  - Browser `localStorage` for immediate UI feedback after a download is started.
- `Reset downloaded marks` button clears both local browser marks and server marks for the current session.

## Changed routes

- `GET /api/files` now returns:
  - `sessionId`
  - `downloadedCount`
  - per-file `downloaded: true|false`
- `POST /api/downloaded/reset` clears server-side downloaded marks.

## Important behavior

The browser cannot reliably know the exact moment a normal native browser download finishes without loading the whole file into JavaScript memory. NitroDrop avoids that slow/memory-heavy approach. Instead, the server marks a file downloaded after streaming completes, and the browser marks it immediately after the user starts a download to prevent duplicate download-all clicks.
