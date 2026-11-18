package com.spsiam.spspeedtest;
import android.content.Context; import android.content.SharedPreferences;
public class SettingsStore {
    private final SharedPreferences p;
    public SettingsStore(Context c) { p = c.getSharedPreferences("sp_speed_test", Context.MODE_PRIVATE); }
    public boolean autoTest() { return p.getBoolean("auto", false); }
    public boolean wifiOnly() { return p.getBoolean("wifi_only", true); }
    public boolean dataSaver() { return p.getBoolean("data_saver", false); }
    public boolean darkTheme() { return p.getBoolean("dark", true); }
    public int serverIndex() { return p.getInt("server", 0); }
    public void set(String k, boolean v) { p.edit().putBoolean(k, v).apply(); }
    public void setServer(int i) { p.edit().putInt("server", i).apply(); }
}
