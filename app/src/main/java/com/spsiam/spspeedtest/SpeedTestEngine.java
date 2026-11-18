package com.spsiam.spspeedtest;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real internet speed measurement only.
 * Download: real HTTPS GETs, bytes actually read / elapsed time.
 * Upload: real HTTPS POSTs of real bytes, bytes actually sent / elapsed time.
 * Ping/jitter/loss: 10 real HTTPS requests, failures counted as loss.
 * No random numbers and no hardcoded speeds are used anywhere for results.
 * If a server is unreachable the test fails with an error, never a fake result.
 */
public class SpeedTestEngine {

    public interface Listener {
        void onPhase(String phase, int progress);
        void onLiveSpeed(double mbps);
        void onComplete(SpeedTestResult result);
        void onError(String message);
    }

    private volatile boolean stopped = false;
    private final List<Double> downloadSamples = new ArrayList<Double>();
    private final List<Double> uploadSamples = new ArrayList<Double>();

    public List<Double> getDownloadSamples() { return new ArrayList<Double>(downloadSamples); }
    public List<Double> getUploadSamples() { return new ArrayList<Double>(uploadSamples); }
    public void stop() { stopped = true; }

    public void runTest(final SpeedServer server, final boolean dataSaver, final String networkType, final Listener listener) {
        stopped = false;
        downloadSamples.clear();
        uploadSamples.clear();
        final long testStart = System.currentTimeMillis();
        final AtomicLong dataUsed = new AtomicLong(0);
        try {
            // --- Ping: 10 real requests ---
            listener.onPhase("Measuring ping…", 5);
            List<Double> pings = new ArrayList<Double>();
            int failed = 0;
            for (int i = 0; i < 10 && !stopped; i++) {
                double ms = singlePing(server.pingUrl);
                if (ms < 0) failed++; else pings.add(ms);
                listener.onPhase("Measuring ping… (" + (i + 1) + "/10)", 5 + i);
            }
            if (stopped) { listener.onError("Test stopped by user."); return; }
            if (pings.isEmpty()) {
                listener.onError("Ping failed: server " + server.name + " (" + server.pingUrl + ") is unreachable. No fake result was generated. Check your connection and try another server.");
                return;
            }
            double ping = mean(pings);
            double jitter = 0;
            if (pings.size() > 1) {
                double sum = 0;
                for (int i = 1; i < pings.size(); i++) sum += Math.abs(pings.get(i) - pings.get(i - 1));
                jitter = sum / (pings.size() - 1);
            }
            double loss = failed * 100.0 / 10.0;

            // --- Download: real data, parallel threads, fixed time window ---
            listener.onPhase("Measuring download…", 20);
            int dlSeconds = dataSaver ? 6 : 10;
            DownloadOutcome dl = measureDownload(server, dlSeconds, listener, dataUsed);
            if (stopped) { listener.onError("Test stopped by user."); return; }
            if (dl.mbps <= 0) {
                // Fallback to Cloudflare only if a different server was selected and failed.
                if (!server.name.equals("Cloudflare")) {
                    SpeedServer cf = SpeedServer.defaultServers().get(0);
                    listener.onPhase("Download server failed — retrying on Cloudflare…", 25);
                    dl = measureDownload(cf, dlSeconds, listener, dataUsed);
                    if (dl.mbps > 0) serverAvailableNote(cf);
                }
                if (dl.mbps <= 0) {
                    listener.onError("Download failed: no data could be downloaded from " + server.name + ". No fake result was generated. Check your connection and try again.");
                    return;
                }
            }
            final String downloadServerUsed = dl.serverUsed;

            // --- Upload: real POSTs to Cloudflare (only endpoint that accepts uploads) ---
            listener.onPhase("Measuring upload…", 60);
            SpeedServer upServer = server.uploadUrl != null ? server : SpeedServer.defaultServers().get(0);
            int ulSeconds = dataSaver ? 5 : 8;
            double upMbps = measureUpload(upServer, ulSeconds, listener, dataUsed);
            if (stopped) { listener.onError("Test stopped by user."); return; }
            if (upMbps <= 0) {
                listener.onError("Upload failed: no data could be uploaded to " + upServer.name + ". Download and ping were measured, but the full test is incomplete — no fake upload value was generated.");
                return;
            }

            // --- IP / ISP (real lookup; "Unavailable" on failure, never invented) ---
            listener.onPhase("Detecting IP and ISP…", 92);
            String[] ipIsp = NetworkInfoUtil.ipAndIsp();

            SpeedTestResult r = new SpeedTestResult();
            r.timestamp = System.currentTimeMillis();
            r.downloadMbps = dl.mbps; r.uploadMbps = upMbps;
            r.pingMs = ping; r.jitterMs = jitter; r.lossPercent = loss;
            r.server = downloadServerUsed + (upServer == server ? "" : " / upload via " + upServer.name);
            r.networkType = networkType; r.ip = ipIsp[0]; r.isp = ipIsp[1];
            r.method = "Download: HTTPS GET " + downloadServerUsed + " (parallel, " + dlSeconds + "s). Upload: HTTPS POST " + upServer.uploadUrl + " (" + ulSeconds + "s). Ping: 10 real HTTPS requests to " + server.pingUrl + ".";
            r.dataUsedBytes = dataUsed.get();
            r.durationMs = System.currentTimeMillis() - testStart;
            listener.onPhase("Done", 100);
            listener.onComplete(r);
        } catch (Exception e) {
            listener.onError("Test failed: " + e.getMessage() + ". No fake result was generated.");
        }
    }

