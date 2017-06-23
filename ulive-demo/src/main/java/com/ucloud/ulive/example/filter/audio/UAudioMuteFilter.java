package com.ucloud.ulive.example.filter.audio;

import com.ucloud.ulive.filter.UAudioCPUFilter;
import com.ucloud.ulive.framework.AudioBufferFrame;

public class UAudioMuteFilter extends UAudioCPUFilter {

    private byte[] muteAudioData;

    @Override
    public void onFrame(AudioBufferFrame audioBufferFrame) {
        if (audioBufferFrame != null && audioBufferFrame.buffer != null) {
            audioBufferFrame.buffer.position(0);
            int len = audioBufferFrame.buffer.limit();
            if (muteAudioData == null) {
                muteAudioData = new byte[len];
                for (int i = 0; i < len; i++) {
                    muteAudioData[i] = 0;
                }
            }
            audioBufferFrame.buffer.put(muteAudioData, 0, len);
        }
    }
}
