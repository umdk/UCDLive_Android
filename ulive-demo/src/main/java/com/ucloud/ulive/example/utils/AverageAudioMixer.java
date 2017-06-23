package com.ucloud.ulive.example.utils;

import android.util.Log;

import static android.content.ContentValues.TAG;

public class AverageAudioMixer {

    public byte[] scale(byte[] orignBuff, int size, float volumeScale) {
        for (int i = 0; i < size; i += 2) {
            short origin = (short) (((orignBuff[i + 1] << 8) | orignBuff[i] & 0xff));
            origin = (short) (origin * volumeScale);
            orignBuff[i + 1] = (byte) (origin >> 8);
            orignBuff[i] = (byte) (origin);
        }
        return orignBuff;
    }

    public byte[] mixRawAudioBytes(byte[][] bMulRoadAudioes) {

        if (bMulRoadAudioes == null || bMulRoadAudioes.length == 0) {
            return null;
        }

        byte[] realMixAudio = bMulRoadAudioes[0];

        if (bMulRoadAudioes.length == 1) {
            return realMixAudio;
        }

        for (int rw = 0; rw < bMulRoadAudioes.length; ++rw) {
            if (bMulRoadAudioes[rw].length != realMixAudio.length) {
                Log.e(TAG, "lifecycle->demo->AverageAudioMixer->column of the road of audio + " + rw + " is diffrent!");
                return null;
            }
        }

        int row = bMulRoadAudioes.length;

        int column = realMixAudio.length / 2;

        short[][] sMulRoadAudioes = new short[row][column];

        for (int r = 0; r < row; ++r) {
            for (int c = 0; c < column; ++c) {
                sMulRoadAudioes[r][c] = (short) ((bMulRoadAudioes[r][c * 2] & 0xff) | (bMulRoadAudioes[r][c * 2 + 1] & 0xff) << 8);
            }
        }

        short[] sMixAudio = new short[column];
        int mixVal;
        int sr;
        for (int sc = 0; sc < column; ++sc) {
            mixVal = 0;
            sr = 0;
            for (; sr < row; ++sr) {
                mixVal += sMulRoadAudioes[sr][sc];
            }
            sMixAudio[sc] = (short) (mixVal / row);
        }

        for (sr = 0; sr < column; ++sr) {
            realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }

        return realMixAudio;
    }
}
