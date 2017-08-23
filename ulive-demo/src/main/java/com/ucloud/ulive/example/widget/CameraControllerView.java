package com.ucloud.ulive.example.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ucloud.ucommon.Utils;
import com.ucloud.ulive.UBuild;
import com.ucloud.ulive.UCameraSessionListener;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UNetworkListener;
import com.ucloud.ulive.UScreenShotListener;
import com.ucloud.ulive.USize;
import com.ucloud.ulive.UStreamStateListener;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;
import com.ucloud.ulive.example.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.ucloud.ulive.example.R.id.btn_toggle_caputre_orientation;

public class CameraControllerView extends RelativeLayout implements View.OnClickListener, UCameraSessionListener, UStreamStateListener, UNetworkListener {

    private static final String TAG = "CameraControllerView";

    class ViewHolder {
        @BindView(R.id.btn_switch_camera)
        Button switchCameraBtn;

        @BindView(R.id.btn_toggle_face_detector)
        Button toggleFaceDetectorBtn;

        @BindView(R.id.btn_toggle_flashmode)
        Button toggleFlashmodeBtn;

        @BindView(R.id.fl_bottombar)
        View bottombar;

        @BindView(R.id.btn_toggle_codec_mode)
        Button toggleCodecModeBtn;

        @BindView(R.id.btn_toggle_render_mode)
        Button toggleRenderModeBtn;

        @BindView(btn_toggle_caputre_orientation)
        Button toggleCaputreOrientationBtn;

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

        @BindView(R.id.btn_toggle_mirror)
        Button toggleMirrorBtn;

        @BindView(R.id.txtv_toggle_debug_log_vivisble)
        TextView toggleDebugLogVivisbleTxtv;

        @BindView(R.id.txtv_clear_debug_log)
        TextView clearDebugLogTxtv;

        @BindView(R.id.btn_captureframe)
        Button captureframeBtn;

        @BindView(R.id.imgview_cameraframe)
        ImageView hintCameraFrameImgView;

        @BindView(R.id.spinner_beatuy)
        AppCompatSpinner beautySpinner;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
            registerListener();
        }

        private void registerListener() {
            //right pannel
            toggleRecordBtn.setOnClickListener(CameraControllerView.this);
            toggleMuteBtn.setOnClickListener(CameraControllerView.this);
            toggleMixBtn.setOnClickListener(CameraControllerView.this);
            toggleMirrorBtn.setOnClickListener(CameraControllerView.this);
            toggleCodecModeBtn.setOnClickListener(CameraControllerView.this);
            toggleRenderModeBtn.setOnClickListener(CameraControllerView.this);
            toggleCaputreOrientationBtn.setOnClickListener(CameraControllerView.this);
            captureframeBtn.setOnClickListener(CameraControllerView.this);
            exitBtn.setOnClickListener(CameraControllerView.this);

            //bottom pannel
            toggleFlashmodeBtn.setOnClickListener(CameraControllerView.this);
            switchCameraBtn.setOnClickListener(CameraControllerView.this);
            toggleFaceDetectorBtn.setOnClickListener(CameraControllerView.this);
            beautySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (clickListener != null) {
                        clickListener.onBeautyTypeItemSelectedListener(position, id);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            //debug pannel
            copyToClipboardTxtv.setOnClickListener(CameraControllerView.this);
            clearDebugLogTxtv.setOnClickListener(CameraControllerView.this);
            toggleDebugLogVivisbleTxtv.setOnClickListener(CameraControllerView.this);
        }
    }

    private ViewHolder viewHolder;

    private AVOption avOption;

    private DebugEnvHolder debugEnvHolder;

    class DebugEnvHolder {
        private StringBuffer logMsg = new StringBuffer("");
        private long networkBlockCount;
    }

    private IClickListener clickListener;

    public CameraControllerView(Context context) {
        super(context);
    }

