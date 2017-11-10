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

import com.ucloud.ulive.UCameraSessionListener;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.USize;
import com.ucloud.ulive.UStreamStateListener;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.ext.faceunity.FaceunityCompat;
import com.ucloud.ulive.example.ext.gpuimage.GPUImageCompatibleFilter;
import com.ucloud.ulive.example.filter.audio.UAudioMuteFilter;
import com.ucloud.ulive.example.filter.audio.URawAudioMixFilter;
import com.ucloud.ulive.example.filter.video.cpu.UGrayCPUFilter;
import com.ucloud.ulive.example.filter.video.gpu.UWhiteningGPUVideoFilter;
import com.ucloud.ulive.filter.UVideoCPUFilter;
import com.ucloud.ulive.filter.UVideoGPUFilter;
import com.ucloud.ulive.filter.UVideoGroupGPUFilter;
import com.ucloud.ulive.filter.video.cpu.USkinBeautyCPUFilter;
import com.ucloud.ulive.filter.video.cpu.USkinBlurCPUFilter;
import com.ucloud.ulive.filter.video.gpu.USkinBeautyGPUFilter;
import com.ucloud.ulive.filter.video.gpu.USkinBeautyGPUFilterEx;
import com.ucloud.ulive.filter.video.gpu.USkinSpecialEffectsFilter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import jp.co.cyberagent.android.gpuimage.GPUImageEmbossFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageGrayscaleFilter;

import static butterknife.ButterKnife.bind;

public class LiveRoomView extends FrameLayout {

    private static final String TAG = "LiveRoomView";

    class ViewHolder {
        LiveCameraView liveCameraView;

        @BindView(R.id.camera_controller_view)
        CameraControllerView cameraControllerView;

        @BindView(R.id.live_finish_container)
        View streamOverContainer;

        @BindView(R.id.btn_finish)
        Button backMainIndexButton;

        @BindView(R.id.filter_controller_view)
        FilterControllerView filterControllerView;

        ViewHolder(View view) {
            bind(this, view);
        }
    }

    class SpecailEffectHolder {
        boolean mute;
        boolean mirror;
        boolean mix;
        boolean faceDetector;
        int filterPosition;
        boolean filterSeekeable;
        int filterProgressNums;
    }

    private ViewHolder viewHolder;

    private UVideoCPUFilter skinCPUFilter;

    private UVideoGPUFilter skinGPUFilter;

    private USkinSpecialEffectsFilter specialEffectsFilter;

    private SpecailEffectHolder specailEffectHolder;

    private AVOption avOption;

    //GPU滤镜组合
    private final List<UVideoGPUFilter> filters = new ArrayList<>();

    private Context mContext;

    public LiveRoomView(Context context) {
        super(context);
        mContext = context;
    }