    private String lastFallbackNote = "";
    private void serverAvailableNote(SpeedServer s) { lastFallbackNote = s.name; }

    /** One real ping: returns ms, or -1 on failure/timeout. */
    private double singlePing(String url) {
        HttpURLConnection c = null;
        try {
            long t0 = System.nanoTime();
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(3000); c.setReadTimeout(3000);
            c.setRequestProperty("Cache-Control", "no-cache");
            c.setRequestProperty("User-Agent", "SP-Speed-Test/1.0");
            int code = c.getResponseCode();
            if (code < 200 || code >= 400) return -1;
            InputStream in = c.getInputStream();
            byte[] buf = new byte[1024];
            while (in.read(buf) != -1) { /* drain small response */ }
            in.close();
            return (System.nanoTime() - t0) / 1_000_000.0;
        } catch (Exception e) { return -1; } finally { if (c != null) c.disconnect(); }
    }

    private static class DownloadOutcome { double mbps; String serverUsed; }

    private DownloadOutcome measureDownload(final SpeedServer server, int seconds, final Listener listener, final AtomicLong dataUsed) {
        final AtomicLong total = new AtomicLong(0);
        final long endAt = System.currentTimeMillis() + seconds * 1000L;
        final long startAt = System.currentTimeMillis();
        List<Thread> threads = new ArrayList<Thread>();
        for (int t = 0; t < 4; t++) {
            Thread th = new Thread(new Runnable() {
                public void run() {
                    byte[] buf = new byte[8192];
                    while (!stopped && System.currentTimeMillis() < endAt) {
                        HttpURLConnection c = null;
                        try {
                            String u = server.downloadUrl;
                            if (u.endsWith("=")) u = u + "5000000";
                            c = (HttpURLConnection) new URL(u).openConnection();
                            c.setConnectTimeout(5000); c.setReadTimeout(8000);
                            c.setRequestProperty("Cache-Control", "no-cache");
                            c.setRequestProperty("User-Agent", "SP-Speed-Test/1.0");
                            if (c.getResponseCode() != 200) break;
                            InputStream in = c.getInputStream();
                            int n;
                            while (!stopped && System.currentTimeMillis() < endAt && (n = in.read(buf)) != -1) {
                                total.addAndGet(n); dataUsed.addAndGet(n);
                            }
                            in.close();
                        } catch (Exception e) { break; } finally { if (c != null) c.disconnect(); }
                    }
                }
            });
            th.start(); threads.add(th);
        }
        // Live reporting from the measuring thread.
        while (!stopped) {
            boolean alive = false;
            for (Thread th : threads) if (th.isAlive()) alive = true;
            long elapsed = System.currentTimeMillis() - startAt;
            if (elapsed > 0) {
                double live = total.get() * 8.0 / (elapsed / 1000.0) / 1_000_000.0;
                listener.onLiveSpeed(live);
                if (elapsed > 1000) downloadSamples.add(live);
            }
            if (!alive) break;
            try { Thread.sleep(200); } catch (InterruptedException e) { break; }
        }
        for (Thread th : threads) { try { th.join(2000); } catch (InterruptedException e) { /* ignore */ } }
        DownloadOutcome o = new DownloadOutcome();
        long elapsedMs = System.currentTimeMillis() - startAt;
        // Ignore the first 500 ms warm-up in the rate, using bytes counted after it is not
        // separable per-thread, so use the full window — still purely measured bytes/time.
        o.mbps = elapsedMs > 0 ? total.get() * 8.0 / (elapsedMs / 1000.0) / 1_000_000.0 : 0;
        o.serverUsed = server.name + " (" + server.location + ")";
        return o;
    }

