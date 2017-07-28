package com.ucloud.ulive.example.stream;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.MainActivity;
import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.widget.LiveCameraView;
import com.ucloud.ulive.example.widget.LiveRoomView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author lw.tan on 2017/3/1.
 * (RTMP推流)Camera采集 + 音频
 */

public class PublishDemo extends Activity {

    @Bind(R.id.liveroom)
    LiveRoomView liveRoomView;

    @Bind(R.id.livecamera)
    LiveCameraView liveCameraView;

    private AVOption avOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);
        initConfig();
        ButterKnife.bind(this);

        liveRoomView.attachView(liveCameraView);

        liveRoomView.init(avOption);
    }

    private void initConfig() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent i = getIntent();
        avOption = new AVOption();
        avOption.streamUrl = i.getStringExtra(MainActivity.KEY_STREAMING_ADDRESS);
        if (TextUtils.isEmpty(avOption.streamUrl)) {
            Toast.makeText(this, "streaming url is null.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        avOption.videoFilterMode = i.getIntExtra(MainActivity.KEY_FILTER, UFilterProfile.FilterMode.GPU);
        avOption.videoCodecType = i.getIntExtra(MainActivity.KEY_CODEC, UVideoProfile.CODEC_MODE_HARD);
        avOption.videoCaptureOrientation = i.getIntExtra(MainActivity.KEY_CAPTURE_ORIENTATION, UVideoProfile.ORIENTATION_PORTRAIT);
        avOption.videoFramerate = i.getIntExtra(MainActivity.KEY_FPS, 20);
        avOption.videoBitrate = i.getIntExtra(MainActivity.KEY_VIDEO_BITRATE, UVideoProfile.VIDEO_BITRATE_NORMAL);
        avOption.videoResolution = i.getIntExtra(MainActivity.KEY_VIDEO_RESOLUTION, UVideoProfile.Resolution.RATIO_AUTO.ordinal());

        if (avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        liveRoomView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        liveRoomView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        liveRoomView.onDestroy();
    }
}
