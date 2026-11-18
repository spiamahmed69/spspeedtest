package com.spsiam.spspeedtest;
import android.content.Context; import android.graphics.Canvas; import android.graphics.Color; import android.graphics.Paint; import android.graphics.RectF; import android.util.AttributeSet; import android.view.View;
public class SpeedometerView extends View {
    private float speed = 0; private float shown = 0;
    private final Paint arcBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcFg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    public SpeedometerView(Context c, AttributeSet a) { super(c, a);
        arcBg.setStyle(Paint.Style.STROKE); arcBg.setStrokeWidth(26); arcBg.setColor(Color.parseColor("#24365E")); arcBg.setStrokeCap(Paint.Cap.ROUND);
        arcFg.setStyle(Paint.Style.STROKE); arcFg.setStrokeWidth(26); arcFg.setColor(Color.parseColor("#38BDF8")); arcFg.setStrokeCap(Paint.Cap.ROUND);
        text.setColor(Color.WHITE); text.setTextSize(42); text.setTextAlign(Paint.Align.CENTER);
    }
    public void setSpeed(float s) { speed = s; invalidate(); }
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        shown += (speed - shown) * 0.25f;
        float cx = getWidth() / 2f, cy = getHeight() * 0.62f, r = Math.min(getWidth(), getHeight()) * 0.36f;
        RectF rect = new RectF(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(rect, 150, 240, false, arcBg);
        // Log scale 0..1000 Mbps so both slow and fast links read well.
        float frac = (float) (Math.log10(1 + shown) / Math.log10(1001));
        if (frac > 0) canvas.drawArc(rect, 150, 240 * frac, false, arcFg);
        canvas.drawText(String.format(java.util.Locale.US, "%.1f", shown), cx, cy + 14, text);
        if (Math.abs(speed - shown) > 0.5f) postInvalidateDelayed(40);
    }
}
