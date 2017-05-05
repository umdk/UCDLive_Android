package com.ucloud.ulive.example;

import android.app.Application;

import com.megvii.facepp.sdk.ext.FaceuHelper;
import com.ucloud.ulive.UStreamingContext;

public class AppContext extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        UStreamingContext.init(getApplicationContext(), "publish3-key");
        FaceuHelper.init(getApplicationContext());
    }
}