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
import com.ucloud.ulive.example.StreamProfileUtil;
import com.ucloud.ulive.widget.UAspectFrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lw.tan on 2017/3/1.
 */

public class LiveCameraView extends UAspectFrameLayout implements TextureView.SurfaceTextureListener {

    public static final String TAG = "LiveCameraView";

    //Views
    protected TextureView mTexturePreview;

    protected List<UCameraSessionListener> mOuterCameraSessionListeners = new ArrayList<>();

    protected List<UStreamStateListener> mOuterStreamStateListeners = new ArrayList<>();

    protected List<UNetworkListener> mOuterNetworkListeners = new ArrayList<>();

    public LiveCameraView(Context context) {
        super(context);
    }

    public LiveCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static StreamEnvHodler streamEnvHolder = new StreamEnvHodler();

    public void addCameraSessionListener(UCameraSessionListener listener) {
        if (listener != null && !mOuterCameraSessionListeners.contains(listener)) {
            mOuterCameraSessionListeners.add(listener);
        }
    }

    public void addStreamStateListener(UStreamStateListener listener) {
        if (listener != null && !mOuterStreamStateListeners.contains(listener)) {
            mOuterStreamStateListeners.add(listener);
        }
    }

    public void addNetworkListener(UNetworkListener listener) {
        if (listener != null && !mOuterNetworkListeners.contains(listener)) {
            mOuterNetworkListeners.add(listener);
        }
    }