    public LiveRoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public LiveRoomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
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
        //设置监听，处理UI Button状态 & 调试日志面板
        viewHolder.liveCameraView.addCameraSessionListener(viewHolder.cameraControllerView);
        viewHolder.liveCameraView.addStreamStateListener(viewHolder.cameraControllerView);
        viewHolder.liveCameraView.addNetworkListener(viewHolder.cameraControllerView);
        //设置监听，处理业务
        viewHolder.liveCameraView.addCameraSessionListener(cameraSessionListener);
        viewHolder.liveCameraView.addStreamStateListener(streamStateListener);
    }

    private final FilterControllerView.ProgressListener filterProgressListneer = new FilterControllerView.ProgressListener() {

        @Override
        public boolean onProgressChanaged(int ...level) {
            if (avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
                if (skinGPUFilter != null) {
                    if (level.length == 1) {
                        skinGPUFilter.setFilterLevel(level[0]);
                    }
                    if (level.length == 3) {
                        skinGPUFilter.setFilterLevel(level[0], level[1], level[2]);
                    }
                }
               /* if (specialEffectsFilter != null) {
                    specialEffectsFilter.setFilterLevel(level1);
                }*/
            }
            else {
                if (skinCPUFilter != null) {
                    skinCPUFilter.setFilterLevel(level[0]);
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

    public void init(AVOption avOption) {
        if (viewHolder.liveCameraView == null) {
            throw new IllegalStateException("LiveCameraView is finish attach?");
        }
        this.avOption = avOption;
        viewHolder.liveCameraView.init(avOption);
        //根据设置的参数值，更新控制面板UI的状态
        viewHolder.cameraControllerView.updateStreamEnvUI(this.avOption);
    }

    private void resetVideoFilterEnv() {
        LiveCameraView.getEasyStreaming().setVideoGPUFilter(null);
        LiveCameraView.getEasyStreaming().setVideoCPUFilter(null);
        viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
    }

    public static String[] GPU_FILTERS_NAME = {"无", "美颜1", "美颜2", "贴纸", "健康", "甜美", "复古", "蓝调", "怀旧", "童话", "组合", "Demo1", "浮雕", "黑白"};

    public UVideoGPUFilter createGPUFilterByPosition(int position) {
        specailEffectHolder.filterProgressNums = 1;
        switch (position) {
            case 1: //展示UCloud SDK提供的美颜滤镜 综合(美颜 + 红润 + 明暗度调节)
                skinGPUFilter = new USkinBeautyGPUFilterEx();
                specailEffectHolder.filterSeekeable = true;
                skinGPUFilter.setFilterLevel(FilterControllerView.LEVEL1, FilterControllerView.LEVEL2, FilterControllerView.LEVEL3);  //设置视频GPU美颜滤镜级别
                specailEffectHolder.filterProgressNums = 3; //需要三个进度条的滤镜
                return skinGPUFilter;
            case 2: //展示UCloud SDK提供的美颜滤镜
                skinGPUFilter = new USkinBeautyGPUFilter();
                specailEffectHolder.filterSeekeable = true;
                skinGPUFilter.setFilterLevel(FilterControllerView.LEVEL1);  //设置视频GPU美颜滤镜级别
                return skinGPUFilter;
            case 3: //展示faceunity贴纸功能
                skinGPUFilter = new FaceunityCompat(mContext);
                filters.clear();
                filters.add(skinGPUFilter);
                specailEffectHolder.filterSeekeable = false;
                return skinGPUFilter;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                specialEffectsFilter = new USkinSpecialEffectsFilter(position - 3); // 1-6， 若修改了position自行对应
                specialEffectsFilter.setFilterLevel(FilterControllerView.LEVEL1); //设置视频GPU美颜滤镜级别
                specailEffectHolder.filterSeekeable = true;
                return specialEffectsFilter;
            case 10: //展示UCloud SDK提供的美颜滤镜2（1也是可以的） + 特殊风格滤镜 组合滤镜的使用（支持任意和其它一个或多个GPU滤镜组合）
                skinGPUFilter = new USkinBeautyGPUFilter(); //美颜滤镜
                specialEffectsFilter = new USkinSpecialEffectsFilter(USkinSpecialEffectsFilter.BLUE); //其他滤镜可以类似的以组合的形式存在
                skinGPUFilter.setFilterLevel(FilterControllerView.LEVEL1);
                specialEffectsFilter.setFilterLevel(FilterControllerView.LEVEL1);
                filters.clear();
                filters.add(skinGPUFilter);
                filters.add(specialEffectsFilter);
                UVideoGroupGPUFilter filter = new UVideoGroupGPUFilter(filters);
                specailEffectHolder.filterSeekeable = true;
                return filter;
            case 11: //展示Demo当中自定义的滤镜
                skinGPUFilter = new UWhiteningGPUVideoFilter();
                specailEffectHolder.filterSeekeable = false;
                return skinGPUFilter;
            case 12: //展示兼容GPUImage, 根据需要自行选择依赖或者移除
                GPUImageFilter gpuImageFilter = new GPUImageEmbossFilter(); //支持适配直接继承android-gpuiamge/GPUImageFilter的滤镜，不支持组合滤镜
                skinGPUFilter = new GPUImageCompatibleFilter<>(gpuImageFilter);
                specailEffectHolder.filterSeekeable = false;
                return skinGPUFilter;
            case 13: //展示兼容GPUImage, 根据需要自行选择依赖或者移除
                gpuImageFilter = new GPUImageGrayscaleFilter();
                specailEffectHolder.filterSeekeable = false;
                skinGPUFilter = new GPUImageCompatibleFilter<>(gpuImageFilter);
                return skinGPUFilter;
            default:
                specailEffectHolder.filterSeekeable = false;
                return null;
        }
    }

    public static String[] CPU_FILTERS_NAME = {"无", "美颜", "模糊", "黑白"};

    public UVideoCPUFilter createCPUFilterByPosition(int position) {
        switch (position) {
            case 1:
                //创建视频CPU美颜滤镜
                skinCPUFilter = new USkinBeautyCPUFilter();
                skinCPUFilter.setFilterLevel(FilterControllerView.LEVEL1);
                specailEffectHolder.filterSeekeable = true;
                return skinCPUFilter;
            case 2:
                skinCPUFilter = new USkinBlurCPUFilter();
                skinCPUFilter.setFilterLevel(FilterControllerView.LEVEL1);
                specailEffectHolder.filterSeekeable = true;
                return skinCPUFilter;
            case 3:
                specailEffectHolder.filterSeekeable = false;
                skinCPUFilter = new UGrayCPUFilter();
                return skinCPUFilter;
            default:
                specailEffectHolder.filterSeekeable = false;
                return null;
        }
    }

    private void handleVideoFilters(SpecailEffectHolder specailEffectHolder) {
        //初始化UI progress
        if (specailEffectHolder.filterPosition == 1) {
            viewHolder.filterControllerView.initProgress(FilterControllerView.LEVEL1, FilterControllerView.LEVEL2, FilterControllerView.LEVEL3);
        }
        else {
            viewHolder.filterControllerView.initProgress(FilterControllerView.LEVEL1);
        }
        if (avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
            UVideoGPUFilter gpuFilter = createGPUFilterByPosition(specailEffectHolder.filterPosition);
            if (gpuFilter != null) {
                viewHolder.liveCameraView.setVideoGPUFilter(gpuFilter);
            }
            else {
                specialEffectsFilter = null;
                skinGPUFilter = null;
                viewHolder.liveCameraView.setVideoGPUFilter(null);
                viewHolder.liveCameraView.setVideoCPUFilter(null);
                viewHolder.filterControllerView.setVisibility(View.GONE, specailEffectHolder.filterProgressNums);
            }
            //处理GPU滤镜模式
            if (specailEffectHolder.filterSeekeable) {
                viewHolder.filterControllerView.setVisibility(View.VISIBLE, specailEffectHolder.filterProgressNums);  //更新UI, 部分滤镜可以调节强度
            }
            else {
                viewHolder.filterControllerView.setVisibility(View.INVISIBLE, specailEffectHolder.filterProgressNums); //更新UI
            }
        }
        else { //处理CPU滤镜模式
            UVideoCPUFilter cpuFilter = createCPUFilterByPosition(specailEffectHolder.filterPosition);
            if (cpuFilter != null) {
                viewHolder.liveCameraView.setVideoCPUFilter(cpuFilter);
            }
            else {
                skinCPUFilter = null;
                viewHolder.liveCameraView.setVideoGPUFilter(null);
                viewHolder.liveCameraView.setVideoCPUFilter(null);
                viewHolder.filterControllerView.setVisibility(View.GONE, specailEffectHolder.filterProgressNums); //更新UI
            }
            if (specailEffectHolder.filterSeekeable) { //若开启了CPU滤镜，设置到ULiveCameraView
                viewHolder.filterControllerView.setVisibility(View.VISIBLE, specailEffectHolder.filterProgressNums);
            }
            else {
                viewHolder.filterControllerView.setVisibility(View.INVISIBLE, specailEffectHolder.filterProgressNums);
            }
        }
    }

    class MyControllerViewListener extends CameraControllerView.ClickListenerImpl {
        @Override
        public boolean onStartButtonClick() {
            if (viewHolder.liveCameraView.isRecording()) {
                viewHolder.liveCameraView.stopRecording();
                return false;
            }
            else {
                viewHolder.liveCameraView.startRecording();
                return true;
            }
        }

        @Override
        public boolean onAudioMuteButtonClick() {
            specailEffectHolder.mute = !specailEffectHolder.mute;
            if (specailEffectHolder.mute) {
                viewHolder.liveCameraView.setAudioCPUFilter(new UAudioMuteFilter());
                return true;
            }
            else {
                viewHolder.liveCameraView.setAudioCPUFilter(null);
                return false;
            }
        }

        @Override
        public boolean onVideoMirrorButtonClick() {
            specailEffectHolder.mirror = !specailEffectHolder.mirror;
            viewHolder.liveCameraView.applyFrontCameraOutputFlip(specailEffectHolder.mirror);
            return specailEffectHolder.mirror;
        }

        @Override
        public boolean onAudioMixButtonClick() {
            specailEffectHolder.mix = !specailEffectHolder.mix;
            if (specailEffectHolder.mix) {
                URawAudioMixFilter rawAudioMixFilter = new URawAudioMixFilter(getContext(), URawAudioMixFilter.Mode.ANY, true);
                viewHolder.liveCameraView.setAudioCPUFilter(rawAudioMixFilter);
            }
            else {
                viewHolder.liveCameraView.setAudioCPUFilter(null);
            }
            return specailEffectHolder.mix;
        }

        @Override
        public boolean onFlashModeButtonClick() {
            return viewHolder.liveCameraView.toggleFlashMode();
        }

        @Override
        public boolean onSwitchCameraButtonClick() {
            return viewHolder.liveCameraView.switchCamera();
        }

        //一般编码方式需要在初始化指定，demo为了展示效果做了切换逻辑
        //整个过程需要重新初始化，重新打开预览
        @Override
        public boolean onVideoCodecButtonClick() {
            resetVideoFilterEnv(); //编码方式的改变，可能引起视频滤镜模式的变化，重置为null
            if (avOption.videoCodecType == UVideoProfile.CODEC_MODE_HARD && avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
                avOption.videoFilterMode = UFilterProfile.FilterMode.CPU;
            }
            //记录当前是否处于推流状态
            boolean recording = viewHolder.liveCameraView.isRecording();

            if (avOption.videoCodecType == UVideoProfile.CODEC_MODE_HARD) {
                avOption.videoCodecType = UVideoProfile.CODEC_MODE_SOFT;
            }
            else {
                avOption.videoCodecType = UVideoProfile.CODEC_MODE_HARD;
            }
            //停止预览，若在推流内部会自动stopRecording.
            viewHolder.liveCameraView.stopRecordingAndDismissPreview();
            //重新初始化，打开预览
            init(avOption);
            if (recording) {
                viewHolder.liveCameraView.startRecording();
                viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }
            handleVideoFilters(specailEffectHolder); //根据状态，恢复设置对应的滤镜类
            return true;
        }

        //一般视频渲染方式需要在初始化指定，demo为了展示效果做了切换逻辑
        //整个过程需要重新初始化，重新打开预览，滤镜类型发生变化，需要重新设置
        @Override
        public boolean onVideoFilterModeButtonClick() {
            resetVideoFilterEnv(); //引起视频滤镜模式的变化，重置为null
            if (avOption.videoCodecType == UVideoProfile.CODEC_MODE_SOFT && avOption.videoFilterMode == UFilterProfile.FilterMode.CPU) {
                avOption.videoCodecType = UVideoProfile.CODEC_MODE_HARD;
            }

            boolean recording = viewHolder.liveCameraView.isRecording(); //记录当前是否处于推流状态

            if (avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) { //模式切换
                avOption.videoFilterMode = UFilterProfile.FilterMode.CPU;
            }
            else {
                avOption.videoFilterMode = UFilterProfile.FilterMode.GPU;
            }
            viewHolder.liveCameraView.stopRecordingAndDismissPreview(); //停止当前预览
            init(avOption); //重新初始化并打开预览
            if (recording) { //若上一次切换前处于推流状态，重新startRecording.
                viewHolder.liveCameraView.startRecording();
                viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }
            handleVideoFilters(specailEffectHolder); //根据状态，恢复设置对应的滤镜类
            return true;
        }

        //一般推流方向需要在初始化指定，demo为了展示效果做了切换逻辑
        //整个过程需要重新初始化，重新打开预览，需要记录当前状态，切换完进行恢复
        @Override
        public boolean onOrientationButtonClick() {
            resetVideoFilterEnv();
            boolean recording = viewHolder.liveCameraView.isRecording(); //记录当前是否处于推流状态
            if (avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) { //修改采集方向值
                avOption.videoCaptureOrientation = UVideoProfile.ORIENTATION_PORTRAIT;
            }
            else {
                avOption.videoCaptureOrientation = UVideoProfile.ORIENTATION_LANDSCAPE;
            }
            viewHolder.liveCameraView.stopRecordingAndDismissPreview(); //停止当前预览
            init(avOption); //重新初始化并打开预览
            if (recording) { //若上一次切换前处于推流状态，重新startRecording.
                viewHolder.liveCameraView.startRecording();
                viewHolder.cameraControllerView.resetSpecialEffectButtonUI(specailEffectHolder);
            }
            return false;
        }

        @Override
        public boolean onExitButtonClick() {
            if (LiveCameraView.getEasyStreaming().isRecording()) {
                LiveCameraView.getEasyStreaming().stopRecording();
            }
            viewHolder.streamOverContainer.setVisibility(View.VISIBLE);
            return false;
        }

        //由于Faceu人脸贴纸授权过期,布局文件中已经屏蔽，Demo仅展示接入过程
        @Override
        public boolean onFaceDetectorButtonClick() {
            if (viewHolder.liveCameraView == null) {
                return false;
            }
            if (avOption.videoFilterMode == UFilterProfile.FilterMode.CPU) {
                Toast.makeText(getContext(), "人脸贴纸，仅GPU模式下支持.", Toast.LENGTH_SHORT).show();
                return false;
            }
            specailEffectHolder.faceDetector = !specailEffectHolder.faceDetector;
            handleVideoFilters(specailEffectHolder);
            return true;
        }

        @Override
        public boolean onBeautyTypeItemSelectedListener(int position, long id) {
            specailEffectHolder.filterPosition = position;
            if (position != 0) {
                viewHolder.cameraControllerView.setDebugPannelVisible(View.INVISIBLE);
            }
            handleVideoFilters(specailEffectHolder);
            return false;
        }
    }

    private final UStreamStateListener streamStateListener = new UStreamStateListener() {
        //stream state
        @Override
        public void onStateChanged(UStreamStateListener.State state, Object o) {
            switch (state) {
                case PREPARED:
                    int filterMode = LiveCameraView.getEasyStreaming().getFilterMode();
                    int codecMode =  LiveCameraView.getEasyStreaming().getCodecMode();
                    //检查设置的滤镜模式&编码模式是否有发生过改变
                    //sdk内部对问题机型，做了兼容性处理
                    if (filterMode != avOption.videoFilterMode || codecMode != avOption.videoCodecType) {
                        if (filterMode != avOption.videoFilterMode) {
                            avOption.videoFilterMode = filterMode;
                            Log.w(TAG, "UCloud->根据云适配，发现当前设备存在兼容性问题需要使用:" + ((avOption.videoFilterMode == UFilterProfile.FilterMode.CPU) ? "CPU滤镜" : "GPU滤镜"));
                        }
                        if (codecMode != avOption.videoCodecType) {
                            avOption.videoCodecType = codecMode;
                            Log.w(TAG, "UCloud->根据云适配，发现当前设备存在兼容性问题需要使用:" + ((avOption.videoCodecType == UVideoProfile.CODEC_MODE_SOFT) ? "软编" : "硬编"));
                        }
                        viewHolder.cameraControllerView.updateStreamEnvUI(avOption);
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onStreamError(UStreamStateListener.Error error, Object extra) {
            switch (error) {
                case IOERROR:
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
                    if (filter instanceof FaceunityCompat) {
                        ((FaceunityCompat) filter).updateCameraFrame(data, width, height);
                    }
                }
            }
        }
    };

    public void onPause() {
        if (viewHolder.liveCameraView != null) {
            viewHolder.liveCameraView.onPause();
        }
    }

    public void onResume() {
        if (viewHolder.liveCameraView != null) {
            viewHolder.liveCameraView.onResume();
        }
    }

    public void onDestroy() {
        if (viewHolder.liveCameraView != null) {
            viewHolder.liveCameraView.onDestroy();
        }
    }

    public void attachView(LiveCameraView cameraView) {
        if (cameraView == null) {
            throw new IllegalArgumentException("LiveCameraView不能为null.");
        }
        viewHolder.liveCameraView = cameraView;
        initCameraStateLisener();
    }
}
