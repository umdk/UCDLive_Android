package com.ucloud.ulive.example.play;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ucloud.ulive.example.MainActivity;
import com.ucloud.ulive.example.R;
import com.ucloud.uvod.UMediaProfile;
import com.ucloud.uvod.UPlayerStateListener;
import com.ucloud.uvod.widget.UVideoView;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;


public class VideoActivity extends Activity implements UPlayerStateListener {

    private static final String TAG = "VideoActivity";

    @Bind(R.id.uvideoview)
    UVideoView videoView;

    @Bind(R.id.txtv_loading)
    View loadingView;

    @Bind(R.id.txtv_block_count)
    TextView networkBlockCountTxtv;

    private long cacheCount = 0;

    private String uri;

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
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        videoView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        videoView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        videoView.onDestroy();
    }

    public void close(View view) {
        finish();
    }

    @Override
    public void onPlayerStateChanged(State state, int extra1, Object extra2) {
        switch (state) {
            case PREPARING:
                loadingView.setVisibility(View.VISIBLE);
                Log.i(TAG, "lifecycle->demo->PREPARING");
                break;
            case PREPARED:
                Log.i(TAG, "lifecycle->demo->PREPARED");
                break;
            case START:
                loadingView.setVisibility(View.GONE);
                Log.i(TAG, "lifecycle->demo->START");
                videoView.applyAspectRatio(UVideoView.VIDEO_RATIO_FILL_PARENT); //set after start
                break;
            case VIDEO_SIZE_CHANGED:
                break;
            case COMPLETED:
                loadingView.setVisibility(View.GONE);
                Log.i(TAG, "lifecycle->demo->COMPLETED");
                break;
            case RECONNECT:
                Log.i(TAG, "lifecycle->demo->RECONNECT");
                break;
            default:
                break;
        }
    }

    @Override
    public void onPlayerInfo(Info info, int extra1, Object extra2) {
        switch (info) {
            case BUFFERING_START:
                loadingView.setVisibility(View.VISIBLE);
                cacheCount++;
                networkBlockCountTxtv.setVisibility(View.VISIBLE);
                networkBlockCountTxtv.setText(String.format(Locale.US, "缓冲次数:%d", cacheCount));
                break;
            case BUFFERING_END:
                loadingView.setVisibility(View.GONE);
                break;
            case BUFFERING_UPDATE:
                break;
            default:
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
            default:
                break;
        }
    }

    private void connect() {
        UMediaProfile profile = new UMediaProfile();
        profile.setInteger(UMediaProfile.KEY_START_ON_PREPARED, 1);
        profile.setInteger(UMediaProfile.KEY_ENABLE_BACKGROUND_PLAY, 0);
        profile.setInteger(UMediaProfile.KEY_LIVE_STREAMING, 1);
        profile.setInteger(UMediaProfile.KEY_MEDIACODEC, 1);

        profile.setInteger(UMediaProfile.KEY_PREPARE_TIMEOUT, 1000 * 5);
        profile.setInteger(UMediaProfile.KEY_MIN_READ_FRAME_TIMEOUT_RECONNECT_INTERVAL, 3);

        profile.setInteger(UMediaProfile.KEY_READ_FRAME_TIMEOUT, 1000 * 5);
        profile.setInteger(UMediaProfile.KEY_MIN_PREPARE_TIMEOUT_RECONNECT_INTERVAL, 3);

        if (uri != null && uri.endsWith("m3u8")) {
            profile.setInteger(UMediaProfile.KEY_MAX_CACHED_DURATION, 0); //m3u8 默认不开启延时丢帧策略
        }

        if (videoView != null && videoView.isInPlaybackState()) {
            videoView.stopPlayback();
            videoView.release(true);
        }
        if (videoView != null) {
            videoView.setMediaPorfile(profile); //set before setVideoPath
            videoView.setOnPlayerStateListener(this); //set before setVideoPath
            videoView.setVideoPath(uri);
        }
        else {
            Log.e(TAG, "lifecycle->dmeo->Are you findViewById(.....) bind UVideoView");
            Toast.makeText(this, "Are you findViewById(.....) bind UVideoView", Toast.LENGTH_SHORT).show();
        }
    }
}
