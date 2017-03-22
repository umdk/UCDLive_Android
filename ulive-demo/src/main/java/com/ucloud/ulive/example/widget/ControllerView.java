package com.ucloud.ulive.example.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
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

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.ucloud.ulive.example.R.id.btn_toggle_caputre_orientation;

public class ControllerView extends RelativeLayout implements View.OnClickListener, UCameraSessionListener, UStreamStateListener, UNetworkListener {

    public static final String TAG = "ControllerView";

    class ViewHolder {
        @Bind(R.id.btn_switch_camera)
        Button switchCameraBtn;

        @Bind(R.id.btn_toggle_face_detector)
        Button toggleFaceDetectorBtn;

        @Bind(R.id.btn_toggle_flashmode)
        Button toggleFlashmodeBtn;

        @Bind(R.id.fl_bottombar)
        FrameLayout bottombar;

        @Bind(R.id.btn_toggle_codec_mode)
        Button toggleCodecModeBtn;

        @Bind(R.id.btn_toggle_render_mode)
        Button toggleRenderModeBtn;

        @Bind(btn_toggle_caputre_orientation)
        Button toggleCaputreOrientationBtn;

        @Bind(R.id.btn_exit)
        Button exitBtn;

        @Bind(R.id.scrollview_rightbar)
        ScrollView rightbarScrollView;

        @Bind(R.id.txtv_bitrate)
        TextView bitrateTxtv;

        @Bind(R.id.txtv_recording_time)
        TextView recordingTimeTxtv;

        @Bind(R.id.txtv_framerate)
        TextView framerateTxtv;

        @Bind(R.id.txtv_stream_env_info)
        TextView streamEnvInfoTxtv;

        @Bind(R.id.txtv_network_block_count)
        TextView networkBlockCountTxtv;

        @Bind(R.id.txtv_debug_log)
        TextView debugLogTxtv;

        @Bind(R.id.scrollview_debug_log_pannel)
        ScrollView debugLogPannelScrollView;

        @Bind(R.id.fl_debug_info_pannel)
        FrameLayout debugInfoPannel;

        @Bind(R.id.txtv_copy_to_clipboard)
        TextView copyToClipboardTxtv;

        @Bind(R.id.btn_toggle_filter)
        Button toggleFilterBtn;

        @Bind(R.id.btn_toggle_record)
        Button toggleRecordBtn;

        @Bind(R.id.btn_toggle_mute)
        Button toggleMuteBtn;

        @Bind(R.id.btn_toggle_mix)
        Button toggleMixBtn;

        @Bind(R.id.btn_toggle_mirror)
        Button toggleMirrorBtn;

        @Bind(R.id.txtv_toggle_debug_log_vivisble)
        TextView toggleDebugLogVivisbleTxtv;

        @Bind(R.id.txtv_clear_debug_log)
        TextView clearDebugLogTxtv;

        @Bind(R.id.btn_captureframe)
        Button captureframeBtn;

        @Bind(R.id.imgview_cameraframe)
        ImageView hintCameraFrameImgView;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
            registerListener();
        }

