package ir.tradeplus.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/** Draws the 7-day trend line chart (gold line + gradient fill) as a Bitmap for RemoteViews. */
public class ChartRenderer {

    public static Bitmap render(long[] values, int widthPx, int heightPx) {
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        if (values == null || values.length == 0) {
            return bitmap;
        }
        if (values.length == 1) {
            values = new long[]{values[0], values[0]};
        }

        long min = values[0], max = values[0];
        for (long v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (min == max) {
            min = min - 1;
            max = max + 1;
        }
        // padding so the line doesn't touch top/bottom
        long range = max - min;
        min -= range * 0.12;
        max += range * 0.12;
        range = max - min;

        int padTop = 8;
        int padBottom = 8;
        int padSide = 8;
        float usableW = widthPx - padSide * 2f;
        float usableH = heightPx - padTop - padBottom;

        int n = values.length;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padSide + (n == 1 ? 0 : (usableW * i / (n - 1)));
            float t = (values[i] - min) / (float) range;
            ys[i] = padTop + usableH * (1f - t);
        }

        // smooth path through points
        Path linePath = new Path();
        linePath.moveTo(xs[0], ys[0]);
        for (int i = 0; i < n - 1; i++) {
            float midX = (xs[i] + xs[i + 1]) / 2f;
            linePath.cubicTo(midX, ys[i], midX, ys[i + 1], xs[i + 1], ys[i + 1]);
        }

        // fill under the line
        Path fillPath = new Path(linePath);
        fillPath.lineTo(xs[n - 1], heightPx);
        fillPath.lineTo(xs[0], heightPx);
        fillPath.close();

        int gold = Color.parseColor("#F3B23A");
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setShader(new LinearGradient(0, 0, 0, heightPx,
                Color.argb(120, 243, 178, 58), Color.argb(0, 243, 178, 58),
                Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4.5f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(gold);
        canvas.drawPath(linePath, linePaint);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(gold);
        for (int i = 0; i < n - 1; i++) {
            canvas.drawCircle(xs[i], ys[i], 3f, dotPaint);
        }

        // highlight last point
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(Color.WHITE);
        canvas.drawCircle(xs[n - 1], ys[n - 1], 7f, ringPaint);
        Paint lastDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        lastDot.setColor(gold);
        canvas.drawCircle(xs[n - 1], ys[n - 1], 5f, lastDot);

        return bitmap;
    }
}
