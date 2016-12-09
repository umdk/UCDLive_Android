package com.ucloud.ulive.example.play;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.ucloud.ulive.example.MainActivity;
import com.ucloud.ulive.example.R;
import com.ucloud.uvod.UMediaProfile;
import com.ucloud.uvod.UPlayerStateListener;
import com.ucloud.uvod.widget.UVideoView;

import butterknife.Bind;
import butterknife.ButterKnife;


public class VideoActivity extends Activity implements UPlayerStateListener {

    private static final String TAG = "VideoActivity";

    @Bind(R.id.uvideoview)
    UVideoView mVideoView;

    String uri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);
        Intent i = getIntent();
        uri = i.getStringExtra(MainActivity.KEY_PLAY_ADDRESS);
        if (TextUtils.isEmpty(uri)) {
            Toast.makeText(this, "uri is null.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        int videoCaptureOrientation = i.getIntExtra(MainActivity.KEY_CAPTURE_ORIENTATION, 1);

        if (videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        UMediaProfile profile = new UMediaProfile();
        profile.setInteger(UMediaProfile.KEY_START_ON_PREPARED, 1);
        profile.setInteger(UMediaProfile.KEY_ENABLE_BACKGROUND_PLAY, 0);
        profile.setInteger(UMediaProfile.KEY_LIVE_STREAMING, 1);

        mVideoView.setMediaPorfile(profile);//set before setVideoPath
        mVideoView.setOnPlayerStateListener(this);//set before setVideoPath
        mVideoView.setVideoPath(uri);
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
                mVideoView.applyAspectRatio(UVideoView.VIDEO_RATIO_FILL_PARENT);//set after start
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
