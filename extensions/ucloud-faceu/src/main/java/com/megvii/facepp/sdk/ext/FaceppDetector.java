package com.megvii.facepp.sdk.ext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.lemon.faceu.openglfilter.detect.CvFace;
import com.lemon.faceu.openglfilter.detect.IFaceDetector;
import com.lemon.faceu.openglfilter.gpuimage.draw.Rotation;
import com.lemon.faceu.sdk.utils.JniEntry;
import com.lemon.faceu.sdk.utils.SdkConstants;
import com.megvii.facepp.sdk.Facepp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @since 2016-09-21
 * @author kevinhuang 
 */
public class FaceppDetector implements Runnable, IFaceDetector {
    static final String TAG = "FaceppDetector";

    // 样本的高和宽
    static final int SAMPLE_WIDTH = 240;
    static final int SAMPLE_HEIGHT = 320;

    Handler mDetectHandler;
    IFaceDetector.FaceDetectorListener mFaceDetectorLsn;
    Context mContext;

    int mInputWidth = -1;
    int mInputHeight = -1;
    int mRotation = 0;
    boolean mMirror = false;

    int mSampleWidth = -1;
    int mSampleHeight = -1;

    ByteBuffer mSampleData;

    final Object mReadyFence = new Object();
    boolean mReady = false;
    boolean mDetecting = false;

    Facepp mFaceTrack;
    int mMaxFaceCount = 0;
    Facepp.Face[] mFaceInfoLst = null;

    public FaceppDetector(IFaceDetector.FaceDetectorListener listener) {
        mFaceDetectorLsn = listener;
    }

    public void init(Context context) {
        if (null != mDetectHandler) {
            throw new RuntimeException("Face detector already initialized!");
        }

        mContext = context;
        new Thread(this, "face_detect").start();
    }

    public void uninit() {
        if (null == mDetectHandler) {
            return;
        }

        synchronized (mReadyFence) {
            if (mReady) {
                mDetectHandler.sendMessage(Message.obtain(mDetectHandler, MSG_QUIT));
            }
        }
    }

    /**
     * 重置当前对象,清理未执行的消息
     */
    public void reset() {
        mInputHeight = -1;
        mInputWidth = -1;
        mSampleData = null;
        mSampleWidth = -1;
        mSampleHeight = -1;

        if (null != mDetectHandler) {
            mDetectHandler.removeMessages(MSG_DETECT);
        }
    }

    public void switchMaxFaceCount(int count) {
        if (null != mDetectHandler) {
            mDetectHandler.sendMessage(Message.obtain(mDetectHandler, MSG_SWITCH_FACE_COUNT, count, 0));
        } else {
            mMaxFaceCount = count;
        }
    }

    /**
     * 更新数据输入的尺寸
     */
    @SuppressWarnings("SuspiciousNameCombination")
    void updatePreviewSize(int width, int height, Rotation rotate, boolean mirror) {
        if (mInputWidth == width && mInputHeight == height && mRotation == rotate.asInt()) {
            return;
        }

        // 正常的方向图像的宽和高
        int nWidth, nHeight;
        if (rotate == Rotation.ROTATION_90 || rotate == Rotation.ROTATION_270) {
            nHeight = width;
            nWidth = height;
        } else {
            nHeight = height;
            nWidth = width;
        }

        // 这个宽和高是正常方向上的值
        if (SAMPLE_HEIGHT / (float) SAMPLE_WIDTH < (float) nHeight / nWidth) {
            mSampleHeight = SAMPLE_HEIGHT;
            mSampleWidth = mSampleHeight * nWidth / nHeight;
        } else {
            mSampleWidth = SAMPLE_WIDTH;
            mSampleHeight = mSampleWidth * nHeight / nWidth;
        }

        mRotation = rotate.asInt();
        mSampleData = ByteBuffer.allocateDirect(mSampleWidth * mSampleHeight).order(ByteOrder.nativeOrder());

        mInputWidth = width;
        mInputHeight = height;
        mMirror = mirror;
    }

