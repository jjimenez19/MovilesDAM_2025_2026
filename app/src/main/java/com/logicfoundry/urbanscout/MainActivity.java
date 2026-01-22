package com.logicfoundry.urbanscout;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements LocationListener {

    // ---VARIABLES DE INTERFAZ---
    private TextView txtRuido, txtUbicacion, txtWifi, txtLuz, txtMovimiento;

    // ---VARIABLES DE SENSORES---
    private GestorDeSensores gestorDeSensores;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private MediaRecorder mediaRecorder;

    // ---VARIABLES DE CONTROL---
    private boolean estaGrabando = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int REQUEST_CODE_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtRuido = findViewById(R.id.txtRuido);
        txtUbicacion = findViewById(R.id.txtUbicacion);
        txtWifi = findViewById(R.id.txtWifi);
        txtLuz = findViewById(R.id.txtLuz);
        txtMovimiento = findViewById(R.id.txtMovimiento);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        configurarSensoresFisicos();

        if (!tienePermisos()) {
            solicitarPermisos();
        }
    }

    // ---CONFIGURACION DE GESTOR DE SENSORES---
    private void configurarSensoresFisicos() {
        gestorDeSensores = new GestorDeSensores(this, new GestorDeSensores.SensorListenerCallback() {
            @Override
            public void onLuzCambiada(float lux, String mensaje) {
                txtLuz.setText(String.format("Luz: %.1f lx\n%s", lux, mensaje));

                if (lux < 10) {
                    getWindow().getDecorView().setBackgroundColor(Color.parseColor("#121212")); // Negro suave
                    cambiarColorTexto(Color.WHITE);
                } else {
                    getWindow().getDecorView().setBackgroundColor(Color.WHITE);
                    cambiarColorTexto(Color.BLACK);
                }
            }

            @Override
            public void onMovimientoDetectado(boolean esBrusco, float fuerzaG) {
                if (esBrusco) {
                    txtMovimiento.setText("¡IMPACTO DETECTADO!\nFuerza: " + String.format("%.2f", fuerzaG) + "G");
                    txtMovimiento.setTextColor(Color.RED);
                } else {
                    txtMovimiento.setText("Movimiento: Estable");
                    int colorTexto = (txtLuz.getText().toString().contains("Muy oscuro")) ? Color.WHITE : Color.BLACK;
                    txtMovimiento.setTextColor(colorTexto);
                    txtMovimiento.setTextColor(Color.parseColor("#4CAF50"));
                }
            }
        });
    }

    private void cambiarColorTexto(int color) {
        txtRuido.setTextColor(color);
        txtUbicacion.setTextColor(color);
        txtWifi.setTextColor(color);
        txtLuz.setTextColor(color);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // 1. Activar sensores fiisicos
        if (gestorDeSensores != null) gestorDeSensores.iniciarEscucha();

        iniciarGPS();

        iniciarMedidorRuido();

        handler.post(loopWifi);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 1. Pausar sensores fisicos
        if (gestorDeSensores != null) gestorDeSensores.detenerEscucha();

        if (locationManager != null) locationManager.removeUpdates(this);

        detenerMedidorRuido();

        handler.removeCallbacks(loopWifi);
    }

    // ---MICROFONO---
    private void iniciarMedidorRuido() {
        if (!tienePermisos()) return;

        if (mediaRecorder == null) {
            try {
                // 1. Configuracion basica
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                String archivoTemporal = getExternalCacheDir().getAbsolutePath() + "/grabacion_temp.3gp";
                mediaRecorder.setOutputFile(archivoTemporal);

                mediaRecorder.prepare();
                mediaRecorder.start();
                estaGrabando = true;

                // 2. Iniciar el loop
                handler.postDelayed(loopRuido, 500);

            } catch (IOException | IllegalStateException e) {
                Log.e("UrbanScout", "Error fatal al iniciar micrófono", e);

                detenerMedidorRuido();
            } catch (Exception e) {
                Log.e("UrbanScout", "Error desconocido en micrófono", e);
                detenerMedidorRuido();
            }
        }
    }

    private void detenerMedidorRuido() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (RuntimeException e) {
            }
            mediaRecorder = null;
            estaGrabando = false;
            handler.removeCallbacks(loopRuido);
        }
    }

    private Runnable loopRuido = new Runnable() {
        @Override
        public void run() {
            if (mediaRecorder != null && estaGrabando) {
                try {
                    double amplitud = mediaRecorder.getMaxAmplitude();

                    if (amplitud > 0) {
                        // Calculo de decibelios
                        double db = 20 * Math.log10(amplitud);

                        String estadoOido = "";

                        if (db < 70) {
                            estadoOido = "(Seguro 🟢)";
                        } else if (db < 85) {
                            estadoOido = "(Medio 🟠)";
                        } else {
                            estadoOido = "(Inseguro 🔴)";
                        }

                        txtRuido.setText(String.format("Ruido: %.1f dB %s", db, estadoOido));
                    }
                } catch (IllegalStateException e) {
                    Log.w("UrbanScout", "El micrófono aún no estaba listo.");
                } catch (Exception e) {
                    Log.e("UrbanScout", "Error leyendo audio", e);
                }

                handler.postDelayed(this, 500);
            }
        }
    };

    private Runnable loopWifi = new Runnable() {
        @Override
        public void run() {
            actualizarWifi();
            handler.postDelayed(this, 2000);
        }
    };

    // ---LOGICA: GPS---
    private void iniciarGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Actualizar cada 5 segundos o 10 metros
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        txtUbicacion.setText("GPS:\nLat: " + location.getLatitude() + "\nLon: " + location.getLongitude());
    }

    // ---LOGICA: WI-FI---
    private void actualizarWifi() {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int nivel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);

            String estado = "";
            switch (nivel) {
                case 5: estado = "Excelente"; break;
                case 4: estado = "Buena"; break;
                case 3: estado = "Regular"; break;
                case 2: estado = "Débil"; break;
                default: estado = "Mala/Sin Señal"; break;
            }
            txtWifi.setText("Señal Wi-Fi: " + nivel + "/5 (" + estado + ")");
        }
    }

    // ---GESTION DE PERMISOS---
    private boolean tienePermisos() {
        int permisoAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int permisoLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permisoAudio == PackageManager.PERMISSION_GRANTED && permisoLoc == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarPermisos() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQUEST_CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos concedidos. Iniciando sensores...", Toast.LENGTH_SHORT).show();
                iniciarMedidorRuido();
                iniciarGPS();
            } else {
                Toast.makeText(this, "Se requieren permisos para funcionar", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(@NonNull String provider) {}
    @Override public void onProviderDisabled(@NonNull String provider) {}
}