package com.kony.push.customfcmservice;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getAction() == null) {
            setContentView(R.layout.activity_main);
        } else {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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

            if (notificationId != -1) {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(notificationId);
                }
            }

            if ("WATCH_VIDEO_ACTION".equals(action)) {
                String videoUrl = intent.getStringExtra("videoUrl");
                if (videoUrl != null) {
                    showVideoDialog(videoUrl);
                }
            } else if ("ACCEPT_ACTION".equals(action)) {
                Toast.makeText(this, "Aceptado", Toast.LENGTH_SHORT).show();
                finish();
            } else if ("DENY_ACTION".equals(action)) {
                Toast.makeText(this, "Denegado", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void showVideoDialog(String videoUrl) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        dialog.setOnDismissListener(d -> finish());

        VideoView videoView = new VideoView(this);
        videoView.setVideoURI(Uri.parse(videoUrl));

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        videoView.setOnCompletionListener(mp -> {
            dialog.dismiss();
            finish();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            Log.e("MainActivity", "Error al reproducir video");
            dialog.dismiss();
            finish();
            return true;
        });

        dialog.setContentView(videoView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        dialog.show();
        videoView.start();
    }
}