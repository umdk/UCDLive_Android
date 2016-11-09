package com.ucloud.ulive.example.play;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.preference.Settings;
import com.ucloud.uvod.widget.v2.UVideoView;

import static com.ucloud.ulive.example.MainActivity.EXTRA_RTMP_ADDRESS;

public class VideoActivity extends Activity implements UVideoView.Callback {

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

        mSettings = new Settings(this);

        int videoCaptureOrientation = mSettings.getVideoCaptureOrientation();

        if (videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        mVideoView = (UVideoView) findViewById(R.id.videoview);

        mVideoView.setPlayType(UVideoView.PlayType.LIVE);
        mVideoView.setPlayMode(UVideoView.PlayMode.NORMAL);
        mVideoView.setRatio(UVideoView.VIDEO_RATIO_FILL_PARENT);
        mVideoView.setDecoder(UVideoView.DECODER_VOD_SW);

        mVideoView.registerCallback(this);

        mVideoView.setVideoPath(rtmpPlayStreamUrl);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            mVideoView.setVolume(0,0);
            mVideoView.stopPlayback();
            mVideoView.release(true);
        }
    }

    @Override
    public void onEvent(int what, Object message) {
        Log.d(TAG, "what:" + what + ", message:" + message);
        switch (what) {
            case UVideoView.Callback.EVENT_PLAY_START:
                break;
            case UVideoView.Callback.EVENT_PLAY_PAUSE:
                break;
            case UVideoView.Callback.EVENT_PLAY_STOP:
                break;
            case UVideoView.Callback.EVENT_PLAY_COMPLETION:
                Toast.makeText(this, "EVENT_PLAY_COMPLETION", Toast.LENGTH_SHORT).show();
                break;
            case UVideoView.Callback.EVENT_PLAY_DESTORY:
                break;
            case UVideoView.Callback.EVENT_PLAY_ERROR:
                Toast.makeText(this, "EVENT_PLAY_ERROR:" + message, Toast.LENGTH_SHORT).show();
                break;
            case UVideoView.Callback.EVENT_PLAY_RESUME:
                break;
            case UVideoView.Callback.EVENT_PLAY_INFO_BUFFERING_START:
                Log.e(TAG, "network block start....");
//              Toast.makeText(VideoActivity.this, "unstable network", Toast.LENGTH_SHORT).show();
                break;
            case UVideoView.Callback.EVENT_PLAY_INFO_BUFFERING_END:
                Log.e(TAG, "network block end....");
                break;
        }
    }

    public void close(View view) {
        finish();
    }
}
