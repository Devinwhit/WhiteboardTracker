package com.devinwhitney.android.whiteboardtracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Created by devin on 10/19/2018.
 */

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Message received!");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("record_workout_channel", "record_workout_channel", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, "record_workout_channel")
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.record_workout_reminder))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(contentIntent(context))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify(1005, notificationBuilder.build());


    }

    private PendingIntent contentIntent(Context context) {
        Intent intent = new Intent(context,AddResults.class);

        return PendingIntent.getActivity(context, 1005, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
