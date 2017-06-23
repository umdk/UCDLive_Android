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
import android.widget.Toast;

import com.ucloud.ulive.UCameraSessionListener;
import com.ucloud.ulive.UEasyStreaming;
import com.ucloud.ulive.UNetworkListener;
import com.ucloud.ulive.USize;
import com.ucloud.ulive.UStreamStateListener;
import com.ucloud.ulive.UStreamingProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.utils.StreamProfileUtil;
import com.ucloud.ulive.widget.UAspectFrameLayout;

import java.util.ArrayList;
import java.util.List;

public class LiveCameraView extends UAspectFrameLayout implements TextureView.SurfaceTextureListener {

    private static final String TAG = "LiveCameraView";

    //Views
    private TextureView textureView;

    private final List<UCameraSessionListener> outerCameraSessionListeners = new ArrayList<>();

    private final List<UStreamStateListener> outerStreamStateListeners = new ArrayList<>();

    private final List<UNetworkListener> outerNetworkListeners = new ArrayList<>();

    public LiveCameraView(Context context) {
        super(context);
    }

    public LiveCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static final StreamEnvHodler STREAM_ENV_HOLDER = new StreamEnvHodler();

    public void addCameraSessionListener(UCameraSessionListener listener) {
        if (listener != null && !outerCameraSessionListeners.contains(listener)) {
            outerCameraSessionListeners.add(listener);
        }
    }

    public void addStreamStateListener(UStreamStateListener listener) {
        if (listener != null && !outerStreamStateListeners.contains(listener)) {
            outerStreamStateListeners.add(listener);
        }
    }

    public void addNetworkListener(UNetworkListener listener) {
        if (listener != null && !outerNetworkListeners.contains(listener)) {
            outerNetworkListeners.add(listener);
        }
    }

    public void init(AVOption profile) {
        if (profile == null) {
            throw new IllegalArgumentException("AVOption is null.");
        }
        STREAM_ENV_HOLDER.avOption = profile;
        STREAM_ENV_HOLDER.streamingProfile = StreamProfileUtil.build(profile);
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

    public void startRecording() {
        if (STREAM_ENV_HOLDER.avOption != null && !isPreviewed()) {
            init(STREAM_ENV_HOLDER.avOption);
        }
        if (!isPreviewed()) {
            Toast.makeText(getContext(), "call start after previewed.", Toast.LENGTH_SHORT).show();
            return;
        }
        STREAM_ENV_HOLDER.streamingProfile.setStreamUrl(STREAM_ENV_HOLDER.avOption.streamUrl);
        getEasyStreaming().startRecording();
    }

    public void stopRecordingAndDismissPreview() {
        stopPreviewTextureView(true); //if true preview ui gone.
    }

    public void stopRecordingNoDismissPreview() {
        stopPreviewTextureView(false);
    }

    private void stoRecordingStilCpature() {
        try {
            if (STREAM_ENV_HOLDER.easyStreamer != null) {
                STREAM_ENV_HOLDER.easyStreamer.stopRecording();
            }
        }
        catch (Exception e) {
            textureView = null;
            STREAM_ENV_HOLDER.easyStreamer = null;
            STREAM_ENV_HOLDER.previewed = false;
            Log.e(TAG, "lifecycle->demo->stopPreviewTextureView->failed.");
        }
    }

    public void stopRecording() {
//        stopRecordingNoDismissPreview();
        stoRecordingStilCpature();
    }

    public boolean isRecording() {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            return STREAM_ENV_HOLDER.easyStreamer.isRecording();
        }
        else {
            return false;
        }
    }