    public void init(AVOption profile) {
        if (profile == null) {
            throw new IllegalArgumentException("AVOption is null.");
        }
        streamEnvHolder.avOption = profile;
        streamEnvHolder.streamingProfile = StreamProfileUtil.build(profile);
        if (streamEnvHolder.avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            if (getContext() instanceof Activity) {
                ((Activity)getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else {
            if (getContext() instanceof Activity) {
                ((Activity)getContext()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        innerStartPreview(streamEnvHolder.streamingProfile);
    }

    public void startPreview() {
        if (streamEnvHolder.easyStreamer != null &&
                streamEnvHolder.tempSurfaceTextureAvalible
                && streamEnvHolder.tempTexture != null
                && streamEnvHolder.tempStWidth > 0
                && streamEnvHolder.tempStHeight > 0) {
            streamEnvHolder.easyStreamer.startPreview(streamEnvHolder.tempTexture, streamEnvHolder.tempStWidth, streamEnvHolder.tempStHeight);
        } else {

        }
        streamEnvHolder.previewed = true;
    }

    public void startRecording() {
        if (!isPreviewed()) {
            Toast.makeText(getContext(), "call start after previewed.", Toast.LENGTH_SHORT).show();
            return;
        }
        streamEnvHolder.streamingProfile.setStreamUrl(streamEnvHolder.avOption.streamUrl);
        getInstance().startRecording();
    }

    public void stopRecordingAndDismissPreview() {
        stopPreviewTextureView(true);//if true preview ui gone.
    }

    public void stopRecordingNoDismissPreview() {
        stopPreviewTextureView(false);
    }

    public void stopRecording() {
        stopRecordingNoDismissPreview();
    }

    public boolean isRecording() {
        if (streamEnvHolder.easyStreamer != null) {
            return streamEnvHolder.easyStreamer.isRecording();
        } else {
            return false;
        }
    }

    public boolean switchCamera() {
        if (streamEnvHolder.easyStreamer != null) {
            return streamEnvHolder.easyStreamer.switchCamera();
        } else {
            return false;
        }
    }

    public boolean toggleFlashMode() {
        if (streamEnvHolder.easyStreamer != null) {
            return streamEnvHolder.easyStreamer.toggleFlashMode();
        } else {
            return false;
        }
    }

    public boolean applyFrontCameraFlip(boolean state) {
        if (streamEnvHolder.easyStreamer != null) {
            streamEnvHolder.easyStreamer.frontCameraFlipHorizontal(state);
            return true;
        } else {
            return false;
        }
    }

    public void release() {
        onPause();
        onDestroy();
        stopRecordingAndDismissPreview();
        streamEnvHolder.easyStreamer = null;
        streamEnvHolder.streamingProfile = null;
        streamEnvHolder.tempSurfaceTextureAvalible = false;
        streamEnvHolder.tempTexture = null;
        streamEnvHolder.tempStHeight = 0;
        streamEnvHolder.tempStWidth = 0;
        mOuterCameraSessionListeners.clear();
        mOuterNetworkListeners.clear();
        mOuterStreamStateListeners.clear();
    }

    public void onPause() {
        if (streamEnvHolder.easyStreamer != null) {
            streamEnvHolder.easyStreamer.onPause();
        }
    }

    public void onDestroy() {
        if (streamEnvHolder.easyStreamer != null) {
            streamEnvHolder.easyStreamer.onDestroy();
        }
    }

    private void innerStartPreview(UStreamingProfile profile) {

//        setShowMode(Mode.ORIGIN);
        setShowMode(Mode.FULL);

        streamEnvHolder.easyStreamer = getInstance();

        streamEnvHolder.streamingProfile = profile;

        streamEnvHolder.easyStreamer.setOnCameraSessionListener(mInnerCameraSessionListener);
        streamEnvHolder.easyStreamer.setOnStreamStateListener(mInnerStreamStateListener);
        streamEnvHolder.easyStreamer.setOnNetworkStateListener(mInnerNetworkStateListener);
        streamEnvHolder.easyStreamer.prepare(streamEnvHolder.streamingProfile);

        initPreviewTextureView();

        streamEnvHolder.previewed = true;
    }

    private void initPreviewTextureView() {
        if (mTexturePreview == null) {
            mTexturePreview = new TextureView(getContext());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            this.removeAllViews();
            this.addView(mTexturePreview);
            mTexturePreview.setKeepScreenOn(true);
            mTexturePreview.setSurfaceTextureListener(this);
        }
    }

    private void stopPreviewTextureView(boolean isRelase) {
        try {
            if (streamEnvHolder.easyStreamer != null) {
                streamEnvHolder.easyStreamer.stopRecording();
                streamEnvHolder.easyStreamer.stopPreview(isRelase);
                streamEnvHolder.easyStreamer.onDestroy();
                streamEnvHolder.tempSurfaceTextureAvalible = !isRelase;
                if (isRelase) {
                    removeAllViews();
                    mTexturePreview = null;
                }
                streamEnvHolder.easyStreamer = null;
            }
        } catch (Exception e) {
            mTexturePreview = null;
            streamEnvHolder.easyStreamer = null;
            Log.e(TAG, "lifecycle->demo->stopPreviewTextureView->failed.");
        } finally {
            streamEnvHolder.previewed = false;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (streamEnvHolder.easyStreamer != null) {
            streamEnvHolder.easyStreamer.startPreview(surface, width, height);
        }
        streamEnvHolder.tempTexture = surface;
        streamEnvHolder.tempStWidth = width;
        streamEnvHolder.tempStHeight = height;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (streamEnvHolder.easyStreamer != null) {
            streamEnvHolder.easyStreamer.updatePreview(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (streamEnvHolder.easyStreamer != null) {
            streamEnvHolder.previewed = false;
            streamEnvHolder.easyStreamer.stopPreview(true);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    UCameraSessionListener mInnerCameraSessionListener = new UCameraSessionListener() {
        @Override
        public USize[] onPreviewSizeChoose(int cameraId, List<USize> cameraSupportPreviewSize) {
            if (mOuterCameraSessionListeners != null && mOuterCameraSessionListeners.size() >= 1) {
                //return the last
                return mOuterCameraSessionListeners.get(mOuterCameraSessionListeners.size() - 1).onPreviewSizeChoose(cameraId, cameraSupportPreviewSize);
            }
            return null;
        }

        @Override
        public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndex, int width, int height) {
            for(UCameraSessionListener listener: mOuterCameraSessionListeners) {
                listener.onCameraOpenSucceed(cameraId, supportCameraIndex, width, height);
            }
        }

        @Override
        public void onCameraError(UCameraSessionListener.Error error, Object extra) {
            for(UCameraSessionListener listener: mOuterCameraSessionListeners) {
                listener.onCameraError(error, extra);
            }
        }

        @Override
        public void onCameraFlashSwitched(int cameraId, boolean currentState) {
            for(UCameraSessionListener listener: mOuterCameraSessionListeners) {
                listener.onCameraFlashSwitched(cameraId, currentState);
            }
        }

        @Override
        public void onPreviewFrame(int cameraId, byte[] data, int width, int height) {
            for(UCameraSessionListener listener: mOuterCameraSessionListeners) {
                listener.onPreviewFrame(cameraId, data, width, height);
            }
        }
    };

    UStreamStateListener mInnerStreamStateListener = new UStreamStateListener() {
        @Override
        public void onStateChanged(UStreamStateListener.State state, Object extra) {
            for(UStreamStateListener listener: mOuterStreamStateListeners) {
                listener.onStateChanged(state, extra);
            }
        }

        @Override
        public void onStreamError(UStreamStateListener.Error error, Object extra) {
            for(UStreamStateListener listener: mOuterStreamStateListeners) {
                listener.onStreamError(error, extra);
            }
        }
    };

    UNetworkListener mInnerNetworkStateListener = new UNetworkListener() {

        @Override
        public void onNetworkStateChanged(UNetworkListener.State state, Object extra) {
            for(UNetworkListener listener: mOuterNetworkListeners) {
                listener.onNetworkStateChanged(state, extra);
            }
        }
    };


    public synchronized static UEasyStreaming getInstance() {
        if (streamEnvHolder.easyStreamer == null) {
            streamEnvHolder.easyStreamer = UEasyStreaming.Factory.newInstance();
        }
        return streamEnvHolder.easyStreamer;
    }

    public synchronized boolean isPreviewed() {
        return streamEnvHolder.previewed;
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
