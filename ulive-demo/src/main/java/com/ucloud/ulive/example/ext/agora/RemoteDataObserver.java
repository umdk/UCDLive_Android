package com.ucloud.ulive.example.ext.agora;

import android.util.Log;

import com.ucloud.ulive.UPosition;
import com.ucloud.ulive.example.widget.LiveCameraView;
import com.ucloud.ulive.framework.AudioBufferFormat;
import com.ucloud.ulive.framework.AudioBufferFrame;
import com.ucloud.ulive.framework.ImageBufferFormat;
import com.ucloud.ulive.framework.ImageBufferFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

public class RemoteDataObserver {
    private static final String TAG = "RemoteDataObserver";

    private long observerInstance = UNINIT;

    private static final int UNINIT = -1;

    private volatile boolean receivingRemoteData = false;

    private ByteBuffer localAudioBuffer;

    private AudioBufferFormat localAudioBufFormat;

    private AudioBufferFormat remoteAudioBufFormat;

    private long count = 0;

    private UPosition[] positions;

    private Map<Integer, ByteBuffer> remoteCacheBuffers;
    private Map<Integer, ImageBufferFormat> remoteVideoFormats;

    public RemoteDataObserver(AudioBufferFormat localAudioBufFormat, AudioBufferFormat remoteAudioBufFormat) {
        this.localAudioBufFormat = localAudioBufFormat;
        this.remoteAudioBufFormat = remoteAudioBufFormat;
        observerInstance = createObserver();

        UPosition remoteVideoPosition1 = new UPosition();
        remoteVideoPosition1.setPosition(UPosition.BOTTOM_RIGHT);
        remoteVideoPosition1.setHorizontalMargin(20);
        remoteVideoPosition1.setVerticalMargin(20);

        UPosition remoteVideoPosition2 = new UPosition();
        remoteVideoPosition2.setPosition(UPosition.BOTTOM_RIGHT);
        remoteVideoPosition2.setHorizontalMargin(20);
        remoteVideoPosition2.setVerticalMargin(600);

        UPosition remoteVideoPosition3 = new UPosition();
        remoteVideoPosition3.setPosition(UPosition.BOTTOM_RIGHT);
        remoteVideoPosition3.setHorizontalMargin(20);
        remoteVideoPosition3.setVerticalMargin(1200);

        positions = new UPosition[] {remoteVideoPosition1, remoteVideoPosition2, remoteVideoPosition3};

        remoteCacheBuffers = new LinkedHashMap<>();
        remoteVideoFormats = new LinkedHashMap<>();
    }

    public void release() {
        if (observerInstance == UNINIT) {
            Log.d(TAG, "have been released");
            return;
        }
        release(observerInstance);
        observerInstance = UNINIT;
    }

    public void enableObserver(boolean enable) {
        if (observerInstance == UNINIT) {
            Log.d(TAG, "have been released");
            return;
        }
        enableObserver(observerInstance, enable);
    }

    private native long createObserver();

    private native void release(long wrapperInstance);

    private native void enableObserver(long wrapperInstance, boolean enable);

    private native void resetRemoteUid(long wrapperInstance, int uid);

    private Object syncObject = new Object();

    public void onVideoFrame(int uid, int index, ByteBuffer buffer, int size, int width, int height, int orientation, double pts) {

        ByteBuffer videoDirectBuffer = remoteCacheBuffers.get(uid);

        if (videoDirectBuffer == null) {
            videoDirectBuffer = ByteBuffer.allocateDirect(size);
            remoteCacheBuffers.put(uid, videoDirectBuffer);
        }

        ImageBufferFormat imgBufFormat = remoteVideoFormats.get(uid);

        if (imgBufFormat == null) {
            imgBufFormat = new ImageBufferFormat(ImageBufferFormat.RGBA, width, height, orientation);
            remoteVideoFormats.put(uid, imgBufFormat);
        }

        if (imgBufFormat.width != width || imgBufFormat.height != height || imgBufFormat.orientation != orientation) {
            imgBufFormat.width = width;
            imgBufFormat.height = height;
            imgBufFormat.orientation = orientation;
            videoDirectBuffer.clear();
            videoDirectBuffer = ByteBuffer.allocateDirect(size);
            remoteCacheBuffers.put(uid, videoDirectBuffer);
            remoteVideoFormats.put(uid, imgBufFormat);
        }

        if (receivingRemoteData) {
            videoDirectBuffer.clear();
            videoDirectBuffer.put(buffer);
            videoDirectBuffer.rewind();
            //当前仅支持RGBA
            ImageBufferFrame bufFrame = new ImageBufferFrame(imgBufFormat, videoDirectBuffer, (long) pts);
            Log.d(TAG, "onVideoFrame: uid =  " + uid + " index = " + index + ", width = " + width + ", height = " + height + ", orientation = " + orientation + ", pts = " + pts);;
            if (index != -1) {
                LiveCameraView.getEasyStreaming().deliverRemoteVideoFrame(uid, bufFrame, positions[index]);
                //说明 LiveCameraView.getEasyStreaming().deliverRemoteVideoFrame(bufFrame, positions[index]);方法已经过时
            }
        }
    }

