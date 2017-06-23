package com.ucloud.ulive.example.screen;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.WindowManager;

import com.ucloud.ulive.UCameraSessionListener;
import com.ucloud.ulive.USize;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.utils.DensityUtil;
import com.ucloud.ulive.example.widget.LiveCameraView;

import java.util.List;


public class FloatingWindow extends LiveCameraView implements View.OnTouchListener {

    private static final float MIN_SCALE_FACTOR = 0.75f;
    private static final float MAX_SCALE_FACTOR = 2.0f;
    private static final int DEFAULT_MARGIN = 20;
    private static final int DEFAULT_WIDTH = 3 * 40;
    private static final int DEFAULT_HEIGHT = 4 * 40;

    private int previewWidth = 0;
    private int previewHeight = 0;

    private int origWidth = 0;
    private int origHeight = 0;
    private float scaleFactor = 1.0f;

    private final WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;

    private final Point displaySize = new Point();
    private final Point origPosition = new Point();

    private final Point initialDown = new Point();
    private final Point startPosition = new Point();
    private final Point currentPosition = new Point();

    private final GestureDetectorCompat gestureDetector;
    private final ScaleGestureDetector scaleDetector;

    private boolean isHide = false;
    private final AVOption avOption;

    public FloatingWindow(Context context, AVOption avOption) {
        super(context);
        this.avOption = avOption;
        avOption.videoResolution = UVideoProfile.Resolution.RATIO_4x3.ordinal(); //默认Camera预览大小写死成了4:3
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initWindow();
        gestureDetector = new GestureDetectorCompat(context, new GestureListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        addCameraSessionListener(cameraSessionListener);
        setOnTouchListener(this);
        init(avOption);
    }

    private void initWindow() {
        windowManager.getDefaultDisplay().getSize(displaySize);
        windowParams = new WindowManager.LayoutParams();
        windowParams.gravity = Gravity.START | Gravity.TOP;

        int orientation = avOption.videoCaptureOrientation;
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            origWidth = DensityUtil.dip2px(getContext(), DEFAULT_WIDTH);
            origHeight = DensityUtil.dip2px(getContext(), DEFAULT_HEIGHT);
            origPosition.x = displaySize.x - origWidth - DEFAULT_MARGIN;
            origPosition.y = DEFAULT_MARGIN;
        }
        else {
            origWidth = DensityUtil.dip2px(getContext(), DEFAULT_HEIGHT);
            origHeight = DensityUtil.dip2px(getContext(), DEFAULT_WIDTH);
            origPosition.x = DEFAULT_MARGIN;
            origPosition.y = DEFAULT_MARGIN;
        }

        windowParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        windowParams.format = PixelFormat.TRANSLUCENT;
        windowParams.width = origWidth;
        windowParams.height = origHeight;
        windowParams.x = origPosition.x;
        windowParams.y = origPosition.y;
    }

    public void show() {
        windowManager.addView(this, windowParams);
        startPreview();
        setShowMode(Mode.ORIGIN);
        isHide = false;
    }

    public void hide() {
        if (!isHide) {
            release();
            windowManager.removeView(this);
            isHide = true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        windowManager.getDefaultDisplay().getSize(displaySize);
        if (newConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            avOption.videoCaptureOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        else {
            avOption.videoCaptureOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        hide();
        initWindow();
        init(avOption);
        if (avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setAspectRatio(((float) previewWidth) / previewHeight);
        }
        else {
            setAspectRatio(((float) previewHeight) / previewWidth);
        }
        show();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startPosition.set(windowParams.x, windowParams.y);
                initialDown.set((int) event.getRawX(), (int) event.getRawY());
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                currentPosition.x = startPosition.x + (int) (event.getRawX() - initialDown.x);
                currentPosition.y = startPosition.y + (int) (event.getRawY() - initialDown.y);
                updateWindowPosition(currentPosition.x, currentPosition.y);
                break;
            default:
                break;
        }
        return true;
    }

    private class GestureListener extends SimpleOnGestureListener {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public boolean onDoubleTap(MotionEvent event) {
            hide();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            return true;
        }
    }

    private class ScaleListener extends SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(scaleFactor, MAX_SCALE_FACTOR));
            int newWidth = (int) (origWidth * scaleFactor);
            int newHeight = (int) (origHeight * scaleFactor);
            updateWindowSize(newWidth, newHeight);
            return true;
        }
    }

    private void updateWindowPosition(int x, int y) {
        windowParams.x = x;
        windowParams.y = y;
        windowManager.updateViewLayout(this, windowParams);
    }

    private void updateWindowSize(int width, int height) {
        windowParams.width = width;
        windowParams.height = height;
        windowManager.updateViewLayout(this, windowParams);
    }

    private final UCameraSessionListener cameraSessionListener = new UCameraSessionListener() {
        @Override
        public USize[] onPreviewSizeChoose(int cameraId, List<USize> cameraSupportPreviewSize) {
            return null;
        }

        @Override
        public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndex, int width, int height) {
            previewWidth = width;
            previewHeight = height;
            if (avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                setAspectRatio(((float) width) / height);
            }
            else {
                setAspectRatio(((float) height) / width);
            }
        }

        @Override
        public void onCameraError(Error error, Object extra) {

        }

        @Override
        public void onCameraFlashSwitched(int cameraId, boolean currentState) {

        }

        @Override
        public void onPreviewFrame(int cameraId, byte[] data, int width, int height) {

        }
    };
}
