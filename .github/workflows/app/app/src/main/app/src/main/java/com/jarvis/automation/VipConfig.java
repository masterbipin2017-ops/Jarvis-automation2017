package com.jarvis.automation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class VipConfig {
    private VipConfig() {}

    public static final Map<String, String> VIP_CONTACTS = new HashMap<>();
    static {
        VIP_CONTACTS.put("wife", "+919506989074");
        VIP_CONTACTS.put("mom", "+919076765442");
        VIP_CONTACTS.put("dad", "+919453878596");
        VIP_CONTACTS.put("boss", "+917355116859");
        VIP_CONTACTS.put("emergency", "+919956895154");
    }

    public static final Set<String> TRUSTED_BLUETOOTH_DEVICES = new HashSet<>();
    static {
        TRUSTED_BLUETOOTH_DEVICES.add("AA:BB:CC:DD:EE:01");
    }

    public static volatile boolean TRUST_ANY_BLUETOOTH_DEVICE = true;
    public static volatile boolean AUTO_REPLY_ENABLED = true;

    public static final String AUTO_REPLY_MESSAGE =
            "I'm driving right now and will reply as soon as I can. "
          + "For emergencies, please call me directly. - Sent automatically by JarvisAutomation";

    public static final long AUTO_REPLY_COOLDOWN_MS = 5 * 60 * 1000L;
    public static final String VOICE_WAKE_ENABLED = "jarvis_voice_enabled";
    public static final float SHAKE_THRESHOLD = 15.0f;
    public static final long SHAKE_COOLDOWN_MS = 1200L;
    public static final String NOTIFICATION_CHANNEL_ID = "jarvis_automation_channel";
    public static final int FOREGROUND_NOTIFICATION_ID = 4201;
}
