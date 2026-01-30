package com.example.brujulaapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class Brujula extends View {

    private float azimuth = 0f;
    private float currentAzimuth = 0f; // Para suavizar el giro

    private Bitmap barcoBitmap;
    private Bitmap backgroundBitmap;

    private Paint paint;

    public Brujula(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    // --- Métodos públicos para que MainActivity los use ---
    public void setBarcoBitmap(Bitmap bitmap) {
        this.barcoBitmap = bitmap;
        invalidate();
    }

    public void setBackgroundBitmap(Bitmap bitmap) {
        this.backgroundBitmap = bitmap;
        invalidate();
    }

    public void updateAzimuth(float azimuth) {
        this.azimuth = azimuth;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;
        int radius = Math.min(w, h) / 3;

        // --- Suavizado del giro ---
        float diff = azimuth - currentAzimuth;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        currentAzimuth += diff * 0.1f;

        // --- Fondo girando ---
        if (backgroundBitmap != null) {
            float scaleBg = (float) (radius * 2) / backgroundBitmap.getWidth();
            canvas.save();
            canvas.rotate(-currentAzimuth, cx, cy);
            canvas.scale(scaleBg, scaleBg, cx, cy);
            canvas.drawBitmap(
                    backgroundBitmap,
                    cx - backgroundBitmap.getWidth() / 2f,
                    cy - backgroundBitmap.getHeight() / 2f,
                    paint
            );
            canvas.restore();
        }

        // --- Aguja/barco fija ---
        if (barcoBitmap != null) {
            float scaleBoat = (float) radius / barcoBitmap.getHeight();
            canvas.save();
            canvas.scale(scaleBoat, scaleBoat, cx, cy);
            canvas.drawBitmap(
                    barcoBitmap,
                    cx - barcoBitmap.getWidth() / 2f,
                    cy - barcoBitmap.getHeight() / 2f,
                    paint
            );
            canvas.restore();
        }

        postInvalidateOnAnimation();
    }
}
