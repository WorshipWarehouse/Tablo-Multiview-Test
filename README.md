# Tablo Multiview for Amazon Fire TV

A native, open-source Android application engineered specifically for **Amazon Fire TV** (and Android TV devices) that connects directly to a **Tablo Gen 4** over your local network and provides a simultaneous four-pane live TV multiview interface.

Self-contained, locally operated, and ready to be sideloaded as an APK.

---

## Key Features

- **Quad-Stream Multiview (4 Panes)**: Watch up to four simultaneous broadcast TV feeds in a clean 2x2 grid, side-by-side split (2-pane), primary focus (3-pane), or single full-screen.
- **Audio Follows Remote Focus**: Sound dynamically switches to whichever video pane is highlighted by your Fire TV remote D-pad. Inactive panes are muted automatically.
- **Independent Stream Isolation**: Switching channels on Pane 2 does not restart or buffer Panes 1, 3, or 4.
- **Direct LAN Communication**: Connects directly to Tablo on port `8885` using HTTP REST and HLS video streaming.
- **Zero Cloud Accounts or Logins**: No cloud database, no subscription, no tracking, and no external proxy servers.
- **Multi-Method Discovery**: Discovers Tablos automatically via UDP broadcast on port 8881, Tablo AssocServer fallback, or subnet scanning, plus full manual IP entry.
- **Local Persistence via Room**: Saves your favorite channel grid presets (e.g. "Sunday Football Quad", "Morning News Grid") and automatically restores your last multiview session.
- **10-Foot UI for Fire TV Remote**: Large high-contrast typography, clear active focus borders, and full D-pad support.

---

## Supported Devices

- **Tablo Gen 4** (2-tuner and 4-tuner models)
- **Amazon Fire TV Stick** (Lite, 4K, 4K Max), Fire TV Cube, Fire TV Smart TVs
- Any Android TV device running Android 7.0 (API 24) or newer

---

## Fire TV Remote Controls

| Remote Button | Action |
| --- | --- |
| **D-Pad Directional** | Move focus between video panes (audio follows immediately) |
| **SELECT / OK** | Open Quick Action Menu (Change Channel, Fullscreen, Layouts, Presets) |
| **BACK** | Close overlay, exit full-screen mode, or return to Home screen |
| **PLAY / PAUSE** | Pause / resume playback |

---

## Quick Installation & Sideloading

1. Build the APK using `./gradlew assembleDebug` or export the generated APK from AI Studio.
2. Enable Developer Options & ADB Debugging on your Fire TV:
   - **Settings** → **My Fire TV** → **About** → Tap device name 7 times.
   - **Developer Options** → Enable **ADB Debugging** and **Install Unknown Apps**.
3. Install via ADB:
   ```bash
   adb connect <FIRE_TV_IP>
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
4. Launch **Tablo Multiview** from your Fire TV home screen or Leanback launcher.

---

## Documentation

- [BUILDING.md](BUILDING.md) — Build prerequisites and Gradle commands
- [TABLO_API.md](TABLO_API.md) — Protocol specification for Tablo Gen 4 local API
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) — Diagnostic steps for network and tuner issues
- [LICENSE](LICENSE) — Open source MIT License
