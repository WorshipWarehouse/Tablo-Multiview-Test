# Troubleshooting Guide

This guide covers common issues and resolutions when running Tablo Multiview on Amazon Fire TV.

---

## 1. Tablo Not Found During Auto-Discovery

### Cause
Many Wi-Fi routers enable "AP Client Isolation", which prevents wireless devices from receiving UDP broadcast packets from other local devices.

### Resolution
1. **Try "Manual IP Address"**:
   - Open your official Tablo mobile app or router DHCP lease table to find the Tablo's IP (e.g. `192.168.1.42`).
   - In Tablo Multiview, select **Manual IP Address**, enter the IP, and press **Connect**.
2. **Disable AP Isolation**:
   - In your router admin console, disable "Client Isolation" or "Guest Network Isolation".
3. **Wired Connection**:
   - Connecting your Tablo or Fire TV via Ethernet ensures unblocked multicast/broadcast transmission.

---

## 2. "Stream Unavailable" on 3rd or 4th Pane

### Cause
Physical Tablo Gen 4 units have either **2 tuners** or **4 tuners**.
- If you have a **2-Tuner Tablo Gen 4**, the hardware can decode a maximum of two unique over-the-air frequencies simultaneously.
- When you launch a 4-pane multiview layout on a 2-tuner device, subchannels broadcasting on the same physical RF frequency share a tuner, but tuning four completely different RF frequencies may exceed hardware tuner limits.

### Resolution
- Switch the layout mode to **2-Pane (Split)** by pressing **SELECT → Layout Mode → 2P**.
- Or tune panes to channels that share physical digital broadcast multiplexes (e.g., 4.1 and 4.2 share a single tuner).

---

## 3. Playback Buffering on Older Fire TV Sticks

### Cause
Older generation Fire TV sticks (e.g., 1st/2nd gen Fire Stick) have 1 GB of RAM and slower quad-core SOCs that can experience high CPU load when decoding four simultaneous 1080i/720p MPEG-2/H.264 video streams.

### Resolution
- For older Fire TV sticks, use **2-Pane (Split)** or **1-Pane Fullscreen** for optimal 60fps hardware decoding.
- On Fire TV Stick 4K, 4K Max, and Fire TV Cube, the hardware decoder easily powers all 4 streams simultaneously.

---

## 4. Resetting Stored Connection

If your Tablo's IP address changes due to DHCP renewal:
1. Go to **Settings & Diagnostics** from the Home screen.
2. Select **Test Ping** or **Change IP**.
3. If necessary, select **Forget This Tablo** to return to the discovery screen.
