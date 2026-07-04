package com.jarvis.automation;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.HashMap;
import java.util.Map;

public class WhatsAppNotificationListener extends NotificationListenerService {
    private static final String PKG_WHATSAPP = "com.whatsapp";
    private static final String PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b";
    private final Map<String, Long> lastReplyTimestamps = new HashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        if (!VipConfig.AUTO_REPLY_ENABLED) return;

        String packageName = sbn.getPackageName();
        if (!PKG_WHATSAPP.equals(packageName) && !PKG_WHATSAPP_BUSINESS.equals(packageName)) return;

        Notification notification = sbn.getNotification();
        if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        Bundle extras = notification.extras;
        CharSequence senderTitle = extras != null ? extras.getCharSequence(Notification.EXTRA_TITLE) : null;
        String sender = senderTitle != null ? senderTitle.toString() : sbn.getKey();

        if ((System.currentTimeMillis() - lastReplyTimestamps.getOrDefault(sender, 0L)) < VipConfig.AUTO_REPLY_COOLDOWN_MS) return;

        Notification.Action replyAction = null;
        if (notification.actions != null) {
            for (Notification.Action action : notification.actions) {
                if (action.getRemoteInputs() != null && action.getRemoteInputs().length > 0) {
                    replyAction = action;
                    break;
                }
            }
        }
        if (replyAction == null) return;

        RemoteInput[] remoteInputs = replyAction.getRemoteInputs();
        Bundle resultsBundle = new Bundle();
        for (RemoteInput remoteInput : remoteInputs) {
            resultsBundle.putCharSequence(remoteInput.getResultKey(), VipConfig.AUTO_REPLY_MESSAGE);
        }

        Intent fillInIntent = new Intent();
        RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, resultsBundle);

        try {
            replyAction.actionIntent.send(getApplicationContext(), 0, fillInIntent);
            lastReplyTimestamps.put(sender, System.currentTimeMillis());
        } catch (PendingIntent.CanceledException e) {}
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {}
}
