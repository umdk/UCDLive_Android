package com.ucloud.ulive.example.ext.agora;


import com.ucloud.ulive.framework.AudioBufferFormat;
import com.ucloud.ulive.framework.AudioBufferFrame;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public final class RemoteAudioCacheUtil {

    private Queue<AudioBufferFrame> tempAudioBufferFrames;
    private Queue<AudioBufferFrame> remoteAudioBufferFrames;
    private int maxAudioBufferFrameNums;
    private int maxBufferSize;
    private AudioBufferFormat format;

    private static RemoteAudioCacheUtil INSTANCE;

    private RemoteAudioCacheUtil() {

    }

    public static synchronized RemoteAudioCacheUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RemoteAudioCacheUtil();
        }
        return INSTANCE;
    }

    public void init(AudioBufferFormat format, int maxQueueSize, int maxBufferSize) {
        this.format = format;
        this.maxAudioBufferFrameNums = maxQueueSize;
        this.maxBufferSize = maxBufferSize;
        clear();
        if (tempAudioBufferFrames == null) {
            tempAudioBufferFrames = new LinkedBlockingQueue<>(maxAudioBufferFrameNums);
        }

        if (remoteAudioBufferFrames == null) {
            remoteAudioBufferFrames = new LinkedBlockingQueue<>();
        }
    }

    private void clear() {
        if (tempAudioBufferFrames != null && tempAudioBufferFrames.size() >= 1) {
            tempAudioBufferFrames.clear();
        }
        if (remoteAudioBufferFrames != null) {
            remoteAudioBufferFrames.clear();
        }
    }

    public AudioBufferFrame poll() {
        if (tempAudioBufferFrames != null) {
            AudioBufferFrame audioBufferFrame = tempAudioBufferFrames.poll();
            if (audioBufferFrame != null) {
                return audioBufferFrame;
            }
            else {
                audioBufferFrame = new AudioBufferFrame(format, this.maxBufferSize);
                return audioBufferFrame;
            }
        }
        else {
            return null;
        }
    }

    public RemoteAudioCacheUtil restore(AudioBufferFrame frame) {
        if (tempAudioBufferFrames != null && frame != null && tempAudioBufferFrames.size() < maxAudioBufferFrameNums && frame.buffer.limit() == maxBufferSize) {
            tempAudioBufferFrames.offer(frame);
        }
        else {
            if (frame != null) {
                frame.buffer = null;
            }
        }
        return this;
    }

    public void cacheRemoteAudio(AudioBufferFrame remoteAudioBufferFrame) {
        if (remoteAudioBufferFrames !=  null) {
            remoteAudioBufferFrames.offer(remoteAudioBufferFrame);
        }
    }

    public AudioBufferFrame getRemoteAudio() {
        if (remoteAudioBufferFrames != null) {
            return remoteAudioBufferFrames.poll();
        }
        else {
            return null;
        }
    }

    public void release() {
        clear();
    }
}