    private double measureUpload(final SpeedServer server, int seconds, final Listener listener, final AtomicLong dataUsed) {
        final AtomicLong total = new AtomicLong(0);
        final long endAt = System.currentTimeMillis() + seconds * 1000L;
        final long startAt = System.currentTimeMillis();
        // Deterministic repeating byte pattern as upload payload — the bytes are real
        // data sent over the network; the pattern itself is not a measurement.
        final byte[] chunk = new byte[1024 * 256];
        for (int i = 0; i < chunk.length; i++) chunk[i] = (byte) (0x5A + (i % 16));
        List<Thread> threads = new ArrayList<Thread>();
        for (int t = 0; t < 4; t++) {
            Thread th = new Thread(new Runnable() {
                public void run() {
                    while (!stopped && System.currentTimeMillis() < endAt) {
                        HttpURLConnection c = null;
                        try {
                            c = (HttpURLConnection) new URL(server.uploadUrl).openConnection();
                            c.setConnectTimeout(5000); c.setReadTimeout(8000);
                            c.setDoOutput(true); c.setRequestMethod("POST");
                            c.setFixedLengthStreamingMode(chunk.length * 4);
                            c.setRequestProperty("Content-Type", "application/octet-stream");
                            c.setRequestProperty("User-Agent", "SP-Speed-Test/1.0");
                            OutputStream out = c.getOutputStream();
                            for (int i = 0; i < 4; i++) out.write(chunk);
                            out.flush(); out.close();
                            int code = c.getResponseCode();
                            if (code >= 200 && code < 400) { total.addAndGet(chunk.length * 4L); dataUsed.addAndGet(chunk.length * 4L); }
                            else break;
                        } catch (Exception e) { break; } finally { if (c != null) c.disconnect(); }
                    }
                }
            });
            th.start(); threads.add(th);
        }
        while (!stopped) {
            boolean alive = false;
            for (Thread th : threads) if (th.isAlive()) alive = true;
            long elapsed = System.currentTimeMillis() - startAt;
            if (elapsed > 0) {
                double live = total.get() * 8.0 / (elapsed / 1000.0) / 1_000_000.0;
                listener.onLiveSpeed(live);
                if (elapsed > 1000) uploadSamples.add(live);
            }
            if (!alive) break;
            try { Thread.sleep(200); } catch (InterruptedException e) { break; }
        }
        for (Thread th : threads) { try { th.join(2000); } catch (InterruptedException e) { /* ignore */ } }
        long elapsedMs = System.currentTimeMillis() - startAt;
        return elapsedMs > 0 ? total.get() * 8.0 / (elapsedMs / 1000.0) / 1_000_000.0 : 0;
    }

    private static double mean(List<Double> v) {
        double s = 0; for (double d : v) s += d; return v.isEmpty() ? 0 : s / v.size();
    }
}
