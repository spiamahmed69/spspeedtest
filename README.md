# SP Speed Test

Premium Android internet speed test — **real measurements only, never simulated**.

Package: `com.spsiam.spspeedtest` • Version 1.0.0

## What it measures (real network requests only)

- **Download:** real HTTPS GETs to `https://speed.cloudflare.com/__down?bytes=N` (4 parallel connections, 10 s, max ~50 MB; 6 s in Data Saver). Fallbacks: OVH Paris `proof.ovh.net/files/10Mb.dat`, Hetzner `speed.hetzner.de/100MB.bin`. Mbps = actual bytes read × 8 / elapsed seconds.
- **Upload:** real HTTPS POSTs of real bytes to `https://speed.cloudflare.com/__up` (8 s, max ~25 MB). Mbps = actual bytes sent × 8 / elapsed seconds. Servers without an upload endpoint use Cloudflare for upload and the result says so.
- **Ping / Jitter / Packet loss:** 10 real HTTPS requests to the selected server. Ping = mean of successful replies, jitter = mean absolute difference between successive replies, loss = failed / 10.
- **Server selection:** every server is pinged with a real request and ranked by measured latency; the nearest (lowest ping) is picked automatically, or tap to choose.
- **Network, IP, ISP:** Android ConnectivityManager for Wi-Fi / Mobile Data, plus `api.ipify.org` and `ip-api.com` — "Unavailable" on failure, never invented.

If a server is unreachable the test shows an error instead of a fake result.

## Features

Live animated speedometer, download/upload graphs, test history (SQLite, with date/time), share/copy result, detailed report (server, timestamp, network, method, data used), automatic test, data saver, dark/light themes, Dashboard / History / Servers / Settings.

## Build

GitHub Actions builds the debug APK on every push to `main` (JDK 17, Gradle 8.7, AGP 8.5.2) and uploads it as artifact `SPSpeedTest-apk`, plus a GitHub Release.
