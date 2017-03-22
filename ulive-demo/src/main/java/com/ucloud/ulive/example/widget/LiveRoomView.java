package com.ucloud.ulive.example.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
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
import com.ucloud.ulive.filter.UAudioCPUFilter;
import com.ucloud.ulive.filter.UVideoGPUFilter;
import com.ucloud.ulive.filter.UVideoGroupGPUFilter;
import com.ucloud.ulive.filter.video.cpu.USkinBeautyCPUFilter;
import com.ucloud.ulive.filter.video.gpu.USkinBeautyGPUFilter;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by lw.tan on 2017/3/2.
 */

public class LiveRoomView extends FrameLayout {

    public static final String TAG = "LiveRoomView";

    class ViewHolder {
        @Bind(R.id.livecamera)
        LiveCameraView mLiveCameraView;

        @Bind(R.id.livecamera_pannel)
        ControllerView mControllerView;

        @Bind(R.id.live_finish_container)
        RelativeLayout mStreamOverContainer;

        @Bind(R.id.btn_finish)
        Button backMainIndexButton;

        @Bind(R.id.filter_level_bar)
        FilterControllerView mFilterControllerView;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
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

    private AVOption mAVOptions;

    private List<UVideoGPUFilter> filters = new ArrayList<>();

    private boolean isNeedContinueCaptureAfterBackToMainHome = false; //不推荐为true 容易出现异常

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
        viewHolder.mControllerView.setOnPannelClickListener(new MyControllerViewListener());
        //for ui
        viewHolder.mLiveCameraView.addCameraSessionListener(viewHolder.mControllerView);
        viewHolder.mLiveCameraView.addStreamStateListener(viewHolder.mControllerView);
        viewHolder.mLiveCameraView.addNetworkListener(viewHolder.mControllerView);
        //for business
        viewHolder.mLiveCameraView.addCameraSessionListener(mCameraSessionListener);
        viewHolder.mLiveCameraView.addStreamStateListener(mStreamStateListener);
        viewHolder.mLiveCameraView.addNetworkListener(mNetworkListener);
        viewHolder.backMainIndexButton.setOnClickListener(backMainIndexButtonClickListener);
        viewHolder.mFilterControllerView.setListener(mFilterProgressListneer);
    }

    FilterControllerView.Listener mFilterProgressListneer = new FilterControllerView.Listener(){

        @Override
        public boolean onProgressChanaged(int level1, int level2, int level3) {
            UAudioCPUFilter audioProfile = LiveCameraView.getInstance().acquireAudioCPUFilter();
            if (audioProfile != null && audioProfile instanceof URawAudioMixFilter) {
                URawAudioMixFilter URawAudioMixFilter = (URawAudioMixFilter) audioProfile;
                URawAudioMixFilter.adjustBackgroundMusicVolumeLevel(level1 * 1.0f /  100.0f);
                URawAudioMixFilter.adjustMiscVolumeLevel(level2 * 1.0f /  100.0f);
                LiveCameraView.getInstance().releaseAudioCPUFilter();
            }
            for(UVideoGPUFilter filter: filters) {
                if (filter instanceof USkinBeautyGPUFilter) {
                    ((USkinBeautyGPUFilter)filter).setFilterLevel(level1, level2, level3);
                }
            }
            return false;
        }
    };

