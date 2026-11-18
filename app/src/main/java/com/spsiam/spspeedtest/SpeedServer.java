package com.spsiam.spspeedtest;
public class SpeedServer {
    public final String name; public final String location;
    public final String pingUrl; public final String downloadUrl; public final String uploadUrl;
    public double latencyMs = -1; public boolean available = false;
    public SpeedServer(String n, String loc, String ping, String dl, String up) {
        name=n; location=loc; pingUrl=ping; downloadUrl=dl; uploadUrl=up;
    }
    public static java.util.List<SpeedServer> defaultServers() {
        java.util.List<SpeedServer> l = new java.util.ArrayList<SpeedServer>();
        l.add(new SpeedServer("Cloudflare", "Global (nearest edge)", "https://speed.cloudflare.com/cdn-cgi/trace", "https://speed.cloudflare.com/__down?bytes=", "https://speed.cloudflare.com/__up"));
        l.add(new SpeedServer("OVH", "Paris, France", "https://proof.ovh.net/", "https://proof.ovh.net/files/10Mb.dat", null));
        l.add(new SpeedServer("Hetzner", "Germany", "https://speed.hetzner.de/", "https://speed.hetzner.de/100MB.bin", null));
        return l;
    }
    public String toString() { return name + " — " + location + (latencyMs>=0 ? " — " + (long)latencyMs + " ms" : " — not measured") + (available ? "" : " — unavailable"); }
}
