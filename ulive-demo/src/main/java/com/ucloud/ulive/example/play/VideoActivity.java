package com.ucloud.ulive.example.play;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.preference.Settings;
import com.ucloud.uvod.UMediaProfile;
import com.ucloud.uvod.UPlayerStateListener;
import com.ucloud.uvod.widget.UVideoView;

import static com.ucloud.ulive.example.MainActivity.EXTRA_RTMP_ADDRESS;

public class VideoActivity extends Activity implements UPlayerStateListener {

    private static final String TAG = "VideoActivity";

    private UVideoView mVideoView;

    String rtmpPlayStreamUrl;

    Settings mSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        Intent i = getIntent();
        if (i.getStringExtra(EXTRA_RTMP_ADDRESS) != null && !i.getStringExtra(EXTRA_RTMP_ADDRESS).isEmpty()) {
            rtmpPlayStreamUrl = i.getStringExtra(EXTRA_RTMP_ADDRESS);
        }

        mVideoView = (UVideoView) findViewById(R.id.uvideoview);

        mSettings = new Settings(this);

        int videoCaptureOrientation = mSettings.getVideoCaptureOrientation();

        if (videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        UMediaProfile profile = new UMediaProfile();
        profile.setInteger(UMediaProfile.KEY_START_ON_PREPARED, 1);
        profile.setInteger(UMediaProfile.KEY_ENABLE_BACKGROUND_PLAY, 0);
        profile.setInteger(UMediaProfile.KEY_LIVE_STREAMING, 1);

        mVideoView.setMediaPorfile(profile);
        mVideoView.applyAspectRatio(UVideoView.VIDEO_RATIO_FILL_PARENT);
        mVideoView.setOnPlayerStateListener(this);
        mVideoView.setVideoPath(rtmpPlayStreamUrl);
    }

    @Override
    public void onPause() {
        super.onPause();
        mVideoView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVideoView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVideoView.onDestroy();
    }

    public void close(View view) {
        finish();
    }

    @Override
    public void onPlayerStateChanged(State state, int extra1, Object extra2) {
        switch (state) {
            case PREPARING:
                break;
            case PREPARED:
                break;
            case START:
                break;
            case VIDEO_SIZE_CHANGED:
                break;
            case COMPLETED:
                break;
        }
    }

    @Override
    public void onPlayerInfo(Info info, int extra1, Object extra2) {
        switch (info) {
            case BUFFERING_START:
                break;
            case BUFFERING_END:
                break;
            case BUFFERING_UPDATE:
                break;
        }
    }

    @Override
    public void onPlayerError(Error error, int extra1, Object extra2) {
        switch (error) {
            case IOERROR:
                Toast.makeText(this, "Error: " + extra1, Toast.LENGTH_SHORT).show();
                break;
            case PREPARE_TIMEOUT:
                break;
            case READ_FRAME_TIMEOUT:
                break;
            case UNKNOWN:
                Toast.makeText(this, "Error: " + extra1, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