    private View.OnClickListener backMainIndexButtonClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Context context = getContext();
            if (context instanceof Activity) {
                ((Activity)(context)).finish();
            }
        }
    };

    public void startPreview(AVOption avOptions) {
        mAVOptions = avOptions;
        if (viewHolder.mLiveCameraView == null) {
            throw new IllegalStateException("LiveRoomView is finish flate?");
        }
        viewHolder.mLiveCameraView.init(avOptions);
        viewHolder.mControllerView.updateStreamEnvUI(mAVOptions);
    }

    public void stopPreview() {
        viewHolder.mLiveCameraView.stopRecordingAndDismissPreview();
    }

    private void recoverSpecialEffect(SpecailEffectHolder specailEffectHolder) {
        if (specailEffectHolder == null) return;
        if (specailEffectHolder.mute) {
            LiveCameraView.getInstance().setAudioCPUFilter(new UAudioMuteFilter());
        } else {
            LiveCameraView.getInstance().setAudioCPUFilter(null);
        }

        if (specailEffectHolder.mix) {
            URawAudioMixFilter URawAudioMixFilter = new URawAudioMixFilter(getContext(), com.ucloud.ulive.example.filter.audio.URawAudioMixFilter.Mode.ANY, true);
            LiveCameraView.getInstance().setAudioCPUFilter(URawAudioMixFilter);
        } else {
            LiveCameraView.getInstance().setAudioCPUFilter(null);
        }

        LiveCameraView.getInstance().frontCameraFlipHorizontal(specailEffectHolder.mirror);

        initFilters(specailEffectHolder);
    }

    private void initFilters(SpecailEffectHolder specailEffectHolder) {
        //cpu filter
        USkinBeautyCPUFilter skinBeautyCPUFilter = new USkinBeautyCPUFilter(getContext());
        skinBeautyCPUFilter.setRadius(20 / 4);
        //gpu filter
        USkinBeautyGPUFilter skinBeautyGPUFilter = new USkinBeautyGPUFilter();
        skinBeautyGPUFilter.setFilterLevel(FilterControllerView.level1, FilterControllerView.level2, FilterControllerView.level3);
        viewHolder.mFilterControllerView.initProgress(FilterControllerView.level1, FilterControllerView.level2, FilterControllerView.level3);
        //add to list
        filters.clear();
        //open group filter ucloud skin beauty & faceu detector
        if (mAVOptions.videoFilterMode == UFilterProfile.FilterMode.GPU) {
            if (specailEffectHolder.beautyFilter) {
                filters.add(skinBeautyGPUFilter);
                viewHolder.mFilterControllerView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.mFilterControllerView.setVisibility(View.GONE);
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
                LiveCameraView.getInstance().setVideoGPUFilter(gpuGroupFilter);
            } else {
                LiveCameraView.getInstance().setVideoGPUFilter(null);
                LiveCameraView.getInstance().setVideoCPUFilter(null);
                viewHolder.mFilterControllerView.setVisibility(View.GONE);
            }
        } else {
            if (specailEffectHolder.beautyFilter) {
                LiveCameraView.getInstance().setVideoCPUFilter(skinBeautyCPUFilter);
            } else {
                LiveCameraView.getInstance().setVideoGPUFilter(null);
                LiveCameraView.getInstance().setVideoCPUFilter(null);
                viewHolder.mFilterControllerView.setVisibility(View.GONE);
            }
        }
    }

    private void resetSpecialEffectEnv() {
        specailEffectHolder.mute = false;
        specailEffectHolder.mirror = false;
        specailEffectHolder.mix = false;
        specailEffectHolder.faceDetector = false;
        specailEffectHolder.beautyFilter = false;
        LiveCameraView.getInstance().setAudioCPUFilter(null);
        LiveCameraView.getInstance().setVideoGPUFilter(null);
        LiveCameraView.getInstance().setVideoCPUFilter(null);
        viewHolder.mControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
    }

    class MyControllerViewListener extends ControllerView.OnControllerViewClickListenerImpl {
        @Override
        public boolean onStartButtonClick() {
            if(LiveCameraView.getInstance().isRecording()) {
                LiveCameraView.getInstance().stopRecording();
                resetSpecialEffectEnv();
                return false;
            } else {
                LiveCameraView.getInstance().startRecording();
                return true;
            }
        }

        @Override
        public boolean onAudioMuteButtonClick() {
            specailEffectHolder.mute = !specailEffectHolder.mute;
            if (specailEffectHolder.mute) {
                LiveCameraView.getInstance().setAudioCPUFilter(new UAudioMuteFilter());
                return true;
            } else {
                LiveCameraView.getInstance().setAudioCPUFilter(null);
                return false;
            }
        }

        @Override
        public boolean onVideoMirrorButtonClick() {
            specailEffectHolder.mirror = !specailEffectHolder.mirror;
            LiveCameraView.getInstance().frontCameraFlipHorizontal(specailEffectHolder.mirror);
            return specailEffectHolder.mirror;
        }

        @Override
        public boolean onAudioMixButtonClick() {
            specailEffectHolder.mix = !specailEffectHolder.mix;
            if (specailEffectHolder.mix) {
                URawAudioMixFilter URawAudioMixFilter = new URawAudioMixFilter(getContext(), com.ucloud.ulive.example.filter.audio.URawAudioMixFilter.Mode.ANY, true);
                LiveCameraView.getInstance().setAudioCPUFilter(URawAudioMixFilter);
            } else {
                LiveCameraView.getInstance().setAudioCPUFilter(null);
            }
            return specailEffectHolder.mix;
        }

        @Override
        public boolean onFlashModeButtonClick() {
            return LiveCameraView.getInstance().toggleFlashMode();
        }

        @Override
        public boolean onSwitchCameraButtonClick() {
            return LiveCameraView.getInstance().switchCamera();
        }

        @Override
        public boolean onVideoCodecButtonClick() {
            if (mAVOptions.videoCodecType == UVideoProfile.CODEC_MODE_HARD && mAVOptions.videoFilterMode == UFilterProfile.FilterMode.GPU) {
//               cpu filter support sw & hard codec mode
//               gpu filter just support hard codec mode
                mAVOptions.videoFilterMode = UFilterProfile.FilterMode.CPU;
            }
            specailEffectHolder.recording = viewHolder.mLiveCameraView.isRecording();
            if (mAVOptions.videoCodecType == UVideoProfile.CODEC_MODE_HARD) {
                mAVOptions.videoCodecType = UVideoProfile.CODEC_MODE_SOFT;
            } else {
                mAVOptions.videoCodecType = UVideoProfile.CODEC_MODE_HARD;
            }
            viewHolder.mLiveCameraView.stopRecordingAndDismissPreview();
            startPreview(mAVOptions);
            if (specailEffectHolder.recording) {
                viewHolder.mLiveCameraView.startRecording();
                recoverSpecialEffect(specailEffectHolder);
                viewHolder.mControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }
            return true;
        }

        @Override
        public boolean onVideoFilterModeButtonClick() {
            if (mAVOptions.videoCodecType == UVideoProfile.CODEC_MODE_SOFT && mAVOptions.videoFilterMode == UFilterProfile.FilterMode.CPU) {
//               soft codec just support cpu filter mode
                mAVOptions.videoCodecType = UVideoProfile.CODEC_MODE_HARD;
            }
            specailEffectHolder.recording = viewHolder.mLiveCameraView.isRecording();

            if (mAVOptions.videoFilterMode == UFilterProfile.FilterMode.GPU) {
                mAVOptions.videoFilterMode = UFilterProfile.FilterMode.CPU;
            } else {
                mAVOptions.videoFilterMode = UFilterProfile.FilterMode.GPU;
            }
            stopPreview();
            startPreview(mAVOptions);
            if (specailEffectHolder.recording) {
                viewHolder.mLiveCameraView.startRecording();
                recoverSpecialEffect(specailEffectHolder);
                viewHolder.mControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }
            return true;
        }

        @Override
        public boolean onExitButtonClick() {
            if (LiveCameraView.getInstance().isRecording()) {
                LiveCameraView.getInstance().stopRecording();
            }
            viewHolder.mStreamOverContainer.setVisibility(View.VISIBLE);
            return false;
        }

        @Override
        public boolean onVidoCaptureOrientationButtonClick() {
            specailEffectHolder.recording = viewHolder.mLiveCameraView.isRecording();
            if (mAVOptions.videoCaptureOrientation ==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mAVOptions.videoCaptureOrientation = UVideoProfile.ORIENTATION_PORTRAIT;
            } else {
                mAVOptions.videoCaptureOrientation = UVideoProfile.ORIENTATION_LANDSCAPE;
            }
            stopPreview();
            startPreview(mAVOptions);
            if (specailEffectHolder.recording) {
                viewHolder.mLiveCameraView.startRecording();
                recoverSpecialEffect(specailEffectHolder);
                viewHolder.mControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }

            return false;
        }

        @Override
        public boolean onFaceDetectorButtonClick() {
            if (mAVOptions.videoFilterMode == UFilterProfile.FilterMode.CPU) {
                Toast.makeText(getContext(), "just support gpu.", Toast.LENGTH_SHORT).show();
                return false;
            }
            specailEffectHolder.faceDetector = !specailEffectHolder.faceDetector;
            initFilters(specailEffectHolder);
            return true;
        }

        @Override
        public boolean onBeautyButtonClick() {
            specailEffectHolder.beautyFilter = !specailEffectHolder.beautyFilter;
            if (specailEffectHolder.beautyFilter) {
                viewHolder.mControllerView.setDebugPannelVisible(View.INVISIBLE);
            }
            initFilters(specailEffectHolder);
            return false;
        }
    }

    UStreamStateListener mStreamStateListener = new UStreamStateListener() {
        //stream state
        @Override
        public void onStateChanged(UStreamStateListener.State state, Object o) {
            switch (state) {
                case PREPARING:
                    break;
                case PREPARED:
                    if (LiveCameraView.getInstance().getFilterMode() != mAVOptions.videoFilterMode) {
                        mAVOptions.videoFilterMode = LiveCameraView.getInstance().getFilterMode();
                        Log.w(TAG,"sync from cloud adapter must use:" + ((mAVOptions.videoFilterMode == UFilterProfile.FilterMode.CPU) ? "CPU filter" : "GPU filter"));
                        viewHolder.mControllerView.updateStreamEnvUI(mAVOptions);
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
            }
        }

        @Override
        public void onStreamError(UStreamStateListener.Error error, Object extra) {
            switch (error) {
                case IOERROR:
                    if (viewHolder.mLiveCameraView.isPreviewed()) {
                        LiveCameraView.getInstance().restart();
                    }
                    break;
            }
        }
    };

    UNetworkListener mNetworkListener = new UNetworkListener() {
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
                    if (viewHolder.mLiveCameraView.isPreviewed()) {
                        LiveCameraView.getInstance().restart();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    UCameraSessionListener mCameraSessionListener = new UCameraSessionListener() {
        @Override
        public USize[] onPreviewSizeChoose(int cameraId, List<USize> cameraSupportPreviewSize) {
            return null;
        }

        @Override
        public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndex, int width, int height) {
            if (mAVOptions.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                viewHolder.mLiveCameraView.setAspectRatio(((float) width) / height);
            } else {
                viewHolder.mLiveCameraView.setAspectRatio(((float) height) / width);
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
                for(UVideoGPUFilter filter: filters) {
                    if (filter instanceof FaceuCompat) {
                        ((FaceuCompat) filter).updateCameraFrame(cameraId, data, width, height);
                    }
                }
            }
        }
    };

    public void onPause() {
        viewHolder.mLiveCameraView.onPause();
        specailEffectHolder.recording = LiveCameraView.getInstance().isRecording();
        if (!isNeedContinueCaptureAfterBackToMainHome) {
            stopPreview();
            isDepenedActivityLifecycle = true;
        }
    }

    public void onResume() {
        if (mAVOptions != null && !isNeedContinueCaptureAfterBackToMainHome && isDepenedActivityLifecycle) {
            startPreview(mAVOptions);
            if (specailEffectHolder.recording) {
                viewHolder.mLiveCameraView.startRecording();
                recoverSpecialEffect(specailEffectHolder);
                viewHolder.mControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }
        }
    }

    public void onDestroy() {
        viewHolder.mLiveCameraView.release();
    }
}