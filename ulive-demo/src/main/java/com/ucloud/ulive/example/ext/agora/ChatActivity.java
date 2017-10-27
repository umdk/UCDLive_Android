package com.ucloud.ulive.example.ext.agora;

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
import com.ucloud.ulive.UCameraSessionListener;
import com.ucloud.ulive.UEasyStreaming;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.USize;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.BaseActivity;
import com.ucloud.ulive.example.MainActivity;
import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.ext.agora.filter.URemoteAudioMixFilter;
import com.ucloud.ulive.example.ext.faceunity.FaceunityCompat;
import com.ucloud.ulive.example.widget.LiveCameraView;
import com.ucloud.ulive.filter.UVideoGPUFilter;
import com.ucloud.ulive.framework.AudioBufferFormat;
import com.ucloud.ulive.framework.ImageBufferFormat;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatActivity extends BaseActivity {

    private static final String TAG = "ChatActivity";

    @BindView(R.id.livecamera)
    LiveCameraView cameraPreview;

    @BindView(R.id.txtv_stream_id)
    TextView streamIdTextv;

    private AVOption avOption;
    private AgoraRTCClient agoraRTCClient;
    private MediaManager mediaManager;
    protected String encryptionKey;
    protected String encryptionMode;
    protected AudioBufferFormat localAudioBufferFormat;
    protected AudioBufferFormat remoteAudioBufferFormat;

    private SpecailEffectHolder specailEffectHolder;

    private UVideoGPUFilter filter;

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
                        //发现连麦用户掉线时，清除小窗口 参数说明 //0 清除所有， 非0固定值 清除指定的窗口
                        LiveCameraView.getEasyStreaming().clearRemoteVideoFrame((Integer)data[0]);
                        Log.d(TAG, "user offline: uid = " + data[0] + " reason = " + data[1]);
                        if (agoraRTCClient != null) {
                            agoraRTCClient.resetRemoteUid((Integer)data[0]); //从连麦列表中清除
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        agoraRTCClient = new AgoraRTCClient(mediaManager, localAudioBufferFormat, remoteAudioBufferFormat);
        RemoteAudioCacheUtil.getInstance().init(remoteAudioBufferFormat, 10, remoteAudioBufferFormat.sampleRate / 10 * 2); //内部每笔音频占用的大小，保持一致
        cameraPreview.init(avOption);
        cameraPreview.addCameraSessionListener(cameraSessionListener);
        filter = new FaceunityCompat(this);
        cameraPreview.setVideoGPUFilter(filter);
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
        //当前仅GPU模式下支持连麦
//        avOption.videoFilterMode = UFilterProfile.FilterMode.GPU;
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
        if (agoraRTCClient != null) {
            agoraRTCClient.release();
        }
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
                if (format == ImageBufferFormat.RGBA) { //GPU 采集模式 回调RGBA格式的数据, GPU模式下需要设备支持GLES3，多人连麦最大支持4v4
                    //agora format: 1: I420 2: ARGB 3: NV21 4: RGBA
                    //将UCloud sdk采集处理的原始视频数据，发生给agora sdk
                    mediaManager.getVideoSource().DeliverFrame(data, width, height, 0, 0, 0, 0, rotation, System.currentTimeMillis(), 4);
                } else if (format == ImageBufferFormat.NV21) { //CPU 采集模式 回调NV21的数据格式, CPU模式下当前仅支持1v1连麦
                    mediaManager.getVideoSource().DeliverFrame(data, width, height, 0, 0, 0, 0, rotation, System.currentTimeMillis(), 3);
                }
            }
        });
    }

    UCameraSessionListener cameraSessionListener = new UCameraSessionListener() {
        @Override
        public USize[] onPreviewSizeChoose(int cameraId, List<USize> supportPreviewSizeList) {
            return new USize[0];
        }

        @Override
        public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndexList, int width, int height) {

        }

        @Override
        public void onCameraError(Error error, Object extra) {

        }

        @Override
        public void onCameraFlashSwitched(int cameraId, boolean currentState) {

        }

        @Override
        public void onPreviewFrame(int cameraId, byte[] data, int width, int height) {
            ((FaceunityCompat) filter).updateCameraFrame(data, width, height);
        }
    };
}
