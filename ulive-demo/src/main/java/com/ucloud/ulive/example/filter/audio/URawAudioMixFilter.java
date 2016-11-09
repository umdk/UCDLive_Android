package com.ucloud.ulive.example.filter.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import com.ucloud.ulive.filter.UAudioCPUFilter;
import java.io.IOException;
import java.io.InputStream;

public class URawAudioMixFilter extends UAudioCPUFilter {

    public static final String TAG = "URawAudioMixFilter";

    private InputStream bgmInputStream;

    private byte[] bgm;

    private AverageAudioMixer averageAudioMixer;

    private Context mContext;

    private float bgmVolumeLevel = 1.0f;

    private float miscVolumeLevel = 1.0f;

    private URawAudioPlayer mRawAudioPlayer;

    private boolean isLooper = false;

    private boolean isMixBgm = false;

    private HeadsetPlugReceiver headsetPlugReceiver;

    private Mode mixMode = Mode.JUST_HEADSET_ON;

    public enum Mode {
        JUST_HEADSET_ON,     //mix when headset on, if not mix by misc(when play)
        ANY,                 //all mix
    }

    public URawAudioMixFilter(Context context, Mode mixMode, boolean isLooper) {
        mContext = context;
        this.isLooper = isLooper;
        this.mixMode = mixMode;
        headsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter  filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        mContext.registerReceiver(headsetPlugReceiver, filter);
        if (mixMode == Mode.JUST_HEADSET_ON) {
            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            isMixBgm = audioManager.isWiredHeadsetOn();
        } else {
            isMixBgm = true;
        }
    }

    @Override
    public void onInit(int size) {
        super.onInit(size);
        init();
        mRawAudioPlayer = new URawAudioPlayer(size);
    }

    private void init() {
        try {
            bgmInputStream = mContext.getAssets().open("raw_audio_mix_bg.pcm");
            bgmInputStream.mark(bgmInputStream.available());
            bgm = new byte[SIZE];
            averageAudioMixer = new AverageAudioMixer();
        } catch (Exception e) {
            bgmInputStream = null;
            Log.e(TAG, "lifecycle->demo->URawAudioMixFilter->onInit failed.");
        }
    }

    //just support PCM s16le, background music pcm samplerate and channels must be same with UAudioProfile.
    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        try {
            if (bgmInputStream == null || bgmInputStream.read(bgm, 0, SIZE) < SIZE) {
                handleReadException();
                return false;
            }
        } catch (Exception e) {
            handleReadException();
            return false;
        }

        if (mRawAudioPlayer != null) {
            if (bgm != null) {
                if (!mRawAudioPlayer.isPlaying()) {
                    mRawAudioPlayer.start();
                }
                mRawAudioPlayer.sendAudio(bgm);
            } else {
                mRawAudioPlayer.stop();
            }
        }

        if (isMixBgm) {
            byte[] bytes = averageAudioMixer.mixRawAudioBytes(new byte[][]{scale(bgm, SIZE, bgmVolumeLevel), scale(orignBuff, SIZE, miscVolumeLevel)});
            System.arraycopy(bytes, 0, targetBuff, 0, bytes.length);
            return true;
        } else {
            return false;
        }
    }

    private void handleReadException() {
        if (isLooper) {
            init();
        }
        if (mRawAudioPlayer != null) {
            mRawAudioPlayer.stop();
        }
    }

    private byte[] scale(byte[] orignBuff, int size, float volumeScale) {
      for (int i = 0; i < size; i += 2) {
          short origin = (short) (((orignBuff[i + 1] << 8) | orignBuff[i] & 0xff));
          origin = (short) (origin * volumeScale);
          orignBuff[i + 1] = (byte) (origin >> 8);
          orignBuff[i] = (byte) (origin);
      }
      return orignBuff;
    }

    public void adjustBackgroundMusicVolumeLevel(float level) {
        bgmVolumeLevel = level;
    }

    public void adjustMiscVolumeLevel(float level) {
        this.miscVolumeLevel = level;
    }

    private static class AverageAudioMixer {

        byte[] mixRawAudioBytes(byte[][] bMulRoadAudioes) {

            if (bMulRoadAudioes == null || bMulRoadAudioes.length == 0)
                return null;

            byte[] realMixAudio = bMulRoadAudioes[0];

            if(bMulRoadAudioes.length == 1) {
                return realMixAudio;
            }

            for(int rw = 0 ; rw < bMulRoadAudioes.length ; ++rw){
                if(bMulRoadAudioes[rw].length != realMixAudio.length){
                    Log.e(TAG, "lifecycle->demo->URawAudioMixFilter->column of the road of audio + " + rw +" is diffrent!");
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (bgmInputStream != null) {
                bgmInputStream.close();
                bgmInputStream = null;
            }
            if (mContext != null) {
                mContext.unregisterReceiver(headsetPlugReceiver);
            }
            mContext = null;
            bgm = null;
            averageAudioMixer = null;
            if (mRawAudioPlayer != null) {
                mRawAudioPlayer.stop();
                mRawAudioPlayer = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class HeadsetPlugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("state")){
                int state =  intent.getIntExtra("state", 0);
                if(0 == state) {
                    Log.e(TAG, "lifecycle->audio->demo->HeadsetPlugReceiver->headset not connected");
                }
                else if(1 == state) {
                    Log.e(TAG, "lifecycle->audio->demo->HeadsetPlugReceiver->headset connected");
                }
                if (mixMode == Mode.JUST_HEADSET_ON) {
                   if (state == 0) {
                       isMixBgm = false;
                   } else {
                       isMixBgm = true;
                   }
                }
            }
        }
    }
}
