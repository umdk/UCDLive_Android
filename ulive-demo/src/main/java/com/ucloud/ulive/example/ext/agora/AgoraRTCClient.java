package com.ucloud.ulive.example.ext.agora;


import com.ucloud.ulive.example.widget.LiveCameraView;
import com.ucloud.ulive.framework.AudioBufferFormat;

public class AgoraRTCClient {

    private final MediaManager mediaManager;
    private RemoteDataObserver remoteDataObserver;
    private final AudioBufferFormat localAudioBufFormat;
    private final AudioBufferFormat remoteAudioBufFormat;

    public AgoraRTCClient(MediaManager mediaManager, AudioBufferFormat localAudioBufFormat, AudioBufferFormat remoteAudioBufFormat) {
        this.mediaManager = mediaManager;
        this.localAudioBufFormat = localAudioBufFormat;
        this.remoteAudioBufFormat = remoteAudioBufFormat;

    }

    public void joinChannel(String channelId, int uid) {
        if (remoteDataObserver == null) {
            remoteDataObserver = new RemoteDataObserver(localAudioBufFormat, remoteAudioBufFormat);
        }
        enableObserver(true);
        mediaManager.joinChannel(channelId, uid);
    }

    public void leaveChannel() {
        enableObserver(false);
        mediaManager.leaveChannel();
        LiveCameraView.getEasyStreaming().clearRemoteVideoFrame(0); //0 清除所有， 其它id 清除指定的窗口
    }

    private void enableObserver(boolean enable) {
        if (remoteDataObserver != null) {
            remoteDataObserver.enableObserver(enable);
        }
    }

    public void release() {
        if (remoteDataObserver != null) {
            remoteDataObserver.release();
            remoteDataObserver = null;
        }
        if (mediaManager != null) {
            mediaManager.release();
        }
    }

    public void startReceiveRemoteData() {
        if (remoteDataObserver != null) {
            remoteDataObserver.startReceiveRemoteData();
        }
    }

    public void stopReceiveRemoteData() {
        if (remoteDataObserver != null) {
            remoteDataObserver.stopReceiveRemoteData();
        }
    }

    public void resetRemoteUid(int uid) {
        if (remoteDataObserver != null) {
            remoteDataObserver.resetRemoteUid(uid);
        }
    }
}
