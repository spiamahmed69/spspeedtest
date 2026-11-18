package com.spsiam.spspeedtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private SettingsStore settings;
    private HistoryStore history;
    private SpeedTestEngine engine;
    private List<SpeedServer> servers;
    private SpeedTestResult currentResult;
    private boolean testing = false;

    private SpeedometerView speedometer;
    private GraphView downloadGraph, uploadGraph;
    private TextView liveSpeed, phaseLabel, downloadValue, uploadValue, pingValue, jitterValue, lossValue, networkInfo, ipInfo, detailReport;
    private ProgressBar progressBar;
    private Button startButton;
    private View panelDashboard, panelHistory, panelServers, panelSettings;
    private ListView historyList, serverList;
    private ArrayAdapter<String> historyAdapter, serverAdapter;
    private List<String> historyStrings = new ArrayList<String>();
    private List<String> serverStrings = new ArrayList<String>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsStore(this);
        setTheme(settings.darkTheme() ? R.style.AppTheme : R.style.LightTheme);
        setContentView(R.layout.activity_main);
        history = new HistoryStore(this);
        engine = new SpeedTestEngine();
        servers = SpeedServer.defaultServers();

        speedometer = findViewById(R.id.speedometer);
        downloadGraph = findViewById(R.id.downloadGraph);
        uploadGraph = findViewById(R.id.uploadGraph);
        liveSpeed = findViewById(R.id.liveSpeed);
        phaseLabel = findViewById(R.id.phaseLabel);
        downloadValue = findViewById(R.id.downloadValue);
        uploadValue = findViewById(R.id.uploadValue);
        pingValue = findViewById(R.id.pingValue);
        jitterValue = findViewById(R.id.jitterValue);
        lossValue = findViewById(R.id.lossValue);
        networkInfo = findViewById(R.id.networkInfo);
        ipInfo = findViewById(R.id.ipInfo);
        detailReport = findViewById(R.id.detailReport);
        progressBar = findViewById(R.id.progressBar);
        startButton = findViewById(R.id.startButton);
        panelDashboard = findViewById(R.id.panelDashboard);
        panelHistory = findViewById(R.id.panelHistory);
        panelServers = findViewById(R.id.panelServers);
        panelSettings = findViewById(R.id.panelSettings);
        historyList = findViewById(R.id.historyList);
        serverList = findViewById(R.id.serverList);

        networkInfo.setText("Network: " + NetworkInfoUtil.networkType(this));
        historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, historyStrings);
        historyList.setAdapter(historyAdapter);
        serverAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, serverStrings);
        serverList.setAdapter(serverAdapter);
        refreshHistory();
        refreshServerList();
        measureServerPings();

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (testing) { engine.stop(); return; }
                startTest();
            }
        });
        findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { shareResult(); }
        });
        findViewById(R.id.copyButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { copyResult(); }
        });
        findViewById(R.id.clearHistoryButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { history.clear(); refreshHistory(); Toast.makeText(MainActivity.this, "History cleared", Toast.LENGTH_SHORT).show(); }
        });
        findViewById(R.id.refreshServersButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { measureServerPings(); }
        });
        findViewById(R.id.navDashboard).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showPanel(0); } });
        findViewById(R.id.navHistory).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showPanel(1); refreshHistory(); } });
        findViewById(R.id.navServers).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showPanel(2); } });
        findViewById(R.id.navSettings).setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showPanel(3); } });

        serverList.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                settings.setServer(position);
                refreshServerList();
                Toast.makeText(MainActivity.this, "Server selected: " + servers.get(position).name, Toast.LENGTH_SHORT).show();
            }
        });

        CheckBox auto = findViewById(R.id.autoTestCheck);
        CheckBox wifi = findViewById(R.id.wifiOnlyCheck);
        CheckBox saver = findViewById(R.id.dataSaverCheck);
        CheckBox dark = findViewById(R.id.darkThemeCheck);
        auto.setChecked(settings.autoTest()); wifi.setChecked(settings.wifiOnly());
        saver.setChecked(settings.dataSaver()); dark.setChecked(settings.darkTheme());
        auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton b, boolean v) { settings.set("auto", v); } });
        wifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton b, boolean v) { settings.set("wifi_only", v); } });
        saver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton b, boolean v) { settings.set("data_saver", v); } });
        dark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) { settings.set("dark", v); recreate(); }
        });

        if (settings.autoTest() && (!settings.wifiOnly() || NetworkInfoUtil.isWifi(this))) {
            startTest();
        }
    }

    private void showPanel(int which) {
        panelDashboard.setVisibility(which == 0 ? View.VISIBLE : View.GONE);
        panelHistory.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
        panelServers.setVisibility(which == 2 ? View.VISIBLE : View.GONE);
        panelSettings.setVisibility(which == 3 ? View.VISIBLE : View.GONE);
    }

    private void startTest() {
        if (NetworkInfoUtil.networkType(this).equals("Offline")) {
            Toast.makeText(this, "No internet connection — test cannot run.", Toast.LENGTH_LONG).show();
            return;
        }
        testing = true;
        startButton.setText(R.string.stop);
        startButton.setBackgroundResource(R.drawable.btn_stop);
        downloadGraph.clear(); uploadGraph.clear();
        speedometer.setSpeed(0);
        final SpeedServer server = servers.get(Math.min(settings.serverIndex(), servers.size() - 1));
        final String net = NetworkInfoUtil.networkType(this);
        networkInfo.setText("Network: " + net + "  •  Server: " + server.name);
        new Thread(new Runnable() {
            public void run() {
                engine.runTest(server, settings.dataSaver(), net, new SpeedTestEngine.Listener() {
                    public void onPhase(final String phase, final int progress) {
                        runOnUiThread(new Runnable() { public void run() { phaseLabel.setText(phase); progressBar.setProgress(progress); } });
                    }
                    public void onLiveSpeed(final double mbps) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                liveSpeed.setText(String.format(Locale.US, "%.2f Mbps", mbps));
                                speedometer.setSpeed((float) mbps);
                            }
                        });
                    }
                    public void onComplete(final SpeedTestResult r) {
                        runOnUiThread(new Runnable() { public void run() { finishTest(r); } });
                    }
                    public void onError(final String message) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                testing = false;
                                startButton.setText(R.string.start);
                                startButton.setBackgroundResource(R.drawable.btn_primary);
                                phaseLabel.setText(message);
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void finishTest(SpeedTestResult r) {
        testing = false;
        currentResult = r;
        startButton.setText(R.string.start);
        startButton.setBackgroundResource(R.drawable.btn_primary);
        progressBar.setProgress(100);
        phaseLabel.setText("Test complete — real measurements, " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(r.timestamp)));
        liveSpeed.setText(String.format(Locale.US, "%.2f Mbps", r.downloadMbps));
        speedometer.setSpeed((float) r.downloadMbps);
        downloadValue.setText(String.format(Locale.US, "%.2f Mbps", r.downloadMbps));
        uploadValue.setText(String.format(Locale.US, "%.2f Mbps", r.uploadMbps));
        pingValue.setText(String.format(Locale.US, "%.1f ms", r.pingMs));
        jitterValue.setText(String.format(Locale.US, "%.1f ms", r.jitterMs));
        lossValue.setText(String.format(Locale.US, "%.1f%%", r.lossPercent));
        ipInfo.setText("IP: " + r.ip + "  •  ISP: " + r.isp + "  •  Server: " + r.server);
        detailReport.setText(r.toReportText());
        downloadGraph.setSamples(engine.getDownloadSamples());
        uploadGraph.setSamples(engine.getUploadSamples());
        history.save(r);
        refreshHistory();
    }

    private void refreshHistory() {
        historyStrings.clear();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        for (SpeedTestResult r : history.all()) {
            historyStrings.add(f.format(new Date(r.timestamp)) + " — ↓ " + String.format(Locale.US, "%.1f", r.downloadMbps)
                    + " Mbps  ↑ " + String.format(Locale.US, "%.1f", r.uploadMbps) + " Mbps  •  " + String.format(Locale.US, "%.0f", r.pingMs) + " ms  •  " + r.server);
        }
        if (historyStrings.isEmpty()) historyStrings.add("No tests yet — run your first real speed test.");
        historyAdapter.notifyDataSetChanged();
    }

    private void refreshServerList() {
        serverStrings.clear();
        for (int i = 0; i < servers.size(); i++) {
            SpeedServer s = servers.get(i);
            serverStrings.add((i == settings.serverIndex() ? "✓ " : "") + s.toString());
        }
        serverAdapter.notifyDataSetChanged();
    }

    /** Real ping (one small HTTPS request per server) to rank servers by latency. */
    private void measureServerPings() {
        new Thread(new Runnable() {
            public void run() {
                for (final SpeedServer s : servers) {
                    long t0 = System.currentTimeMillis();
                    String ok = NetworkInfoUtil.httpGet(s.pingUrl, 4000);
                    s.latencyMs = ok != null ? (System.currentTimeMillis() - t0) : -1;
                    s.available = ok != null;
                }
                // Rank by measured latency, unavailable last.
                java.util.Collections.sort(servers, new java.util.Comparator<SpeedServer>() {
                    public int compare(SpeedServer a, SpeedServer b) {
                        if (a.latencyMs < 0 && b.latencyMs < 0) return 0;
                        if (a.latencyMs < 0) return 1;
                        if (b.latencyMs < 0) return -1;
                        return Double.compare(a.latencyMs, b.latencyMs);
                    }
                });
                runOnUiThread(new Runnable() { public void run() { refreshServerList(); } });
            }
        }).start();
    }

    private void shareResult() {
        if (currentResult == null) { Toast.makeText(this, "Run a test first.", Toast.LENGTH_SHORT).show(); return; }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, currentResult.toReportText());
        startActivity(Intent.createChooser(i, "Share speed test result"));
    }

    private void copyResult() {
        if (currentResult == null) { Toast.makeText(this, "Run a test first.", Toast.LENGTH_SHORT).show(); return; }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("SP Speed Test", currentResult.toReportText()));
        Toast.makeText(this, "Result copied", Toast.LENGTH_SHORT).show();
    }
}
