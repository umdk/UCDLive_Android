package com.ucloud.ulive.example.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.megvii.facepp.sdk.ext.FaceuHelper;
import com.ucloud.ulive.UCameraSessionListener;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UNetworkListener;
import com.ucloud.ulive.USize;
import com.ucloud.ulive.UStreamStateListener;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.ext.faceu.FaceuCompat;
import com.ucloud.ulive.example.filter.audio.UAudioMuteFilter;
import com.ucloud.ulive.example.filter.audio.URawAudioMixFilter;
import com.ucloud.ulive.filter.UVideoGPUFilter;
import com.ucloud.ulive.filter.UVideoGroupGPUFilter;
import com.ucloud.ulive.filter.video.cpu.USkinBeautyCPUFilter;
import com.ucloud.ulive.filter.video.gpu.USkinBeautyGPUFilter;
import com.ucloud.ulive.widget.UAspectFrameLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

import static butterknife.ButterKnife.bind;

public class LiveRoomView extends FrameLayout {

    private static final String TAG = "LiveRoomView";

    public LiveCameraView getLiveCameraView() {
        return viewHolder.liveCameraView;
    }

    class ViewHolder {
        LiveCameraView liveCameraView;

        @Bind(R.id.camera_controller_view)
        CameraControllerView cameraControllerView;

        @Bind(R.id.live_finish_container)
        View streamOverContainer;

        @Bind(R.id.btn_finish)
        Button backMainIndexButton;

        @Bind(R.id.filter_controller_view)
        FilterControllerView filterControllerView;

        ViewHolder(View view) {
            bind(this, view);
        }
    }

    class SpecailEffectHolder {
        boolean mute;
        boolean mirror;
        boolean mix;
        boolean recording;
        boolean faceDetector;
        boolean beautyFilter;
        int faceuFilterIndex = 0;
    }

    private ViewHolder viewHolder;

    private SpecailEffectHolder specailEffectHolder;

    private AVOption avOption;

    private final List<UVideoGPUFilter> filters = new ArrayList<>();

    private final boolean continueCaptureAfterBackHome = false; //不推荐为true 容易出现异常

    private boolean isDepenedActivityLifecycle = false;

    public LiveRoomView(Context context) {
        super(context);
    }

    public LiveRoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LiveRoomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        specailEffectHolder = new SpecailEffectHolder();
        viewHolder = new ViewHolder(this);

