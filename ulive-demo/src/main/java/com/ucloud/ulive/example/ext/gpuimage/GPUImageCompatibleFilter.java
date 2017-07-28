//gpuimage filter add in build.gradle
/*compile 'jp.co.cyberagent.android.gpuimage:gpuimage-library:1.4.1'*/

package com.ucloud.ulive.example.ext.gpuimage;
import android.opengl.GLES20;

import com.ucloud.ulive.filter.UGPUImageCompat;

import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;


public class GPUImageCompatibleFilter<T extends GPUImageFilter> extends UGPUImageCompat {

    private T innerGPUImageFilter;

    public GPUImageCompatibleFilter(T filter) {
        if (filter instanceof GPUImageFilterGroup) {
            throw new IllegalStateException("不支持适配GPUImage的组合滤镜");
        }
        innerGPUImageFilter = filter;
    }

    public T getGPUImageFilter() {
        return innerGPUImageFilter;
    }

    @Override
    public void onInit(int width, int height) {
        innerGPUImageFilter.init();
        innerGPUImageFilter.onOutputSizeChanged(width, height);
        isInitialized = true;
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        innerGPUImageFilter.onDraw(cameraTexture, innerShapeBuffer, innerTextureBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        innerGPUImageFilter.destroy();
    }

    @Override
    public void onDirectionUpdate(int directionFlag) {
        super.onDirectionUpdate(directionFlag);
    }
}
