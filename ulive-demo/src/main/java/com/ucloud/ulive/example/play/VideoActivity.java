package com.ucloud.ulive.example.play;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
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

    private static final int MIN_RECONNECT_READ_FRAME_TIMEOUT_COUNT = 3;

    private static final int MIN_RECONNECT_PREPARE_TIMEOUT_COUNT = 3;

    private static final int MAX_RECONNECT_COUNT = 10;

    private int readFrameTimeoutCount = 0;

    private int prepareTimeoutCount = 0;

    private int reconnectCount = 0;

    private  UMediaProfile profile;

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
        connect();
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
                Log.i(TAG, "lifecycle->demo->PREPARING");
                break;
            case PREPARED:
                prepareTimeoutCount = 0;
                Log.i(TAG, "lifecycle->demo->PREPARED");
                break;
            case START:
                Log.i(TAG, "lifecycle->demo->START");
                mVideoView.applyAspectRatio(UVideoView.VIDEO_RATIO_FILL_PARENT);//set after start
                break;
            case VIDEO_SIZE_CHANGED:
                break;
            case COMPLETED:
                Log.i(TAG, "lifecycle->demo->COMPLETED");
                break;
        }
    }

    @Override
    public void onPlayerInfo(Info info, int extra1, Object extra2) {
        switch (info) {
            case BUFFERING_START:
                break;
            case BUFFERING_END:
                readFrameTimeoutCount = 0;
                prepareTimeoutCount = 0;
                if (reconnectCount != 0) {
                    Log.i(TAG, "lifecycle-demo->Play Succeed, reconnect count = " + reconnectCount);
                    Toast.makeText(this, "Play Succeed, reconnect count = " + reconnectCount, Toast.LENGTH_SHORT).show();
                    reconnectCount = 0;
                }
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
                prepareTimeoutCount++;
                Log.w(TAG, "lifecycle->demo->PREPARE_TIMEOUT->" + prepareTimeoutCount);
                if (prepareTimeoutCount >= MIN_RECONNECT_PREPARE_TIMEOUT_COUNT) {//reconnect
                    reconnect();
                }
                break;
            case READ_FRAME_TIMEOUT:
                readFrameTimeoutCount++;
                Log.w(TAG, "lifecycle->demo->READ_FRAME_TIMEOUT->" + readFrameTimeoutCount);
                if (readFrameTimeoutCount >= MIN_RECONNECT_READ_FRAME_TIMEOUT_COUNT) {//reconnect
                    reconnect();
                }
                break;
            case UNKNOWN:
                Toast.makeText(this, "Error: " + extra1, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void reconnect() {
        readFrameTimeoutCount = 0;
        prepareTimeoutCount = 0;
        if (reconnectCount < MAX_RECONNECT_COUNT) {
            reconnectCount++;
            Log.e(TAG, "lifecycle->demo->Play failed, reconnect count = " + reconnectCount);
            connect();
        } else {
            if (mVideoView != null) {
                mVideoView.stopPlayback();
                mVideoView.release(true);
            }
            Log.e(TAG, "lifecycle->demo->Play failed, reconnect MAX count = " + reconnectCount + " reconnect stop.");
            Toast.makeText(this, "Play failed, reconnect MAX count = " + reconnectCount + " reconnect stop.", Toast.LENGTH_SHORT).show();
        }
    }

    private void connect() {
        profile = new UMediaProfile();
        profile.setInteger(UMediaProfile.KEY_START_ON_PREPARED, 1);
        profile.setInteger(UMediaProfile.KEY_ENABLE_BACKGROUND_PLAY, 0);
        profile.setInteger(UMediaProfile.KEY_LIVE_STREAMING, 1);
        profile.setInteger(UMediaProfile.KEY_PREPARE_TIMEOUT, 1000 * 5); //live-streaming 1 default 5s 0 10s
        profile.setInteger(UMediaProfile.KEY_READ_FRAME_TIMEOUT, 1000 * 5); //live-streaming 1 default 5s 0 10s

        if (mVideoView != null && mVideoView.isInPlaybackState()) {
            mVideoView.stopPlayback();
            mVideoView.release(true);
        }
        if (mVideoView != null) {
            mVideoView.setMediaPorfile(profile);//set before setVideoPath
            mVideoView.setOnPlayerStateListener(this);//set before setVideoPath
            mVideoView.setVideoPath(uri);
        } else {
            Log.e(TAG, "lifecycle->dmeo->Are you findViewById(.....) bind UVideoView");
            Toast.makeText(this, "Are you findViewById(.....) bind UVideoView", Toast.LENGTH_SHORT).show();
        }
    }
}
