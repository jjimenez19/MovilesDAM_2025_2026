package com.example.regresoseguro;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private static final String PREFS = "prefs_regreso_seguro";
    private static final String KEY_BASE_LAT = "base_lat";
    private static final String KEY_BASE_LON = "base_lon";
    private static final String KEY_BASE_OK = "base_ok";

    private static final float PRECISION_BUENA_M = 10f;

    private CompassView compassView;
    private TextView tvEstado;
    private TextView tvPrecision;
    private TextView tvDistancia;
    private TextView tvBase;
    private TextView tvActual;

    private MaterialButton btnGuardarBase;

    private SensorManager sensorManager;
    private Sensor sensorRotacion;
    private Sensor sensorAcelerometro;
    private Sensor sensorMagnetometro;

    private final float[] rotMat = new float[9];
    private final float[] orient = new float[3];

    private final float[] acc = new float[3];
    private final float[] mag = new float[3];
    private boolean hayAcc = false;
    private boolean hayMag = false;

    private float azimutDeg = 0f;
    private float azimutSuavizado = 0f;
    private boolean primerAzimut = true;

    private LocationManager locationManager;
    private Location ubicacionActual;
    private float precisionActual = Float.NaN;

    private boolean baseGuardada = false;
    private double baseLat = 0.0;
    private double baseLon = 0.0;

    private Float bearingSuavizado = null;

    private final ActivityResultLauncher<String[]> lanzadorPermisosUbicacion =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    res -> {
                        boolean ok = Boolean.TRUE.equals(res.get(Manifest.permission.ACCESS_FINE_LOCATION))
                                || Boolean.TRUE.equals(res.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                        if (ok) {
                            iniciarUbicacion();
                            actualizarUI();
                        } else {
                            tvEstado.setText("Estado: permiso de ubicación denegado");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        compassView = findViewById(R.id.compassView);
        tvEstado = findViewById(R.id.tvEstado);
        tvPrecision = findViewById(R.id.tvPrecision);
        tvDistancia = findViewById(R.id.tvDistancia);
        tvBase = findViewById(R.id.tvBase);
        tvActual = findViewById(R.id.tvActual);

        btnGuardarBase = findViewById(R.id.btnGuardarBase);
        MaterialButton btnBorrarBase = findViewById(R.id.btnBorrarBase);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorRotacion = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorAcelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagnetometro = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        cargarBase();
        pedirPermisosUbicacionSiHaceFalta();
        actualizarUI();

        btnGuardarBase.setOnClickListener(v -> guardarPuntoBase());

        btnBorrarBase.setOnClickListener(v -> {
            baseGuardada = false;
            guardarBase();
            bearingSuavizado = null;
            compassView.quitarObjetivo();
            actualizarUI();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensorRotacion != null) {
            sensorManager.registerListener(this, sensorRotacion, SensorManager.SENSOR_DELAY_GAME);
        } else {
            if (sensorAcelerometro != null) {
                sensorManager.registerListener(this, sensorAcelerometro, SensorManager.SENSOR_DELAY_GAME);
            }
            if (sensorMagnetometro != null) {
                sensorManager.registerListener(this, sensorMagnetometro, SensorManager.SENSOR_DELAY_GAME);
            }
        }

        if (tienePermisoUbicacion()) {
            iniciarUbicacion();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);

        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException ignored) {
        }
    }

    private void guardarPuntoBase() {
        if (ubicacionActual == null) {
            tvEstado.setText("Estado: aún no hay ubicación para guardar");
            return;
        }

        baseLat = ubicacionActual.getLatitude();
        baseLon = ubicacionActual.getLongitude();
        baseGuardada = true;
        guardarBase();
        bearingSuavizado = null;

        if (!Float.isNaN(precisionActual) && precisionActual > PRECISION_BUENA_M) {
            tvEstado.setText("Estado: punto base guardado (precisión baja: ±" + (int) precisionActual + " m)");
        } else if (!Float.isNaN(precisionActual)) {
            tvEstado.setText("Estado: punto base guardado (±" + (int) precisionActual + " m)");
        } else {
            tvEstado.setText("Estado: punto base guardado");
        }

        actualizarUI();
    }

    private void pedirPermisosUbicacionSiHaceFalta() {
        if (!tienePermisoUbicacion()) {
            lanzadorPermisosUbicacion.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean tienePermisoUbicacion() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine || coarse;
    }

    private void iniciarUbicacion() {
        boolean gps = false;
        boolean net = false;

        try {
            gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }

        try {
            net = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        try {
            if (gps) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 800, 1f, this);
            }
            if (!gps && net) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1200, 2f, this);
            }
            if (net && gps) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500, 3f, this);
            }
        } catch (SecurityException ignored) {
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        ubicacionActual = location;
        precisionActual = location.hasAccuracy() ? location.getAccuracy() : Float.NaN;
        actualizarUI();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int tipo = event.sensor.getType();

        if (tipo == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotMat, event.values);
            SensorManager.getOrientation(rotMat, orient);

            float az = (float) Math.toDegrees(orient[0]);
            azimutDeg = normalizar(az);

            if (primerAzimut) {
                azimutSuavizado = azimutDeg;
                primerAzimut = false;
            } else {
                azimutSuavizado = suavizarAngulo(azimutSuavizado, azimutDeg, 0.15f);
            }

            compassView.setAzimut(azimutSuavizado);
            actualizarObjetivoEnBrujula();
        }

        if (tipo == Sensor.TYPE_ACCELEROMETER) {
            acc[0] = event.values[0];
            acc[1] = event.values[1];
            acc[2] = event.values[2];
            hayAcc = true;
            recalcularAzimutFallback();
        }

        if (tipo == Sensor.TYPE_MAGNETIC_FIELD) {
            mag[0] = event.values[0];
            mag[1] = event.values[1];
            mag[2] = event.values[2];
            hayMag = true;
            recalcularAzimutFallback();
        }
    }

    private void recalcularAzimutFallback() {
        boolean usarFallback = sensorRotacion == null && hayAcc && hayMag;

        if (usarFallback) {
            float[] R = new float[9];
            float[] I = new float[9];

            boolean ok = SensorManager.getRotationMatrix(R, I, acc, mag);
            if (ok) {
                float[] o = new float[3];
                SensorManager.getOrientation(R, o);

                float az = (float) Math.toDegrees(o[0]);
                azimutDeg = normalizar(az);

                if (primerAzimut) {
                    azimutSuavizado = azimutDeg;
                    primerAzimut = false;
                } else {
                    azimutSuavizado = suavizarAngulo(azimutSuavizado, azimutDeg, 0.15f);
                }

                compassView.setAzimut(azimutSuavizado);
                actualizarObjetivoEnBrujula();
            }
        }
    }

    private void actualizarObjetivoEnBrujula() {
        if (baseGuardada && ubicacionActual != null) {
            Location base = new Location("base");
            base.setLatitude(baseLat);
            base.setLongitude(baseLon);

            float bearing = ubicacionActual.bearingTo(base);
            float b = normalizar(bearing);

            if (bearingSuavizado == null) {
                bearingSuavizado = b;
            } else {
                bearingSuavizado = suavizarAngulo(bearingSuavizado, b, 0.10f);
            }

            float metros = ubicacionActual.distanceTo(base);

            compassView.setObjetivo(bearingSuavizado);
            compassView.setDistanciaBase(metros);
        } else {
            compassView.quitarObjetivo();
        }
    }

    private void actualizarUI() {
        if (tienePermisoUbicacion()) {
            if (!tvEstado.getText().toString().startsWith("Estado: punto base")) {
                tvEstado.setText("Estado: activo");
            }
        } else {
            tvEstado.setText("Estado: falta permiso de ubicación");
        }

        if (!Float.isNaN(precisionActual)) {
            tvPrecision.setText("Precisión: ±" + (int) precisionActual + " m");
        } else {
            tvPrecision.setText("Precisión: --");
        }

        if (ubicacionActual != null) {
            tvActual.setText("Posición actual: " + formCoord(ubicacionActual.getLatitude()) + ", " + formCoord(ubicacionActual.getLongitude()));
        } else {
            tvActual.setText("Posición actual: --");
        }

        if (baseGuardada) {
            tvBase.setText("Punto base: " + formCoord(baseLat) + ", " + formCoord(baseLon));
        } else {
            tvBase.setText("Punto base: no guardado");
        }

        if (baseGuardada && ubicacionActual != null) {
            Location base = new Location("base");
            base.setLatitude(baseLat);
            base.setLongitude(baseLon);

            float metros = ubicacionActual.distanceTo(base);
            tvDistancia.setText("Distancia a punto base: " + formDist(metros));
        } else {
            tvDistancia.setText("Distancia a punto base: --");
        }

        btnGuardarBase.setEnabled(ubicacionActual != null);

        actualizarObjetivoEnBrujula();
    }

    private void cargarBase() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        baseGuardada = sp.getBoolean(KEY_BASE_OK, false);
        baseLat = Double.longBitsToDouble(sp.getLong(KEY_BASE_LAT, Double.doubleToLongBits(0.0)));
        baseLon = Double.longBitsToDouble(sp.getLong(KEY_BASE_LON, Double.doubleToLongBits(0.0)));
    }

    private void guardarBase() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(KEY_BASE_OK, baseGuardada);
        ed.putLong(KEY_BASE_LAT, Double.doubleToLongBits(baseLat));
        ed.putLong(KEY_BASE_LON, Double.doubleToLongBits(baseLon));
        ed.apply();
    }

    private float normalizar(float deg) {
        float r = deg % 360f;
        if (r < 0f) {
            r += 360f;
        }
        return r;
    }

    private float suavizarAngulo(float actual, float objetivo, float alpha) {
        float diff = ((objetivo - actual + 540f) % 360f) - 180f;
        float nuevo = actual + (diff * alpha);
        return normalizar(nuevo);
    }

    private String formCoord(double d) {
        return String.format(java.util.Locale.US, "%.5f", d);
    }

    private String formDist(float metros) {
        if (metros >= 1000f) {
            return String.format(java.util.Locale.US, "%.2f km", (metros / 1000f));
        }
        return String.format(java.util.Locale.US, "%.0f m", metros);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
