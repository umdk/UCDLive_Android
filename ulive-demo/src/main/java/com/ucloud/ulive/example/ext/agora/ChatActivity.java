package com.ucloud.ulive.example.ext.agora;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.ucloud.ulive.UAudioProfile;
import com.ucloud.ulive.UEasyStreaming;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.MainActivity;
import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.ext.agora.filter.URemoteAudioMixFilter;
import com.ucloud.ulive.example.widget.LiveCameraView;
import com.ucloud.ulive.framework.AudioBufferFormat;
import com.ucloud.ulive.framework.ImageBufferFormat;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ChatActivity extends Activity {

    private static final String TAG = "ChatActivity";

    @Bind(R.id.livecamera)
    LiveCameraView cameraPreview;

    @Bind(R.id.txtv_stream_id)
    TextView streamIdTextv;

    private AVOption avOption;
    private AgoraRTCClient agoraRTCClient;
    private MediaManager mediaManager;
    protected String encryptionKey;
    protected String encryptionMode;
    protected AudioBufferFormat localAudioBufferFormat;
    protected AudioBufferFormat remoteAudioBufferFormat;

    private SpecailEffectHolder specailEffectHolder;

    class SpecailEffectHolder {
        boolean line;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        initConfig();
        specailEffectHolder = new SpecailEffectHolder();
        localAudioBufferFormat = new AudioBufferFormat(UAudioProfile.FORMAT_PCM_16BIT, UAudioProfile.SAMPLE_RATE_44100_HZ, UAudioProfile.CHANNEL_IN_STEREO);
        remoteAudioBufferFormat = new AudioBufferFormat(UAudioProfile.FORMAT_PCM_16BIT, UAudioProfile.SAMPLE_RATE_44100_HZ, UAudioProfile.CHANNEL_IN_STEREO);
        mediaManager = new MediaManager(this, localAudioBufferFormat, remoteAudioBufferFormat, avOption.videoCaptureOrientation);
        mediaManager.registerUiHandler(new MediaManager.MediaUiHandler() {
            @Override
            public void onMediaEvent(int event, Object... data) {
                Log.d(TAG, "onMediaEvent event = " + event);
                switch (event) {
                    case JOIN_CHANNEL_RESULT:
                        Log.d(TAG, "join channel: channel = " + data[0] + " uid = " + data[1] + " elapsed = " + data[2]);
                        break;
                    case FIRST_FRAME_DECODED:
                        Log.d(TAG, "first frame decoded: uid = " + data[0] + " width = " + data[1] + " height = " + data[2] + " elapsed = " + data[3]);
                        break;
                    case USER_JOINED:
                        specailEffectHolder.line = true; //简单维护双方是否处于在连麦状态
                        setOnProcessedFrameListener();
                        agoraRTCClient.startReceiveRemoteData();
                        Log.d(TAG, "user joined: uid = " + data[0] + " elapsed = " + data[1]);
                        break;
                    case WARN:
                        Log.d(TAG, "warn: msg = " + data[0]);
                        break;
                    case ERROR:
                        Log.d(TAG, "error: msg = " + data[0]);
                        break;
                    case LEAVE_CHANNEL:
                        Log.d(TAG, "leave channel uid = " + data[0]);
                        break;
                    case USER_OFFLINE:
                        specailEffectHolder.line = false;
                        //发现连麦用户掉线时，清除小窗口
//                        LiveCameraView.getEasyStreaming().setOnProcessedFrameListener(null);
                        Log.d(TAG, "user offline: uid = " + data[0] + " reason = " + data[1]);
                        break;
                    default:
                        break;
                }
            }
        });
        agoraRTCClient = new AgoraRTCClient(mediaManager, localAudioBufferFormat, remoteAudioBufferFormat);
        RemoteAudioCacheUtil.getInstance().init(remoteAudioBufferFormat, 10, remoteAudioBufferFormat.sampleRate / 10 * 2); //内部每笔音频占用的大小，保持一致
        cameraPreview.init(avOption);
    }

    private void initConfig() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent i = getIntent();
        avOption = new AVOption();
        avOption.streamUrl = i.getStringExtra(MainActivity.KEY_STREAMING_ADDRESS);
        if (TextUtils.isEmpty(avOption.streamUrl)) {
            Toast.makeText(this, "RTMP推流URL不能为null.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        avOption.videoFilterMode = i.getIntExtra(MainActivity.KEY_FILTER, UFilterProfile.FilterMode.GPU);
        avOption.videoCodecType = UVideoProfile.CODEC_MODE_HARD;
        avOption.videoCaptureOrientation = i.getIntExtra(MainActivity.KEY_CAPTURE_ORIENTATION, UVideoProfile.ORIENTATION_PORTRAIT);
        avOption.videoFramerate = i.getIntExtra(MainActivity.KEY_FPS, 20);
        avOption.videoBitrate = i.getIntExtra(MainActivity.KEY_VIDEO_BITRATE, UVideoProfile.VIDEO_BITRATE_NORMAL);
        avOption.videoResolution = i.getIntExtra(MainActivity.KEY_VIDEO_RESOLUTION, UVideoProfile.Resolution.RATIO_AUTO.ordinal());
        avOption.audioChannels = UAudioProfile.CHANNEL_IN_STEREO;
        avOption.audioSampleRate = UAudioProfile.SAMPLE_RATE_44100_HZ;
        avOption.audioSource = UAudioProfile.AUDIO_SOURCE_DEFAULT;

        if (avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        avOption.streamId = i.getStringExtra(MainActivity.KEY_STREAMING_ID);
        streamIdTextv.setText(avOption.streamId);

        encryptionKey = getIntent().getStringExtra(MainActivity.KEY_ENCRYPTION_KEY);
        encryptionMode = getIntent().getStringExtra(MainActivity.KEY_ENCRYPTION_MODE);
    }

    @Override
    public void onStop() {
        super.onStop();
        //切后台，停止接受本地视频回调数据
        cameraPreview.onPause();
        handleLeaveChannel(); //发送退出房间信息, 如果连麦双方都切后台，下次需要各自joinChannel
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraPreview.onResume();
        if (specailEffectHolder.line) {
            handleJoinChannel(); //重新加入房间
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraPreview.onDestroy();
        RemoteAudioCacheUtil.getInstance().release();
        specailEffectHolder.line = false;
    }

    public void onJoinChannel(View view) {
        String appId = getString(R.string.app_id);
        if (TextUtils.isEmpty(appId)) {
            Toast.makeText(this, "请在strings.xml当中设置strings.app_id", Toast.LENGTH_LONG).show();
            return;
        }
        handleJoinChannel();
    }

    public void onStartRtmp(View view) {
        cameraPreview.startRecording();
    }

    private void handleJoinChannel() {
        setOnProcessedFrameListener();
        agoraRTCClient.joinChannel(avOption.streamId, 0);
        agoraRTCClient.startReceiveRemoteData();
        URemoteAudioMixFilter remoteAudioMixFilter = new URemoteAudioMixFilter();
        cameraPreview.setAudioCPUFilter(remoteAudioMixFilter);
    }

    private void handleLeaveChannel() {
        LiveCameraView.getEasyStreaming().setOnProcessedFrameListener(null);
        cameraPreview.setAudioCPUFilter(null);
        agoraRTCClient.stopReceiveRemoteData();
        agoraRTCClient.leaveChannel();
    }

    public void onLeaveChannel(View view) {
        specailEffectHolder.line = false;
        handleLeaveChannel();
    }

    public void onStopRtmp(View view) {
        cameraPreview.stopRecording();
    }

    public void onSwitchCamera(View view) {
        cameraPreview.switchCamera();
    }

    public void onExit(View view) {
        if (agoraRTCClient != null) {
            cameraPreview.setAudioCPUFilter(null);
            agoraRTCClient.stopReceiveRemoteData();
            agoraRTCClient.leaveChannel();
            agoraRTCClient.release();
        }
        if (cameraPreview != null && cameraPreview.isRecording()) {
            cameraPreview.stopRecording();
        }
        finish();
    }

    private void setOnProcessedFrameListener() {
        //监听经美颜等特效处理完后的视频数据
        LiveCameraView.getEasyStreaming().setOnProcessedFrameListener(new UEasyStreaming.OnProcessedFrameListener() {
            @Override
            public void onProcessedFrame(byte[] data, int width, int height, long timestamp, int rotation, int format) {
                if (format == ImageBufferFormat.RGBA) {
                    //agora format: 1: I420 2: ARGB 3: NV21 4: RGBA
                    //将UCloud sdk采集处理的原始视频数据，发生给agora sdk
                    mediaManager.getVideoSource().DeliverFrame(data, width, height, 0, 0, 0, 0, rotation, System.currentTimeMillis(), 4);
                } else if (format == ImageBufferFormat.NV21) {
                    mediaManager.getVideoSource().DeliverFrame(data, width, height, 0, 0, 0, 0, rotation, System.currentTimeMillis(), 3);
                }
            }
        });
    }
}
