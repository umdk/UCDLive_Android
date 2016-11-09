package com.ucloud.ulive.example.filter.video.cpu;

import com.ucloud.ulive.filter.UVideoCPUFilter;

public class UGrayCPUFilter extends UVideoCPUFilter {
    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        System.arraycopy(orignBuff,0,targetBuff,0,SIZE_Y);
        for (int i = SIZE_Y; i < SIZE_TOTAL; i++) {
            targetBuff[i] = 127;
        }
        return true;
    }
}
