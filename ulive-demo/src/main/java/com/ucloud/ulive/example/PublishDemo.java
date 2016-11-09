package com.ucloud.ulive.example;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ucloud.ulive.UAudioProfile;
import com.ucloud.ulive.UCameraProfile;
import com.ucloud.ulive.UCameraSessionListener;
import com.ucloud.ulive.UEasyStreaming;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UNetworkListener;
import com.ucloud.ulive.UScreenShotListener;
import com.ucloud.ulive.USize;
import com.ucloud.ulive.UStreamStateListener;
import com.ucloud.ulive.UStreamingProfile;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.common.util.DeviceUtils;
import com.ucloud.ulive.common.util.StringUtil;
import com.ucloud.ulive.example.filter.audio.UAudioMuteFilter;
import com.ucloud.ulive.example.filter.audio.URawAudioMixFilter;
import com.ucloud.ulive.example.permission.PermissionsActivity;
import com.ucloud.ulive.example.preference.Settings;
import com.ucloud.ulive.filter.UAudioCPUFilter;
import com.ucloud.ulive.filter.UVideoGPUFilter;
import com.ucloud.ulive.filter.UVideoGroupGPUFilter;
import com.ucloud.ulive.filter.video.cpu.USkinBeautyCPUFilter;
import com.ucloud.ulive.filter.video.gpu.USkinBeautyGPUFilter;
import com.ucloud.ulive.widget.UAspectFrameLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class PublishDemo extends Activity implements TextureView.SurfaceTextureListener, UCameraSessionListener,
        UStreamStateListener, UNetworkListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "ulive-demo";

    protected UEasyStreaming mEasyStreaming;
    protected String mRtmpAddress = "";
    protected Settings mSettings;
    protected UStreamingProfile mStreamingProfile;

    //Views
    protected TextureView mTexturePreview;
    protected Button mBackImgBtn;
    protected ViewGroup mContainer;
    protected UAspectFrameLayout mPreviewContainer;
    protected TextView mBitrateTxtv;
    protected TextView mFpsTxtv;
    protected TextView mRecordedTimeTxtv;
    protected TextView mOutputStreamInfoTxtv;
    protected TextView mNetworkblockTxtv;
    protected TextView mDebugLogTxtv;
    private ScrollView mScrollView;
    private Button mRecordingBtn;

    /* 磨皮、美白、肤色 */
    private SeekBar mSeekBar1;
    private SeekBar mSeekBar2;
    private SeekBar mSeekBar3;
    private TextView mV1;
    private TextView mV2;
    private TextView mV3;
    private int level1 = 60;
    private int level2 = 26;
    private int level3 = 15;
    private View mFilterLevelBar;
    private ImageView screenshotView;
    private FrameLayout mDebugInfoLayout;

    private int currentFilterType = UFilterProfile.FilterMode.GPU;
    private int videoCaptureOrientation = UVideoProfile.ORIENTATION_PORTRAIT;
    private int currentCameraIndex = UCameraProfile.CAMERA_FACING_FRONT;

    private int networkblockCount = 0;
    private List<UVideoGPUFilter> filters = new ArrayList<>();
    private USkinBeautyCPUFilter mSkinBeautyCPUFilter;
    private USkinBeautyGPUFilter mSkinBeautyGPUFilter;
    private UVideoGroupGPUFilter mGpuGroupFilter;

    private boolean isRecording;
    private boolean isNeedInitStreamingEnv = true;
    private boolean isFrontCameraOutputNeedFlip = false;
    private boolean isNeedContinueCaptureAfterBackToMainHome = false;
    private boolean isMute = false;
    private boolean isMix = false;
    private boolean isOpenBeautyFilter = false;
    private boolean isNeedRePreview = false;
    private int faceuFilterIndex = 0;

    private SurfaceTexture tempTexture;

    private int tempStWidth, tempStHeight;

    private  StringBuffer mLogMsg = new StringBuffer("");

    private final int mLogMsgLenLimit = 3500;

    private boolean isShowDebugInfo = false;

    private Handler eventHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_layout_live_room_view);
        init();
    }

    private void init() {
        initView();
        initConfig();
    }

    private void initConfig() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent i = getIntent();
        if (i.getStringExtra(MainActivity.EXTRA_RTMP_ADDRESS) != null && !i.getStringExtra(MainActivity.EXTRA_RTMP_ADDRESS).isEmpty()) {
            mRtmpAddress = i.getStringExtra(MainActivity.EXTRA_RTMP_ADDRESS);
        }
        mSettings = new Settings(this);
        currentFilterType = mSettings.getFilterType();
        videoCaptureOrientation = mSettings.getVideoCaptureOrientation();
        initBtnState();
    }

    private void initFilters() {
        // new cpu filter
        mSkinBeautyCPUFilter = new USkinBeautyCPUFilter(this);
        mSkinBeautyCPUFilter.setRadius(20 / 4);
        //new gpu filter
        mSkinBeautyGPUFilter = new USkinBeautyGPUFilter();
        mSkinBeautyGPUFilter.setFilterLevel(level1, level2, level3);
        //add to list
        filters.clear();
        //init group filter ucloud skin beauty & faceu detector
        if (currentFilterType == UFilterProfile.FilterMode.GPU) {
            if (isOpenBeautyFilter) {
                filters.add(mSkinBeautyGPUFilter);
                mFilterLevelBar.setVisibility(View.VISIBLE);
            } else {
                mFilterLevelBar.setVisibility(View.GONE);
            }
            if (filters.size() > 0) {
                mGpuGroupFilter = new UVideoGroupGPUFilter(filters);
                mEasyStreaming.setVideoGPUFilter(mGpuGroupFilter);
            } else {
                mFilterLevelBar.setVisibility(View.GONE);
                mEasyStreaming.setVideoGPUFilter(null);
                mEasyStreaming.setVideoCPUFilter(null);
            }
        } else {
            if (isOpenBeautyFilter) {
                mEasyStreaming.setVideoCPUFilter(mSkinBeautyCPUFilter);
            } else {
                mEasyStreaming.setVideoGPUFilter(null);
                mEasyStreaming.setVideoCPUFilter(null);
            }
            mFilterLevelBar.setVisibility(View.GONE);
        }
    }

    private void initPreviewTextureView() {
        if (mTexturePreview == null) {
            mTexturePreview = new TextureView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            mPreviewContainer.removeAllViews();
            mPreviewContainer.addView(mTexturePreview);
            mTexturePreview.setKeepScreenOn(true);
            mTexturePreview.setSurfaceTextureListener(this);
        }
    }

    private void stopPreviewTextureView(boolean isRelase) {
        try {
            if (mEasyStreaming != null) {
                mEasyStreaming.stopRecording();
                mEasyStreaming.stopPreview(isRelase);
                mEasyStreaming.onDestroy();
                isNeedRePreview = !isRelase;
                if (isRelase) {
                    mPreviewContainer.removeAllViews();
                    mTexturePreview = null;
                }
            }
            isNeedInitStreamingEnv = true;
        } catch (Exception e) {
            mTexturePreview = null;
            Log.e(TAG, "lifecycle->demo->stopPreviewTextureView->failed.");
        }
    }

    private void initView() {
        mFilterLevelBar = findViewById(R.id.filter_level_bar);
        mDebugInfoLayout = (FrameLayout) findViewById(R.id.info_layout);
        mRecordingBtn = (Button) findViewById(R.id.img_bt_record);
        screenshotView = (ImageView) findViewById(R.id.takescreenshot);
        mPreviewContainer = (UAspectFrameLayout) findViewById(R.id.container);
        initPreviewTextureView();
        mBackImgBtn = (Button) findViewById(R.id.btn_finish);
        mContainer = (ViewGroup) findViewById(R.id.live_finish_container);
        mBackImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mRecordedTimeTxtv = (TextView) findViewById(R.id.recorded_time_txtv);
        mOutputStreamInfoTxtv = (TextView) findViewById(R.id.output_url_txtv);
        mNetworkblockTxtv = (TextView) findViewById(R.id.network_overflow_count);
        mBitrateTxtv = (TextView) findViewById(R.id.bitrate_txtv);
        mFpsTxtv = (TextView) findViewById(R.id.fps_txtv);
        mScrollView = (ScrollView) findViewById(R.id.scrollview);
        mDebugLogTxtv = (TextView) findViewById(R.id.status_info);
        if (isShowDebugInfo) {
            mDebugInfoLayout.setVisibility(View.VISIBLE);
            findViewById(R.id.copy_to_clipboard_txtv).setVisibility(View.VISIBLE);
            findViewById(R.id.clear_debug_info_txtv).setVisibility(View.VISIBLE);
        } else {
            mDebugInfoLayout.setVisibility(View.GONE);
            findViewById(R.id.copy_to_clipboard_txtv).setVisibility(View.GONE);
            findViewById(R.id.clear_debug_info_txtv).setVisibility(View.GONE);
        }
        mDebugLogTxtv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        mV1 = (TextView) findViewById(R.id.progress1);
        mV2 = (TextView) findViewById(R.id.progress2);
        mV3 = (TextView) findViewById(R.id.progress3);
        mV1.setText(String.valueOf(level1));
        mV2.setText(String.valueOf(level2));
        mV3.setText(String.valueOf(level3));
        mSeekBar1 = (SeekBar) findViewById(R.id.seek_bar_1);
        mSeekBar1.setProgress(level1);
        mSeekBar1.setOnSeekBarChangeListener(this);
        mSeekBar2 = (SeekBar) findViewById(R.id.seek_bar_2);
        mSeekBar2.setProgress(level2);
        mSeekBar2.setOnSeekBarChangeListener(this);
        mSeekBar3 = (SeekBar) findViewById(R.id.seek_bar_3);
        mSeekBar3.setProgress(level3);
        mSeekBar3.setOnSeekBarChangeListener(this);
    }

    private void initBtnState() {
        if (videoCaptureOrientation == UVideoProfile.ORIENTATION_PORTRAIT) {
            ((Button)findViewById(R.id.btn_toggle_caputre_orientation)).setText("横屏");
        } else {
            ((Button)findViewById(R.id.btn_toggle_caputre_orientation)).setText("竖屏");
        }

        if (currentFilterType == UFilterProfile.FilterMode.GPU) {
            ((Button)findViewById(R.id.btn_toggle_filter_mode)).setText("CPU");
        } else {
            ((Button)findViewById(R.id.btn_toggle_filter_mode)).setText("GPU");
        }
    }

    public void initStreamingEnv() {
        if (videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        initBtnState();
        mPreviewContainer.setShowMode(UAspectFrameLayout.Mode.FULL);
        mEasyStreaming = UEasyStreaming.Factory.newInstance();
        UVideoProfile videoProfile = new UVideoProfile().fps(mSettings.getVideoFps())
                .bitrate(mSettings.getVideoBitrate())
                .resolution(mSettings.getResolution())
                .captureOrientation(videoCaptureOrientation);
        UAudioProfile audioProfile = new UAudioProfile();
        audioProfile.bitrate(UAudioProfile.AUDIO_BITRATE_NORMAL);
        UFilterProfile filterProfile = new UFilterProfile().mode(currentFilterType);
        UCameraProfile cameraProfile = new UCameraProfile().frontCameraFlip(isFrontCameraOutputNeedFlip)
                .setCameraIndex(currentCameraIndex);
        mStreamingProfile = new UStreamingProfile.Builder()
                .setAudioProfile(audioProfile)
                .setVideoProfile(videoProfile)
                .setFilterProfile(filterProfile)
                .setCameraProfile(cameraProfile)
//                .build(mRtmpAddress)
                .build();
        mEasyStreaming.setOnCameraSessionListener(this);
        mEasyStreaming.setOnStreamStateListener(this);
        mEasyStreaming.setOnNetworkStateListener(this);
        mEasyStreaming.prepare(mStreamingProfile);
        if (isMute) {
            mEasyStreaming.setAudioCPUFilter(new UAudioMuteFilter());
        } else {
            mEasyStreaming.setAudioCPUFilter(null);
        }

        if (isMix) {
            URawAudioMixFilter rawAudioMixFilter = new URawAudioMixFilter(this, com.ucloud.ulive.example.filter.audio.URawAudioMixFilter.Mode.ANY, true);
            mEasyStreaming.setAudioCPUFilter(rawAudioMixFilter);
        } else {
            mEasyStreaming.setAudioCPUFilter(null);
        }

        if (currentFilterType == UFilterProfile.FilterMode.GPU) {
            mEasyStreaming.frontCameraFlipHorizontal(isFrontCameraOutputNeedFlip);
        }
        initPreviewTextureView();
        if (isOpenBeautyFilter) {
            initFilters();
        } else {
            mFilterLevelBar.setVisibility(View.GONE);
        }
        handleShowStreamingInfo(mStreamingProfile);
        isNeedInitStreamingEnv = false;
    }

    public void onToggleDebugInfoVisibleBtnClick(View view) {
        int visibility = mDebugInfoLayout.getVisibility();
        if (visibility == View.VISIBLE) {
            mDebugInfoLayout.setVisibility(View.GONE);
            findViewById(R.id.copy_to_clipboard_txtv).setVisibility(View.GONE);
            findViewById(R.id.clear_debug_info_txtv).setVisibility(View.GONE);
        } else {
            findViewById(R.id.copy_to_clipboard_txtv).setVisibility(View.VISIBLE);
            findViewById(R.id.clear_debug_info_txtv).setVisibility(View.VISIBLE);
            mDebugInfoLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mEasyStreaming != null) mEasyStreaming.onResume();
        if (isNeedInitStreamingEnv) {
            initStreamingEnv();
        }
        if (isRecording) {
            mStreamingProfile.setStreamUrl(mRtmpAddress);
            mEasyStreaming.startRecording();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mEasyStreaming != null) {
            mEasyStreaming.onPause();
            isRecording = mEasyStreaming.isRecording();
            if (!isNeedContinueCaptureAfterBackToMainHome && isRecording) {
                mEasyStreaming.stopRecording();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mEasyStreaming != null) {
            mEasyStreaming.onDestroy();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mEasyStreaming != null) {
            Log.i(TAG, "lifecycle->demo->onSurfaceTextureAvailable width = " + width + ", height = " + height);
            mEasyStreaming.startPreview(surface, width, height);
        }
        tempTexture = surface;
        tempStWidth = width;
        tempStHeight = height;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (mEasyStreaming != null) {
            Log.i(TAG, "lifecycle->demo->onSurfaceTextureSizeChanged width = " + width + ", height = " + height);
            mEasyStreaming.updatePreview(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mEasyStreaming != null) {
            Log.i(TAG, "lifecycle->demo->onSurfaceTextureDestroyed.");
            mEasyStreaming.stopPreview(true);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void onStopStreamingBtnClick(View view) {
        view.setEnabled(false);
        if (mEasyStreaming != null && mEasyStreaming.isRecording()) {
            mEasyStreaming.stopRecording();
        }
        mContainer.setVisibility(View.VISIBLE);
    }

    public void onToggleRecordBtnClick(View view) {
        if (!mEasyStreaming.isRecording()) {
            // can delete, just for icon change
            if (isNeedInitStreamingEnv) {
                initStreamingEnv();
            }
            if (isNeedRePreview) {
                mEasyStreaming.startPreview(tempTexture, tempStWidth, tempStHeight);
            }
            mStreamingProfile.setStreamUrl(mRtmpAddress); // delay set rtmp url
            mEasyStreaming.startRecording();
            handleShowStreamingInfo(mStreamingProfile);
        } else {
            mEasyStreaming.stopRecording();
            stopPreviewTextureView(false);
        }
    }

    public void onToggleFlashModeBtnClick(View view) {
        if (mEasyStreaming != null) {
            boolean retVal = mEasyStreaming.toggleFlashMode();
            if (retVal) {
                appendDebugLogInfo("toggle flash mode succeed.");
            } else {
                appendDebugLogInfo("toggle flash mode failed: front camera no flash.");
            }
        }
    }

    public void onSwitchCameraBtnClick(View view) {
        if (mEasyStreaming != null) {
            boolean retVal = mEasyStreaming.switchCamera();
            appendDebugLogInfo("switch camear " + (retVal ? "succeed." : "failed."));
        }
    }

    public void onToggleFilterBtnClick(View view) {
        isOpenBeautyFilter = !isOpenBeautyFilter;
        if (isOpenBeautyFilter) {
            appendDebugLogInfo("set skin beauty filter.");
        } else {
            appendDebugLogInfo("removed skin beauty filter.");
        }
        initFilters();
    }

    public void onToggleMuteBtnClick(View view) {
        if (mEasyStreaming != null) {
            isMute = !isMute;
            if (isMute) {
                appendDebugLogInfo("toggle to mute...");
                mEasyStreaming.setAudioCPUFilter(new UAudioMuteFilter());
            } else {
                appendDebugLogInfo("toggle to unmute...");
                mEasyStreaming.setAudioCPUFilter(null);
            }

            if (isMute) {
                ((Button) view).setText("非静音");
            } else {
                ((Button) view).setText("静音");
            }
        } else {
            appendDebugLogInfo("please call after UEasyStreaming prepared.");
        }
    }

    public void onToggleRawAudioMixClick(View view) {
        if (mEasyStreaming != null) {
            isMix = !isMix;
            if (isMix) {
                if (mEasyStreaming.isRecording()) {
                    appendDebugLogInfo("raw audio mixer start.");
                } else {
                    appendDebugLogInfo("raw audio mixer delay after UEasyStreaming started.");
                }
                URawAudioMixFilter URawAudioMixFilter = new URawAudioMixFilter(this, com.ucloud.ulive.example.filter.audio.URawAudioMixFilter.Mode.ANY, true);
                mEasyStreaming.setAudioCPUFilter(URawAudioMixFilter);
            } else {
                appendDebugLogInfo("raw audio mixer stop");
                mEasyStreaming.setAudioCPUFilter(null);
            }

            if (isMix) {
                ((Button) view).setText("非混音");
            } else {
                ((Button) view).setText("混音");
            }
        } else {
            appendDebugLogInfo("please call after UEasyStreaming prepared.");
        }
    }

    //just support gpu filter & front camera
    public void onToggleFrontCameraOutputFlipBtnClick(View view) {
        if (mStreamingProfile != null && mStreamingProfile.getFilterProfile().getFilterMode() == UFilterProfile.FilterMode.CPU) {
            Toast.makeText(this, "sorry, just support gpu filter -> front camera", Toast.LENGTH_SHORT).show();
            return;
        }
        isFrontCameraOutputNeedFlip = !isFrontCameraOutputNeedFlip;
        mEasyStreaming.frontCameraFlipHorizontal(isFrontCameraOutputNeedFlip);
        if (isFrontCameraOutputNeedFlip) {
            appendDebugLogInfo("mirror.");
            ((Button)view).setText("非镜像");
        } else {
            appendDebugLogInfo("no mirror...");
            ((Button)view).setText("镜像");
        }
    }

    public void onToggleFilterModeBtnClick(View view) {
        isRecording = mEasyStreaming.isRecording();
        if (isRecording) {
            mEasyStreaming.stopRecording();
        }
        stopPreviewTextureView(true);
        if (currentFilterType == UFilterProfile.FilterMode.GPU) {
            currentFilterType = UFilterProfile.FilterMode.CPU;
            appendDebugLogInfo("toggle cpu filter.");
        } else {
            currentFilterType = UFilterProfile.FilterMode.GPU;
            appendDebugLogInfo("toggle gpu filter.");
        }
        initStreamingEnv(); // all init, camera index, is mirror
        if (isNeedRePreview) {
            mEasyStreaming.startPreview(tempTexture, tempStWidth, tempStHeight);
        }
        if (isRecording) {
            //set before start
            mStreamingProfile.setStreamUrl(mRtmpAddress);  // delay set rtmp url
            mEasyStreaming.startRecording();
            handleShowStreamingInfo(mStreamingProfile);
        }
    }

    public void onToggleCaptureOrientationBtnClick(View view) {
        isRecording = mEasyStreaming.isRecording();
        if (isRecording) {
            mEasyStreaming.stopRecording();
        }
        stopPreviewTextureView(true);
        if (videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            videoCaptureOrientation = UVideoProfile.ORIENTATION_PORTRAIT;
            appendDebugLogInfo("toggle portait mode.");
        } else {
            videoCaptureOrientation = UVideoProfile.ORIENTATION_LANDSCAPE;
            appendDebugLogInfo("toggle landscape mode.");
        }
        initStreamingEnv();
        if (isRecording) {
            //set before start
            mStreamingProfile.setStreamUrl(mRtmpAddress);  // delay set rtmp url
            mEasyStreaming.startRecording();
            handleShowStreamingInfo(mStreamingProfile);
        }
    }

    public void onVideoFrameCaptureBtnClick(View view) {
        mEasyStreaming.takeScreenShot(new UScreenShotListener() {
            @Override
            public void onScreenShotResult(Bitmap bitmap) {
                if (bitmap != null) {
                    appendDebugLogInfo("video frame capture.");
                    screenshotView.setVisibility(View.VISIBLE);
                    screenshotView.setImageBitmap(bitmap);
                    eventHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            screenshotView.setVisibility(View.GONE);
                        }
                    },800);
                }
            }
        });
    }

    public void onClearDebugLogBtnClick(View view) {
        if (mDebugLogTxtv != null) {
            clearLog();
        }
    }

    public void onCopyDebugLogBtnClick(View view) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("text", mBitrateTxtv.getText().toString()
                + "  "+mRecordedTimeTxtv.getText().toString() + "\n"
                + mFpsTxtv.getText().toString()
                + mNetworkblockTxtv.getText().toString() + "\n"
                +mOutputStreamInfoTxtv.getText() +mLogMsg.toString());
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(this, "copy to clipboard.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPreviewFrame(int cameraId, byte[] data, int width, int height) {
    }

    @Override
    public USize[] onPreviewSizeChoose(int cameraId, List<USize> cameraSupportPreviewSize) {
       /* USize[] sizes = new USize[2];
        sizes[0] = new USize(1280, 720);
        sizes[1] = new USize(640, 368);*/
        Log.i(TAG, "lifecycle->demo->camera->onPreviewSizeChoose.");
        return null;
    }

    @Override
    public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndex, int width, int height) {
        String cameraIndexs = "";
        currentCameraIndex = cameraId;
        for (int camearIndex : supportCameraIndex) {
            cameraIndexs += camearIndex + ",";
        }
        if (videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            mPreviewContainer.setAspectRatio(((float) width) / height);
        } else {
            mPreviewContainer.setAspectRatio(((float) height) / width);
        }

        Log.i(TAG, "lifecycle->demo->camera->onCameraOpenSuccessed->cameraId = " + cameraId + ", support " +
                "camera index = " + cameraIndexs + "," + width + "x" + height);
    }

    @Override
    public void onCameraError(UCameraSessionListener.Error error, Object extra) {
        Log.i(TAG, "lifecycle->demo->camera->onCameraError error = " + error);
        switch (error) {
            case NO_NV21_PREVIEW_FORMAT:
                break;
            case NO_SUPPORT_PREVIEW_SIZE:
                break;
            case NO_PERMISSION:
                break;
            case REQUEST_FLASH_MODE_FAILED:
                break;
            case START_PREVIEW_FAILED:
                break;
        }
    }

    @Override
    public void onCameraFlashSwitched(int cameraId, boolean currentState) {
        Log.i(TAG, "lifecycle->demo->camera->onCameraFlashSwitched cameraId = " + cameraId + ", currentState = " +
                "" + "" + currentState);
    }

    @Override
    public void onStateChanged(UStreamStateListener.State state, Object extra) {
        Log.i(TAG, "lifecycle->demo->stream->state-> msg = " + state);
        switch (state) {
            case PREPARING:
                appendDebugLogInfo("streaming env preparing...");
                Log.i(TAG, "lifecycle->demo->stream->preparing");
                break;
            case PREPARED:
                appendDebugLogInfo("streaming env streaming prepared.");
                Log.i(TAG, "lifecycle->demo->stream->prepared");
                break;
            case CONNECTING:
                appendDebugLogInfo("streaming connecting...");
                Log.i(TAG, "lifecycle->demo->stream->connecting");
                break;
            case CONNECTED:
                appendDebugLogInfo("streaming connected.");
                Log.i(TAG, "lifecycle->demo->stream->connected");
                break;
            case START:
                appendDebugLogInfo("streaming start.");
                mRecordingBtn.setText("结束");
                Log.i(TAG, "lifecycle->demo->stream->start");
                break;
            case STOP:
                appendDebugLogInfo("streaming stop.");
                // clear info
                mBitrateTxtv.setText("0.0K/s");
                mFpsTxtv.setText("0.00fps");
                mRecordedTimeTxtv.setText("00:00");
                mRecordingBtn.setText("开始");
                Log.i(TAG, "lifecycle->demo->stream->stop");
                break;
            case NETWORK_BLOCK:
                networkblockCount++;
                mNetworkblockTxtv.setText("network block count:" + networkblockCount);
                Log.e(TAG, "lifecycle->dmeo->nework block total = " + networkblockCount + ", server ip = " + mEasyStreaming.getServerIPAddress() + ", current free buffer = " + extra);
                break;
        }
    }

    @Override
    public void onStreamError(UStreamStateListener.Error error, Object extra) {
//        Log.i(TAG, "lifecycle->demo->stream->state-> msg = " + error);
        switch (error) {
            case AUDIO_PREPARE_FAILED:
                mEasyStreaming = null;
                appendDebugLogInfo("audio env prepare failed.");
                Log.i(TAG, "lifecycle->demo->stream->audio env prepare failed");
                finish();
                break;
            case VIDEO_PREPARE_FAILED:
                mEasyStreaming = null;
                appendDebugLogInfo("video env prepare failed.");
                Log.i(TAG, "lifecycle->demo->stream->video env prepare failed.");
                finish();
                break;
            case INVALID_STREAMING_URL:
                appendDebugLogInfo("invalid streaming url:" + error.toString());
                Log.i(TAG, "lifecycle->demo->stream->invalid streaming url.");
                break;
            case SIGNATRUE_FAILED:
                appendDebugLogInfo("streaming signature failed!");
                Log.i(TAG, "lifecycle->demo->stream->signature failed.");
                break;
            case IOERROR:
                appendDebugLogInfo("streaming io error:" + error.toString() + ", extra = " + extra);
                Log.i(TAG, "lifecycle->demo->stream->io error");
                if (mEasyStreaming != null) {
                    mEasyStreaming.restart(); // to do reconnect
                }
                break;
            case UNKNOWN:
                appendDebugLogInfo("streaming unknown error");
                Log.i(TAG, "lifecycle->demo->stream->unkown error");
                break;
        }
    }

    @Override
    public void onNetworkStateChanged(UNetworkListener.State state, Object extra) {
        switch (state) {
            case NETWORK_SPEED:
                //当前手机实时全局网络速度
                if (mBitrateTxtv != null) {
                    mBitrateTxtv.setVisibility(View.VISIBLE);
                    long speed = Long.valueOf((int) extra);
                    if (speed > 1024) {
                        mBitrateTxtv.setText(speed / 1024 + "K/s");
                    } else {
                        mBitrateTxtv.setText(speed + "B/s");
                    }
                }
                if (mFpsTxtv != null) {
                    mFpsTxtv.setVisibility(View.VISIBLE);
                    mFpsTxtv.setText(String.format("draw fps:%.2f, send fps:%.2f", mEasyStreaming.getDrawFps(), mEasyStreaming.getSendFps()));
                }
                break;
            case PUBLISH_STREAMING_TIME:
                //sdk内部记录的推流时间,若推流被中断stop之后，该值会重新从0开始计数
                if (mRecordedTimeTxtv != null) {
                    mRecordedTimeTxtv.setVisibility(View.VISIBLE);
                    long time = (long) extra;
                    String retVal = StringUtil.getTimeFormatString(time);
                    mRecordedTimeTxtv.setText(retVal);
                }
                break;
            case DISCONNECT:
                appendDebugLogInfo("network disconnect.");
                //当前网络状态处于断开状态
                Log.i(TAG, "lifecycle->demo->event->network disconnect.");
                break;
            case RECONNECT:
                //网络重新连接
                Log.i(TAG, "lifecycle->demo->event->restart->after network reconnect:" + "," + mEasyStreaming.isRecording());
                appendDebugLogInfo("network reconnected");
                if (mEasyStreaming != null) {
                    mEasyStreaming.restart(); //todo reconnect
                }
                break;
            default:
                break;
        }
    }

    public void handleShowStreamingInfo(UStreamingProfile streamingProfile) {
        if (mOutputStreamInfoTxtv != null) {
            mOutputStreamInfoTxtv.setVisibility(View.VISIBLE);
            String info = "url:" + streamingProfile.getStreamUrl() + "\n" +
                    "resolution:" + mEasyStreaming.getVideoOutputSize().toString() + "\n" +
                    "filter type:" + (streamingProfile.getFilterProfile().getFilterMode() == UFilterProfile.FilterMode.GPU ? "GPU" : "CPU") + "\n" +
                    "video bitrate:" + videoBitrateMode(streamingProfile.getVideoProfile().getBitrate()) + "\n" +
                    "audio bitrate:" + audioBitrateMode(streamingProfile.getAudioProfile().getBitrate()) + "\n" +
                    "video fps:" + streamingProfile.getVideoProfile().getFps() + "\n" +
                    "brand:" + DeviceUtils.getDeviceBrand() + "_" + DeviceUtils.getDeviceModel() + "\n" +
                    "sdk version:" + com.ucloud.ulive.UBuild.VERSION + "\n" +
                    "android sdk version:" + Build.VERSION.SDK_INT;
            mOutputStreamInfoTxtv.setText(info);
            Log.i(TAG, "@@" + info);
        }
    }

    private void appendDebugLogInfo(String message) {
        if (mDebugLogTxtv != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            String date = sdf.format(System.currentTimeMillis());
            while(mLogMsg.length() > mLogMsgLenLimit ){
                int idx = mLogMsg.indexOf("\n");
                if (idx == 0)
                    idx = 1;
                mLogMsg = mLogMsg.delete(0, idx);
            }
            mLogMsg = mLogMsg.append("\n" + "["+date+"]" + message);
            mDebugLogTxtv.setText(mLogMsg);
//            mDebugLogTxtv.setTextColor(Color.GREEN | Color.GRAY);
        }
    }

    protected void clearLog() {
        mLogMsg.setLength(0);
        mDebugLogTxtv.setText("");
    }

    public String videoBitrateMode(int value) {
        switch (value) {
            case UVideoProfile.VIDEO_BITRATE_LOW:
                return "VIDEO_BITRATE_LOW";
            case UVideoProfile.VIDEO_BITRATE_NORMAL:
                return "VIDEO_BITRATE_NORMAL";
            case UVideoProfile.VIDEO_BITRATE_MEDIUM:
                return "VIDEO_BITRATE_MEDIUM";
            case UVideoProfile.VIDEO_BITRATE_HIGH:
                return "VIDEO_BITRATE_HIGH";
            default:
                return value + "";
        }
    }

    public String audioBitrateMode(int value) {
        switch (value) {
            case UAudioProfile.AUDIO_BITRATE_NORMAL:
                return "AUDIO_BITRATE_NORMAL";
            default:
                return value + "";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == PermissionsActivity.PERMISSIONS_GRANTED) {
            mEasyStreaming.prepare(mStreamingProfile);
            Log.i(TAG, "lifecycle->demo->permissions granted");
        } else {
            finish();
            Log.i(TAG, "lifecycle->demo->permissions denied");
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seek_bar_1:
                mV1.setText(String.valueOf(progress));
                level1 = progress;
                break;
            case R.id.seek_bar_2:
                mV2.setText(String.valueOf(progress));
                level2 = progress;
                break;
            case R.id.seek_bar_3:
                mV3.setText(String.valueOf(progress));
                level3 = progress;
                break;
        }
        if (mEasyStreaming != null) {
            UAudioCPUFilter audioProfile = mEasyStreaming.acquireAudioCPUFilter();
            if (audioProfile != null && audioProfile instanceof URawAudioMixFilter) {
                URawAudioMixFilter URawAudioMixFilter = (URawAudioMixFilter) audioProfile;
                URawAudioMixFilter.adjustBackgroundMusicVolumeLevel(level1 * 1.0f /  100.0f);
                URawAudioMixFilter.adjustMiscVolumeLevel(level2 * 1.0f /  100.0f);
                mEasyStreaming.releaseAudioCPUFilter();
            }
            for(UVideoGPUFilter filter: filters) {
                if (filter instanceof USkinBeautyGPUFilter) {
                    ((USkinBeautyGPUFilter)filter).setFilterLevel(level1, level2, level3);
                }
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}