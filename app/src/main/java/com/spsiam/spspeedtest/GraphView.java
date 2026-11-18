package com.spsiam.spspeedtest;
import android.content.Context; import android.graphics.Canvas; import android.graphics.Color; import android.graphics.Paint; import android.graphics.Path; import android.util.AttributeSet; import android.view.View;
import java.util.ArrayList; import java.util.List;
public class GraphView extends View {
    private final List<Double> samples = new ArrayList<Double>();
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    public GraphView(Context c, AttributeSet a) { super(c, a);
        line.setColor(Color.parseColor("#38BDF8")); line.setStrokeWidth(4); line.setStyle(Paint.Style.STROKE);
        fill.setColor(Color.parseColor("#332563EB")); fill.setStyle(Paint.Style.FILL);
    }
    public synchronized void setSamples(List<Double> s) { samples.clear(); samples.addAll(s); invalidate(); }
    public synchronized void clear() { samples.clear(); invalidate(); }
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (samples.size() < 2) return;
        double max = 1; for (double v : samples) if (v > max) max = v;
        Path p = new Path(); float w = getWidth(), h = getHeight();
        for (int i = 0; i < samples.size(); i++) {
            float x = w * i / (samples.size() - 1);
            float y = (float) (h - 8 - (samples.get(i) / max) * (h - 16));
            if (i == 0) { p.moveTo(x, y); } else p.lineTo(x, y);
        }
        canvas.drawPath(p, line);
    }
}
