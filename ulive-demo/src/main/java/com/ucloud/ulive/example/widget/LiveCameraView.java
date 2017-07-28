package com.ucloud.ulive.example.widget;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.ucloud.ulive.UCameraSessionListener;
import com.ucloud.ulive.UEasyStreaming;
import com.ucloud.ulive.UNetworkListener;
import com.ucloud.ulive.USize;
import com.ucloud.ulive.UStreamStateListener;
import com.ucloud.ulive.UStreamingProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.filter.audio.UAudioMuteFilter;
import com.ucloud.ulive.example.utils.StreamProfileUtil;
import com.ucloud.ulive.filter.UAudioCPUFilter;
import com.ucloud.ulive.filter.UVideoCPUFilter;
import com.ucloud.ulive.filter.UVideoGPUFilter;

import java.util.ArrayList;
import java.util.List;

/**
 ** 封装面向View推流工具，关键方法

  1.添加LiveCameraView(在xml布局文件或者动态addView都可以)

  <com.ucloud.ulive.example.widget.LiveCameraView
    android:id="@+id/livecamera"
    android:background="@color/transparent"
    android:layout_gravity="center"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>

   2.打开预览
     liveCameraView.init(AVOption), AVOption属于Demo封装的类，包括了主要的推流参数属性

   3.停止推流,销毁预览
     liveCameraView.stopRecordingAndDismissPreview(); 若调用了下次需要重新调用 liveCameraView.init(AVOption)

   4.停止推流，不销毁预览
     liveCameraView.stopRecordingStilCpature(); 下次可以直接调用liveCamera.startRecording().

   5.生命周期依赖
      在Activity的onPause或者onStop中调用 -> liveCameraView.onPause();
      在Activity的onResume中调用 -> liveCameraView.onResume();
      在Activity的onDestroy中调用 -> liveCameraView.onDestroy();

      依赖已经管理好了推流的状态信息
 */
public class LiveCameraView extends FrameLayout {

    private static final String TAG = "LiveCameraView";

    private TextureView textureView;

    private final List<UCameraSessionListener> outerCameraSessionListeners = new ArrayList<>();

    private final List<UStreamStateListener> outerStreamStateListeners = new ArrayList<>();

    private final List<UNetworkListener> outerNetworkListeners = new ArrayList<>();

    private boolean initState = false;

    /**
     * 推流相关的参数
     */
    private static class StreamEnvHodler {

        private boolean previewed = false;

        private UEasyStreaming easyStreamer;

        private AVOption avOption;

        private SurfaceTexture tempTexture;

        private int tempStWidth;

        private int tempStHeight;

        private boolean tempSurfaceTextureAvalible = false;

        private UStreamingProfile streamingProfile;
    }

    private static final StreamEnvHodler STREAM_ENV_HOLDER = new StreamEnvHodler();

    private static class SpecailEffectHolder {
        boolean recording;
        boolean mirror;
        boolean mute;
    }

    private SpecailEffectHolder specailEffectHolder = new SpecailEffectHolder();

    private UVideoCPUFilter videoCPUFilter;

    private UVideoGPUFilter videoGPUFilter;

    private UAudioCPUFilter audioCPUFilter;

    public LiveCameraView(Context context) {
        super(context);
    }

    public LiveCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static synchronized UEasyStreaming getEasyStreaming() {
        if (STREAM_ENV_HOLDER.easyStreamer == null) {
            STREAM_ENV_HOLDER.easyStreamer = UEasyStreaming.Factory.newInstance();
        }
        return STREAM_ENV_HOLDER.easyStreamer;
    }

    /**
     * 添加Camera状态监听
     * @param listener
     */
    public void addCameraSessionListener(UCameraSessionListener listener) {
        if (listener != null && !outerCameraSessionListeners.contains(listener)) {
            outerCameraSessionListeners.add(listener);
        }
    }

    /**
     * 添加推流状态监听
     * @param listener
     */
    public void addStreamStateListener(UStreamStateListener listener) {
        if (listener != null && !outerStreamStateListeners.contains(listener)) {
            outerStreamStateListeners.add(listener);
        }
    }

    /**
     * 添加网络状态监听
     * @param listener
     */
    public void addNetworkListener(UNetworkListener listener) {
        if (listener != null && !outerNetworkListeners.contains(listener)) {
            outerNetworkListeners.add(listener);
        }
    }

