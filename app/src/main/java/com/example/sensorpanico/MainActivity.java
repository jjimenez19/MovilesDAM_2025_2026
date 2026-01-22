package com.example.sensorpanico;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView txtEstado;
    private RelativeLayout layout;
    private boolean enPanico = false;

    // Receptor para actualizar la UI cuando el servicio detecta pánico
    private final BroadcastReceiver panicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            actualizarUIPanico();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtEstado = findViewById(R.id.txtEstado);
        layout = findViewById(R.id.layout);

        verificarPermisosYIniciarServicio();
    }

    private void verificarPermisosYIniciarServicio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                iniciarServicio();
            }
        } else {
            iniciarServicio();
        }
    }

    private void iniciarServicio() {
        Intent serviceIntent = new Intent(this, PanicService.class);
        startForegroundService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar el receptor para enterarnos de eventos de pánico
        IntentFilter filter = new IntentFilter("com.example.sensorpanico.PANIC_EVENT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(panicReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(panicReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(panicReceiver);
    }

    private void actualizarUIPanico() {
        if (!enPanico) {
            enPanico = true;
            layout.setBackgroundColor(Color.RED);
            txtEstado.setText("¡¡MODO PÁNICO ACTIVADO!! 😱");

            // Volver al estado normal después de 4 segundos (igual que el servicio)
            layout.postDelayed(() -> {
                enPanico = false;
                layout.setBackgroundColor(Color.GREEN);
                txtEstado.setText("Todo tranquilo 😌");
            }, 4000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarServicio();
            } else {
                Toast.makeText(this, "Se necesita permiso de notificación para el servicio", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
