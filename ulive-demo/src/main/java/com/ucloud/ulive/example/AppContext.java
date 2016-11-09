package com.ucloud.ulive.example;

import android.app.Application;

import com.ucloud.ulive.UStreamingContext;

public class AppContext extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        UStreamingContext.init(getApplicationContext(), "publish3-key");
    }
}
