package com.ucloud.ulive.example.filter.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import java.util.LinkedList;

/**
 * @author lw.tan on 2016/10/25.
 */
class URawAudioPlayer {

    private static final String TAG = "URawAudioPlayer";

    private static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;

    private static final int SAMPLE_RATE_IN_HZ = 44100;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final Object syncObject = new Object();

    private final LinkedList<byte[]> audioBuffer = new LinkedList<>();

    private final LinkedList<byte[]> audioPool = new LinkedList<>();

    private boolean isRunning;

    private final int bufferSize;

    URawAudioPlayer(int bufferSize) {
        isRunning = false;
        this.bufferSize = bufferSize;
    }

    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        synchronized (syncObject) {
            audioBuffer.clear();
        }
        PlayThread playThread = new PlayThread(bufferSize);
        playThread.start();
    }

    public void stop() {
        isRunning = false;
    }

    public void sendAudio(byte[] data) {
        synchronized (syncObject) {
            if (!isRunning) {
                return;
            }
            byte[] tempData = get(data.length);
            System.arraycopy(data, 0, tempData, 0, tempData.length);
            audioBuffer.add(tempData);
        }
    }

    public boolean isPlaying() {
        return isRunning;
    }

    private final class PlayThread extends Thread {

        private final int bufferSize;

        private PlayThread(int bufferSize) {
            super("URawAudioPlayer-PlayThread");
            this.bufferSize = bufferSize;
        }

        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            AudioTrack audioTrack = null;
            while (isRunning && (audioTrack == null || (audioTrack.getState() != AudioRecord.STATE_INITIALIZED))) {
                if (audioTrack != null) {
                    audioTrack.release();
                }
                audioTrack = new AudioTrack(STREAM_TYPE, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);
                yield();
            }
            audioTrack.play();
            while (isRunning) {
                synchronized (syncObject) {
                    if (!audioBuffer.isEmpty()) {
                        byte[] data = audioBuffer.removeFirst();
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
            if (audioPool.size() > 0) {
                return audioPool.removeFirst();
            }
            else {
                return new byte[size];
            }
        }
    }

    private void release(byte[] data) {
        synchronized (syncObject) {
            if (audioPool.size() < 2) {
                audioPool.add(data);
            }
        }
    }
}
