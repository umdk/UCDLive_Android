package com.ucloud.ulive.example.filter.video.gpu;

import android.opengl.GLES20;

import com.ucloud.ulive.filter.UBaseVideoGPUFilter;

class UColorMixGPUFilter extends UBaseVideoGPUFilter {

    private static final String FRAGMENT_SHADER = ""
            + "precision mediump float;"
            + "varying mediump vec2 vCamTextureCoord;"
            + "uniform sampler2D uCamTexture;"
            + "uniform vec4 mixcolor;"
            + "void main(){"
            + "    vec4  color = texture2D(uCamTexture, vCamTextureCoord);"
            + "    gl_FragColor = vec4(mix(color.rgb,mixcolor.rgb,mixcolor.a),1.0);"
            + "}";

    private int mixColorLoc;
    private float rcolorValue;
    private float gcolorValue;
    private float bcolorValue;
    private float acolorValue;

    UColorMixGPUFilter(float rcolorValue, float gcolorValue, float bcolorValue, float acolorValue) {
        super(null, FRAGMENT_SHADER);
        this.rcolorValue = rcolorValue;
        this.gcolorValue = gcolorValue;
        this.bcolorValue = bcolorValue;
        this.acolorValue = acolorValue;
    }

    @Override
    public void onInit(int width, int height) {
        super.onInit(width, height);
        mixColorLoc = GLES20.glGetUniformLocation(glProgram, "mixcolor");
    }

    @Override
    public void onPreDraw() {
        super.onPreDraw();
        GLES20.glUniform4f(mixColorLoc, rcolorValue, gcolorValue, bcolorValue, acolorValue);
    }

    public void setMixColor(float r, float g, float b, float a) {
        this.rcolorValue = r;
        this.gcolorValue = g;
        this.bcolorValue = b;
        this.acolorValue = a;
    }
}
