package com.example.regresoseguro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class CompassView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float azimutDeg = 0f;
    private float objetivoDeg = 0f;
    private boolean hayObjetivo = false;

    private float distanciaBaseM = Float.NaN;

    private static final float DIST_PUNTO_ENTRAR_M = 6f;
    private static final float DIST_PUNTO_SALIR_M = 10f;

    private boolean modoPunto = false;

    public CompassView(Context context) {
        super(context);
        init();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void setAzimut(float azimut) {
        azimutDeg = normalizar(azimut);
        invalidate();
    }

    public void setObjetivo(float bearingAbsoluto) {
        objetivoDeg = normalizar(bearingAbsoluto);
        hayObjetivo = true;
        invalidate();
    }

    public void setDistanciaBase(float metros) {
        distanciaBaseM = metros;

        if (!Float.isNaN(distanciaBaseM)) {
            if (modoPunto) {
                if (distanciaBaseM > DIST_PUNTO_SALIR_M) {
                    modoPunto = false;
                }
            } else {
                if (distanciaBaseM < DIST_PUNTO_ENTRAR_M) {
                    modoPunto = true;
                }
            }
        }

        invalidate();
    }

    public void quitarObjetivo() {
        hayObjetivo = false;
        distanciaBaseM = Float.NaN;
        modoPunto = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        float radio = Math.min(w, h) * 0.35f;
        float radioExterior = radio * 1.18f;

        canvas.save();
        canvas.rotate(-azimutDeg, cx, cy);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
        paint.setColor(0xFF000000);
        canvas.drawCircle(cx, cy, radio, paint);

        paint.setStrokeWidth(4f);
        canvas.drawCircle(cx, cy, radio * 0.72f, paint);
        canvas.drawCircle(cx, cy, radio * 0.45f, paint);

        paint.setStrokeWidth(3f);
        for (int i = 0; i < 360; i += 10) {
            float r1 = (i % 90 == 0) ? (radio * 0.86f) : (radio * 0.90f);
            float r2 = radio * 0.98f;
            float ang = (float) Math.toRadians(i - 90f);

            float x1 = cx + (float) Math.cos(ang) * r1;
            float y1 = cy + (float) Math.sin(ang) * r1;

            float x2 = cx + (float) Math.cos(ang) * r2;
            float y2 = cy + (float) Math.sin(ang) * r2;

            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(radio * 0.18f);

        float textoR = radio * 0.62f;
        dibujarCardinal(canvas, "N", cx, cy, textoR, 0f, 0xFFD32F2F);
        dibujarCardinal(canvas, "E", cx, cy, textoR, 90f, 0xFF000000);
        dibujarCardinal(canvas, "S", cx, cy, textoR, 180f, 0xFF000000);
        dibujarCardinal(canvas, "O", cx, cy, textoR, 270f, 0xFF000000);

        dibujarFlecha(canvas, cx, cy, radioExterior * 0.62f, radioExterior * 0.98f, 0f, 0xFFD32F2F);

        canvas.restore();

        if (!hayObjetivo) {
            paint.setColor(0xFF000000);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10f);
            canvas.drawLine(cx - radio * 0.35f, cy, cx + radio * 0.35f, cy, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(radio * 0.16f);
            paint.setColor(0xFF000000);
            drawTextoCentrado(canvas, "Guarda un punto base", cx, cy + radio + (radio * 0.32f));
            return;
        }

        float relObjetivo = normalizar(objetivoDeg - azimutDeg);

        if (modoPunto) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF000000);
            canvas.drawCircle(cx, cy, radio * 0.09f, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(radio * 0.16f);
            paint.setColor(0xFF000000);
            drawTextoCentrado(canvas, "Estás cerca del punto base", cx, cy + radio + (radio * 0.32f));
        } else {
            dibujarFlecha(canvas, cx, cy, radio * 0.55f, radio * 0.98f, relObjetivo, 0xFF000000);

            paint.setColor(0xFF000000);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(radio * 0.16f);

            if (!Float.isNaN(distanciaBaseM)) {
                drawTextoCentrado(canvas, formDist(distanciaBaseM), cx, cy + radio + (radio * 0.32f));
            } else {
                drawTextoCentrado(canvas, "Volviendo al punto base", cx, cy + radio + (radio * 0.32f));
            }
        }
    }

    private void dibujarCardinal(Canvas canvas, String t, float cx, float cy, float r, float angDeg, int color) {
        float ang = (float) Math.toRadians(angDeg - 90f);
        float x = cx + (float) Math.cos(ang) * r;
        float y = cy + (float) Math.sin(ang) * r;

        paint.setColor(color);
        float ancho = paint.measureText(t);
        canvas.drawText(t, x - (ancho / 2f), y + (paint.getTextSize() / 3f), paint);
    }

    private void dibujarFlecha(Canvas canvas, float cx, float cy, float rBase, float rPunta, float relDeg, int color) {
        float ang = (float) Math.toRadians(relDeg - 90f);

        float xPunta = cx + (float) Math.cos(ang) * rPunta;
        float yPunta = cy + (float) Math.sin(ang) * rPunta;

        float xBase = cx + (float) Math.cos(ang) * rBase;
        float yBase = cy + (float) Math.sin(ang) * rBase;

        float angIzq = (float) Math.toRadians(relDeg - 90f - 22f);
        float angDer = (float) Math.toRadians(relDeg - 90f + 22f);

        float xIzq = cx + (float) Math.cos(angIzq) * (rBase + (rPunta - rBase) * 0.25f);
        float yIzq = cy + (float) Math.sin(angIzq) * (rBase + (rPunta - rBase) * 0.25f);

        float xDer = cx + (float) Math.cos(angDer) * (rBase + (rPunta - rBase) * 0.25f);
        float yDer = cy + (float) Math.sin(angDer) * (rBase + (rPunta - rBase) * 0.25f);

        Path flecha = new Path();
        flecha.moveTo(xPunta, yPunta);
        flecha.lineTo(xIzq, yIzq);
        flecha.lineTo(xBase, yBase);
        flecha.lineTo(xDer, yDer);
        flecha.close();

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(flecha, paint);
    }

    private void drawTextoCentrado(Canvas canvas, String text, float x, float y) {
        float ancho = paint.measureText(text);
        canvas.drawText(text, x - (ancho / 2f), y, paint);
    }

    private float normalizar(float deg) {
        float r = deg % 360f;
        if (r < 0f) {
            r += 360f;
        }
        return r;
    }

    private String formDist(float metros) {
        if (metros >= 1000f) {
            return String.format(java.util.Locale.US, "%.2f km", (metros / 1000f));
        }
        return String.format(java.util.Locale.US, "%.0f m", metros);
    }
}
