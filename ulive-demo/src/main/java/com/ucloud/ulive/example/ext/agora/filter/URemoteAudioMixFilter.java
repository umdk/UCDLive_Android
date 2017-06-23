package com.ucloud.ulive.example.ext.agora.filter;

import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.ucloud.ulive.example.ext.agora.RemoteAudioCacheUtil;
import com.ucloud.ulive.example.utils.AverageAudioMixer;
import com.ucloud.ulive.filter.UAudioCPUFilter;
import com.ucloud.ulive.framework.AudioBufferFrame;

public class URemoteAudioMixFilter extends UAudioCPUFilter {

    private static final String TAG = "URemoteAudioMixFilter";

    private byte[] remoteAudioData;

    private AverageAudioMixer averageAudioMixer;

    private float remoteVolumeLevel = 1.0f;

    private float localVolumeLevel = 1.0f;

    private byte[] originCacheBuffer;

    private boolean muteRemoteAudio = false;

    private boolean muteLocalAudio = false;

    private byte[] muteAudioData;

    public URemoteAudioMixFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
    }

    @Override
    public void onInit(int size) {
        super.onInit(size);
        init();
    }

    private void init() {
        try {
            remoteAudioData = new byte[size];
            averageAudioMixer = new AverageAudioMixer();
        }
        catch (Exception e) {
            Log.e(TAG, "lifecycle->demo->URemoteAudioMixFilter->onInit failed.");
        }
    }

    @Override
    public void onFrame(AudioBufferFrame originLocalFrame) {
        AudioBufferFrame remoteAudioBufferFrame = RemoteAudioCacheUtil.getInstance().getRemoteAudio();
        if (remoteAudioBufferFrame == null || remoteAudioBufferFrame.buffer == null) {
            Log.d(TAG, "lifecycle->demo->URemoteAudioMixFilter->remote->audio->pop->failed.");
            return;
        }

        remoteAudioBufferFrame.buffer.position(0);
        remoteAudioBufferFrame.buffer.get(remoteAudioData, 0, remoteAudioBufferFrame.buffer.limit());

        int len = remoteAudioBufferFrame.buffer.limit();

        int i = 0;

        for (; i < len; i++) {
            if (remoteAudioData[i] != 0) {
                break;
            }
        }

        if (i == len || muteRemoteAudio) {
            Log.d(TAG, "lifecycle->URemoteAudioMixFilter->remote audio is mute.");
            remoteAudioBufferFrame.buffer.flip();
            RemoteAudioCacheUtil.getInstance().restore(remoteAudioBufferFrame);
            return;
        }

        if (!muteLocalAudio) {
            if (originCacheBuffer == null) {
                originCacheBuffer = new byte[len];
            }

            originLocalFrame.buffer.position(0);
            originLocalFrame.buffer.get(originCacheBuffer, 0, originLocalFrame.buffer.limit());

            byte[] bytes = averageAudioMixer.mixRawAudioBytes(new byte[][]{averageAudioMixer.scale(remoteAudioData, size, remoteVolumeLevel), averageAudioMixer.scale(originCacheBuffer, size, localVolumeLevel)});
            if (bytes != null) {
                originLocalFrame.buffer.position(0);
                originLocalFrame.buffer.put(bytes, 0, len); //local + remote mixer audio
            }
        }
        else {
            if (originLocalFrame != null && originLocalFrame.buffer != null) {
                originLocalFrame.buffer.position(0);
                len = originLocalFrame.buffer.limit();
                if (muteAudioData == null) {
                    muteAudioData = new byte[len];
                    for (i = 0; i < len; i++) {
                        muteAudioData[i] = 0;
                    }
                }
                originLocalFrame.buffer.put(muteAudioData, 0, len);
            }
        }
        remoteAudioBufferFrame.buffer.flip();
        RemoteAudioCacheUtil.getInstance().restore(remoteAudioBufferFrame);
    }

    public void adjustRemoteVolumeLevel(float level) {
        remoteVolumeLevel = level;
    }

    public void adjustLocalVolumeLevel(float level) {
        this.localVolumeLevel = level;
    }

    public void setRemoteAudioState(boolean isMute) {
        muteRemoteAudio = isMute;
    }

    public void setLocalAudioState(boolean isMute) {
        muteLocalAudio = isMute;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            remoteAudioData = null;
            averageAudioMixer = null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
