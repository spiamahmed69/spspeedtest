package com.spsiam.spspeedtest;
public class SpeedTestResult {
    public long timestamp; public double downloadMbps; public double uploadMbps;
    public double pingMs; public double jitterMs; public double lossPercent;
    public String server; public String networkType; public String ip; public String isp;
    public String method; public long dataUsedBytes; public long durationMs;
    public String toReportText() {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return "SP Speed Test — " + f.format(new java.util.Date(timestamp)) + "\n"
            + "Download: " + String.format(java.util.Locale.US, "%.2f Mbps", downloadMbps) + "\n"
            + "Upload: " + String.format(java.util.Locale.US, "%.2f Mbps", uploadMbps) + "\n"
            + "Ping: " + String.format(java.util.Locale.US, "%.1f ms", pingMs) + "\n"
            + "Jitter: " + String.format(java.util.Locale.US, "%.1f ms", jitterMs) + "\n"
            + "Packet loss: " + String.format(java.util.Locale.US, "%.1f%%", lossPercent) + "\n"
            + "Server: " + server + "\nNetwork: " + networkType + "\nIP: " + ip + "\nISP: " + isp + "\n"
            + "Method: " + method + "\nData used: " + String.format(java.util.Locale.US, "%.1f MB", dataUsedBytes/1048576.0)
            + "\nDuration: " + (durationMs/1000) + " s";
    }
}
