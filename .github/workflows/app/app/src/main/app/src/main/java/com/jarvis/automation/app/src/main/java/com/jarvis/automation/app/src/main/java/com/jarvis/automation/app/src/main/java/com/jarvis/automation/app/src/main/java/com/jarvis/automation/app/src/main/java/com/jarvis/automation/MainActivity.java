package com.jarvis.automation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private TextView statusText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        refreshStatus();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("JarvisAutomation");
        title.setTextSize(24f);
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);

        statusText = new TextView(this);
        statusText.setTextSize(14f);
        statusText.setPadding(0, 0, 0, dp(24));
        root.addView(statusText);

        Button grantPermissionsButton = new Button(this);
        grantPermissionsButton.setText("1. Grant Core Permissions");
        grantPermissionsButton.setOnClickListener(v -> requestCorePermissions());
        root.addView(grantPermissionsButton);

        Button notificationAccessButton = new Button(this);
        notificationAccessButton.setText("2. Enable WhatsApp Auto-Reply Access");
        notificationAccessButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(notificationAccessButton);

        Button batteryButton = new Button(this);
        batteryButton.setText("3. Disable Battery Optimisation");
        batteryButton.setOnClickListener(v -> requestIgnoreBatteryOptimizations());
        root.addView(batteryButton);

        Button startButton = new Button(this);
        startButton.setText("4. Start Jarvis Background Service");
        startButton.setOnClickListener(v -> {
            BackgroundAutomationService.start(this);
            Toast.makeText(this, "Jarvis automation service started.", Toast.LENGTH_SHORT).show();
            refreshStatus();
        });
        root.addView(startButton);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String[] buildRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.CALL_PHONE);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions.toArray(new String[0]);
    }

    private void requestCorePermissions() {
        List<String> missing = new ArrayList<>();
        for (String permission : buildRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (missing.isEmpty()) {
            refreshStatus();
            return;
        }
        ActivityCompat.requestPermissions(this, missing.toArray(new String[0]), 9001);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatus();
    }

    private void requestIgnoreBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            try { startActivity(intent); } catch (Exception e) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        StringBuilder sb = new StringBuilder();
        boolean corePermissionsGranted = true;
        for (String permission : buildRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                corePermissionsGranted = false;
                break;
            }
        }
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean listenerEnabled = !TextUtils.isEmpty(enabledListeners) && enabledListeners.contains(getPackageName());

        sb.append("Core permissions: ").append(corePermissionsGranted ? "GRANTED" : "MISSING").append("\n");
        sb.append("WhatsApp listener access: ").append(listenerEnabled ? "ENABLED" : "DISABLED").append("\n");
        statusText.setText(sb.toString());
    }
}
