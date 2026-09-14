# Tablo Multiview (React)

A modern, high-performance web application engineered for **Tablo Gen 4** devices and multi-stream live television. Watch up to four simultaneous broadcast OTA and FAST channels with dynamic audio focus, live scorebugs, and customizable layouts.

---

## Key Features

- **Quad-Stream Multiview (4 Panes)**: Watch up to four simultaneous broadcast TV feeds in a clean 2x2 grid, side-by-side split (2-pane), primary focus (3-pane), or single full-screen.
- **Audio Follows Focus**: Sound dynamically switches to whichever video pane is highlighted or selected. Inactive panes are muted automatically.
- **Independent HLS Stream Isolation**: Switching channels on any pane does not interrupt, reload, or buffer other panes.
- **Direct Tablo LAN Communication**: Connects directly to Tablo Gen 4 devices on port `8885` using HTTP REST and HLS video streaming.
- **Channel Guide & FAST TV**: Browse both OTA antenna broadcasts and streaming FAST channels, with live program airings, descriptions, and favorite channel toggles.
- **Cloud DVR Library**: Access recorded shows, movies, and sports games with categories and scheduling controls.
- **Saved Layout Presets**: Save and name multi-stream presets (e.g., "Sunday GameDay Quad", "Morning News Grid") and resume with one click.
- **Keyboard & D-Pad Navigation**: Full arrow-key navigation between panes, `Space` for pause/play, `F` or `Enter` for fullscreen zoom, `A` to toggle audio, and `C` to swap channels.

---

## Development

```bash
# Install dependencies
npm install

# Start development server on port 3000
npm run dev

# Build for production
npm run build
```

---

## Keyboard Shortcuts

| Key | Action |
| --- | --- |
| **1 / 2 / 3 / 4** | Switch Layout Mode (Single, Split, Triple, Quad) |
| **Arrow Keys** | Navigate focus between video panes (audio follows immediately) |
| **F / Enter** | Toggle Fullscreen Zoom for active pane |
| **A** | Toggle or cycle audio routing |
| **C / Down** | Open Channel Switcher Drawer |
