package com.kony.push.customfcmservice;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kony.push.customfcmservice.R.layout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        handleIntentAction(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentAction(intent);
    }

    private void handleIntentAction(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            int notificationId = intent.getIntExtra("notificationId", -1);

            // Cerrar notificación al pulsar botón
            if (notificationId != -1) {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(notificationId);
                }
            }

            if ("ACCEPT_ACTION".equals(action)) {
                Log.d("MainActivity", "Acción: ACEPTAR pulsada");
                Toast.makeText(this, "Aceptado", Toast.LENGTH_SHORT).show();
            } else if ("DENY_ACTION".equals(action)) {
                Log.d("MainActivity", "Acción: DENEGAR pulsada");
                Toast.makeText(this, "Denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}