        viewHolder.cameraControllerView.setOnPanelClickListener(new MyControllerViewListener());
        viewHolder.backMainIndexButton.setOnClickListener(backMainButtonClickListener);
        viewHolder.filterControllerView.setListener(filterProgressListneer);
    }

    private void initCameraStateLisener() {
        if (viewHolder.liveCameraView != null) {
            //for ui
            viewHolder.liveCameraView.addCameraSessionListener(viewHolder.cameraControllerView);
            viewHolder.liveCameraView.addStreamStateListener(viewHolder.cameraControllerView);
            viewHolder.liveCameraView.addNetworkListener(viewHolder.cameraControllerView);
            //for business
            viewHolder.liveCameraView.addCameraSessionListener(cameraSessionListener);
            viewHolder.liveCameraView.addStreamStateListener(streamStateListener);
            viewHolder.liveCameraView.addNetworkListener(newtworkListener);
        }
    }

    private final FilterControllerView.ProgressListener filterProgressListneer = new FilterControllerView.ProgressListener() {

        @Override
        public boolean onProgressChanaged(int level1, int level2, int level3) {
            for (UVideoGPUFilter filter: filters) {
                if (filter instanceof USkinBeautyGPUFilter) {
                    ((USkinBeautyGPUFilter) filter).setFilterLevel(level1, level2, level3);
                }
            }
            return false;
        }
    };

    private final View.OnClickListener backMainButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Context context = getContext();
            if (context instanceof Activity) {
                ((Activity) (context)).finish();
            }
        }
    };

    public void startPreview(AVOption avOptions) {
        if (viewHolder.liveCameraView != null) {
            avOption = avOptions;

            viewHolder.liveCameraView.init(avOptions);
            viewHolder.cameraControllerView.updateStreamEnvUI(avOption);
        }
        else {
            Log.e(TAG, "LiveCameraView is finish attach?");
        }
    }

    private void stopPreview() {
        if (viewHolder.liveCameraView != null) {
            viewHolder.liveCameraView.stopRecordingAndDismissPreview();
        }
    }

    private void recoverSpecialEffect(SpecailEffectHolder specailEffectHolder) {
        if (specailEffectHolder == null) {
            return;
        }
        if (specailEffectHolder.mute) {
            LiveCameraView.getEasyStreaming().setAudioCPUFilter(new UAudioMuteFilter());
        }
        else {
            LiveCameraView.getEasyStreaming().setAudioCPUFilter(null);
        }

        if (specailEffectHolder.mix) {
            URawAudioMixFilter rawAudioMixFilter = new URawAudioMixFilter(getContext(), com.ucloud.ulive.example.filter.audio.URawAudioMixFilter.Mode.ANY, true);
            LiveCameraView.getEasyStreaming().setAudioCPUFilter(rawAudioMixFilter);
        }
        else {
            LiveCameraView.getEasyStreaming().setAudioCPUFilter(null);
        }

        LiveCameraView.getEasyStreaming().frontCameraFlipHorizontal(specailEffectHolder.mirror);

        initFilters(specailEffectHolder);
    }

    private void initFilters(SpecailEffectHolder specailEffectHolder) {
        //cpu filter
        USkinBeautyCPUFilter skinBeautyCPUFilter = new USkinBeautyCPUFilter(getContext());
        skinBeautyCPUFilter.setRadius(20 / 4);
        //gpu filter
        USkinBeautyGPUFilter skinBeautyGPUFilter = new USkinBeautyGPUFilter();
        skinBeautyGPUFilter.setFilterLevel(FilterControllerView.LEVEL1, FilterControllerView.LEVEL2, FilterControllerView.LEVEL3);
        viewHolder.filterControllerView.initProgress(FilterControllerView.LEVEL1, FilterControllerView.LEVEL2, FilterControllerView.LEVEL3);
        //add to list
        filters.clear();
        //open group filter ucloud skin beauty & faceu detector
        if (avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
            if (specailEffectHolder.beautyFilter) {
                filters.add(skinBeautyGPUFilter);
                viewHolder.filterControllerView.setVisibility(View.VISIBLE);
            }
            else {
                viewHolder.filterControllerView.setVisibility(View.GONE);
            }
            if (specailEffectHolder.faceDetector) {
                FaceuCompat faceuCompat = new FaceuCompat((Activity) getContext());
                faceuCompat.setFaceuFilter(FaceuHelper.getFaceuFilter(specailEffectHolder.faceuFilterIndex));
                specailEffectHolder.faceuFilterIndex++;
                if (specailEffectHolder.faceuFilterIndex == FaceuHelper.sItems.length) {
                    specailEffectHolder.faceuFilterIndex = 0;
                }
                filters.add(faceuCompat);
            }
            if (filters.size() > 0) {
                UVideoGroupGPUFilter gpuGroupFilter = new UVideoGroupGPUFilter(filters);
                LiveCameraView.getEasyStreaming().setVideoGPUFilter(gpuGroupFilter);
            }
            else {
                LiveCameraView.getEasyStreaming().setVideoGPUFilter(null);
                LiveCameraView.getEasyStreaming().setVideoCPUFilter(null);
                viewHolder.filterControllerView.setVisibility(View.GONE);
            }
        }
        else {
            if (specailEffectHolder.beautyFilter) {
                LiveCameraView.getEasyStreaming().setVideoCPUFilter(skinBeautyCPUFilter);
            }
            else {
                LiveCameraView.getEasyStreaming().setVideoGPUFilter(null);
                LiveCameraView.getEasyStreaming().setVideoCPUFilter(null);
                viewHolder.filterControllerView.setVisibility(View.GONE);
            }
        }
    }

    private void resetSpecialEffectEnv() {
        specailEffectHolder.mute = false;
        specailEffectHolder.mirror = false;
        specailEffectHolder.mix = false;
        specailEffectHolder.faceDetector = false;
        specailEffectHolder.beautyFilter = false;
        LiveCameraView.getEasyStreaming().setAudioCPUFilter(null);
        LiveCameraView.getEasyStreaming().setVideoGPUFilter(null);
        LiveCameraView.getEasyStreaming().setVideoCPUFilter(null);
        viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
    }

    class MyControllerViewListener extends CameraControllerView.ClickListenerImpl {
        @Override
        public boolean onStartButtonClick() {
            if (LiveCameraView.getEasyStreaming().isRecording()) {
                LiveCameraView.getEasyStreaming().stopRecording();
                resetSpecialEffectEnv();
                return false;
            }
            else {
                LiveCameraView.getEasyStreaming().startRecording();
                return true;
            }
        }

        @Override
        public boolean onAudioMuteButtonClick() {
            specailEffectHolder.mute = !specailEffectHolder.mute;
            if (specailEffectHolder.mute) {
                LiveCameraView.getEasyStreaming().setAudioCPUFilter(new UAudioMuteFilter());
                return true;
            }
            else {
                LiveCameraView.getEasyStreaming().setAudioCPUFilter(null);
                return false;
            }
        }

        @Override
        public boolean onVideoMirrorButtonClick() {
            specailEffectHolder.mirror = !specailEffectHolder.mirror;
            LiveCameraView.getEasyStreaming().frontCameraFlipHorizontal(specailEffectHolder.mirror);
            return specailEffectHolder.mirror;
        }

        @Override
        public boolean onAudioMixButtonClick() {
            specailEffectHolder.mix = !specailEffectHolder.mix;
            if (specailEffectHolder.mix) {
                URawAudioMixFilter rawAudioMixFilter = new URawAudioMixFilter(getContext(), com.ucloud.ulive.example.filter.audio.URawAudioMixFilter.Mode.ANY, true);
                LiveCameraView.getEasyStreaming().setAudioCPUFilter(rawAudioMixFilter);
            }
            else {
                LiveCameraView.getEasyStreaming().setAudioCPUFilter(null);
            }
            return specailEffectHolder.mix;
        }

        @Override
        public boolean onFlashModeButtonClick() {
            return LiveCameraView.getEasyStreaming().toggleFlashMode();
        }

        @Override
        public boolean onSwitchCameraButtonClick() {
            return LiveCameraView.getEasyStreaming().switchCamera();
        }

        @Override
        public boolean onVideoCodecButtonClick() {
            if (viewHolder.liveCameraView == null) {
                return false;
            }

            if (avOption.videoCodecType == UVideoProfile.CODEC_MODE_HARD && avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
//               cpu filter support sw & hard codec mode
//               gpu filter just support hard codec mode
                avOption.videoFilterMode = UFilterProfile.FilterMode.CPU;
            }

            specailEffectHolder.recording = viewHolder.liveCameraView.isRecording();

            if (avOption.videoCodecType == UVideoProfile.CODEC_MODE_HARD) {
                avOption.videoCodecType = UVideoProfile.CODEC_MODE_SOFT;
            }
            else {
                avOption.videoCodecType = UVideoProfile.CODEC_MODE_HARD;
            }
            viewHolder.liveCameraView.stopRecordingAndDismissPreview();
            startPreview(avOption);
            if (specailEffectHolder.recording) {
                viewHolder.liveCameraView.startRecording();
                recoverSpecialEffect(specailEffectHolder);
                viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }
            return true;
        }

        @Override
        public boolean onVideoFilterModeButtonClick() {
            if (viewHolder.liveCameraView == null) {
                return false;
            }

            if (avOption.videoCodecType == UVideoProfile.CODEC_MODE_SOFT && avOption.videoFilterMode == UFilterProfile.FilterMode.CPU) {
//               soft codec just support cpu filter mode
                avOption.videoCodecType = UVideoProfile.CODEC_MODE_HARD;
            }

            specailEffectHolder.recording = viewHolder.liveCameraView.isRecording();

            if (avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
                avOption.videoFilterMode = UFilterProfile.FilterMode.CPU;
            }
            else {
                avOption.videoFilterMode = UFilterProfile.FilterMode.GPU;
            }
            stopPreview();
            startPreview(avOption);
            if (specailEffectHolder.recording) {
                viewHolder.liveCameraView.startRecording();
                recoverSpecialEffect(specailEffectHolder);
                viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }
            return true;
        }

        @Override
        public boolean onExitButtonClick() {
            if (LiveCameraView.getEasyStreaming().isRecording()) {
                LiveCameraView.getEasyStreaming().stopRecording();
            }
            viewHolder.streamOverContainer.setVisibility(View.VISIBLE);
            return false;
        }

        @Override
        public boolean onOrientationButtonClick() {
            if (viewHolder.liveCameraView == null) {
                return false;
            }
            specailEffectHolder.recording = viewHolder.liveCameraView.isRecording();
            if (avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                avOption.videoCaptureOrientation = UVideoProfile.ORIENTATION_PORTRAIT;
            }
            else {
                avOption.videoCaptureOrientation = UVideoProfile.ORIENTATION_LANDSCAPE;
            }
            stopPreview();
            startPreview(avOption);
            if (specailEffectHolder.recording) {
                viewHolder.liveCameraView.startRecording();
                recoverSpecialEffect(specailEffectHolder);
                viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }

            return false;
        }

        @Override
        public boolean onFaceDetectorButtonClick() {
            if (viewHolder.liveCameraView == null) {
                return false;
            }

            if (avOption.videoFilterMode == UFilterProfile.FilterMode.CPU) {
                Toast.makeText(getContext(), "just support gpu.", Toast.LENGTH_SHORT).show();
                return false;
            }
            specailEffectHolder.faceDetector = !specailEffectHolder.faceDetector;
            initFilters(specailEffectHolder);
            return true;
        }

        @Override
        public boolean onBeautyButtonClick() {
            if (viewHolder.liveCameraView == null) {
                return false;
            }

            specailEffectHolder.beautyFilter = !specailEffectHolder.beautyFilter;
            if (specailEffectHolder.beautyFilter) {
                viewHolder.cameraControllerView.setDebugPannelVisible(View.INVISIBLE);
            }
            initFilters(specailEffectHolder);
            return false;
        }
    }

    private final UStreamStateListener streamStateListener = new UStreamStateListener() {
        //stream state
        @Override
        public void onStateChanged(UStreamStateListener.State state, Object o) {
            switch (state) {
                case PREPARING:
                    break;
                case PREPARED:
                    int filterMode = LiveCameraView.getEasyStreaming().getFilterMode();
                    int codecMode =  LiveCameraView.getEasyStreaming().getCodecMode();
                    if (filterMode != avOption.videoFilterMode || codecMode != avOption.videoCodecType) {
                        if (filterMode != avOption.videoFilterMode) {
                            avOption.videoFilterMode = filterMode;
                            Log.w(TAG, "sync from cloud adapter must use:" + ((avOption.videoFilterMode == UFilterProfile.FilterMode.CPU) ? "CPU filter" : "GPU filter"));
                        }
                        if (codecMode != avOption.videoCodecType) {
                            avOption.videoCodecType = codecMode;
                            Log.w(TAG, "sync from cloud adapter must use:" + ((avOption.videoCodecType == UVideoProfile.CODEC_MODE_SOFT) ? "SOFT codec" : "HARD codec"));
                        }
                        viewHolder.cameraControllerView.updateStreamEnvUI(avOption);
                    }
                    break;
                case CONNECTING:
                    break;
                case CONNECTED:
                    break;
                case START:
                    break;
                case STOP:
                    break;
                case NETWORK_BLOCK:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onStreamError(UStreamStateListener.Error error, Object extra) {
            switch (error) {
                case IOERROR:
                    if (viewHolder.liveCameraView.isPreviewed()) {
                        LiveCameraView.getEasyStreaming().restart();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private final UNetworkListener newtworkListener = new UNetworkListener() {
        @Override
        public void onNetworkStateChanged(State state, Object o) {
            switch (state) {
                case NETWORK_SPEED:
                    break;
                case PUBLISH_STREAMING_TIME:
                    break;
                case DISCONNECT:
                    break;
                case RECONNECT:
                    //网络重新连接
                    if (viewHolder.liveCameraView.isPreviewed()) {
                        LiveCameraView.getEasyStreaming().restart();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private final UCameraSessionListener cameraSessionListener = new UCameraSessionListener() {
        @Override
        public USize[] onPreviewSizeChoose(int cameraId, List<USize> supportPreviewSizeList) {
            return null;
        }

        @Override
        public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndexList, int width, int height) {
            if (viewHolder.liveCameraView != null) {
                viewHolder.liveCameraView.setShowMode(UAspectFrameLayout.Mode.FULL);
                if (avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    viewHolder.liveCameraView.setAspectRatio(((float) width) / height);
                }
                else {
                    viewHolder.liveCameraView.setAspectRatio(((float) height) / width);
                }
            }
        }

        @Override
        public void onCameraError(UCameraSessionListener.Error error, Object extra) {
        }

        @Override
        public void onCameraFlashSwitched(int cameraId, boolean currentState) {
        }

        @Override
        public void onPreviewFrame(int cameraId, byte[] data, int width, int height) {
            if (filters != null) {
                for (UVideoGPUFilter filter: filters) {
                    if (filter instanceof FaceuCompat) {
                        ((FaceuCompat) filter).updateCameraFrame(cameraId, data, width, height);
                    }
                }
            }
        }
    };

    public void onPause() {
        if (viewHolder.liveCameraView != null) {
            viewHolder.liveCameraView.onPause();
            specailEffectHolder.recording = LiveCameraView.getEasyStreaming().isRecording();
            if (!continueCaptureAfterBackHome) {
                stopPreview();
                isDepenedActivityLifecycle = true;
            }
        }
    }

    public void onResume() {
        if (viewHolder.liveCameraView != null) {
            if (avOption != null && !continueCaptureAfterBackHome && isDepenedActivityLifecycle) {
                startPreview(avOption);
                if (specailEffectHolder.recording) {
                    viewHolder.liveCameraView.startRecording();
                    recoverSpecialEffect(specailEffectHolder);
                    viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
                }
            }
        }
    }

    public void onDestroy() {
        if (viewHolder.liveCameraView != null) {
            viewHolder.liveCameraView.release();
        }
    }

    public void attachView(LiveCameraView cameraView) {

        viewHolder.liveCameraView = cameraView;

        initCameraStateLisener();
    }
}
