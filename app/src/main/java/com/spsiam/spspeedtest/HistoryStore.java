package com.spsiam.spspeedtest;
import android.content.ContentValues; import android.content.Context; import android.database.Cursor; import android.database.sqlite.SQLiteDatabase; import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList; import java.util.List;
public class HistoryStore extends SQLiteOpenHelper {
    public HistoryStore(Context c) { super(c, "speed_history.db", null, 1); }
    public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE h(id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, dl REAL, ul REAL, ping REAL, jitter REAL, loss REAL, server TEXT, net TEXT, ip TEXT, isp TEXT, method TEXT, data INTEGER, dur INTEGER)"); }
    public void onUpgrade(SQLiteDatabase db, int a, int b) { db.execSQL("DROP TABLE IF EXISTS h"); onCreate(db); }
    public void save(SpeedTestResult r) {
        ContentValues v = new ContentValues();
        v.put("ts", r.timestamp); v.put("dl", r.downloadMbps); v.put("ul", r.uploadMbps);
        v.put("ping", r.pingMs); v.put("jitter", r.jitterMs); v.put("loss", r.lossPercent);
        v.put("server", r.server); v.put("net", r.networkType); v.put("ip", r.ip); v.put("isp", r.isp);
        v.put("method", r.method); v.put("data", r.dataUsedBytes); v.put("dur", r.durationMs);
        getWritableDatabase().insert("h", null, v);
    }
    public List<SpeedTestResult> all() {
        List<SpeedTestResult> out = new ArrayList<SpeedTestResult>();
        Cursor c = getReadableDatabase().query("h", null, null, null, null, null, "ts DESC");
        while (c.moveToNext()) {
            SpeedTestResult r = new SpeedTestResult();
            r.timestamp = c.getLong(1); r.downloadMbps = c.getDouble(2); r.uploadMbps = c.getDouble(3);
            r.pingMs = c.getDouble(4); r.jitterMs = c.getDouble(5); r.lossPercent = c.getDouble(6);
            r.server = c.getString(7); r.networkType = c.getString(8); r.ip = c.getString(9); r.isp = c.getString(10);
            r.method = c.getString(11); r.dataUsedBytes = c.getLong(12); r.durationMs = c.getLong(13);
            out.add(r);
        }
        c.close(); return out;
    }
    public void clear() { getWritableDatabase().delete("h", null, null); }
}