    /**
     * 数据帧到了
     *
     * @param yuvData 数据帧,格式是yuv420sp
     */
    public void onFrameAvailable(int width, int height, Rotation rotate, boolean mirror,
                                 byte[] yuvData, @SdkConstants.NotationClockwiseDirection int direction) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }

            if (mDetecting) {
                return;
            }

            updatePreviewSize(width, height, rotate, mirror);

            JniEntry.YuvToGrayAndScale(yuvData, mInputWidth, mInputHeight, mRotation, mMirror,
                    mSampleData.array(), mSampleWidth, mSampleHeight);

//            if (count > 0) {
//                count--;
//                // 将灰度数组生成bitmap的代码,用于测试
//                ByteBuffer grayBuffer = ByteBuffer.allocateDirect(mSampleWidth * mSampleHeight * 4).order(ByteOrder.nativeOrder());
//
//                byte[] grayData = grayBuffer.array();
//                byte[] sampleData = mSampleData.array();
//                int offset = 0;
//                for (int i = 0; i < mSampleHeight; ++i) {
//                    for (int j = 0; j < mSampleWidth; ++j) {
//                        offset = i * mSampleWidth + j;
//                        grayData[offset * 4 + 1] = grayData[offset * 4 + 2] = grayData[offset * 4] = sampleData[offset];
//                        grayData[offset * 4 + 3] = (byte) 0xff;
//                    }
//                }
//
//                Bitmap grayBitmap = Bitmap.createBitmap(mSampleWidth, mSampleHeight, Bitmap.Config.ARGB_8888);
//                grayBitmap.copyPixelsFromBuffer(grayBuffer);
//
//                saveMyBitmap("faceu" + count, grayBitmap);
//
//            }

            mDetectHandler.sendMessage(Message.obtain(mDetectHandler, MSG_DETECT, direction, 0, mSampleData));
        }
    }

    @Override
    public int getFaceDetectResult(CvFace[] detectResult,
                                   Rect firstFaceRect,
                                   int imageScaleWidth,
                                   int imageScaleHeight,
                                   int outputWidth,
                                   int outputHeight) {
        float width = 100f, height = 100f;
        Facepp.Face[] cvFaceLst = null;
        int faceCount = 0;
        synchronized (FaceppDetector.class) {
            width = mSampleWidth;
            height = mSampleHeight;

            cvFaceLst = mFaceInfoLst;
            faceCount = Math.min(mMaxFaceCount, null == mFaceInfoLst ? 0 : mFaceInfoLst.length);
        }

        int widthTranslate = (imageScaleWidth - outputWidth) / 2;
        int heightTranslate = (imageScaleHeight - outputHeight) / 2;

        if (null != cvFaceLst) {
            for (int i = 0; i < faceCount; ++i) {
                PointF[] pointFs = cvFaceLst[i].points;
                PointF[] fResult = detectResult[i].getFacePoints();
                int pointCount = Math.min(pointFs.length, fResult.length);

                // 将坐标计算到屏幕范围内
                for (int j = 0; j < pointCount; ++j) {
                    fResult[j].x = pointFs[j].x / width * imageScaleWidth - widthTranslate;
                    fResult[j].y = pointFs[j].y / height * imageScaleHeight - heightTranslate;
                }
            }
        }
        return faceCount;
    }

