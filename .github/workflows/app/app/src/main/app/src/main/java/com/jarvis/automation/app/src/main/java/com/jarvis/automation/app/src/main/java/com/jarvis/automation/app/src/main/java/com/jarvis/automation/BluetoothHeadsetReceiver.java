package com.jarvis.automation;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;

public class BluetoothHeadsetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            BackgroundAutomationService.start(context);
            return;
        }

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) return;

            String address = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        address = device.getAddress();
                    }
                } else {
                    address = device.getAddress();
                }
            } catch (SecurityException e) {}

            boolean trusted = VipConfig.TRUST_ANY_BLUETOOTH_DEVICE || (address != null && VipConfig.TRUSTED_BLUETOOTH_DEVICES.contains(address));
            if (trusted) {
                BackgroundAutomationService.start(context);
            }
        }
    }
}
