package com.kony.push.customfcmservice;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.konylabs.fcm.KonyFCMService;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class CustomFCMService extends KonyFCMService {
    private static int pushMsgNotificationId = 0;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("FCMService", "#### on Message Received method");
    }

    /**
     * Se llama cuando el token de Firebase cambia o se genera por primera vez.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCMService", "#### Nuevo Token generado: " + token);
    }

    @Override
    @SuppressWarnings("DiscouragedApi")
    public void showPushMessageNotification(Context context, Map<String, String> data) {
        Log.d("FCMService", "#### Payload recibido: " + data.toString());
        String pkgName = context.getPackageName();

        int resId = context.getResources().getIdentifier("notify_push_msg", "string", pkgName);
        String enableNotifyPushMsg = (resId != 0) ? context.getString(resId) : "true";

        if ("true".equalsIgnoreCase(enableNotifyPushMsg)) {

            // --- ICONO ---
            resId = context.getResources().getIdentifier("notify_push_msg_icon", "string", pkgName);
            String iconName = (resId != 0) ? context.getString(resId) : null;

            if (iconName == null || iconName.equals("icon")) {
                resId = context.getResources().getIdentifier("app_notify_push_msg_icon", "string", pkgName);
                if (resId != 0) iconName = context.getString(resId);
            }

            int icon = (iconName != null) ? context.getResources().getIdentifier(iconName, "drawable", pkgName) : 0;
            if (icon == 0) {
                icon = context.getResources().getIdentifier("icon", "drawable", pkgName);
                if (icon == 0) icon = android.R.drawable.sym_def_app_icon;
            }

            // --- TÍTULO ---
            String title = data.get("title");
            if (title == null || title.isEmpty()) {
                resId = context.getResources().getIdentifier("notify_push_msg_title_keys", "string", pkgName);
                if (resId != 0) {
                    String titleKeysStr = context.getString(resId);
                    if (!titleKeysStr.trim().isEmpty()) {
                        String[] titleKeys = titleKeysStr.split(",");
                        for (String key : titleKeys) {
                            title = data.get(key.trim());
                            if (title != null && !title.isEmpty()) break;
                        }
                    }
                }
            }
            if (title == null || title.isEmpty()) {
                resId = context.getResources().getIdentifier("notify_push_msg_default_title", "string", pkgName);
                if (resId != 0) title = context.getString(resId);
            }
            if (title == null || title.isEmpty()) {
                title = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
            }

            // --- DESCRIPCIÓN ---
            String desc = data.get("content");
            if (desc == null) desc = data.get("message");
            if (desc == null) {
                resId = context.getResources().getIdentifier("notify_push_msg_default_desc", "string", pkgName);
                desc = (resId != 0) ? context.getString(resId) : "";
            }

            // --- IMAGEN ---
            String image = data.get("image");
            Bitmap myBitmap = null;
            if (image != null && !image.isEmpty()) {
                InputStream input = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(image);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.connect();
                    input = connection.getInputStream();
                    myBitmap = BitmapFactory.decodeStream(input);
                } catch (IOException e) {
                    Log.e("FCMService", "Error cargando imagen: " + e.getMessage());
                } finally {
                    if (input != null) {
                        try { input.close(); } catch (IOException ignored) {}
                    }
                    if (connection != null) connection.disconnect();
                }
            }

            // --- CONSTRUCCIÓN ---
            int notificationId = generatePushMessageNotificationId();
            PendingIntent contentIntent = createNotificationPendingIntent(context, data, notificationId);

            String channelId = "push_" + pkgName + "_high";
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(icon)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(desc)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            // 1. Botón "Navegar" (Si viene urlPage)
            String urlPage = data.get("urlPage");
            if (urlPage != null && !urlPage.isEmpty()) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlPage));
                PendingIntent webPendingIntent = PendingIntent.getActivity(context, notificationId + 3, webIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                notificationBuilder.addAction(0, "Navegar", webPendingIntent);
            }

            // 2. Botón "Ver Video" (Si viene videoUrl)
            String videoUrl = data.get("videoUrl");
            if (videoUrl != null && !videoUrl.isEmpty()) {
                Intent videoIntent = new Intent(context, MainActivity.class);
                videoIntent.setAction("WATCH_VIDEO_ACTION");
                videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                videoIntent.putExtra("videoUrl", videoUrl);
                videoIntent.putExtra("notificationId", notificationId);

                PendingIntent videoPendingIntent = PendingIntent.getActivity(context, notificationId + 4, videoIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                notificationBuilder.addAction(0, "Ver Video", videoPendingIntent);
            }

            // --- ESTILOS ---
            if (myBitmap != null) {
                notificationBuilder.setLargeIcon(myBitmap)
                        .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(myBitmap).bigLargeIcon(null));
            } else {
                notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(desc));
            }

            // --- CANAL (Android O+) ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel(channelId, "Notificaciones Importantes", NotificationManager.IMPORTANCE_HIGH);
                if (mNotificationManager != null) {
                    mNotificationManager.createNotificationChannel(channel);
                }
                notificationBuilder.setChannelId(channelId);
            }

            // --- MOSTRAR ---
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, notificationBuilder.build());
                }
            } else {
                notificationManager.notify(notificationId, notificationBuilder.build());
            }
        }
    }

    private int generatePushMessageNotificationId() {
        pushMsgNotificationId++;
        if (pushMsgNotificationId > 50) pushMsgNotificationId = 1;
        return pushMsgNotificationId;
    }
}