    /**
     * 根据AVOption初始化&打开预览
     * @param avOption
     */
    public void init(AVOption avOption) {
        if (avOption == null) {
            throw new IllegalArgumentException("AVOption is null.");
        }
        STREAM_ENV_HOLDER.avOption = avOption;
        STREAM_ENV_HOLDER.streamingProfile = StreamProfileUtil.build(avOption);
        if (STREAM_ENV_HOLDER.avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            if (getContext() instanceof Activity) {
                ((Activity) getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        else {
            if (getContext() instanceof Activity) {
                ((Activity) getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        innerStartPreview(STREAM_ENV_HOLDER.streamingProfile);
    }

    protected void startPreview() {
        if (STREAM_ENV_HOLDER.easyStreamer != null
                && STREAM_ENV_HOLDER.tempSurfaceTextureAvalible
                && STREAM_ENV_HOLDER.tempTexture != null
                && STREAM_ENV_HOLDER.tempStWidth > 0
                && STREAM_ENV_HOLDER.tempStHeight > 0) {
            STREAM_ENV_HOLDER.easyStreamer.startPreview(STREAM_ENV_HOLDER.tempTexture, STREAM_ENV_HOLDER.tempStWidth, STREAM_ENV_HOLDER.tempStHeight);
        }
        STREAM_ENV_HOLDER.previewed = true;
    }

    /**
     * 开始推流
     */
    public void startRecording() {
        if (!isPreviewed()) {
            init(STREAM_ENV_HOLDER.avOption);
        }
        STREAM_ENV_HOLDER.streamingProfile.setStreamUrl(STREAM_ENV_HOLDER.avOption.streamUrl);
        getEasyStreaming().startRecording();
    }

    /**
     * 停止推流 & 预览效果销毁，下次需要重新调用LiveCameraPreview.init(AVOption)方法开启预览
     */
    public void stopRecordingAndDismissPreview() {
        stopPreviewTextureView(true);
    }

    /**
     * 停止推流 & 预览效果不消失
     */
    private void stopRecordingStilCpature() {
        try {
            if (STREAM_ENV_HOLDER.easyStreamer != null) {
                STREAM_ENV_HOLDER.easyStreamer.stopRecording();
            }
        }
        catch (Exception e) {
            textureView = null;
            STREAM_ENV_HOLDER.easyStreamer = null;
            STREAM_ENV_HOLDER.previewed = false;
        }
    }

    /**
     * 停止推流 默认预览效果不消失
     */
    public void stopRecording() {
        stopRecordingStilCpature();
    }

    /**
     * 是否处于推流状态
     * @return
     */
    public boolean isRecording() {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            return STREAM_ENV_HOLDER.easyStreamer.isRecording();
        }
        else {
            return false;
        }
    }

    /**
     * 切换摄像头
     * @return
     */
    public boolean switchCamera() {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            return STREAM_ENV_HOLDER.easyStreamer.switchCamera();
        }
        else {
            return false;
        }
    }

    /**
     * 切换摄像头闪关灯状态
     * @return
     */
    public boolean toggleFlashMode() {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            return STREAM_ENV_HOLDER.easyStreamer.toggleFlashMode();
        }
        else {
            return false;
        }
    }

    /**
     * 设置前置摄像头编码时是否需要镜像
     * @param state
     * @return
     */
    public boolean applyFrontCameraOutputFlip(boolean state) {
        specailEffectHolder.mirror = state;
        if (isPreviewed()) {
            STREAM_ENV_HOLDER.easyStreamer.frontCameraFlipHorizontal(state);
            return true;
        }
        else {
            return false;
        }
    }

    //删除release方法，状态整合到onDestroy方法

    /**
     * 在Activity onPause方法或者onStop中调用
     */
    public void onPause() {
        specailEffectHolder.recording = isRecording();
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            initState = true;
            STREAM_ENV_HOLDER.easyStreamer.onPause();
        }
        stopRecordingAndDismissPreview();
    }

    /**
     * 在Activity onResume方法中调用
     */
    public void onResume() {
        if (initState) { //第一次onResume不调用，onPause或者onStop时 置为true
            init(STREAM_ENV_HOLDER.avOption);
        }
    }

    /**
     * 在Activity onDestroy方法中调用
     */
    public void onDestroy() {
        onPause();
        STREAM_ENV_HOLDER.easyStreamer = null;
        STREAM_ENV_HOLDER.streamingProfile = null;
        STREAM_ENV_HOLDER.tempSurfaceTextureAvalible = false;
        STREAM_ENV_HOLDER.tempTexture = null;
        STREAM_ENV_HOLDER.tempStHeight = 0;
        STREAM_ENV_HOLDER.tempStWidth = 0;
        outerCameraSessionListeners.clear();
        outerNetworkListeners.clear();
        outerStreamStateListeners.clear();
        audioCPUFilter = null;
        videoCPUFilter = null;
        videoGPUFilter = null;
        specailEffectHolder.mirror = false;
        specailEffectHolder.recording = false;
    }

    /**
     * 处理prepare & start preview逻辑
     * @param profile 推流参数信息
     */
    private void innerStartPreview(UStreamingProfile profile) {
//        setShowMode(Mode.FULL);
        STREAM_ENV_HOLDER.easyStreamer = getEasyStreaming();
        STREAM_ENV_HOLDER.streamingProfile = profile;
        STREAM_ENV_HOLDER.easyStreamer.setOnCameraSessionListener(innerCameraSessionListener);
        STREAM_ENV_HOLDER.easyStreamer.setOnStreamStateListener(innerStreamStateListener);
        STREAM_ENV_HOLDER.easyStreamer.setOnNetworkStateListener(innerNetworkStateListener);
        boolean isSucceed = STREAM_ENV_HOLDER.easyStreamer.prepare(STREAM_ENV_HOLDER.streamingProfile);
        if (!isSucceed) {
            Log.w(TAG, "推流prepare方法返回false, 状态异常，检查是否重复调用了UEasyStreaming.prepare.");
        }
        initPreviewTextureView();
    }

    /**
     * 处理添加预览View
     */
    private void initPreviewTextureView() {
        if (textureView == null) {
            textureView = new TextureView(getContext());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            this.removeAllViews();
            this.addView(textureView);
            textureView.setKeepScreenOn(true);
            textureView.setSurfaceTextureListener(surfaceTextureListenerImpl);
        }
    }

    /**
     * 内部停止预览
     * @param isRemoveAllViews true销毁掉预览View
     */
    private void stopPreviewTextureView(boolean isRemoveAllViews) {
        try {
            if (STREAM_ENV_HOLDER.easyStreamer != null) {
                STREAM_ENV_HOLDER.easyStreamer.setOnCameraSessionListener(null);
                STREAM_ENV_HOLDER.easyStreamer.stopRecording();
                STREAM_ENV_HOLDER.easyStreamer.stopPreview(isRemoveAllViews);
                STREAM_ENV_HOLDER.easyStreamer.onDestroy();
                STREAM_ENV_HOLDER.tempSurfaceTextureAvalible = !isRemoveAllViews;
                if (isRemoveAllViews) {
                    removeAllViews();
                    textureView = null;
                }
                STREAM_ENV_HOLDER.easyStreamer = null;
            }
        }
        catch (Exception e) {
            textureView = null;
            STREAM_ENV_HOLDER.easyStreamer = null;
            Log.e(TAG, "stopPreviewTextureView方法发生异常.");
        }
        finally {
            STREAM_ENV_HOLDER.previewed = false;
        }
    }

    TextureView.SurfaceTextureListener surfaceTextureListenerImpl  = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (STREAM_ENV_HOLDER.easyStreamer != null) {
                STREAM_ENV_HOLDER.previewed = true; //记录当前LiveCameraView是否处于预览状态
                STREAM_ENV_HOLDER.easyStreamer.startPreview(surface, width, height); //开启预览
                STREAM_ENV_HOLDER.easyStreamer.frontCameraFlipHorizontal(specailEffectHolder.mirror);
            }
            STREAM_ENV_HOLDER.tempTexture = surface;
            STREAM_ENV_HOLDER.tempStWidth = width;
            STREAM_ENV_HOLDER.tempStHeight = height;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            if (STREAM_ENV_HOLDER.easyStreamer != null) {
                STREAM_ENV_HOLDER.easyStreamer.updatePreview(width, height); //更新预览
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (STREAM_ENV_HOLDER.easyStreamer != null) {
                STREAM_ENV_HOLDER.previewed = false; //记录当前LiveCameraView是否处于预览状态
                STREAM_ENV_HOLDER.easyStreamer.stopPreview(true); //停止预览
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private final UCameraSessionListener innerCameraSessionListener = new UCameraSessionListener() {
        @Override
        public USize[] onPreviewSizeChoose(int cameraId, List<USize> supportPreviewSizeList) {
            if (outerCameraSessionListeners.size() >= 1) {
                return outerCameraSessionListeners.get(outerCameraSessionListeners.size() - 1).onPreviewSizeChoose(cameraId, supportPreviewSizeList);
            }
            return null;
        }

        @Override
        public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndexList, int width, int height) {
//            if (STREAM_ENV_HOLDER.avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//                setAspectRatio(((float) width) / height);
//            }
//            else {
//                setAspectRatio(((float) height) / width);
//            }

            for (UCameraSessionListener listener: outerCameraSessionListeners) {
                listener.onCameraOpenSucceed(cameraId, supportCameraIndexList, width, height);
            }
        }

        @Override
        public void onCameraError(UCameraSessionListener.Error error, Object extra) {
            for (UCameraSessionListener listener: outerCameraSessionListeners) {
                listener.onCameraError(error, extra);
            }
        }

        @Override
        public void onCameraFlashSwitched(int cameraId, boolean currentState) {
            for (UCameraSessionListener listener: outerCameraSessionListeners) {
                listener.onCameraFlashSwitched(cameraId, currentState);
            }
        }

        @Override
        public void onPreviewFrame(int cameraId, byte[] data, int width, int height) {
            for (UCameraSessionListener listener: outerCameraSessionListeners) {
                listener.onPreviewFrame(cameraId, data, width, height);
            }
        }
    };

    private final UStreamStateListener innerStreamStateListener = new UStreamStateListener() {
        @Override
        public void onStateChanged(UStreamStateListener.State state, Object extra) {
            switch (state) {
                case PREPARED:
                    //若上一次销毁了预览需要重新设置
                    STREAM_ENV_HOLDER.easyStreamer.setAudioCPUFilter(audioCPUFilter);
                    STREAM_ENV_HOLDER.easyStreamer.setVideoGPUFilter(videoGPUFilter);
                    STREAM_ENV_HOLDER.easyStreamer.setVideoCPUFilter(videoCPUFilter);
                    if (specailEffectHolder.recording) {
                        startRecording();
                    }
                    break;
                default:
                    break;
            }
            for (UStreamStateListener listener: outerStreamStateListeners) {
                listener.onStateChanged(state, extra);
            }
        }

        @Override
        public void onStreamError(UStreamStateListener.Error error, Object extra) {
            if (isPreviewed()) {
                getEasyStreaming().restart();
            }
            for (UStreamStateListener listener: outerStreamStateListeners) {
                listener.onStreamError(error, extra);
            }
        }
    };

    private final UNetworkListener innerNetworkStateListener = new UNetworkListener() {

        @Override
        public void onNetworkStateChanged(UNetworkListener.State state, Object extra) {
            switch (state) {
                case NETWORK_SPEED:
                    break;
                case PUBLISH_STREAMING_TIME:
                    break;
                case DISCONNECT:
                    break;
                case RECONNECT:
                    //网络重新连接
                    if (isPreviewed()) {
                        LiveCameraView.getEasyStreaming().restart();
                    }
                    break;
                default:
                    break;
            }

            for (UNetworkListener listener: outerNetworkListeners) {
                listener.onNetworkStateChanged(state, extra);
            }
        }
    };

    //删除过时的方法 UEasyStreaming getInstance()

    //返回是否开启了Camera预览
    public synchronized boolean isPreviewed() {
        return STREAM_ENV_HOLDER.previewed;
    }

    public void setVideoCPUFilter(UVideoCPUFilter videoCPUFilter) {
        this.videoCPUFilter = videoCPUFilter;
        if (isPreviewed()) {
            getEasyStreaming().setVideoCPUFilter(videoCPUFilter);
        }
    }

    public void setVideoGPUFilter(UVideoGPUFilter videoGPUFilter) {
        this.videoGPUFilter = videoGPUFilter;
        if (isPreviewed()) {
            getEasyStreaming().setVideoGPUFilter(videoGPUFilter);
        }
    }

    public void setAudioCPUFilter(UAudioCPUFilter audioCPUFilter) {
        this.audioCPUFilter = audioCPUFilter;
        if (this.audioCPUFilter != null && audioCPUFilter instanceof UAudioMuteFilter) {
            //由于SDK提供的面向接口,可自定义扩展音频滤镜的操作，SDK内部并不知道您对音频做了什么样的操作(比如混音，静音, 以及连麦时的音频处理等，若需要区分可以自行判断)
            specailEffectHolder.mute = true;
            //当前处于静音
        }
        if (this.audioCPUFilter == null) {
            specailEffectHolder.mute = false;
        }
        //混音 & 静音 & 连麦时的音频处理滤镜 存在的形式只能有一种，若先设置了混音，再设置静音，后面的操作会覆盖前一次
        getEasyStreaming().setAudioCPUFilter(audioCPUFilter);
        //若连麦过程中需要对主播音频或者副主播，发出的音频进行静音处理，不要通过设置UAudioMuteFilter处理，请调用
//        URemoteAudioMixFilter.setRemoteAudioState(isMute)
//        URemoteAudioMixFilter.setLocalAudioState(isMute)
    }
}
