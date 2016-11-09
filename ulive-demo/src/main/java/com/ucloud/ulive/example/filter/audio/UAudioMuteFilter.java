package com.ucloud.ulive.example.filter.audio;

import com.ucloud.ulive.filter.UAudioCPUFilter;

public class UAudioMuteFilter extends UAudioCPUFilter {

    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        for (int i = 0; i < SIZE; i++) {
            orignBuff[i] = 0;
        }
        return false;
    }
}