        private void registerListener() {
            //right pannel
            toggleRecordBtn.setOnClickListener(ControllerView.this);
            toggleMuteBtn.setOnClickListener(ControllerView.this);
            toggleMixBtn.setOnClickListener(ControllerView.this);
            toggleMirrorBtn.setOnClickListener(ControllerView.this);
            toggleCodecModeBtn.setOnClickListener(ControllerView.this);
            toggleRenderModeBtn.setOnClickListener(ControllerView.this);
            toggleCaputreOrientationBtn.setOnClickListener(ControllerView.this);
            captureframeBtn.setOnClickListener(ControllerView.this);
            exitBtn.setOnClickListener(ControllerView.this);

            //bottom pannel
            toggleFlashmodeBtn.setOnClickListener(ControllerView.this);
            switchCameraBtn.setOnClickListener(ControllerView.this);
            toggleFaceDetectorBtn.setOnClickListener(ControllerView.this);
            toggleFilterBtn.setOnClickListener(ControllerView.this);

            //debug pannel
            copyToClipboardTxtv.setOnClickListener(ControllerView.this);
            clearDebugLogTxtv.setOnClickListener(ControllerView.this);
            toggleDebugLogVivisbleTxtv.setOnClickListener(ControllerView.this);
        }
    }

    private ViewHolder mViewHolder;

    private DebugEnvHolder mDebugEnvHolder;

    class DebugEnvHolder {
        private StringBuffer mLogMsg = new StringBuffer("");
        private long networkBlockCount;
    }

    private OnControllerViewClickListener mOuterOnControllerViewClickListener;

    public ControllerView(Context context) {
        super(context);
    }

    public ControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ControllerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mViewHolder = new ViewHolder(this);
        mDebugEnvHolder = new DebugEnvHolder();
    }

    public void setOnPannelClickListener(OnControllerViewClickListener listener) {
        mOuterOnControllerViewClickListener = listener;
    }


    private void initBtnState(AVOption avOption) {
        if (avOption == null || mViewHolder == null) {
            return;
        }
        if (avOption.videoCaptureOrientation == UVideoProfile.ORIENTATION_PORTRAIT) {
            mViewHolder.toggleCaputreOrientationBtn.setText(getResources().getString(R.string.controller_landspace));
        } else {
            mViewHolder.toggleCaputreOrientationBtn.setText(getResources().getString(R.string.controller_portrait));
        }

        if (avOption.videoFilterMode == UFilterProfile.FilterMode.GPU) {
            mViewHolder.toggleRenderModeBtn.setText(getResources().getString(R.string.controller_cpu));
        } else {
            mViewHolder.toggleRenderModeBtn.setText(getResources().getString(R.string.controller_gpu));
        }
        if (avOption.videoCodecType == UVideoProfile.CODEC_MODE_HARD) {
            mViewHolder.toggleCodecModeBtn.setText(getResources().getString(R.string.controller_sw));
        } else {
            mViewHolder.toggleCodecModeBtn.setText(getResources().getString(R.string.controller_hw));
        }
    }

    @Override
    public void onClick(View v) {
        //right pannel
        switch (v.getId()) {
            case R.id.btn_toggle_record:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onStartButtonClick();
                }
                break;
            case R.id.btn_toggle_mute:
                if (mOuterOnControllerViewClickListener != null) {
                    boolean flag = mOuterOnControllerViewClickListener.onAudioMuteButtonClick();
                    if (flag) {
                        appendDebugLogInfo("audio mute.");
                        mViewHolder.toggleMuteBtn.setText(getResources().getString(R.string.controller_unmute));
                    } else {
                        appendDebugLogInfo("audio cancel mute.");
                        mViewHolder.toggleMuteBtn.setText(getResources().getString(R.string.controller_mute));
                    }
                }
                break;
            case R.id.btn_toggle_mix:
                if (mOuterOnControllerViewClickListener != null) {
                    boolean flag = mOuterOnControllerViewClickListener.onAudioMixButtonClick();
                    if (flag) {
                        if (LiveCameraView.getInstance().isRecording()) {
                            appendDebugLogInfo("raw audio mixer start.");
                        } else {
                            appendDebugLogInfo("raw audio mixer delay after UEasyStreaming started.");
                        }
                        mViewHolder.toggleMixBtn.setText(getResources().getString(R.string.controller_no_mix));
                    } else {
                        mViewHolder.toggleMixBtn.setText(getResources().getString(R.string.controller_mix));
                        appendDebugLogInfo("raw audio mixer stop");
                    }
                }
                break;
            case R.id.btn_toggle_mirror:
                if (mOuterOnControllerViewClickListener != null) {
                    boolean flag = mOuterOnControllerViewClickListener.onVideoMirrorButtonClick();
                    if (flag) {
                        appendDebugLogInfo("front camera mirror.");
                        mViewHolder.toggleMirrorBtn.setText(getResources().getString(R.string.controller_no_mirror));
                    } else {
                        appendDebugLogInfo("front camera cancel mirror.");
                        mViewHolder.toggleMirrorBtn.setText(getResources().getString(R.string.controller_mirror));
                    }
                }
                break;
            case R.id.btn_toggle_codec_mode:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onVideoCodecButtonClick();
                }
                break;
            case R.id.btn_toggle_render_mode:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onVideoFilterModeButtonClick();
                }
                break;
            case btn_toggle_caputre_orientation:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onVidoCaptureOrientationButtonClick();
                }
                break;
            case R.id.btn_captureframe:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onVidoCaptureFrameButtonClick();
                    LiveCameraView.getInstance().takeScreenShot(new UScreenShotListener() {
                        @Override
                        public void onScreenShotResult(Bitmap bitmap) {
                            if (bitmap != null) {
                                appendDebugLogInfo("video frame capture succeed.");
                                mViewHolder.hintCameraFrameImgView.setVisibility(View.VISIBLE);
                                mViewHolder.hintCameraFrameImgView.setImageBitmap(bitmap);
                                getHandler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mViewHolder.hintCameraFrameImgView.setVisibility(View.GONE);
                                    }
                                }, 800);
                            } else {
                                appendDebugLogInfo("video frame capture failed.");
                            }
                        }
                    });
                }
                break;
            case R.id.btn_exit:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onExitButtonClick();
                }
                break;
        }

        //bottom pannel
        switch (v.getId()) {
            case R.id.btn_toggle_flashmode:
                if (mOuterOnControllerViewClickListener != null) {
                    boolean flag = mOuterOnControllerViewClickListener.onFlashModeButtonClick();
                    if (flag) {
                        appendDebugLogInfo("toggle flash mode: " + (LiveCameraView.getInstance().isFlashModeOn() ? "open" : "closed") + " succeed.");
                    } else {
                        appendDebugLogInfo("toggle flash mode failed.");
                    }
                }
                break;
            case R.id.btn_switch_camera:
                if (mOuterOnControllerViewClickListener != null) {
                    boolean flag = mOuterOnControllerViewClickListener.onSwitchCameraButtonClick();
                    if (flag) {
                        appendDebugLogInfo("switch camera " + (flag ? "succeed." : "failed."));
                    }
                }
                break;
            case R.id.btn_toggle_face_detector:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onFaceDetectorButtonClick();
                }
                break;
            case R.id.btn_toggle_filter:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onBeautyButtonClick();
                }
                break;
        }

        //debug pannel
        switch (v.getId()) {
            case R.id.txtv_toggle_debug_log_vivisble:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onDebugVisibleButtonClick();
                }
                mViewHolder.debugInfoPannel.setVisibility(mViewHolder.debugInfoPannel.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                mViewHolder.clearDebugLogTxtv.setVisibility(mViewHolder.debugInfoPannel.getVisibility());
                mViewHolder.copyToClipboardTxtv.setVisibility(mViewHolder.debugInfoPannel.getVisibility());
                break;
            case R.id.txtv_clear_debug_log:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onClearDebugButtonClick();
                }
                mViewHolder.debugLogTxtv.setText("");
                mDebugEnvHolder.mLogMsg.setLength(0);
                break;
            case R.id.txtv_copy_to_clipboard:
                if (mOuterOnControllerViewClickListener != null) {
                    mOuterOnControllerViewClickListener.onCopyToClipboardButtonClick();
                    ClipboardManager clipboardManager = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("text", mViewHolder.bitrateTxtv.getText().toString()
                            + "  "+mViewHolder.recordingTimeTxtv.getText().toString() + "\n"
                            + mViewHolder.framerateTxtv.getText().toString()
                            + mViewHolder.networkBlockCountTxtv.getText().toString() + "\n"
                            +mViewHolder.streamEnvInfoTxtv.getText() + mDebugEnvHolder.mLogMsg.toString());
                    clipboardManager.setPrimaryClip(clipData);
                    Toast.makeText(getContext(), "copy to clipboard.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void setDebugPannelVisible(int visible) {
        mViewHolder.debugInfoPannel.setVisibility(visible);
        mViewHolder.clearDebugLogTxtv.setVisibility(visible);
        mViewHolder.copyToClipboardTxtv.setVisibility(visible);
    }

    public void resetSpecialEffectButtonUI(LiveRoomView.SpecailEffectHolder specailEffectHolder) {

        if (specailEffectHolder.mirror) {
            appendDebugLogInfo("front camera mirror.");
            mViewHolder.toggleMirrorBtn.setText(getResources().getString(R.string.controller_no_mirror));
        } else {
            mViewHolder.toggleMirrorBtn.setText(getResources().getString(R.string.controller_mirror));
        }

        if (specailEffectHolder.mix) {
            if (LiveCameraView.getInstance().isRecording()) {
                appendDebugLogInfo("raw audio mixer start.");
            } else {
                appendDebugLogInfo("raw audio mixer delay after UEasyStreaming started.");
            }
            mViewHolder.toggleMixBtn.setText(getResources().getString(R.string.controller_no_mix));
        } else {
            mViewHolder.toggleMixBtn.setText(getResources().getString(R.string.controller_mix));
        }

        if (specailEffectHolder.mute) {
            appendDebugLogInfo("audio mute.");
            mViewHolder.toggleMuteBtn.setText(getResources().getString(R.string.controller_unmute));
        } else {
            mViewHolder.toggleMuteBtn.setText(getResources().getString(R.string.controller_mute));
        }
    }

    private void appendDebugLogInfo(String message) {
        if (mViewHolder.debugLogTxtv != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
            String date = sdf.format(System.currentTimeMillis());
            int mLogMsgLenLimit = 3500;
            while (mDebugEnvHolder.mLogMsg.length() > mLogMsgLenLimit) {
                int idx = mDebugEnvHolder.mLogMsg.indexOf("\n");
                if (idx == 0)
                    idx = 1;
                mDebugEnvHolder.mLogMsg = mDebugEnvHolder.mLogMsg.delete(0, idx);
            }
            mDebugEnvHolder.mLogMsg = mDebugEnvHolder.mLogMsg.append("\n" + "[").append(date).append("]").append(message);
            mViewHolder.debugLogTxtv.setText(mDebugEnvHolder.mLogMsg);
        }
    }

    @Override
    public USize[] onPreviewSizeChoose(int cameraId, List<USize> cameraSupportPreviewSize) {
        return null;
    }

    @Override
    public void onCameraOpenSucceed(int cameraId, List<Integer> supportCameraIndex, int width, int height) {
        appendDebugLogInfo("camera open succeed: " + cameraId + ", support camera index = " + supportCameraIndex.toString());
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
                mDebugEnvHolder.networkBlockCount = 0;
                mViewHolder.networkBlockCountTxtv.setText("0");
                mViewHolder.networkBlockCountTxtv.setVisibility(View.GONE);
                break;
            case PREPARED:
                appendDebugLogInfo("streaming env streaming prepared.");
                break;
            case CONNECTING:
                appendDebugLogInfo("streaming connecting...");
                break;
            case CONNECTED:
                appendDebugLogInfo("streaming connected.");
                break;
            case START:
                appendDebugLogInfo("streaming start.");
                mViewHolder.toggleRecordBtn.setText(getResources().getString(R.string.controller_stop));
                break;
            case STOP:
                appendDebugLogInfo("streaming stop.");
                // clear info
                mViewHolder.bitrateTxtv.setText(String.format(getResources().getString(R.string.info_bitrate), "0.0"));
                mViewHolder.framerateTxtv.setVisibility(View.GONE);
                mViewHolder.recordingTimeTxtv.setText(getResources().getString(R.string.info_time));
                mViewHolder.toggleRecordBtn.setText(getResources().getString(R.string.controller_start));
                break;
            case NETWORK_BLOCK:
                mDebugEnvHolder.networkBlockCount++;
                mViewHolder.networkBlockCountTxtv.setText(String.format(getResources().getString(R.string.info_network_block_count), "" + mDebugEnvHolder.networkBlockCount));
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
                appendDebugLogInfo("streaming io error:" + error.toString() + ", extra = " + extra);
                break;
            case UNKNOWN:
                appendDebugLogInfo("streaming unknown error");
                break;
        }
    }

    //network
    @Override
    public void onNetworkStateChanged(UNetworkListener.State state, Object extra) {
        switch (state) {
            case NETWORK_SPEED:
                //推流数据统计
                mViewHolder.bitrateTxtv.setVisibility(View.VISIBLE);
                int speed = (int) extra;
                if (speed > 1024) {
                    mViewHolder.bitrateTxtv.setText(String.format(getResources().getString(R.string.info_bitrate), (speed / 1024) + ""));
                } else {
                    mViewHolder.bitrateTxtv.setText(String.format(getResources().getString(R.string.info_bitrate_bs), (speed) + ""));
                }
                mViewHolder.framerateTxtv.setVisibility(View.VISIBLE);
                mViewHolder.framerateTxtv.setText(String.format(Locale.US, "draw fps:%.2f, send fps:%.2f", LiveCameraView.getInstance().getDrawFps(), LiveCameraView.getInstance().getSendFps()));
                break;
            case PUBLISH_STREAMING_TIME:
                //sdk内部记录的推流时间,若推流被中断stop之后，该值会重新从0开始计数
                mViewHolder.recordingTimeTxtv.setVisibility(View.VISIBLE);
                long time = (long) extra;
                String retVal = Utils.getTimeFormatString(time);
                mViewHolder.recordingTimeTxtv.setText(retVal);
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
        if (mViewHolder.streamEnvInfoTxtv != null) {
            mViewHolder.streamEnvInfoTxtv.setVisibility(View.VISIBLE);
            String streamEnvInfo = "url:" + option.streamUrl + "\n" +
                    "resolution:" + LiveCameraView.getInstance().getVideoOutputSize() + "\n" +
                    "codec type:" + (option.videoCodecType == UVideoProfile.CODEC_MODE_HARD ? "HARD" : "SOFT") + "\n" +
                    "filter type:" + (option.videoFilterMode == UFilterProfile.FilterMode.GPU ? "GPU" : "CPU") + "\n" +
                    "video bitrate:" + option.videoBitrate + "\n" +
                    "video fps:" + option.videoFramerate + "\n" +
                    "audio bitrate:" + option.audioBitrate + "\n" +
                    "brand:" + Build.BRAND + "_" + Build.MODEL + "\n" +
                    "sdk version:" + UBuild.VERSION + "\n" +
                    "android sdk version:" + Build.VERSION.SDK_INT + "\n";

            mViewHolder.streamEnvInfoTxtv.setText(streamEnvInfo);
        }
    }

    public interface OnControllerViewClickListener {
        boolean onStartButtonClick();

        boolean onAudioMuteButtonClick();

        boolean onAudioMixButtonClick();

        boolean onVideoMirrorButtonClick();

        boolean onVideoCodecButtonClick();

        boolean onVideoFilterModeButtonClick();

        boolean onVidoCaptureOrientationButtonClick();

        boolean onVidoCaptureFrameButtonClick();

        boolean onExitButtonClick();

        boolean onFlashModeButtonClick();

        boolean onSwitchCameraButtonClick();

        boolean onFaceDetectorButtonClick();

        boolean onBeautyButtonClick();

        boolean onDebugVisibleButtonClick();

        boolean onClearDebugButtonClick();

        boolean onCopyToClipboardButtonClick();
    }

    public static class OnControllerViewClickListenerImpl implements OnControllerViewClickListener {

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
        public boolean onVidoCaptureOrientationButtonClick() {
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
        public boolean onBeautyButtonClick() {
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
    }
}