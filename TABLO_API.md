# Tablo Gen 4 Local Network API Reference

Tablo Gen 4 devices operate a local HTTP server on port `8885` and expose a RESTful JSON API. This document summarizes the endpoints utilized by Tablo Multiview.

Reference: [Tablo API Community Docs](https://jessedp.github.io/tablo-api-docs/)

---

## 1. Network Discovery

### UDP Broadcast
- **Port**: 8881 (broadcast destination), 8882 (listen socket)
- **Target Addresses**: `255.255.255.255`, `224.0.0.1`, and local interface subnet broadcasts
- **Payload**: `"Tablo"`
- **Response**: Datagram containing device IP or server JSON payload

### Tablo AssocServer Fallback
When UDP broadcast is filtered by consumer Wi-Fi AP client isolation:
- **URL**: `https://api.tablotv.com/assocserver/getipinfo/`
- **Response**:
  ```json
  {
    "cpes": [
      {
        "server_id": "g4_...",
        "private_ip": "192.168.1.42",
        "name": "Living Room Tablo"
      }
    ]
  }
  ```

---

## 2. Server Information

- **Method**: `GET`
- **Endpoint**: `http://<TABLO_IP>:8885/server/info`
- **Headers**: `Accept: application/json`
- **Sample Response**:
  ```json
  {
    "server_id": "g4_1a2b3c4d",
    "name": "Living Room Tablo",
    "timezone": "America/New_York",
    "version": "2.2.42",
    "local_address": "192.168.1.42",
    "model": {
      "name": "Tablo Gen 4",
      "wifi": true,
      "tuners": 2,
      "type": "2-Tuner"
    },
    "availability": "ready"
  }
  ```

---

## 3. Channel Guide

### List Channels
- **Method**: `GET`
- **Endpoint**: `http://<TABLO_IP>:8885/guide/channels`
- **Response**: Array of channel path strings (e.g. `["/guide/channels/612675", "/guide/channels/612676"]`)

### Batch Query Channels
- **Method**: `POST`
- **Endpoint**: `http://<TABLO_IP>:8885/batch`
- **Body**: JSON array of path strings
- **Response**: JSON dictionary mapping path keys to channel objects:
  ```json
  {
    "/guide/channels/612675": {
      "object_id": 612675,
      "channel": {
        "major": 4,
        "minor": 1,
        "network": "NBC",
        "call_sign": "WNBC-HD",
        "resolution": "1080i",
        "audio": "ac3"
      }
    }
  }
  ```

---

## 4. Live Stream Playback

### Start Stream
- **Method**: `POST`
- **Endpoint**: `http://<TABLO_IP>:8885/guide/channels/<CHANNEL_ID>/watch`
- **Body**: `{}`
- **Response**:
  ```json
  {
    "token": "0123456789abcdef",
    "playlist_url": "http://192.168.1.42:80/stream/pl.m3u8?token=0123456789abcdef",
    "expires": "2026-09-13T12:00:00Z"
  }
  ```

### Stop Stream / Free Tuner
- **Method**: `POST`
- **Endpoint**: `http://<TABLO_IP>:8885/stream/stop`
- **Body**: `{"token": "0123456789abcdef"}`