//    @Override
//    public int getFaceDetectResult(PointF[][] detectResult, int imageScaleWidth, int imageScaleHeight,
//                                   int outputWidth, int outputHeight) {
//        float width = 100f, height = 100f;
//        Facepp.Face[] cvFaceLst = null;
//        int faceCount = 0;
//        synchronized (FaceppDetector.class) {
//            width = mSampleWidth;
//            height = mSampleHeight;
//
//            cvFaceLst = mFaceInfoLst;
//            faceCount = Math.min(mMaxFaceCount, null == mFaceInfoLst ? 0 : mFaceInfoLst.length);
//        }
//
//        int widthTranslate = (imageScaleWidth - outputWidth) / 2;
//        int heightTranslate = (imageScaleHeight - outputHeight) / 2;
//
//        if (null != cvFaceLst) {
//            for (int i = 0; i < faceCount; ++i) {
//                PointF[] pointFs = cvFaceLst[i].points;
//                PointF[] fResult = detectResult[i];
//                int pointCount = Math.min(pointFs.length, fResult.length);
//
//                // 将坐标计算到屏幕范围内
//                for (int j = 0; j < pointCount; ++j) {
//                    fResult[j].x = pointFs[j].x / width * imageScaleWidth - widthTranslate;
//                    fResult[j].y = pointFs[j].y / height * imageScaleHeight - heightTranslate;
//                }
//            }
//        }
//        return faceCount;
//    }

    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mDetectHandler = new DetectHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Detect thread exiting");
        synchronized (mReadyFence) {
            mReady = mDetecting = false;
            mDetectHandler = null;
        }

        if (null != mFaceTrack) {
            mFaceTrack.release();
        }
        mFaceTrack = null;
    }

    byte[] readFromAssets(String assetPath) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = mContext.getResources().getAssets().open(assetPath);
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "read from asset failed, errMsg: " + e.getMessage());
            baos = null;
        } finally {
            MiscUtils.safeClose(is);
        }
        
        return null == baos ? null : baos.toByteArray();
    }

    void handleDetect(ByteBuffer sampleData, @SdkConstants.NotationClockwiseDirection int direction) {
        mDetecting = true;

        if (null == mFaceTrack) {
            byte[] modelData = readFromAssets("megviifacepp_0_2_0_model");
            if (null != modelData) {
                mFaceTrack = new Facepp();
                String errType = mFaceTrack.init(mContext, modelData);
                Log.d(TAG, "errType: " + errType);
                
                Facepp.FaceppConfig config = mFaceTrack.getFaceppConfig();
                config.detectionMode = Facepp.FaceppConfig.DETECTION_MODE_TRACKING;
                mFaceTrack.setFaceppConfig(config);
            }
        }

        if (null == mFaceTrack) {
            return;
        }

        long startTick = System.currentTimeMillis();
        Facepp.Face[] faces = mFaceTrack.detect(sampleData.array(), mSampleWidth, mSampleHeight, Facepp.IMAGEMODE_GRAY);
        for (int i = 0; i < faces.length; ++i) {
            mFaceTrack.getLandMark(faces, i, Facepp.facePoints);
        }

        synchronized (FaceppDetector.class) {
            if (null != faces && faces.length > 0) {
                mFaceInfoLst = faces;
            } else {
                mFaceInfoLst = null;
            }
        }

        mDetecting = false;
        mFaceDetectorLsn.onDetectFinish();
    }

    /**
     * 切换需要识别的人数
     */
    void handleSwitchFaceCount(int count) {
        if (count == mMaxFaceCount) {
            return;
        }

        Log.w(TAG, "facepp not support set max face count");
    }

    final static int MSG_DETECT = 0;
    final static int MSG_QUIT = 1;
    final static int MSG_SWITCH_FACE_COUNT = 2;

    static class DetectHandler extends Handler {
        WeakReference<FaceppDetector> wrFaceDeteor;

        public DetectHandler(FaceppDetector detector) {
            wrFaceDeteor = new WeakReference<FaceppDetector>(detector);
        }

        @Override
        public void handleMessage(Message msg) {
            if (wrFaceDeteor.get() == null) {
                return;
            }

            switch (msg.what) {
                case MSG_DETECT:
                    wrFaceDeteor.get().handleDetect((ByteBuffer) msg.obj, msg.arg1);
                    break;
                case MSG_SWITCH_FACE_COUNT:
                    wrFaceDeteor.get().handleSwitchFaceCount(msg.arg1);
                    break;
                case MSG_QUIT:
                    getLooper().quit();
                    break;
            }
        }
    }

    private int count = 5;

    public void saveMyBitmap(String bitName, Bitmap mBitmap) {
        String path = "/sdcard/" + bitName + ".png";
        Log.i("copy", "saveMyBitmap: " + path);
        File f = new File(path);
        try {
            f.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
