package com.example.tlfn;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, proximity;
    private TextView tvFocusStatus, tvPostureWarning;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFocusStatus = findViewById(R.id.tvFocusStatus);
        tvPostureWarning = findViewById(R.id.tvPostureWarning);

        // Inicializar Sensores
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectPostureAndOrientation(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            detectProximity(event.values[0]);
        }
    }

    private void detectPostureAndOrientation(float[] values) {
        float x = values[0];
        float y = values[1];
        float z = values[2];

        // 1. Lógica de Enfoque: Boca abajo (Face Down)
        // Cuando el teléfono está boca abajo, el eje Z es cercano a -9.8
        if (z < -8.0) {
            tvFocusStatus.setText("MODO ENFOQUE ACTIVO 🧘");
            tvFocusStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else if (z > 8.0) {
            tvFocusStatus.setText("Teléfono boca arriba - Inactivo");
            tvFocusStatus.setTextColor(getResources().getColor(android.R.color.black));
        }

        // 2. Lógica de Postura: "Text-Neck"
        // Si el usuario sostiene el móvil frente a él (Y alto), medimos la inclinación
        // Un valor de Y alto y Z bajo indica que el móvil está vertical (viendo pantalla)
        if (y > 7.0 && z < 4.0) {
            tvPostureWarning.setText("¡CUIDADO! Inclina menos el cuello");
            tvPostureWarning.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            // Opcional: vibrar si la mala postura persiste
        } else {
            tvPostureWarning.setText("Postura: Correcta");
            tvPostureWarning.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    private void detectProximity(float distance) {
        // Si la distancia es 0, algo está cubriendo el sensor (la mesa o el bolsillo)
        if (distance == 0) {
            // Combinado con la aceleración boca abajo, confirma que está en la mesa
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar los sensores al abrir la app
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Detener sensores para ahorrar batería al salir
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No es necesario para este ejemplo
    }
}