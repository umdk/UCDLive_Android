package com.ucloud.ulive.example.stream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.ucloud.ulive.UAudioStreaming;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.BaseActivity;
import com.ucloud.ulive.example.MainActivity;
import com.ucloud.ulive.example.R;
import com.ucloud.ulive.example.filter.audio.UAudioMuteFilter;
import com.ucloud.ulive.example.filter.audio.URawAudioMixFilter;
import com.ucloud.ulive.example.utils.StreamProfileUtil;
import com.ucloud.ulive.example.widget.AudioControllerView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author michael on 2017/07/18.
 * 纯音频RTMP推流
 */
public class AudioDemo extends BaseActivity {

    private AVOption avOption;

    private UAudioStreaming streamer;

    @BindView(R.id.live_finish_container)
    View streamOverContainer;

    @BindView(R.id.live_audio_panel)
    AudioControllerView controllerView;

    @BindView(R.id.btn_finish)
    Button backMainIndexButton;

    private EffectHolder effectHolder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        initConfig();
        initView();
        streamer = UAudioStreaming.Factory.newInstance();
        streamer.setOnNetworkStateListener(controllerView);
        streamer.setOnStreamStateListener(controllerView);
        streamer.prepare(StreamProfileUtil.build(avOption));
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

    class MyControllerViewListener extends AudioControllerView.ClickListenerImpl {

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
                URawAudioMixFilter rawAudioMixFilter = new URawAudioMixFilter(AudioDemo.this, com.ucloud.ulive.example.filter.audio.URawAudioMixFilter.Mode.ANY, true);
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

    class EffectHolder {
        boolean mute;
        boolean mix;
    }
}