    public CameraControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraControllerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        viewHolder = new ViewHolder(this);
        debugEnvHolder = new DebugEnvHolder();
    }

    public void setOnPanelClickListener(IClickListener listener) {
        clickListener = listener;
    }


    private void initBtnState(AVOption avOption) {
        if (avOption == null || viewHolder == null) {
            return;
        }
        this.avOption = avOption;

        if (avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
            viewHolder.beautySpinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, LiveRoomView.GPU_FILTERS_NAME));
        }
        else {
            viewHolder.beautySpinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, LiveRoomView.CPU_FILTERS_NAME));
        }

        if (avOption.videoCaptureOrientation == UVideoProfile.ORIENTATION_PORTRAIT) {
            viewHolder.toggleCaputreOrientationBtn.setText(getResources().getString(R.string.controller_landspace));
        }
        else {
            viewHolder.toggleCaputreOrientationBtn.setText(getResources().getString(R.string.controller_portrait));
        }

        if (avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
            viewHolder.toggleRenderModeBtn.setText(getResources().getString(R.string.controller_cpu));
        }
        else {
            viewHolder.toggleRenderModeBtn.setText(getResources().getString(R.string.controller_gpu));
        }
        if (avOption.videoCodecType == UVideoProfile.CODEC_MODE_HARD) {
            viewHolder.toggleCodecModeBtn.setText(getResources().getString(R.string.controller_sw));
        }
        else {
            viewHolder.toggleCodecModeBtn.setText(getResources().getString(R.string.controller_hw));
        }
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
                        if (LiveCameraView.getEasyStreaming().isRecording()) {
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
            case R.id.btn_toggle_mirror:
                if (clickListener != null) {
                    boolean flag = clickListener.onVideoMirrorButtonClick();
                    if (flag) {
                        appendDebugLogInfo("front camera mirror.");
                        viewHolder.toggleMirrorBtn.setText(getResources().getString(R.string.controller_no_mirror));
                    }
                    else {
                        appendDebugLogInfo("front camera cancel mirror.");
                        viewHolder.toggleMirrorBtn.setText(getResources().getString(R.string.controller_mirror));
                    }
                }
                break;
            case R.id.btn_toggle_codec_mode:
                if (clickListener != null) {
                    clickListener.onVideoCodecButtonClick();
                }
                break;
            case R.id.btn_toggle_render_mode:
                if (clickListener != null) {
                    clickListener.onVideoFilterModeButtonClick();
                }
                break;
            case btn_toggle_caputre_orientation:
                if (clickListener != null) {
                    clickListener.onOrientationButtonClick();
                }
                break;
            case R.id.btn_captureframe:
                if (clickListener != null) {
                    clickListener.onVidoCaptureFrameButtonClick();
                    LiveCameraView.getEasyStreaming().takeScreenShot(new UScreenShotListener() {
                        @Override
                        public void onScreenShotResult(Bitmap bitmap) {
                            if (bitmap != null) {
                                appendDebugLogInfo("video frame capture succeed.");
                                viewHolder.hintCameraFrameImgView.setVisibility(View.VISIBLE);
                                viewHolder.hintCameraFrameImgView.setImageBitmap(bitmap);
                                getHandler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        viewHolder.hintCameraFrameImgView.setVisibility(View.GONE);
                                    }
                                }, 800);
                            }
                            else {
                                appendDebugLogInfo("video frame capture failed.");
                            }
                        }
                    });
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

        //bottom pannel
        switch (v.getId()) {
            case R.id.btn_toggle_flashmode:
                if (clickListener != null) {
                    boolean flag = clickListener.onFlashModeButtonClick();
                    if (flag) {
                        appendDebugLogInfo("toggle flash mode: " + (LiveCameraView.getEasyStreaming().isFlashModeOn() ? "open" : "closed") + " succeed.");
                    }
                    else {
                        appendDebugLogInfo("toggle flash mode failed.");
                    }
                }
                break;
            case R.id.btn_switch_camera:
                if (clickListener != null) {
                    boolean flag = clickListener.onSwitchCameraButtonClick();
                    if (flag) {
                        appendDebugLogInfo("switch camera " + (flag ? "succeed." : "failed."));
                    }
                }
                break;
            case R.id.btn_toggle_face_detector:
                if (clickListener != null) {
                    clickListener.onFaceDetectorButtonClick();
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

    public void setDebugPannelVisible(int visible) {
        viewHolder.debugInfoPannel.setVisibility(visible);
        viewHolder.clearDebugLogTxtv.setVisibility(visible);
        viewHolder.copyToClipboardTxtv.setVisibility(visible);
    }

    public void resetSpecialEffectButtonUI(LiveRoomView.SpecailEffectHolder specailEffectHolder) {

        if (specailEffectHolder.mirror) {
            appendDebugLogInfo("front camera mirror.");
            viewHolder.toggleMirrorBtn.setText(getResources().getString(R.string.controller_no_mirror));
        }
        else {
            viewHolder.toggleMirrorBtn.setText(getResources().getString(R.string.controller_mirror));
        }

        if (specailEffectHolder.mix) {
            if (LiveCameraView.getEasyStreaming().isRecording()) {
                appendDebugLogInfo("raw audio mixer start.");
            }
            else {
                appendDebugLogInfo("raw audio mixer delay after UEasyStreaming started.");
            }
            viewHolder.toggleMixBtn.setText(getResources().getString(R.string.controller_no_mix));
        }
        else {
            viewHolder.toggleMixBtn.setText(getResources().getString(R.string.controller_mix));
        }

        if (specailEffectHolder.mute) {
            appendDebugLogInfo("audio mute.");
            viewHolder.toggleMuteBtn.setText(getResources().getString(R.string.controller_unmute));
        }
        else {
            viewHolder.toggleMuteBtn.setText(getResources().getString(R.string.controller_mute));
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

    @Override
    public USize[] onPreviewSizeChoose(int cameraId, List<USize> supportPreviewSizeList) {
        return null;
    }

    @Override
    public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndexList, int width, int height) {
        appendDebugLogInfo("camera open succeed: " + cameraId + ", support camera index = " + supportCameraIndexList.toString());
    }

    @Override
    public void onCameraError(UCameraSessionListener.Error error, Object extra) {
        Log.e(TAG, "lifecycle->demo->camera->onCameraError error = " + error + ", extra = " + extra);
        appendDebugLogInfo("onCameraError:" + error + ", extra = " + extra);
        switch (error) {
            case NO_NV21_PREVIEW_FORMAT:
                break;
            case NO_SUPPORT_PREVIEW_SIZE:
                break;
            case NO_PERMISSION:
                break;
            case REQUEST_FLASH_MODE_FAILED:
                break;
            case START_PREVIEW_FAILED:
                break;
            default:
                break;
        }
    }

    @Override
    public void onCameraFlashSwitched(int cameraId, boolean currentState) {
    }

    @Override
    public void onPreviewFrame(int cameraId, byte[] data, int width, int height) {
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
                appendDebugLogInfo("streaming start ip = " + LiveCameraView.getEasyStreaming().getServerIPAddress());
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
                appendDebugLogInfo("streaming io error:" + error.toString() + ", extra = " + extra + ", server:" + LiveCameraView.getEasyStreaming().getServerIPAddress());
                break;
            case UNKNOWN:
                appendDebugLogInfo("streaming unknown error");
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
                viewHolder.framerateTxtv.setText(String.format(Locale.US, "draw fps:%.2f, send fps:%.2f", LiveCameraView.getEasyStreaming().getDrawFps(), LiveCameraView.getEasyStreaming().getSendFps()));
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

    public void updateStreamEnvUI(AVOption option) {
        initBtnState(option);
        if (viewHolder.streamEnvInfoTxtv != null) {
            viewHolder.streamEnvInfoTxtv.setVisibility(View.VISIBLE);
            String streamEnvInfo = "url:" + option.streamUrl + "\n"
                    + "resolution:" + LiveCameraView.getEasyStreaming().getMediaOutputSize() + "\n"
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

        boolean onVideoMirrorButtonClick();

        boolean onVideoCodecButtonClick();

        boolean onVideoFilterModeButtonClick();

        boolean onOrientationButtonClick();

        boolean onVidoCaptureFrameButtonClick();

        boolean onExitButtonClick();

        boolean onFlashModeButtonClick();

        boolean onSwitchCameraButtonClick();

        boolean onFaceDetectorButtonClick();

        boolean onDebugVisibleButtonClick();

        boolean onClearDebugButtonClick();

        boolean onCopyToClipboardButtonClick();

        boolean onBeautyTypeItemSelectedListener(int position, long id);
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
        public boolean onVideoMirrorButtonClick() {
            return false;
        }

        @Override
        public boolean onVideoCodecButtonClick() {
            return false;
        }

        @Override
        public boolean onVideoFilterModeButtonClick() {
            return false;
        }

        @Override
        public boolean onOrientationButtonClick() {
            return false;
        }

        @Override
        public boolean onVidoCaptureFrameButtonClick() {
            return false;
        }

        @Override
        public boolean onExitButtonClick() {
            return false;
        }

        @Override
        public boolean onFlashModeButtonClick() {
            return false;
        }

        @Override
        public boolean onSwitchCameraButtonClick() {
            return false;
        }

        @Override
        public boolean onFaceDetectorButtonClick() {
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
        public boolean onBeautyTypeItemSelectedListener(int position, long id) {
            return false;
        }
    }
}