    public boolean switchCamera() {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            return STREAM_ENV_HOLDER.easyStreamer.switchCamera();
        }
        else {
            return false;
        }
    }

    public boolean toggleFlashMode() {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            return STREAM_ENV_HOLDER.easyStreamer.toggleFlashMode();
        }
        else {
            return false;
        }
    }

    public boolean applyFrontCameraFlip(boolean state) {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            STREAM_ENV_HOLDER.easyStreamer.frontCameraFlipHorizontal(state);
            return true;
        }
        else {
            return false;
        }
    }

    public void release() {
        onPause();
        onDestroy();
        stopRecordingAndDismissPreview();
        STREAM_ENV_HOLDER.easyStreamer = null;
        STREAM_ENV_HOLDER.streamingProfile = null;
        STREAM_ENV_HOLDER.tempSurfaceTextureAvalible = false;
        STREAM_ENV_HOLDER.tempTexture = null;
        STREAM_ENV_HOLDER.tempStHeight = 0;
        STREAM_ENV_HOLDER.tempStWidth = 0;
        outerCameraSessionListeners.clear();
        outerNetworkListeners.clear();
        outerStreamStateListeners.clear();
    }

    public void onPause() {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            STREAM_ENV_HOLDER.easyStreamer.onPause();
        }
    }

    public void onDestroy() {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            STREAM_ENV_HOLDER.easyStreamer.onDestroy();
            STREAM_ENV_HOLDER.easyStreamer = null;
        }
    }

    private void innerStartPreview(UStreamingProfile profile) {
//        setShowMode(Mode.ORIGIN);
        setShowMode(Mode.FULL);

        STREAM_ENV_HOLDER.easyStreamer = getEasyStreaming();

        STREAM_ENV_HOLDER.streamingProfile = profile;

        STREAM_ENV_HOLDER.easyStreamer.setOnCameraSessionListener(innerCameraSessionListener);
        STREAM_ENV_HOLDER.easyStreamer.setOnStreamStateListener(innerStreamStateListener);
        STREAM_ENV_HOLDER.easyStreamer.setOnNetworkStateListener(innerNetworkStateListener);

        boolean isSucceed = STREAM_ENV_HOLDER.easyStreamer.prepare(STREAM_ENV_HOLDER.streamingProfile);

        if (!isSucceed) {
            Log.e(TAG, "lifecycle->env prepared failed.");
            Toast.makeText(getContext(), "env prepared failed.", Toast.LENGTH_LONG).show();
        }
        initPreviewTextureView();

        STREAM_ENV_HOLDER.previewed = true;
    }

    private void initPreviewTextureView() {
        if (textureView == null) {
            textureView = new TextureView(getContext());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            this.removeAllViews();
            this.addView(textureView);
            textureView.setKeepScreenOn(true);
            textureView.setSurfaceTextureListener(this);
        }
    }

    private void stopPreviewTextureView(boolean isRelase) {
        try {
            if (STREAM_ENV_HOLDER.easyStreamer != null) {
                STREAM_ENV_HOLDER.easyStreamer.stopRecording();
                STREAM_ENV_HOLDER.easyStreamer.stopPreview(isRelase);
                STREAM_ENV_HOLDER.easyStreamer.onDestroy();
                STREAM_ENV_HOLDER.tempSurfaceTextureAvalible = !isRelase;
                if (isRelase) {
                    removeAllViews();
                    textureView = null;
                }
                STREAM_ENV_HOLDER.easyStreamer = null;
            }
        }
        catch (Exception e) {
            textureView = null;
            STREAM_ENV_HOLDER.easyStreamer = null;
            Log.e(TAG, "lifecycle->demo->stopPreviewTextureView->failed.");
        }
        finally {
            STREAM_ENV_HOLDER.previewed = false;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            STREAM_ENV_HOLDER.easyStreamer.startPreview(surface, width, height);
        }
        STREAM_ENV_HOLDER.tempTexture = surface;
        STREAM_ENV_HOLDER.tempStWidth = width;
        STREAM_ENV_HOLDER.tempStHeight = height;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            STREAM_ENV_HOLDER.easyStreamer.updatePreview(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (STREAM_ENV_HOLDER.easyStreamer != null) {
            STREAM_ENV_HOLDER.previewed = false;
            STREAM_ENV_HOLDER.easyStreamer.stopPreview(true);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private final UCameraSessionListener innerCameraSessionListener = new UCameraSessionListener() {
        @Override
        public USize[] onPreviewSizeChoose(int cameraId, List<USize> supportPreviewSizeList) {
            if (outerCameraSessionListeners != null && outerCameraSessionListeners.size() >= 1) {
                //return the last
                return outerCameraSessionListeners.get(outerCameraSessionListeners.size() - 1).onPreviewSizeChoose(cameraId, supportPreviewSizeList);
            }
            return null;
        }

        @Override
        public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndexList, int width, int height) {
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
            for (UStreamStateListener listener: outerStreamStateListeners) {
                listener.onStateChanged(state, extra);
            }
        }

        @Override
        public void onStreamError(UStreamStateListener.Error error, Object extra) {
            for (UStreamStateListener listener: outerStreamStateListeners) {
                listener.onStreamError(error, extra);
            }
        }
    };

    private final UNetworkListener innerNetworkStateListener = new UNetworkListener() {

        @Override
        public void onNetworkStateChanged(UNetworkListener.State state, Object extra) {
            for (UNetworkListener listener: outerNetworkListeners) {
                listener.onNetworkStateChanged(state, extra);
            }
        }
    };


    public static synchronized UEasyStreaming getEasyStreaming() {
        if (STREAM_ENV_HOLDER.easyStreamer == null) {
            STREAM_ENV_HOLDER.easyStreamer = UEasyStreaming.Factory.newInstance();
        }
        return STREAM_ENV_HOLDER.easyStreamer;
    }

    @Deprecated
    public static synchronized UEasyStreaming getInstance() {
        return getEasyStreaming();
    }

    public synchronized boolean isPreviewed() {
        return STREAM_ENV_HOLDER.previewed;
    }

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
}
