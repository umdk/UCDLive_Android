package com.ucloud.ulive.example.ext.agora;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.R;
import com.ucloud.ulive.framework.AudioBufferFormat;

import java.util.ArrayList;
import java.util.List;

import io.agora.extvideo.AgoraVideoSource;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

public class MediaManager extends IRtcEngineEventHandler {

    private static final String TAG = MediaManager.class.getSimpleName();

    private final Context context;

    private final List<MediaUiHandler> uiHandlers = new ArrayList<>(3);

    private RtcEngine rtcEngine;

    private String channelId;

    private AgoraVideoSource videoSource;

    private int videoProfile = VideoProfile.VIDEO_PROFILE_240P;  //320 * 240

    private boolean swapWidth = true;

    private final AudioBufferFormat localAudioBufferFormat;

    private final AudioBufferFormat remoteAudioBufferFormat;

    public interface MediaUiHandler {
        int JOIN_CHANNEL_RESULT = 1;

        int FIRST_FRAME_DECODED = 2;

        int ON_VENDOR_MSG = 3;

        int USER_JOINED = 4;

        int ERROR = 5;

        int WARN = 6;

        int LEAVE_CHANNEL = 7;

        int USER_OFFLINE = 8;

        void onMediaEvent(int event, Object... data);
    }

    MediaManager(Context context, AudioBufferFormat localAudioBufFormat, AudioBufferFormat remoteAudioBufFormat, int orientation) {
        this.context = context;
        this.localAudioBufferFormat = localAudioBufFormat;
        this.remoteAudioBufferFormat = remoteAudioBufFormat;
        swapWidth = orientation != UVideoProfile.ORIENTATION_LANDSCAPE;
        init();
    }


    public void release() {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    private void init() {
        String appId = context.getString(R.string.app_id);

        if (TextUtils.isEmpty(appId)) {
           Log.e(TAG, "Please set your app_id to strings.app_id");
           return;
        }

        Log.d(TAG, "init " + appId + ", sdk version:" +  RtcEngine.getSdkVersion());

        rtcEngine = RtcEngine.create(context, appId, this);
        videoSource = new AgoraVideoSource(); // define main class for customize video source
        videoSource.Attach();
        rtcEngine.enableVideo();
    }

    public void registerUiHandler(MediaUiHandler uiHandler) {
        if (!uiHandlers.contains(uiHandler)) {
            uiHandlers.add(uiHandler);
        }
    }

    public void unRegisterUiHandler(MediaUiHandler uiHandler) {
        uiHandlers.remove(uiHandler);
    }

    public void setVideoProfile(int profile, boolean swap) {
        videoProfile = profile;
        swapWidth = swap;
    }

    public void joinChannel(final String channelId, int uid) {
        Log.d(TAG, "joinChannel " + channelId);

        this.channelId = channelId;
        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
//https://docs.agora.io/cn/1.9/user_guide/API/android_api_live.html?highlight=sethighqualityaudioparameters
//https://docs.agora.io/cn/1.9/user_guide/API/raw_data_api.html#setrecordingaudioframeparameters
//https://docs.agora.io/cn/1.9/user_guide/troubleshooting/error.html
        rtcEngine.setRecordingAudioFrameParameters(localAudioBufferFormat.sampleRate, localAudioBufferFormat.channels, Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, localAudioBufferFormat.sampleRate / 10);
        rtcEngine.setPlaybackAudioFrameParameters(remoteAudioBufferFormat.sampleRate, remoteAudioBufferFormat.channels, Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, remoteAudioBufferFormat.sampleRate / 10);
        rtcEngine.setVideoProfile(videoProfile, swapWidth);
        rtcEngine.setClientRole(ClientRole.CLIENT_ROLE_BROADCASTER, null);
        rtcEngine.joinChannel(null, channelId, null, uid);
    }

    public void leaveChannel() {
        Log.d(TAG, "leaveChannel " + channelId);
        if (rtcEngine != null) {
            rtcEngine.stopAudioRecording();
            rtcEngine.leaveChannel();
            rtcEngine.stopPreview();
            channelId = null;
        }
    }

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        Log.d(TAG, "onFirstRemoteVideoDecoded uid = " + uid + " width = " + width + " height = " + height + " " + " elapsed = " + elapsed);
        for (MediaUiHandler uiHandler : uiHandlers) {
            uiHandler.onMediaEvent(MediaUiHandler.FIRST_FRAME_DECODED, uid, width, height, elapsed);
        }
    }

    @Override
    public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
        Log.d(TAG, "onFirstLocalVideoFrame " + " " + width + " " + height + " " + elapsed);
    }

    //用户进入
    @Override
    public void onUserJoined(int uid, int elapsed) {
        Log.d(TAG, "onUserJoined " + " uid = " + uid + " " + elapsed + " " + elapsed);
        for (MediaUiHandler uiHandler : uiHandlers) {
            uiHandler.onMediaEvent(MediaUiHandler.USER_JOINED, uid);
        }
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        Log.d(TAG, "onUserOffline " + " uid = " + uid + " " + reason + " " + reason);
        for (MediaUiHandler uiHandler : uiHandlers) {
            uiHandler.onMediaEvent(MediaUiHandler.USER_OFFLINE, uid, reason);
        }
    }

    //监听其他用户是否关闭视频
    @Override
    public void onUserMuteVideo(int uid, boolean muted) {
    }

    //更新聊天数据
    @Override
    public void onRtcStats(RtcStats stats) {
    }

    @Override
    public void onLeaveChannel(RtcStats stats) {
        Log.d(TAG, "onLeaveChannel " + " uid = " + stats.users);
        for (MediaUiHandler uiHandler : uiHandlers) {
            uiHandler.onMediaEvent(MediaUiHandler.LEAVE_CHANNEL, stats.users);
        }
    }

    @Override
    public void onError(int err) {
        super.onError(err);
        Log.d(TAG, "onError " + err);

        for (MediaUiHandler uiHandler : uiHandlers) {
            uiHandler.onMediaEvent(MediaUiHandler.ERROR, err);
        }
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        Log.d(TAG, "onJoinChannelSuccess " + channel + " " + uid + " " + elapsed);

        for (MediaUiHandler uiHandler : uiHandlers) {
            uiHandler.onMediaEvent(MediaUiHandler.JOIN_CHANNEL_RESULT, channel, uid, elapsed);
        }
    }

    public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
        Log.d(TAG, "onRejoinChannelSuccess " + channel + " " + uid + " " + elapsed);
    }

    //https://docs.agora.io/cn/1.9/user_guide/API/android_api.html
    public void onWarning(int warn) {
        Log.d(TAG, "onWarning " + warn);

        for (MediaUiHandler uiHandler : uiHandlers) {
            uiHandler.onMediaEvent(MediaUiHandler.WARN, warn);
        }
    }

    public void onMediaEngineEvent(int code) {
        Log.d(TAG, "onMediaEngineEvent " + code);
    }

    public void onVendorMessage(int uid, byte[] data) {
        Log.d(TAG, "onVendorMessage " + uid + " " + data.toString());

        for (MediaUiHandler uiHandler : uiHandlers) {
            uiHandler.onMediaEvent(MediaUiHandler.ON_VENDOR_MSG, uid, data);
        }
    }

    public AgoraVideoSource getVideoSource() {
        return videoSource;
    }

    public RtcEngine getRtcEngine() {
        return rtcEngine;
    }
}
