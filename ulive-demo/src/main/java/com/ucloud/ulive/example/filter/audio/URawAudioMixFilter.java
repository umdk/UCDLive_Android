package com.ucloud.ulive.example.filter.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import com.ucloud.ulive.example.utils.AverageAudioMixer;
import com.ucloud.ulive.filter.UAudioCPUFilter;
import com.ucloud.ulive.framework.AudioBufferFrame;

import java.io.IOException;
import java.io.InputStream;

public class URawAudioMixFilter extends UAudioCPUFilter {

    private static final String TAG = "URawAudioMixFilter";

    private InputStream bgmInputStream;

    private byte[] bgm;

    private AverageAudioMixer averageAudioMixer;

    private Context context;

    private float localAudioLevel = 1.0f;

    private float mixerAudioLevel = 1.0f;

    private URawAudioPlayer rawAudioPlayer;

    private boolean isLooper = false;

    private boolean isMixBgm = false;

    private final HeadsetPlugReceiver headsetPlugReceiver;

    private Mode mixMode = Mode.JUST_HEADSET_ON;

    private byte[] originCacheBuffer;

    public enum Mode {
        JUST_HEADSET_ON,     //mix when headset on, if not mix by misc(when play)
        ANY,                 //all mix
    }

    public URawAudioMixFilter(Context context, Mode mixMode, boolean isLooper) {
        this.context = context;
        this.isLooper = isLooper;
        this.mixMode = mixMode;
        headsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter  filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        this.context.registerReceiver(headsetPlugReceiver, filter);
        if (mixMode == Mode.JUST_HEADSET_ON) {
            AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
            isMixBgm = audioManager.isWiredHeadsetOn();
        }
        else {
            isMixBgm = true;
        }
    }

    @Override
    public void onInit(int size) {
        super.onInit(size);
        init();
        rawAudioPlayer = new URawAudioPlayer(size);
    }

    private void init() {
        try {
            bgmInputStream = context.getAssets().open("raw_audio_mix_bg.pcm");
            bgmInputStream.mark(bgmInputStream.available());
            bgm = new byte[size];
            averageAudioMixer = new AverageAudioMixer();
        }
        catch (Exception e) {
            bgmInputStream = null;
            Log.e(TAG, "lifecycle->demo->URawAudioMixFilter->onInit failed.");
        }
    }

    @Override
    public void onFrame(AudioBufferFrame frame) {
        try {
            if (bgmInputStream == null || bgmInputStream.read(bgm, 0, size) < size) {
                handleReadException();
                return;
            }
        }
        catch (Exception e) {
            handleReadException();
            return;
        }

        if (rawAudioPlayer != null) {
            if (bgm != null) {
                if (!rawAudioPlayer.isPlaying()) {
                    rawAudioPlayer.start();
                }
                rawAudioPlayer.sendAudio(bgm);
            }
            else {
                rawAudioPlayer.stop();
            }
        }

        if (isMixBgm) {
            int len = frame.buffer.limit();
            if (originCacheBuffer == null) {
                originCacheBuffer = new byte[len];
            }
            frame.buffer.position(0);
            frame.buffer.get(originCacheBuffer, 0, frame.buffer.limit());

            byte[] bytes = averageAudioMixer.mixRawAudioBytes(new byte[][]{averageAudioMixer.scale(bgm, size, localAudioLevel), averageAudioMixer.scale(originCacheBuffer, size, mixerAudioLevel)});
            if (bytes != null) {
                frame.buffer.position(0);
                frame.buffer.put(bytes, 0, len);
            }
        }
    }

    //just support PCM s16le,
    // background music pcm:  samplerate = 44100 channels = 2, pcm format = s16le if different with UAudioProfile demo may be error.
    private void handleReadException() {
        if (isLooper) {
            init();
        }
        if (rawAudioPlayer != null) {
            rawAudioPlayer.stop();
        }
    }

    public void adjustLocalAudioLevel(float level) {
        localAudioLevel = level;
    }

    public void adjustMixerAudioLevel(float level) {
        this.mixerAudioLevel = level;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (bgmInputStream != null) {
                bgmInputStream.close();
                bgmInputStream = null;
            }
            if (context != null) {
                context.unregisterReceiver(headsetPlugReceiver);
            }
            context = null;
            bgm = null;
            averageAudioMixer = null;
            if (rawAudioPlayer != null) {
                rawAudioPlayer.stop();
                rawAudioPlayer = null;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class HeadsetPlugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                int state =  intent.getIntExtra("state", 0);
                if (0 == state) {
                    Log.e(TAG, "lifecycle->demo->URawAudioMixFilter->HeadsetPlugReceiver->headset not connected");
                }
                else if (1 == state) {
                    Log.e(TAG, "lifecycle->demo->URawAudioMixFilter->HeadsetPlugReceiver->headset connected");
                }
                if (mixMode == Mode.JUST_HEADSET_ON) {
                    isMixBgm = state != 0;
                }
            }
        }
    }
}
