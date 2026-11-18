package com.spsiam.spspeedtest;
import android.content.Context; import android.net.ConnectivityManager; import android.net.NetworkInfo;
import java.io.BufferedReader; import java.io.InputStreamReader; import java.net.HttpURLConnection; import java.net.URL;
public class NetworkInfoUtil {
    public static String networkType(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return "Unknown";
            NetworkInfo n = cm.getActiveNetworkInfo();
            if (n == null || !n.isConnected()) return "Offline";
            if (n.getType() == ConnectivityManager.TYPE_WIFI) return "Wi-Fi";
            if (n.getType() == ConnectivityManager.TYPE_MOBILE) return "Mobile Data (" + n.getSubtypeName() + ")";
            if (n.getType() == ConnectivityManager.TYPE_ETHERNET) return "Ethernet";
            return n.getTypeName();
        } catch (Exception e) { return "Unknown"; }
    }
    public static boolean isWifi(Context c) { return networkType(c).startsWith("Wi-Fi"); }
    public static String httpGet(String url, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(timeoutMs); conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("User-Agent", "SP-Speed-Test/1.0");
            conn.setRequestProperty("Cache-Control", "no-cache");
            if (conn.getResponseCode() != 200) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close(); return sb.toString();
        } catch (Exception e) { return null; } finally { if (conn != null) conn.disconnect(); }
    }
    // Returns {ip, isp} — both "Unavailable" on failure, never invented.
    public static String[] ipAndIsp() {
        String ip = "Unavailable", isp = "Unavailable";
        String j = httpGet("https://api.ipify.org?format=json", 5000);
        if (j != null) { int s = j.indexOf("\"ip\":\""); if (s >= 0) { int e = j.indexOf('"', s + 6); if (e > s) ip = j.substring(s + 6, e); } }
        if (!"Unavailable".equals(ip)) {
            String k = httpGet("https://ip-api.com/json/" + ip + "?fields=status,isp,org,country,city", 5000);
            if (k != null && k.contains("\"status\":\"success\"")) {
                isp = extract(k, "isp"); if ("Unavailable".equals(isp)) isp = extract(k, "org");
                String city = extract(k, "city"), country = extract(k, "country");
                if (!"Unavailable".equals(city) || !"Unavailable".equals(country)) isp = isp + " — " + city + ", " + country;
            }
        }
        return new String[]{ip, isp};
    }
    private static String extract(String json, String key) {
        int s = json.indexOf("\"" + key + "\":\""); if (s < 0) return "Unavailable";
        int e = json.indexOf('"', s + key.length() + 4); return e > s ? json.substring(s + key.length() + 4, e) : "Unavailable";
    }
}
