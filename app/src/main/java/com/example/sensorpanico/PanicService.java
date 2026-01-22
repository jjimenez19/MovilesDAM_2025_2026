package com.example.sensorpanico;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PanicService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "PanicSensorChannel";
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private MediaPlayer mediaPlayer;
    private boolean enPanico = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        crearNotificacion();
        return START_STICKY;
    }

    private void crearNotificacion() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Servicio de Pánico",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sensor de Pánico Activo")
                .setContentText("La aplicación está vigilando en segundo plano")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double magnitud = Math.sqrt(x * x + y * y + z * z);

            if ((magnitud > 25 || magnitud < 2) && !enPanico) {
                activarPanico();
            }
        }
    }

    private void activarPanico() {
        enPanico = true;
        reproducirAudio();
        vibrar();

        // Enviamos un broadcast para que la Activity se entere si está abierta
        Intent intent = new Intent("com.example.sensorpanico.PANIC_EVENT");
        sendBroadcast(intent);

        // Reiniciar estado después de 4 segundos
        new android.os.Handler().postDelayed(() -> {
            enPanico = false;
            detenerAudio();
        }, 4000);
    }

    private void reproducirAudio() {
        detenerAudio();
        mediaPlayer = MediaPlayer.create(this, R.raw.panico1);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    private void detenerAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void vibrar() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            long[] patron = {0, 500, 100, 500, 100, 500, 100, 1000};
            v.vibrate(VibrationEffect.createWaveform(patron, -1));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        detenerAudio();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
