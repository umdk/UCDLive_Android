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
                videoView.applyAspectRatio(UVideoView.VIDEO_RATIO_FILL_PARENT);
                Log.i(TAG, "lifecycle->demo->START");
                break;
            case VIDEO_SIZE_CHANGED:
                break;
            case COMPLETED:
                loadingView.setVisibility(View.GONE);
                Log.i(TAG, "lifecycle->demo->COMPLETED");
                break;
            case RECONNECT:
                if (extra1 < 0) {
                    Log.e(TAG, "lifecycle->demo->RECONNECT reconnect failed.");
                }
                else if (extra1 == 0) {
                    Log.e(TAG, "lifecycle->demo->RECONNECT reconnect failed & info = " + extra2);
                }
                else {
                    Log.e(TAG, "lifecycle->demo->RECONNECT reconnect count = " + extra1 + ", info = " + extra2);
                }
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
            case VIDEO_RENDERING_START:
                break;
            case AUDIO_RENDERING_START:
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
        profile.setInteger(UMediaProfile.KEY_START_ON_PREPARED, 1); //当prepread成功后自动开始播放，(无须自己监听prepared消息调用start方法) 直播推荐开启(1开启，0不开启), 默认不开启
        profile.setInteger(UMediaProfile.KEY_LIVE_STREAMING, 1); //标识播放的流为直播源，还是点播源(0点播，1直播),播放器对不同场景，做了不同的优化
        profile.setInteger(UMediaProfile.KEY_MEDIACODEC, 0); //视频解码方式，推荐软解, (默认为0软解)
        profile.setInteger(UMediaProfile.KEY_RENDER_SURFACUE, 1); //视频渲染方式，推荐

        profile.setInteger(UMediaProfile.KEY_PREPARE_TIMEOUT, 1000 * 15); //设置第一次播放流地址时，PREPARE_TIMEOUT超时时间(超过设置的值，sdk内部会做重连动作，单位ms)
        profile.setInteger(UMediaProfile.KEY_READ_FRAME_TIMEOUT, 1000 * 15); //设置播放过程中，READ_FRAME_TIMEOUT网络卡顿出现读取数据超时(超过设置的值，sdk内部会做重连动作，单位ms)
        //若需要区分4G是否继续播放等与用户确认相关的操作，设置为0，自行根据Android API监听网络状态调用setVideoPath做重连控制操作。
        profile.setInteger(UMediaProfile.KEY_ENABLE_NETWORK_RECOVERY_RECONNECT, 1); //当发生网络切换恢复时SDK内部会做重连（默认为0不开启)
        profile.setInteger(UMediaProfile.KEY_MAX_RECONNECT_COUNT, 5); //当发生IOERROR PREPARE_TIMEOUT READ_FRAME_TIMEOUT 最大重连次数，默认5次

        profile.setInteger(UMediaProfile.KEY_ENABLE_BACKGROUND_PLAY, 1); //设置切换到后台是否继续播放，直播推荐开启，(默认为0不开启)

        //设置播放器允许累积的最大延迟，当网络发生波动，SDK内部会做跳帧处理，若设置的太小容易频繁丢帧，一般推荐2s左右，播放器跳帧时会参考该值，适用直播 单位ms
        profile.setInteger(UMediaProfile.KEY_MAX_CACHED_DURATION, 2000);
        profile.setInteger(UMediaProfile.KEY_CHECK_DROP_FRAME_INTERVAL, 1000 * 30); //设置丢帧的检测频率，适用直播 默认30s检测一次 单位ms
        //以上两个参数决定了播放器网络波动后的延时&平滑度

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
    }
}
