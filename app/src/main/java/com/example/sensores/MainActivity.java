package com.example.sensores;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private PreviewView viewFinder;
    private ImageView chargerEnemigo;
    private TextView contadorPuntos;
    private int puntuacion = 0;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private long ultimoTiempo = 0;

    // Variables para la lógica del juego
    private float posicionEnemigo = 0f; // Posición angular del enemigo
    private Random random = new Random();

    // Variables para el cálculo de rotación
    private float anguloActualHorizontal = 0f;
    private static final float SENSIBILIDAD = 40.0f; // Ajusta esto para que se mueva más o menos rápido

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder = findViewById(R.id.viewFinder);
        chargerEnemigo = findViewById(R.id.charger_enemigo);
        contadorPuntos = findViewById(R.id.contador_puntos);

        chargerEnemigo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                puntuacion++;
                contadorPuntos.setText(String.valueOf(puntuacion));

                // Hacemos que el enemigo "desaparezca"
                chargerEnemigo.setVisibility(View.GONE);

                // Calculamos una nueva posición aleatoria en el círculo (-180 a 180 grados)
                posicionEnemigo = random.nextFloat() * 360f - 180f;

                // Usamos un Handler para que "reaparezca" después de un corto tiempo
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        chargerEnemigo.setVisibility(View.VISIBLE);
                    }
                }, 500); // 500ms de retraso
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // VERIFICAR PERMISOS ANTES DE ARRANCAR
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);
            } catch (Exception e) {
                Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            long tiempoActual = System.currentTimeMillis();

            if (ultimoTiempo != 0) {
                // 1. Calculamos cuánto tiempo pasó desde el último dato (en segundos)
                float dt = (tiempoActual - ultimoTiempo) / 1000.0f;

                // 2. event.values[1] es el giro horizontal.
                // CAMBIO: Usamos "+" para invertir el sentido si antes iba al revés.
                float velocidadY = event.values[1];

                // 3. Convertimos velocidad a grados y sumamos al ángulo actual
                // Fórmula: ángulo = velocidad * tiempo
                anguloActualHorizontal += Math.toDegrees(velocidadY) * dt;

                // 4. Normalizamos a 360 grados (Efecto bucle)
                if (anguloActualHorizontal > 180) anguloActualHorizontal -= 360;
                if (anguloActualHorizontal < -180) anguloActualHorizontal += 360;

                // 5. Dibujamos en pantalla
                // La posición del enemigo en la pantalla ahora depende de dónde está el jugador
                // mirando (anguloActualHorizontal) y dónde está el enemigo (posicionEnemigo).
                float anguloRelativo = anguloActualHorizontal - posicionEnemigo;

                // Normalizamos el ángulo relativo para que el giro sea siempre por el camino más corto
                if (anguloRelativo > 180) anguloRelativo -= 360;
                if (anguloRelativo < -180) anguloRelativo += 360;

                float factorVisual = 25.0f;
                chargerEnemigo.setTranslationX(anguloRelativo * factorVisual);
            }
            ultimoTiempo = tiempoActual;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // Manejar la respuesta del usuario al permiso
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Necesitas la cámara para jugar", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}