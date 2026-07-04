package com.jarvis.automation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class VoiceCommandManager implements RecognitionListener {
    private static final String TAG = "VoiceCommandManager";

    public interface TorchCallback {
        void setTorch(boolean on);
    }

    private final Context context;
    private final TorchCallback torchCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer speechRecognizer;
    private volatile boolean running = false;
    private volatile boolean listening = false;

    public VoiceCommandManager(Context context, TorchCallback torchCallback) {
        this.context = context.getApplicationContext();
        this.torchCallback = torchCallback;
    }

    public void start() {
        if (running) return;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return;
        }
        running = true;
        handler.post(this::initAndListen);
    }

    public void stop() {
        running = false;
        handler.post(() -> {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
            listening = false;
        });
    }

    private void initAndListen() {
        if (!running) return;
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(this);
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        try {
            listening = true;
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            restartWithDelay(2000);
        }
    }

    private void restartWithDelay(long delayMs) {
        listening = false;
        if (!running) return;
        handler.postDelayed(this::initAndListen, delayMs);
    }

    @Override
    public void onResults(Bundle results) {
        listening = false;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            for (String phrase : matches) {
                if (handleCommand(phrase.toLowerCase(Locale.getDefault()).trim())) {
                    break;
                }
            }
        }
        restartWithDelay(400);
    }

    @Override
    public void onError(int error) {
        listening = false;
        long delay = (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) ? 400 : 1500;
        restartWithDelay(delay);
    }

    private boolean handleCommand(String phrase) {
        if (phrase.contains("call ")) {
            String target = phrase.substring(phrase.indexOf("call ") + 5).trim();
            return placeVipCall(target);
        }
        if (phrase.contains("emergency")) {
            return placeVipCall("emergency");
        }
        if (phrase.contains("play music") || phrase.equals("play") || phrase.contains("resume music")) {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            return true;
        }
        if (phrase.contains("pause music") || phrase.equals("pause") || phrase.contains("stop music")) {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            return true;
        }
        if (phrase.contains("next track") || phrase.contains("next song") || phrase.contains("skip song")) {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            return true;
        }
        if (phrase.contains("previous track") || phrase.contains("previous song") || phrase.contains("last song")) {
            dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            return true;
        }
        if (phrase.contains("torch on") || phrase.contains("flashlight on") || phrase.contains("light on")) {
            if (torchCallback != null) torchCallback.setTorch(true);
            return true;
        }
        if (phrase.contains("torch off") || phrase.contains("flashlight off") || phrase.contains("light off")) {
            if (torchCallback != null) torchCallback.setTorch(false);
            return true;
        }
        return false;
    }

    private boolean placeVipCall(String spokenName) {
        String bestMatchNumber = null;
        for (Map.Entry<String, String> entry : VipConfig.VIP_CONTACTS.entrySet()) {
            if (spokenName.contains(entry.getKey())) {
                bestMatchNumber = entry.getValue();
                break;
            }
        }
        if (bestMatchNumber == null) return false;

        boolean hasCallPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED;

        Intent callIntent = new Intent(hasCallPermission ? Intent.ACTION_CALL : Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + bestMatchNumber));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(callIntent);
        } catch (Exception e) {}
        return true;
    }

    private void dispatchMediaKey(int keyCode) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        long eventTime = android.os.SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
        KeyEvent up = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0);
        audioManager.dispatchMediaKeyEvent(down);
        audioManager.dispatchMediaKeyEvent(up);
    }

    @Override public void onReadyForSpeech(Bundle params) { }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { }
    @Override public void onPartialResults(Bundle partialResults) { }
    @Override public void onEvent(int eventType, Bundle params) { }
}
