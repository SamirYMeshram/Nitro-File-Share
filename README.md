# NitroDrop Native

Native device-to-device file transfer at maximum sustained speed.


## Gradle sync compatibility update

This ZIP pins Android Gradle Plugin to `8.13.2` instead of `9.2.0`. AGP 9.2 requires Gradle 9.4.1, so Android Studio installs using Gradle 8.x can show a sync error before the project opens. AGP 8.13.2 supports API level 36.1 and Kotlin 2.3, which keeps the project easier to import on current stable Android Studio setups.

Recommended import settings:

- Gradle JDK: JDK 17
- Android SDK Platform: API 36 installed
- Android SDK Build Tools: 35.0.0 or newer
- Sync after extracting the ZIP into a short path, for example `D:\Android\NitroDropNative`

## What is implemented

This project is a fully native Android/Kotlin codebase with:

- Jetpack Compose UI
- Kotlin coroutines + Flow/StateFlow
- Raw TCP socket transfer engine
- LAN manual IP path for immediate testing
- LAN NSD discovery/advertising architecture
- Wi-Fi Direct discovery architecture
- Wi-Fi Aware capability gate
- Large chunk streaming defaults: 4 MB, with 1 MB and 8 MB constants
- 8 MB socket buffers
- Sliding-window speed tracking
- Average speed, peak speed, ETA, stability score
- Foreground service and Wi-Fi performance lock
- Resume-capable receiver partial files
- SHA-256 checksum verification
- Room transfer history
- DataStore settings

## Current implementation phase

The codebase is structured for the full transport ladder, but the immediately usable transfer path is:

1. Receiver device opens **Receive Files** and taps **Start Receiver Socket**.
2. Sender device opens **Send Files**, selects a file, enters receiver IP address, then taps **Start LAN TCP Transfer**.

NSD and Wi-Fi Direct modules are included and ready for production wiring, but the safest first test path is manual LAN TCP because it avoids device/OEM Wi-Fi Direct quirks.

## Run

1. Open the project in Android Studio.
2. Let Gradle sync.
3. Install on two physical Android devices.
4. Put both devices on the same Wi-Fi network.
5. Grant Nearby Wi-Fi / notification permissions.
6. On the receiver device, start receiver socket.
7. On the sender device, enter the receiver device IP address and send a file.

## Find receiver IP

On most Android devices:

Settings → Wi-Fi → current network → IP address.

You can also use `adb shell ip addr show wlan0`.

## Performance tuning

- Use 5 GHz or 6 GHz Wi-Fi where available.
- Keep both devices near the router for LAN mode.
- Disable battery saver during large transfers.
- Avoid logging inside transfer loops.
- Keep checksum enabled for correctness; disable only when speed testing trusted local transfers.
- Use 8 MB chunks on fast devices, 4 MB default, 1 MB for weaker/unstable links.
- Router quality usually determines LAN speed more than app code once buffers and chunks are large.

## Debugging

- If sender cannot connect, confirm receiver is listening on port 8988 and both devices are on same subnet.
- If Android kills the transfer, check notification permission and foreground service restrictions.
- If speed drops over time, check thermal throttling and router/client Wi-Fi link rate.
- If saved file is missing, check Downloads/NitroDrop.
- If checksum mismatch occurs, delete the `.nitro_part` receiver partial file and retry.
