package com.ucloud.ulive.example.ext.gpuimage;
/*
import android.opengl.GLES20;

import com.ucloud.ulive.filter.UGPUImageCompat;
import com.ucloud.ulive.filter.UVideoGPUFilter;

import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;


public class GPUImageCompatibleFilter<T extends GPUImageFilter> extends UVideoGPUFilter {

    private T innerGPUImageFilter;

    private FloatBuffer innerShapeBuffer;
    private FloatBuffer innerTextureBuffer;

    public GPUImageCompatibleFilter(T filter) {
        innerGPUImageFilter = filter;
    }

    public T getGPUImageFilter() {
        return innerGPUImageFilter;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        innerGPUImageFilter.init();
        innerGPUImageFilter.onOutputSizeChanged(VWidth, VHeight);
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        innerGPUImageFilter.onDraw(cameraTexture, innerShapeBuffer, innerTextureBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        innerGPUImageFilter.destroy();
    }

    @Override
    public void onDirectionUpdate(int _directionFlag) {
        if (directionFlag != _directionFlag) {
            innerShapeBuffer = UGPUImageCompat.getGPUImageCompatShapeVerticesBuffer();
            innerTextureBuffer = UGPUImageCompat.getGPUImageCompatTextureVerticesBuffer(directionFlag);
        }
    }
}*/