    public void onAudioFrame(ByteBuffer buffer, int length, int bytesPerSample, int sampleRate,
                             int channels, double pts, boolean remote) {
        count++;
        if (receivingRemoteData) {
            if (count % 200 == 0) {
//                Log.d(TAG, "onAudioFrame: length = " + length + ", bytesPerSample = " + bytesPerSample + ", sampleRate = " + sampleRate + ", channels = " + channels + ", pts = " + pts + ",remote = " + remote);
            }
            if (remote) {
                onRemoteAudioFrame(buffer, length, bytesPerSample, sampleRate, channels, pts);
            }
            else {
                onLocalAudioFrame(buffer, length, bytesPerSample, sampleRate, channels, pts);
            }
        }
    }

    private void onRemoteAudioFrame(ByteBuffer buffer, int length, int bytesPerSample, int sampleRate, int channels, double pts) {
        AudioBufferFrame audioBufferFrame = RemoteAudioCacheUtil.getInstance().poll();
        if (audioBufferFrame == null) {
            Log.d(TAG, "mix->remote->audio->poll->failed.");
        } else {
            audioBufferFrame.buffer.put(buffer);
            audioBufferFrame.buffer.flip();
            RemoteAudioCacheUtil.getInstance().cacheRemoteAudio(audioBufferFrame);
        }
    }

    private void onLocalAudioFrame(ByteBuffer buffer, int length, int bytesPerSample,
                                   int sampleRate, int channels, double pts) {
        if (localAudioBuffer == null) {
            localAudioBuffer = ByteBuffer.allocateDirect(length);
            localAudioBuffer.order(ByteOrder.nativeOrder());
            int sampleFormat = AudioBufferFormat.FORMAT_PCM_S16BIT;
            if (bytesPerSample == 2) {
                sampleFormat = AudioBufferFormat.FORMAT_PCM_S16BIT;
            }
            localAudioBufFormat = new AudioBufferFormat(sampleFormat, sampleRate, channels);
        }
        else if (localAudioBufFormat.sampleRate != sampleRate
                || localAudioBufFormat.channels != channels) {
            int sampleFormat = AudioBufferFormat.FORMAT_PCM_S16BIT;
            if (bytesPerSample == 2) {
                sampleFormat = AudioBufferFormat.FORMAT_PCM_S16BIT;
            }
            localAudioBufFormat = new AudioBufferFormat(sampleFormat, sampleRate, channels);
            localAudioBuffer.clear();
            localAudioBuffer = null;
            localAudioBuffer = ByteBuffer.allocateDirect(length);
        }
        localAudioBuffer.clear();
        localAudioBuffer.put(buffer);
        localAudioBuffer.flip();
        AudioBufferFrame bufFrame = new AudioBufferFrame(localAudioBufFormat, localAudioBuffer, (long) pts);
        LiveCameraView.getEasyStreaming().deliverLocalAudioFrame(bufFrame);
    }

    static {
        try {
            System.loadLibrary("apm-remote-data-observer");
        }
        catch (UnsatisfiedLinkError error) {
            Log.e(TAG, "No libapm-remote-data-observer.so! Please check");
        }
    }

    public void startReceiveRemoteData() {
        receivingRemoteData = true;
    }

    public void stopReceiveRemoteData() {
        receivingRemoteData = false;
        LiveCameraView.getEasyStreaming().deliverLocalAudioFrame(null);
    }

    public void resetRemoteUid(int uid) {
        resetRemoteUid(observerInstance, uid);
    }
}
