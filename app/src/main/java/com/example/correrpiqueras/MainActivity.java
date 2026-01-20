package com.example.correrpiqueras;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView backgroundImageView;
    private TextView peakSpeedTextView;
    private TextView currentSpeedTextView;
    private TextView animalNameTextView;
    private Button startStopButton;

    private View previewMenu;
    private Button debugPrevButton, debugNextButton;
    private TextView debugAnimalNameTextView, debugAnimalSpeedTextView;
    private List<Map.Entry<Float, String>> animalList;
    private int currentPreviewIndex = 0;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    private boolean isTracking = false;
    private float maxSpeed = 0.0f;

    private long lastTimestamp = 0;
    private long trackingStartTime = 0;
    private final float[] velocity = new float[3];
    private final float[] gravity = new float[3];
    private static final float NOISE_THRESHOLD = 0.5f;

    private static final TreeMap<Float, String> animalSpeeds = new TreeMap<>();
    private static final Map<String, String> animalImages = new TreeMap<>();

    static {
        addAnimal(0.0f, "Piedra", "piedra");
        addAnimal(0.3f, "Caracol", "caracol");
        addAnimal(1.0f, "Tortuga de tierra", "tortuga_de_tierra");
        addAnimal(3.0f, "Pingüino (andando)", "pinguino_andando");
        addAnimal(5.0f, "Humano (andando)", "humano_andando");
        addAnimal(8.0f, "Rata", "rata");
        addAnimal(10.0f, "Conejillo de Indias", "conejillo_de_indias");
        addAnimal(12.0f, "Gallina", "gallina");
        addAnimal(15.0f, "Cerdo", "cerdo");
        addAnimal(20.0f, "Ardilla", "ardilla");
        addAnimal(25.0f, "Elefante", "elefante");
        addAnimal(45.0f, "Humano (atleta)", "humano_atleta");
        addAnimal(48.0f, "Gato", "gato");
        addAnimal(50.0f, "Oso Pardo", "oso_pardo");
        addAnimal(55.0f, "Canguro", "canguro");
        addAnimal(70.0f, "Caballo", "caballo");
        addAnimal(110.0f, "Guepardo", "guepardo");
    }

    private static void addAnimal(float speed, String name, String imageName) {
        animalSpeeds.put(speed, name);
        animalImages.put(name, imageName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backgroundImageView = findViewById(R.id.backgroundImageView);
        peakSpeedTextView = findViewById(R.id.peakSpeedTextView);
        currentSpeedTextView = findViewById(R.id.currentSpeedTextView);
        animalNameTextView = findViewById(R.id.animalNameTextView);
        startStopButton = findViewById(R.id.startStopButton);

        previewMenu = findViewById(R.id.preview_menu);
        debugPrevButton = findViewById(R.id.debug_prev_button);
        debugNextButton = findViewById(R.id.debug_next_button);
        debugAnimalNameTextView = findViewById(R.id.debug_animal_name_textview);
        debugAnimalSpeedTextView = findViewById(R.id.debug_animal_speed_textview);

        animalList = new ArrayList<>(animalSpeeds.entrySet());

        debugPrevButton.setOnClickListener(v -> showPreviousAnimal());
        debugNextButton.setOnClickListener(v -> showNextAnimal());

        debugAnimalNameTextView.setText("--.-");
        debugAnimalSpeedTextView.setText("");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometerSensor == null) {
            Toast.makeText(this, "Acelerómetro no disponible.", Toast.LENGTH_LONG).show();
            startStopButton.setEnabled(false);
            animalNameTextView.setText("Sensor no compatible");
        }

        startStopButton.setOnClickListener(v -> {
            if (isTracking) {
                stopTracking();
            } else {
                startTracking();
            }
        });
    }

    private void showPreviousAnimal() {
        currentPreviewIndex--;
        if (currentPreviewIndex < 0) {
            currentPreviewIndex = animalList.size() - 1;
        }
        updatePreviewUI();
    }

    private void showNextAnimal() {
        currentPreviewIndex++;
        if (currentPreviewIndex >= animalList.size()) {
            currentPreviewIndex = 0;
        }
        updatePreviewUI();
    }

    private void updatePreviewUI() {
        Map.Entry<Float, String> currentAnimal = animalList.get(currentPreviewIndex);
        float speed = currentAnimal.getKey();
        showTestAnimal(speed);
    }

    private void showTestAnimal(float speedKmh) {
        float testMaxSpeed = speedKmh / 3.6f;
        updateResultUI(testMaxSpeed);
    }

    private void startTracking() {
        isTracking = true;
        maxSpeed = 0.0f;
        velocity[0] = 0; velocity[1] = 0; velocity[2] = 0;
        gravity[0] = 0; gravity[1] = 0; gravity[2] = 0;
        lastTimestamp = 0;
        trackingStartTime = 0;

        previewMenu.setVisibility(View.GONE);
        backgroundImageView.setImageResource(android.R.color.transparent);
        startStopButton.setText("Detener");
        peakSpeedTextView.setText("--.- km/h");
        currentSpeedTextView.setText("Actual: --.- km/h");
        animalNameTextView.setText("¡Prepárate!");

        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopTracking() {
        isTracking = false;
        sensorManager.unregisterListener(this);
        previewMenu.setVisibility(View.VISIBLE);
        startStopButton.setText("Iniciar de nuevo");
        currentSpeedTextView.setText("");
        updateResultUI(maxSpeed);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isTracking || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        if (trackingStartTime == 0) {
            trackingStartTime = event.timestamp;
            lastTimestamp = event.timestamp;
        }
        final float alpha = 0.8f;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        if (event.timestamp - trackingStartTime < 1_000_000_000) {
            if(animalNameTextView.getText().equals("¡Prepárate!")){
                animalNameTextView.setText("¡Corre!");
            }
            lastTimestamp = event.timestamp;
            return;
        }
        float dt = (event.timestamp - lastTimestamp) / 1_000_000_000.0f;
        lastTimestamp = event.timestamp;
        if (dt > 0.5f) return;
        float ax = event.values[0] - gravity[0];
        float ay = event.values[1] - gravity[1];
        float az = event.values[2] - gravity[2];
        if (Math.sqrt(ax * ax + ay * ay + az * az) > NOISE_THRESHOLD) {
            velocity[0] += ax * dt;
            velocity[1] += ay * dt;
            velocity[2] += az * dt;
        } else {
            velocity[0] = 0; velocity[1] = 0; velocity[2] = 0;
        }
        float currentSpeed = (float) Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);
        float rawSpeedKmh = currentSpeed * 3.6f;
        float boostedSpeedKmh;
        if (rawSpeedKmh <= 10.0f) {
            boostedSpeedKmh = rawSpeedKmh;
        } else if (rawSpeedKmh <= 25.0f) {
            float boostLow = 1.5f;
            boostedSpeedKmh = 10.0f + (rawSpeedKmh - 10.0f) * boostLow;
        } else {
            float boostHigh = 2.5f;
            float speedAt25 = 10.0f + (25.0f - 10.0f) * 1.5f;
            boostedSpeedKmh = speedAt25 + (rawSpeedKmh - 25.0f) * boostHigh;
        }
        float boostedSpeedMs = boostedSpeedKmh / 3.6f;
        currentSpeedTextView.setText(String.format("Actual: %.1f km/h", boostedSpeedKmh));
        if (boostedSpeedMs > maxSpeed) {
            maxSpeed = boostedSpeedMs;
            peakSpeedTextView.setText(String.format("%.1f km/h", maxSpeed * 3.6f));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void updateResultUI(float finalMaxSpeedInMs) {
        float userSpeedKmh = finalMaxSpeedInMs * 3.6f;
        Float closestSpeed = animalSpeeds.floorKey(userSpeedKmh);
        String animalName = animalSpeeds.get(closestSpeed != null ? closestSpeed : 0.0f);

        if (animalName != null) {
            animalNameTextView.setText(animalName);
            peakSpeedTextView.setText(String.format("%.1f km/h", userSpeedKmh));
            String imageName = animalImages.get(animalName);
            if (imageName != null) {
                int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());
                if (imageResId != 0) {
                    backgroundImageView.setImageResource(imageResId);
                } else {
                    backgroundImageView.setImageResource(android.R.color.darker_gray);
                }
            }

            for (int i = 0; i < animalList.size(); i++) {
                if (animalList.get(i).getValue().equals(animalName)) {
                    currentPreviewIndex = i;
                    break;
                }
            }
            Map.Entry<Float, String> previewAnimal = animalList.get(currentPreviewIndex);
            debugAnimalNameTextView.setText(previewAnimal.getValue());
            debugAnimalSpeedTextView.setText(String.format("%.1f km/h", previewAnimal.getKey()));

        } else {
            animalNameTextView.setText("---");
        }
    }
}
