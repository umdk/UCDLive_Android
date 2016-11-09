package com.ucloud.ulive.example.filter.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import java.util.LinkedList;

/**
 * Created by lw.tan on 2016/10/25.
 */

public class URawAudioPlayer {

    private static final String TAG = "URawAudioPlayer";

    private final static int STREAM_TYPE = AudioManager.STREAM_MUSIC;

    private final static int SAMPLE_RATE_IN_HZ = 44100;

    private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;

    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final Object syncObject = new Object();

    private LinkedList<byte[]> mAudioBuffer = new LinkedList<>();

    private LinkedList<byte[]> mAudioPool = new LinkedList<>();

    private boolean mIsRunning;

    private int mBufferSize;

    public URawAudioPlayer(int bufferSize) {
        mIsRunning = false;
        mBufferSize = bufferSize;
    }

    public void start() {
        if (mIsRunning) {
            return;
        }
        mIsRunning = true;
        synchronized (syncObject) {
            mAudioBuffer.clear();
        }
        PlayThread playThread = new PlayThread(mBufferSize);
        playThread.start();
    }

    public void stop() {
        mIsRunning = false;
    }

    public void sendAudio(byte[] data) {
        synchronized (syncObject) {
            if (!mIsRunning) {
                return;
            }
            byte[] tempData = get(data.length);
            System.arraycopy(data, 0, tempData, 0, tempData.length);
            mAudioBuffer.add(tempData);
        }
    }

    public boolean isPlaying() {
        return mIsRunning;
    }

    private class PlayThread extends Thread {

        private int bufferSize;

        public PlayThread(int bufferSize) {
            super("URawAudioPlayer-PlayThread");
            this.bufferSize = bufferSize;
        }

        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            AudioTrack audioTrack = null;
            while (mIsRunning && (audioTrack == null || (audioTrack.getState() != AudioRecord.STATE_INITIALIZED))) {
                if (audioTrack != null) {
                    audioTrack.release();
                }
                audioTrack = new AudioTrack(STREAM_TYPE, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);
                yield();
            }
            audioTrack.play();
            while (mIsRunning) {
                synchronized (syncObject) {
                    if (!mAudioBuffer.isEmpty()) {
                        byte[] data = mAudioBuffer.removeFirst();
                        audioTrack.write(data, 0, data.length);
                        release(data);
                    }
                }
            }
            audioTrack.stop();
            audioTrack.release();
        }
    }

    private byte[] get(int size) {
        synchronized (syncObject) {
            if (mAudioPool.size() > 0) {
                byte[] bytes = mAudioPool.removeFirst();
                return bytes;
            } else {
                byte[] bytes = new byte[size];
                return bytes;
            }
        }
    }

    private void release(byte[] data) {
        synchronized (syncObject) {
            if (mAudioPool.size() < 2) {
               mAudioPool.add(data);
            }
        }
    }
}
