package com.ucloud.ulive.example.ext.faceu;

import android.app.Activity;
import android.graphics.Rect;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.lemon.faceu.openglfilter.common.FilterConstants;
import com.lemon.faceu.openglfilter.detect.CvFace;
import com.lemon.faceu.openglfilter.detect.DirectionDetector;
import com.lemon.faceu.openglfilter.detect.IFaceDetector;
import com.lemon.faceu.openglfilter.gpuimage.base.GPUImageFilterGroupBase;
import com.lemon.faceu.openglfilter.gpuimage.draw.Rotation;
import com.megvii.facepp.sdk.ext.FaceppDetector;
import com.ucloud.ulive.UStreamingContext;
import com.ucloud.ulive.filter.UGPUImageCompat;
import com.ucloud.ulive.filter.UVideoGPUFilter;

import java.nio.FloatBuffer;

import static com.lemon.faceu.openglfilter.common.FilterConstants.MAX_FACE_COUNT;

public class FaceuCompat extends UVideoGPUFilter implements IFaceDetector.FaceDetectorListener {

    private static final String TAG = "FaceuCompat";

    private IFaceDetector faceDetector;
    private CvFace[] faceDetectResultList;
    private Rect firstFaceRect;
    private DirectionDetector directionDetector;

    private GPUImageFilterGroupBase faceuFilter;
    private FloatBuffer glCubeBuffer;
    private FloatBuffer glTextureBuffer;

    private final Activity activity;
    private int cameraId = -1;
    private Rotation rotation = Rotation.NORMAL;
    private boolean isVerticalFlip = true;

    public FaceuCompat(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onInit(int width, int height) {
        super.onInit(width, height);
        faceuFilter.init();
        faceuFilter.onOutputSizeChanged(width, height);
        initFaceDetector(FaceDetectorType.FACEPP);
        directionDetector = new DirectionDetector(false);
        directionDetector.start();
        faceDetectResultList = new CvFace[FilterConstants.MAX_FACE_COUNT];
        for (int i = 0; i < MAX_FACE_COUNT; ++i) {
            faceDetectResultList[i] = new CvFace();
        }
        firstFaceRect = new Rect();
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        int faceCount = faceDetector.getFaceDetectResult(faceDetectResultList, firstFaceRect, width, height, width, height);
        faceuFilter.setFaceDetResult(faceCount, faceDetectResultList, width, height);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        faceuFilter.draw(cameraTexture, targetFrameBuffer, glCubeBuffer, glTextureBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDirectionUpdate(int directionFlag) {
        if (this.directionFlag != directionFlag) {
            glCubeBuffer = UGPUImageCompat.getShapeVerticesBuffer();
            glTextureBuffer = UGPUImageCompat.getTextureVerticesBuffer(this.directionFlag);
        }
    }

    @Override
    public void onDetectFinish() {
    }

    public void setFaceuFilter(GPUImageFilterGroupBase faceuFilter) {
        this.faceuFilter = faceuFilter;
    }

    private void initFaceDetector(FaceDetectorType type) {
        if (null != faceDetector) {
            return;
        }
        faceDetector = new FaceppDetector(this);
        faceDetector.switchMaxFaceCount(FilterConstants.MAX_FACE_COUNT); // 需要在init之前调用
        faceDetector.init(UStreamingContext.APP_CONTEXT);
    }

    public void updateCameraFrame(int cameraId, final byte[] data, int width, int height) {
        try {
            if (this.cameraId != cameraId) {
                rotation = getRotation(cameraId);
                this.cameraId = cameraId;
                isVerticalFlip = this.cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT;
            }
            if (null != faceDetector && null != directionDetector) {
                faceDetector.onFrameAvailable(width, height, rotation, isVerticalFlip, data, directionDetector.getDirection());
            }
        }
        catch (Exception e) {
            Log.d(TAG, "lifecycle->updateCameraFrame->failed.");
        }
    }

    @Override
    public void onDestroy() {
        faceuFilter.onDestroy();
        faceuFilter.releaseNoGLESRes();
        if (null != directionDetector) {
            directionDetector.stop();
            directionDetector = null;
        }
        if (null != faceDetector) {
            faceDetector.uninit();
            faceDetector = null;
        }
    }

    private Rotation getRotation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotationInt = activity.getWindowManager().getDefaultDisplay().getRotation();
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
            default:
                break;
        }
        // 前后摄像头横竖屏旋转角度不一样
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            degrees = (info.orientation + degrees) % 360;
        }
        else if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
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
            default:
                break;
        }
        return rotation;
    }
}
