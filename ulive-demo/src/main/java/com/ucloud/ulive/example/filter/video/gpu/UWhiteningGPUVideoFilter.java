package com.ucloud.ulive.example.filter.video.gpu;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import com.ucloud.ulive.filter.UVideoGPUFilter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class UWhiteningGPUVideoFilter extends UVideoGPUFilter {

    private final byte[] colorMap;

    private int glProgram;

    private int glCamTextureLoc;

    private int glCamPostionLoc;

    private int glCamTextureCoordLoc;

    private int glColorMapTextureLoc;

    private static final String VERTEX_SHADER = ""
            + "attribute vec4 position;"
            + "attribute vec2 inputTextureCoordinate;"
            + "varying vec2 textureCoordinate;"
            + "void main(){"
            + "   gl_Position= position;"
            + "   textureCoordinate = inputTextureCoordinate;"
            + "}";

    private static final String FRAGMENT_SHADER = ""
            + "precision mediump float;"
            + "varying mediump vec2 textureCoordinate;"
            + "uniform sampler2D inputImageTexture;"
            + "uniform sampler2D uColorMapTexture;"
            + "void main(){"
            + "   vec4 c1 = texture2D(inputImageTexture, textureCoordinate);"
            + "   float r = texture2D(uColorMapTexture, vec2(c1.r,0.0)).r;"
            + "   float g = texture2D(uColorMapTexture, vec2(c1.g,0.0)).g;"
            + "   float b = texture2D(uColorMapTexture, vec2(c1.b,0.0)).b;"
            + "   gl_FragColor = vec4(r,g,b,1.0);"
            + "}";

    private int imageTexture;

    public UWhiteningGPUVideoFilter() {
        colorMap = new byte[1024];
        int cur = -1;
        for (int i = 0; i < 256; i++) {
            colorMap[++cur] = ((byte) (int) (255 * Math.pow(i / 255.0, 0.7)));
            colorMap[++cur] = ((byte) (int) (255 * Math.pow(i / 255.0, 0.7)));
            colorMap[++cur] = ((byte) (int) (255 * Math.pow(i / 255.0, 0.65)));
            colorMap[++cur] = 0;
        }
    }

    private static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        if (vertexShaderCode == null || fragmentShaderCode == null) {
            throw new RuntimeException("invalid shader code");
        }
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        int[] status = new int[1];
        GLES20.glCompileShader(vertexShader);
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("vertext shader compile failed:" + GLES20.glGetShaderInfoLog(vertexShader));
        }
        GLES20.glCompileShader(fragmentShader);
        GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("fragment shader compile failed:" + GLES20.glGetShaderInfoLog(fragmentShader));
        }
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("link program failed:" + GLES20.glGetProgramInfoLog(program));
        }
        return program;
    }

    @Override
    public void onInit(int width, int height) {
        super.onInit(width, height);
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        ByteBuffer result = ByteBuffer.allocateDirect(colorMap.length).
                order(ByteOrder.nativeOrder());
        result.position(0);
        result.put(colorMap);
        result.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 256, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, result);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        imageTexture = texture[0];
        glProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        GLES20.glUseProgram(glProgram);
        glCamTextureLoc = GLES20.glGetUniformLocation(glProgram, "inputImageTexture");
        glColorMapTextureLoc = GLES20.glGetUniformLocation(glProgram, "uColorMapTexture");
        glCamPostionLoc = GLES20.glGetAttribLocation(glProgram, "position");
        glCamTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "inputTextureCoordinate");
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        GLES20.glUseProgram(glProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cameraTexture);
        GLES20.glUniform1i(glCamTextureLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTexture);
        GLES20.glUniform1i(glColorMapTextureLoc, 1);
        GLES20.glEnableVertexAttribArray(glCamPostionLoc);
        GLES20.glEnableVertexAttribArray(glCamTextureCoordLoc);
        shapeBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamPostionLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, shapeBuffer);
        textrueBuffer.position(0);
        GLES20.glVertexAttribPointer(glCamTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false,
                2 * 4, textrueBuffer);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndecesBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, drawIndecesBuffer);
        GLES20.glFinish();
        GLES20.glDisableVertexAttribArray(glCamPostionLoc);
        GLES20.glDisableVertexAttribArray(glCamTextureCoordLoc);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteProgram(glProgram);
        GLES20.glDeleteTextures(1, new int[]{imageTexture}, 0);
    }
}
