package com.ucloud.ulive.example.screen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.ucloud.ulive.UScreenStreaming;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.MainActivity;
import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.filter.audio.UAudioMuteFilter;
import com.ucloud.ulive.example.filter.audio.URawAudioMixFilter;
import com.ucloud.ulive.example.utils.StreamProfileUtil;
import com.ucloud.ulive.example.widget.ScreenControllerView;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.lemon.faceu.openglfilter.common.FilterCore.getContext;

/**
 * @author michael on 2017/04/18.
 */
public class ScreenDemo extends Activity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    private AVOption avOption;

    private UScreenStreaming streamer;

    @Bind(R.id.live_finish_container)
    View streamOverContainer;

    @Bind(R.id.live_screen_panel)
    ScreenControllerView controllerView;

    @Bind(R.id.btn_finish)
    Button backMainIndexButton;

    private EffectHolder effectHolder;

    private FloatingWindow floatingWindow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        initConfig();
        initView();
        streamer = UScreenStreaming.Factory.newInstance();
        streamer.prepare(StreamProfileUtil.build(avOption));
        streamer.setOnNetworkStateListener(controllerView);
        streamer.setOnStreamStateListener(controllerView);
        controllerView.updateStreamEnvUI(avOption, streamer);
        controllerView.setStreamer(streamer);
        effectHolder = new EffectHolder();
        if (avOption.videoCaptureOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
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
        avOption.videoCaptureOrientation = i.getIntExtra(MainActivity.KEY_CAPTURE_ORIENTATION, UVideoProfile.ORIENTATION_PORTRAIT);
        avOption.videoFramerate = i.getIntExtra(MainActivity.KEY_FPS, 20);
        avOption.videoBitrate = i.getIntExtra(MainActivity.KEY_VIDEO_BITRATE, UVideoProfile.VIDEO_BITRATE_NORMAL);
        avOption.videoResolution = i.getIntExtra(MainActivity.KEY_VIDEO_RESOLUTION, UVideoProfile.Resolution.RATIO_AUTO.ordinal());
    }

    private void initView() {
        ButterKnife.bind(this);
        backMainIndexButton.setOnClickListener(backBtnClickListener);
        controllerView.setOnPannelClickListener(new MyControllerViewListener());
    }

    @Override
    public void onPause() {
        super.onPause();
        streamer.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        streamer.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        streamer.onDestroy();
    }

    class MyControllerViewListener extends ScreenControllerView.ClickListenerImpl {

        @Override
        public boolean onStartButtonClick() {
            if (streamer.isRecording()) {
                streamer.stopRecording();
                return false;
            }
            else {
                streamer.startRecording();
                return true;
            }
        }

        @Override
        public boolean onAudioMuteButtonClick() {
            effectHolder.mute = !effectHolder.mute;
            if (effectHolder.mute) {
                streamer.setAudioCPUFilter(new UAudioMuteFilter());
            }
            else {
                streamer.setAudioCPUFilter(null);
            }
            return effectHolder.mute;
        }

        @Override
        public boolean onAudioMixButtonClick() {
            effectHolder.mix = !effectHolder.mix;
            if (effectHolder.mix) {
                URawAudioMixFilter rawAudioMixFilter = new URawAudioMixFilter(getContext(), com.ucloud.ulive.example.filter.audio.URawAudioMixFilter.Mode.ANY, true);
                streamer.setAudioCPUFilter(rawAudioMixFilter);
            }
            else {
                streamer.setAudioCPUFilter(null);
            }
            return effectHolder.mix;
        }

        @Override
        public boolean onExitButtonClick() {
            if (streamer.isRecording()) {
                streamer.stopRecording();
            }
            streamOverContainer.setVisibility(View.VISIBLE);
            return true;
        }

        @Override
        public boolean onFloatWindowButtonClick() {
            effectHolder.floating = !effectHolder.floating;
            if (effectHolder.floating) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ScreenDemo.this)) {
                    requestDrawOverLays();
                }
                else {
                    showFloatWindow();
                }
            }
            else {
                hideFloatWindow();
            }
            return effectHolder.floating;
        }
    }

    private final View.OnClickListener backBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Context context = v.getContext();
            if (context instanceof Activity) {
                ((Activity) (context)).finish();
            }
        }
    };

    // 申请悬浮窗权限
    private void requestDrawOverLays() {
        Intent intent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        }
        startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ScreenDemo.this)) {
                Toast.makeText(this, R.string.float_window_permission_request_failed, Toast.LENGTH_SHORT).show();
            }
            else {
                showFloatWindow();
            }
        }
    }

    private void showFloatWindow() {
        floatingWindow = new FloatingWindow(this, avOption);
        floatingWindow.show();
    }

    private void hideFloatWindow() {
        if (floatingWindow != null) {
            floatingWindow.hide();
            floatingWindow = null;
        }
    }


    class EffectHolder {
        boolean mute;
        boolean mix;
        boolean floating;
    }
}
