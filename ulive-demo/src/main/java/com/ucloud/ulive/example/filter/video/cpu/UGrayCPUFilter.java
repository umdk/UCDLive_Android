package com.ucloud.ulive.example.filter.video.cpu;


import com.ucloud.ulive.filter.UVideoCPUFilter;

public class UGrayCPUFilter extends UVideoCPUFilter {
    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        System.arraycopy(orignBuff, 0, targetBuff, 0, sizeY);
        for (int i = sizeY; i < sizeTotal; i++) {
            targetBuff[i] = 127;
        }
        return true;
    }
}
