package com.ucloud.ulive.example.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ucloud.ucommon.Utils;
import com.ucloud.ulive.UBuild;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UNetworkListener;
import com.ucloud.ulive.UScreenStreaming;
import com.ucloud.ulive.UStreamStateListener;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ScreenControllerView extends RelativeLayout implements View.OnClickListener, UStreamStateListener, UNetworkListener {

    private static final String TAG = "ControllerView";

    private UScreenStreaming streamer;

    class ViewHolder {
        @BindView(R.id.btn_exit)
        Button exitBtn;

        @BindView(R.id.scrollview_rightbar)
        ScrollView rightbarScrollView;

        @BindView(R.id.txtv_bitrate)
        TextView bitrateTxtv;

        @BindView(R.id.txtv_recording_time)
        TextView recordingTimeTxtv;

        @BindView(R.id.txtv_framerate)
        TextView framerateTxtv;

        @BindView(R.id.txtv_stream_env_info)
        TextView streamEnvInfoTxtv;

        @BindView(R.id.txtv_network_block_count)
        TextView networkBlockCountTxtv;

        @BindView(R.id.txtv_debug_log)
        TextView debugLogTxtv;

        @BindView(R.id.scrollview_debug_log_pannel)
        ScrollView debugLogPannelScrollView;

        @BindView(R.id.fl_debug_info_pannel)
        FrameLayout debugInfoPannel;

        @BindView(R.id.txtv_copy_to_clipboard)
        TextView copyToClipboardTxtv;

        @BindView(R.id.btn_toggle_record)
        Button toggleRecordBtn;

        @BindView(R.id.btn_toggle_mute)
        Button toggleMuteBtn;

        @BindView(R.id.btn_toggle_mix)
        Button toggleMixBtn;

        @BindView(R.id.btn_toggle_float_window)
        Button toggleFloatWindow;

        @BindView(R.id.txtv_toggle_debug_log_vivisble)
        TextView toggleDebugLogVivisbleTxtv;

        @BindView(R.id.txtv_clear_debug_log)
        TextView clearDebugLogTxtv;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
            registerListener();
        }

        private void registerListener() {
            //right pannel
            toggleRecordBtn.setOnClickListener(ScreenControllerView.this);
            toggleMuteBtn.setOnClickListener(ScreenControllerView.this);
            toggleMixBtn.setOnClickListener(ScreenControllerView.this);
            toggleFloatWindow.setOnClickListener(ScreenControllerView.this);
            exitBtn.setOnClickListener(ScreenControllerView.this);

            //debug pannel
            copyToClipboardTxtv.setOnClickListener(ScreenControllerView.this);
            clearDebugLogTxtv.setOnClickListener(ScreenControllerView.this);
            toggleDebugLogVivisbleTxtv.setOnClickListener(ScreenControllerView.this);
        }
    }

    private ViewHolder viewHolder;

    private DebugEnvHolder debugEnvHolder;

    class DebugEnvHolder {
        private StringBuffer logMsg = new StringBuffer("");
        private long networkBlockCount;
    }

    private IClickListener clickListener;

    public ScreenControllerView(Context context) {
        super(context);
    }

    public ScreenControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScreenControllerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        viewHolder = new ViewHolder(this);
        debugEnvHolder = new DebugEnvHolder();
    }

    public void setOnPannelClickListener(IClickListener listener) {
        clickListener = listener;
    }

    public void setStreamer(UScreenStreaming streamer) {
        this.streamer = streamer;
    }

    private void initBtnState(AVOption avOption) {
    }

    @Override
    public void onClick(View v) {
        //right pannel
        switch (v.getId()) {
            case R.id.btn_toggle_record:
                if (clickListener != null) {
                    clickListener.onStartButtonClick();
                }
                break;
            case R.id.btn_toggle_mute:
                if (clickListener != null) {
                    boolean flag = clickListener.onAudioMuteButtonClick();
                    if (flag) {
                        appendDebugLogInfo("audio mute.");
                        viewHolder.toggleMuteBtn.setText(getResources().getString(R.string.controller_unmute));
                    }
                    else {
                        appendDebugLogInfo("audio cancel mute.");
                        viewHolder.toggleMuteBtn.setText(getResources().getString(R.string.controller_mute));
                    }
                }
                break;
            case R.id.btn_toggle_mix:
                if (clickListener != null) {
                    boolean flag = clickListener.onAudioMixButtonClick();
                    if (flag) {
                        if (streamer.isRecording()) {
                            appendDebugLogInfo("raw audio mixer start.");
                        }
                        else {
                            appendDebugLogInfo("raw audio mixer delay after UEasyStreaming started.");
                        }
                        viewHolder.toggleMixBtn.setText(getResources().getString(R.string.controller_no_mix));
                    }
                    else {
                        viewHolder.toggleMixBtn.setText(getResources().getString(R.string.controller_mix));
                        appendDebugLogInfo("raw audio mixer stop");
                    }
                }
                break;
            case R.id.btn_toggle_float_window:
                if (clickListener != null) {
                    boolean flag = clickListener.onFloatWindowButtonClick();
                    if (flag) {
                        appendDebugLogInfo("open float window.");
                        viewHolder.toggleFloatWindow.setText(getResources().getString(R.string.controller_no_float_window));
                    }
                    else {
                        appendDebugLogInfo("close float window.");
                        viewHolder.toggleFloatWindow.setText(getResources().getString(R.string.controller_float_window));
                    }
                }
                break;
            case R.id.btn_exit:
                if (clickListener != null) {
                    clickListener.onExitButtonClick();
                }
                break;
            default:
                break;
        }

        //debug pannel
        switch (v.getId()) {
            case R.id.txtv_toggle_debug_log_vivisble:
                if (clickListener != null) {
                    clickListener.onDebugVisibleButtonClick();
                }
                viewHolder.debugInfoPannel.setVisibility(viewHolder.debugInfoPannel.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                viewHolder.clearDebugLogTxtv.setVisibility(viewHolder.debugInfoPannel.getVisibility());
                viewHolder.copyToClipboardTxtv.setVisibility(viewHolder.debugInfoPannel.getVisibility());
                break;
            case R.id.txtv_clear_debug_log:
                if (clickListener != null) {
                    clickListener.onClearDebugButtonClick();
                }
                viewHolder.debugLogTxtv.setText("");
                debugEnvHolder.logMsg.setLength(0);
                break;
            case R.id.txtv_copy_to_clipboard:
                if (clickListener != null) {
                    clickListener.onCopyToClipboardButtonClick();
                    ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("text", viewHolder.bitrateTxtv.getText().toString()
                            + "  "
                            + viewHolder.recordingTimeTxtv.getText().toString() + "\n"
                            + viewHolder.framerateTxtv.getText().toString()
                            + viewHolder.networkBlockCountTxtv.getText().toString() + "\n"
                            + viewHolder.streamEnvInfoTxtv.getText() + debugEnvHolder.logMsg.toString());
                    clipboardManager.setPrimaryClip(clipData);
                    Toast.makeText(getContext(), "copy to clipboard.", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private void appendDebugLogInfo(String message) {
        if (viewHolder.debugLogTxtv != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
            String date = sdf.format(System.currentTimeMillis());
            int mLogMsgLenLimit = 3500;
            while (debugEnvHolder.logMsg.length() > mLogMsgLenLimit) {
                int idx = debugEnvHolder.logMsg.indexOf("\n");
                if (idx == 0) {
                    idx = 1;
                }
                debugEnvHolder.logMsg = debugEnvHolder.logMsg.delete(0, idx);
            }
            debugEnvHolder.logMsg = debugEnvHolder.logMsg.append("\n" + "[").append(date).append("]").append(message);
            viewHolder.debugLogTxtv.setText(debugEnvHolder.logMsg);
        }
    }

    //stream state
    @Override
    public void onStateChanged(UStreamStateListener.State state, Object extra) {
        Log.i(TAG, "lifecycle->demo->stream->onStateChanged state = " + state);
        switch (state) {
            case PREPARING:
                appendDebugLogInfo("streaming env preparing...");
                debugEnvHolder.networkBlockCount = 0;
                viewHolder.networkBlockCountTxtv.setText("0");
                viewHolder.networkBlockCountTxtv.setVisibility(View.GONE);
                break;
            case PREPARED:
                appendDebugLogInfo("streaming env prepared.");
                break;
            case CONNECTING:
                appendDebugLogInfo("streaming connecting...");
                break;
            case CONNECTED:
                appendDebugLogInfo("streaming connected.");
                break;
            case START:
                appendDebugLogInfo("streaming start.");
                viewHolder.toggleRecordBtn.setText(getResources().getString(R.string.controller_stop));
                break;
            case STOP:
                appendDebugLogInfo("streaming stop.");
                // clear info
                viewHolder.bitrateTxtv.setText(String.format(getResources().getString(R.string.info_bitrate), "0.0"));
                viewHolder.framerateTxtv.setVisibility(View.GONE);
                viewHolder.recordingTimeTxtv.setText(getResources().getString(R.string.info_time));
                viewHolder.toggleRecordBtn.setText(getResources().getString(R.string.controller_start));
                break;
            case NETWORK_BLOCK:
                debugEnvHolder.networkBlockCount++;
                viewHolder.networkBlockCountTxtv.setVisibility(View.VISIBLE);
                viewHolder.networkBlockCountTxtv.setText(String.format(getResources().getString(R.string.info_network_block_count), "" + debugEnvHolder.networkBlockCount));
                break;
            default:
                break;
        }
    }

    @Override
    public void onStreamError(UStreamStateListener.Error error, Object extra) {
        Log.e(TAG, "lifecycle->demo->stream->error-> msg = " + error + ", extra = " + extra);
        switch (error) {
            case AUDIO_PREPARE_FAILED:
                appendDebugLogInfo("audio env prepare failed.");
                break;
            case VIDEO_PREPARE_FAILED:
                appendDebugLogInfo("video env prepare failed.");
                break;
            case INVALID_STREAMING_URL:
                appendDebugLogInfo("invalid streaming url:" + error.toString());
                break;
            case SIGNATRUE_FAILED:
                appendDebugLogInfo("url signature failed->" + error.toString());
                break;
            case IOERROR:
                appendDebugLogInfo("streaming io error:" + error.toString() + ", extra = " + extra + ", server:" + streamer.getServerIPAddress());
                break;
            case UNKNOWN:
                appendDebugLogInfo("streaming unknown error");
                if (error.toString() != null) {
                    appendDebugLogInfo(error.toString());
                }
                break;
            default:
                break;
        }
    }

    //network
    @Override
    public void onNetworkStateChanged(UNetworkListener.State state, Object extra) {
        switch (state) {
            case NETWORK_SPEED:
                //推流数据统计
                viewHolder.bitrateTxtv.setVisibility(View.VISIBLE);
                int speed = (int) extra;
                if (speed > 1024) {
                    viewHolder.bitrateTxtv.setText(String.format(getResources().getString(R.string.info_bitrate), (speed / 1024) + ""));
                }
                else {
                    viewHolder.bitrateTxtv.setText(String.format(getResources().getString(R.string.info_bitrate_bs), (speed) + ""));
                }
                viewHolder.framerateTxtv.setVisibility(View.VISIBLE);
//                mViewHolder.framerateTxtv.setText(String.format(Locale.US, "draw fps:%.2f, send fps:%.2f", streamer.getDrawFps(), streamer.getSendFps()));
                break;
            case PUBLISH_STREAMING_TIME:
                //sdk内部记录的推流时间,若推流被中断stop之后，该值会重新从0开始计数
                viewHolder.recordingTimeTxtv.setVisibility(View.VISIBLE);
                long time = (long) extra;
                String retVal = Utils.getTimeFormatString(time);
                viewHolder.recordingTimeTxtv.setText(retVal);
                break;
            case DISCONNECT:
                appendDebugLogInfo("network disconnect.");
                break;
            case RECONNECT:
                appendDebugLogInfo("network reconnected.");
                break;
            default:
                break;
        }
    }

    public void updateStreamEnvUI(AVOption option, UScreenStreaming streamer) {
        initBtnState(option);
        if (viewHolder.streamEnvInfoTxtv != null) {
            viewHolder.streamEnvInfoTxtv.setVisibility(View.VISIBLE);
            String streamEnvInfo = "url:" + option.streamUrl + "\n"
                    + "resolution:" + streamer.getMediaOutputSize() + "\n"
                    + "codec type:" + (option.videoCodecType == UVideoProfile.CODEC_MODE_HARD ? "HARD" : "SOFT") + "\n"
                    + "filter type:" + (option.videoFilterMode == UFilterProfile.FilterMode.GPU ? "GPU" : "CPU") + "\n"
                    + "video bitrate:" + option.videoBitrate + "\n"
                    + "video fps:" + option.videoFramerate + "\n"
                    + "audio bitrate:" + option.audioBitrate + "\n"
                    + "brand:" + Build.BRAND + "_" + Build.MODEL + "\n"
                    + "sdk version:" + UBuild.VERSION + "\n"
                    + "android sdk version:" + Build.VERSION.SDK_INT + "\n";

            viewHolder.streamEnvInfoTxtv.setText(streamEnvInfo);
        }
    }

    public interface IClickListener {
        boolean onStartButtonClick();

        boolean onAudioMuteButtonClick();

        boolean onAudioMixButtonClick();

        boolean onExitButtonClick();

        boolean onDebugVisibleButtonClick();

        boolean onClearDebugButtonClick();

        boolean onCopyToClipboardButtonClick();

        boolean onFloatWindowButtonClick();
    }

    public static class ClickListenerImpl implements IClickListener {

        @Override
        public boolean onStartButtonClick() {
            return false;
        }

        @Override
        public boolean onAudioMuteButtonClick() {
            return false;
        }

        @Override
        public boolean onAudioMixButtonClick() {
            return false;
        }

        @Override
        public boolean onExitButtonClick() {
            return false;
        }

        @Override
        public boolean onDebugVisibleButtonClick() {
            return false;
        }

        @Override
        public boolean onClearDebugButtonClick() {
            return false;
        }

        @Override
        public boolean onCopyToClipboardButtonClick() {
            return false;
        }

        @Override
        public boolean onFloatWindowButtonClick() {
            return false;
        }
    }
}
