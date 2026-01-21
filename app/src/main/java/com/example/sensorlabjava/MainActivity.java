package com.example.sensorlabjava;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;


public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private SensorManager sensorManager;

    private Sensor acelerometro;
    private Sensor sensorLuz;
    private Sensor sensorProximidad;

    private TextView txtAcelerometro;
    private TextView txtLuz;
    private TextView txtProximidad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        txtAcelerometro = findViewById(R.id.txtAcelerometro);
        txtLuz = findViewById(R.id.txtLuz);
        txtProximidad = findViewById(R.id.txtProximidad);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorProximidad = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        
        if (acelerometro == null) txtAcelerometro.setText("No hay acelerómetro.");
        if (sensorLuz == null) txtLuz.setText("No hay sensor de luz.");
        if (sensorProximidad == null) txtProximidad.setText("No hay sensor de proximidad.");
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (acelerometro != null) {
            sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
        }
        if (sensorLuz != null) {
            sensorManager.registerListener(this, sensorLuz, SensorManager.SENSOR_DELAY_UI);
        }
        if (sensorProximidad != null) {
            sensorManager.registerListener(this, sensorProximidad, SensorManager.SENSOR_DELAY_UI);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        int tipoSensor = event.sensor.getType();


        if (tipoSensor == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            String texto = String.format(Locale.getDefault(), "X: %.2f\nY: %.2f\nZ: %.2f", x, y, z);
            txtAcelerometro.setText(texto);
        }


        else if (tipoSensor == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            txtLuz.setText("Luminosidad: " + lux + " lux");
        }


        else if (tipoSensor == Sensor.TYPE_PROXIMITY) {
            float distancia = event.values[0];

            float rangoMax = event.sensor.getMaximumRange();

            if (distancia < rangoMax) {
                txtProximidad.setText("¡OBJETO DETECTADO! (" + distancia + " cm)");
            } else {
                txtProximidad.setText("Despejado (" + distancia + " cm)");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}