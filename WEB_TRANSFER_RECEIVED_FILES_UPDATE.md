# Web Transfer Received File Result Update

This update improves PC → phone Web Transfer behavior.

After a laptop/PC uploads a file through the browser, the Android app now shows a received-file result card instead of silently saving the file only.

## Added

- Received Successfully card in the Web Transfer screen
- File name, file type, size, save location, sender IP, average speed, peak speed, duration, checksum status
- Dynamic primary action:
  - Play video
  - Play audio
  - View image
  - Open PDF
  - Install APK
  - Open file
- Share button
- Copy URI button
- Delete button
- Recently received list for multiple uploads in the same session
- MediaStore content URI opening with `Intent.ACTION_VIEW`
- Safe sharing with `Intent.ACTION_SEND` and `FLAG_GRANT_READ_URI_PERMISSION`

## Notes

Files are still saved to `Downloads/NitroDrop` through MediaStore after being written as `.nitro_part` internally.
