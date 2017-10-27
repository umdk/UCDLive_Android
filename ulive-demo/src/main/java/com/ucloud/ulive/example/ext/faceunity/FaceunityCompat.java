package com.ucloud.ulive.example.ext.faceunity;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.faceunity.wrapper.faceunity;
import com.ucloud.ulive.filter.UGPUImageCompat;

import java.io.InputStream;
import java.nio.FloatBuffer;

public class FaceunityCompat extends UGPUImageCompat {

    private static final String TAG = "faceunity";

    private Context mContext;
    private int[] itemsArray = new int[1];
    private int mFrameId;
    private byte[] data;
    private byte[] itemData;

    public FaceunityCompat(Context context) {
        this.mContext = context;
        try {
            InputStream is = mContext.getAssets().open("faceunity/v3.mp3");
            byte[] v3data = new byte[is.available()];
            int len = is.read(v3data);
            is.close();
            faceunity.fuSetup(v3data, null, authpack.A());
            Log.e(TAG, "fuSetup v3 len " + len);

            is = mContext.getAssets().open("faceunity/Mood.mp3");
            itemData = new byte[is.available()];
            is.read(itemData);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInit(int width, int height) {
        super.onInit(width, height);
        itemsArray[0] = faceunity.fuCreateItemFromPackage(itemData); // 加载速度需要500ms...
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        int fuTex = faceunity.fuDualInputToTexture(data, cameraTexture, 0, width, height, mFrameId++, itemsArray);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        super.onDraw(fuTex, innerShapeBuffer, innerTextureBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void updateCameraFrame(byte[] data, int width, int height) {
        this.data = data;
        this.width = width;
        this.height = height;
    }

    @Override
    public void onDirectionUpdate(int directionFlag) {
        super.onDirectionUpdate(directionFlag);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        int item = itemsArray[0];
//        itemsArray[0] = 0;
//        faceunity.fuDestroyItem(item);
        faceunity.fuDestroyAllItems();
    }

}
