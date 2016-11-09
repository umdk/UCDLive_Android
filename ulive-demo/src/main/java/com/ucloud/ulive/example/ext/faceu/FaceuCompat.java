package com.ucloud.ulive.example.ext.faceu;

import android.app.Activity;
import android.graphics.PointF;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.lemon.faceu.openglfilter.common.FilterConstants;
import com.lemon.faceu.openglfilter.detect.DirectionDetector;
import com.lemon.faceu.openglfilter.gpuimage.base.GPUImageFilterGroupBase;
import com.lemon.faceu.openglfilter.gpuimage.draw.Rotation;
import com.lemon.faceusdkdemo.detect.FaceDetectorType;
import com.lemon.faceusdkdemo.detect.IFaceDetector;
import com.megvii.facepp.sdk.ext.FaceppDetector;
import com.ucloud.ulive.UStreamingContext;
import com.ucloud.ulive.filter.UGPUImageCompat;
import com.ucloud.ulive.filter.UVideoGPUFilter;

import java.nio.FloatBuffer;

public class FaceuCompat extends UVideoGPUFilter implements IFaceDetector.FaceDetectorListener {

    private static final String TAG = "FaceuCompat";

    private IFaceDetector mFaceDetector;
    private PointF[][] mFaceDetectResultList;
    private DirectionDetector mDirectionDetector;

    private GPUImageFilterGroupBase mFaceuFilter;
    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;

    private Activity mActivity;
    private int mCameraId = -1;
    private Rotation mRotation = Rotation.NORMAL;
    private boolean isVerticalFlip = true;

    public FaceuCompat(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        mFaceuFilter.init();
        mFaceuFilter.onOutputSizeChanged(VWidth, VHeight);
        initFaceDetector(FaceDetectorType.FACEPP);
        mDirectionDetector = new DirectionDetector(false);
        mDirectionDetector.start();
        mFaceDetectResultList = new PointF[FilterConstants.MAX_FACE_COUNT][106];
        for (int i = 0; i < FilterConstants.MAX_FACE_COUNT; ++i) {
            PointF[] pointFs = mFaceDetectResultList[i];
            for (int j = 0; j < pointFs.length; ++j) {
                pointFs[j] = new PointF(0, 0);
            }
        }
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        int faceCount = mFaceDetector.getFaceDetectResult(mFaceDetectResultList, SIZE_WIDTH, SIZE_HEIGHT, SIZE_WIDTH, SIZE_HEIGHT);
        mFaceuFilter.setFaceDetResult(faceCount, mFaceDetectResultList, SIZE_WIDTH, SIZE_HEIGHT);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        mFaceuFilter.draw(cameraTexture, targetFrameBuffer, mGLCubeBuffer, mGLTextureBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDirectionUpdate(int _directionFlag) {
        if (directionFlag != _directionFlag) {
            mGLCubeBuffer = UGPUImageCompat.getGPUImageCompatShapeVerticesBuffer();
            mGLTextureBuffer = UGPUImageCompat.getGPUImageCompatTextureVerticesBuffer(directionFlag);
        }
    }

    @Override
    public void onDetectFinish() {
    }

    public void setFaceuFilter(GPUImageFilterGroupBase faceuFilter) {
        mFaceuFilter = faceuFilter;
    }

    private void initFaceDetector(FaceDetectorType type) {
        if (null != mFaceDetector) {
            return;
        }
        mFaceDetector = new FaceppDetector(this);
        mFaceDetector.switchMaxFaceCount(FilterConstants.MAX_FACE_COUNT); // 需要在init之前调用
        mFaceDetector.init(UStreamingContext.appContext);
    }

    public void updateCameraFrame(int cameraId, final byte[] data, int width, int height) {
        try {
            if (mCameraId != cameraId) {
                mRotation = getRotation(cameraId);
                mCameraId = cameraId;
                isVerticalFlip = mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
            if (null != mFaceDetector && null != mDirectionDetector) {
                mFaceDetector.onFrameAvailable(width, height, mRotation, isVerticalFlip, data, mDirectionDetector.getDirection());
            }
        }catch (Exception e) {
            Log.d(TAG, "lifecycle->updateCameraFrame->failed.");
        }
    }

    @Override
    public void onDestroy() {
        mFaceuFilter.onDestroy();
        mFaceuFilter.releaseNoGLESRes();
        if (null != mDirectionDetector) {
            mDirectionDetector.stop();
            mDirectionDetector = null;
        }
        if (null != mFaceDetector) {
            mFaceDetector.uninit();
            mFaceDetector = null;
        }
    }

    private Rotation getRotation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotationInt = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotationInt) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        // 前后摄像头横竖屏旋转角度不一样
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            degrees = (info.orientation + degrees) % 360;
        } else if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            degrees = (info.orientation - degrees + 360) % 360;
        }
        Rotation rotation = Rotation.NORMAL;
        switch (degrees) {
            case 90:
                rotation = Rotation.ROTATION_90;
                break;
            case 180:
                rotation = Rotation.ROTATION_180;
                break;
            case 270:
                rotation = Rotation.ROTATION_270;
                break;
        }
        return rotation;
    }

}
