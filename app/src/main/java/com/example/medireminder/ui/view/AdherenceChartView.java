package com.example.medireminder.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** A very small custom bar chart: taken vs missed for the last 7 days. */
public class AdherenceChartView extends View {

    private static final String[] DAYS = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] taken = new int[7];
    private int[] missed = new int[7];

    public AdherenceChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(int[] taken, int[] missed) {
        this.taken = taken;
        this.missed = missed;
        invalidate(); // ask Android to draw again
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight() - 24; // leave room for the day labels
        if (width <= 0 || height <= 0) {
            return;
        }

        int max = 1;
        for (int i = 0; i < 7; i++) {
            max = Math.max(max, taken[i] + missed[i]);
        }

        float groupWidth = width / 7f;
        float barWidth = groupWidth / 3f;
        for (int i = 0; i < 7; i++) {
            float x = i * groupWidth + (groupWidth - 2 * barWidth) / 2f;
            float takenHeight = height * taken[i] / (float) max;
            float missedHeight = height * missed[i] / (float) max;

            paint.setColor(Color.parseColor("#2E7D32"));
            canvas.drawRect(x, height - takenHeight, x + barWidth, height, paint);

            paint.setColor(Color.parseColor("#C62828"));
            canvas.drawRect(x + barWidth, height - missedHeight,
                    x + 2 * barWidth, height, paint);

            paint.setColor(Color.parseColor("#555555"));
            paint.setTextSize(20);
            canvas.drawText(DAYS[i], i * groupWidth + groupWidth / 2f - 12,
                    height + 20, paint);
        }
    }
}
