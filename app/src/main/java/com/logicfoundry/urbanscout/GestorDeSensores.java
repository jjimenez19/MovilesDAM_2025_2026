package com.logicfoundry.urbanscout;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GestorDeSensores implements SensorEventListener {

    // Interfaz para comunicarse con MainActivity
    public interface SensorListenerCallback {
        void onLuzCambiada(float lux, String mensaje);
        void onMovimientoDetectado(boolean esBrusco, float fuerzaG);
    }

    private SensorManager sensorManager;
    private Sensor sensorLuz;
    private Sensor sensorAcelerometro;
    private SensorListenerCallback callback;
    private Context context;

    // Constructor
    public GestorDeSensores(Context context, SensorListenerCallback callback) {
        this.context = context;
        this.callback = callback;
        inicializarSensores();
    }

    private void inicializarSensores() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            sensorAcelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    public void iniciarEscucha() {
        if (sensorManager != null) {
            if (sensorLuz != null) {
                sensorManager.registerListener(this, sensorLuz, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (sensorAcelerometro != null) {
                sensorManager.registerListener(this, sensorAcelerometro, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    public void detenerEscucha() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // ---LOGICA DEL LISTENER---

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 1. Logica del Sensor de Luz
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            String mensaje = (lux < 100) ? "Muy oscuro" : "Iluminación adecuada";

            if (callback != null) callback.onLuzCambiada(lux, mensaje);
        }

        // 2. Logica del Acelerometro
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double aceleracionTotal = Math.sqrt(x*x + y*y + z*z);
            double fuerzaG = aceleracionTotal / SensorManager.GRAVITY_EARTH;

            boolean esMovimientoBrusco = fuerzaG > 2.5;

            if (callback != null) {
                callback.onMovimientoDetectado(esMovimientoBrusco, (float) fuerzaG);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}