package com.ucloud.ulive.example;

import android.Manifest;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ucloud.ucommon.Utils;
import com.ucloud.ulive.UBuild;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.permission.PermissionsActivity;
import com.ucloud.ulive.example.permission.PermissionsChecker;
import com.ucloud.ulive.example.utils.DemoStatistics;
import com.ucloud.ulive.example.utils.StreamProfileUtil;
import com.umeng.analytics.MobclickAgent;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    public static final String KEY_STREAMING_ADDRESS = "streaming-address";

    public static final String KEY_STREAMING_ID = "streaming-id";

    public static final String KEY_PLAY_ADDRESS = "play-address";

    public static final String KEY_CAPTURE_ORIENTATION = "capture-orientation";

    public static final String KEY_FILTER = "capture-filter";

    public static final String KEY_CODEC = "capture-codec";

    public static final String KEY_FPS = "capture-fps";

    public static final String KEY_VIDEO_BITRATE = "video-bitrate";

    public static final String KEY_VIDEO_RESOLUTION = "video-resolution";

    public static final String KEY_ENCRYPTION_MODE = "encryption-mode"; //for argo

    public static final String KEY_ENCRYPTION_KEY = "encryption-key"; //for argo

    private static final int LIVE_REQUEST_CODE = 200;

    private static final int PLAYER_REQUEST_CODE = 201;

    private static final int LIVE_AUDIO_REQUEST_CODE = 202;

    private String streamId = "ucloud_test";

    private String[] demoDirects;

    private String[] demoNames;

    @BindView(R.id.listview)
    ListView listView;

    @BindView(R.id.txtv_version)
    TextView versionTxtv;

    @BindView(R.id.rg_filter)
    RadioGroup filterRg;

    @BindView(R.id.rg_codec)
    RadioGroup codecRg;

    @BindView(R.id.rg_videobitrate)
    RadioGroup videoBitrateRg;

    @BindView(R.id.rg_videoaspect)
    RadioGroup resolutionRg;

    @BindView(R.id.edtxt_framerate)
    EditText framerateEdtxt;

    @BindView(R.id.rg_capture_orientation)
    RadioGroup captureOrientationRg;

    @BindView(R.id.rg_line)
    RadioGroup lineRg;

    @BindView(R.id.edtxt_streaming_id)
    EditText streamIdEdtxt;

    private PermissionsChecker permissionsChecker; //for android target version >=23

    private int currentPosition = 0;

    /**
     * 音频 +视频(Camera + Screen)
     */
    private final String[] liveNeedPermissions = new String[]{
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * 纯音频
     */
    private final String[] liveAudioNeedPermissions = new String[]{
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final String[] playerNeedPermissions = new String[]{
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        streamId = (int) Math.floor((new Random().nextDouble() * 10000.0 + 10000.0)) + "";
        permissionsChecker = new PermissionsChecker(this);
        demoNames = getResources().getStringArray(R.array.demoNames);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, demoNames));
        listView.setOnItemClickListener(this);
        demoDirects = getResources().getStringArray(R.array.demoDirects);
        versionTxtv.setText(String.format("%s %s", UBuild.NAME + "-" +UBuild.VERSION, getResources().getString(R.string.sdk_address)));
        streamIdEdtxt.setText(streamId);
        filterRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (codecRg.getCheckedRadioButtonId() == R.id.rb_swcodec && checkedId == R.id.rb_gpufilter) {
                    Toast.makeText(MainActivity.this, "sorry, soft codec just support cpu filter mode", Toast.LENGTH_SHORT).show();
                    group.check(R.id.rb_cpufilter);
                }
            }
        });
        codecRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_swcodec && filterRg.getCheckedRadioButtonId() == R.id.rb_gpufilter) {
                    Toast.makeText(MainActivity.this, "sorry, gpu filter just support hard codec mode", Toast.LENGTH_SHORT).show();
                    group.check(R.id.rb_hwcodec);
                }
            }
        });
        if(StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_BITRATE == UVideoProfile.VIDEO_BITRATE_LOW) {
            videoBitrateRg.check(R.id.rb_videobitrate_low);
        }
        else if(StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_BITRATE == UVideoProfile.VIDEO_BITRATE_NORMAL) {
            videoBitrateRg.check(R.id.rb_videobitrate_normal);
        }
        else if(StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_BITRATE == UVideoProfile.VIDEO_BITRATE_MEDIUM) {
            videoBitrateRg.check(R.id.rb_videobitrate_medium);
        }
        else if(StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_BITRATE == UVideoProfile.VIDEO_BITRATE_HIGH) {
            videoBitrateRg.check(R.id.rb_videobitrate_high);
        }
    }

    private void startActivity(int index) {
        Intent intent = new Intent();
        intent.putExtra(KEY_FILTER, filterType(filterRg.getCheckedRadioButtonId()));
        intent.putExtra(KEY_CODEC, codecType(codecRg.getCheckedRadioButtonId()));
        intent.putExtra(KEY_VIDEO_BITRATE, videoBitrate(videoBitrateRg.getCheckedRadioButtonId()));
        intent.putExtra(KEY_VIDEO_RESOLUTION, videoResolution(resolutionRg.getCheckedRadioButtonId()));
        intent.putExtra(KEY_FPS, framerate(framerateEdtxt));
        intent.putExtra(KEY_CAPTURE_ORIENTATION, captureOrientation(captureOrientationRg.getCheckedRadioButtonId()));
        line(intent, lineRg.getCheckedRadioButtonId(), streamIdEdtxt);
        intent.putExtra(KEY_STREAMING_ID, streamIdEdtxt.getText().toString());
        intent.putExtra(KEY_ENCRYPTION_KEY, "Encryption");
        intent.putExtra(KEY_ENCRYPTION_MODE, "AES-128-XTS");
        intent.setAction(demoDirects[index]);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LIVE_REQUEST_CODE) {
            if (!permissionsChecker.lacksPermissions(liveNeedPermissions)) {
                startActivity(currentPosition);
            }
        }
        else if (requestCode == PLAYER_REQUEST_CODE) {
            if (!permissionsChecker.lacksPermissions(playerNeedPermissions)) {
                startActivity(currentPosition);
            }
        }
        else if (requestCode == LIVE_AUDIO_REQUEST_CODE) {
            if (!permissionsChecker.lacksPermissions(liveAudioNeedPermissions)) {
                startActivity(currentPosition);
            }
        }
    }

    private int filterType(int id) {
        return id == R.id.rb_cpufilter ? UFilterProfile.FilterMode.CPU : UFilterProfile.FilterMode.GPU;
    }

    private int codecType(int id) {
        return id == R.id.rb_hwcodec ? UVideoProfile.CODEC_MODE_HARD : UVideoProfile.CODEC_MODE_SOFT;
    }

    private int videoBitrate(int id) {
        switch (id) {
            case R.id.rb_videobitrate_low:
                return UVideoProfile.VIDEO_BITRATE_LOW;
            case R.id.rb_videobitrate_normal:
                return UVideoProfile.VIDEO_BITRATE_NORMAL;
            case R.id.rb_videobitrate_medium:
                return UVideoProfile.VIDEO_BITRATE_MEDIUM;
            case R.id.rb_videobitrate_high:
                return UVideoProfile.VIDEO_BITRATE_HIGH;
            default:
                return UVideoProfile.VIDEO_BITRATE_LOW;
        }
    }

    private int videoResolution(int id) {
        switch (id) {
            case R.id.rb_videoaspect_auto:
                return UVideoProfile.Resolution.RATIO_AUTO.ordinal();
            case R.id.rb_videoaspect_4x3:
                return UVideoProfile.Resolution.RATIO_4x3.ordinal();
            case R.id.rb_videoaspect_16x9:
                return UVideoProfile.Resolution.RATIO_16x9.ordinal();
            default:
                return UVideoProfile.Resolution.RATIO_AUTO.ordinal();
        }
    }

    private int framerate(EditText editText) {
        if (editText == null) {
            return 20;
        }
        String framerate = editText.getText().toString();

        if (TextUtils.isEmpty(framerate) || !TextUtils.isDigitsOnly(framerate)) {
            return 20;
        }
        else {
            return Integer.parseInt(editText.getText().toString().trim());
        }
    }

    private int captureOrientation(int id) {
        return id == R.id.rb_capture_orientation_landspace ? UVideoProfile.ORIENTATION_LANDSCAPE : UVideoProfile.ORIENTATION_PORTRAIT;
    }

    private void line(Intent intent, int id, EditText editText) {
        if (id == R.id.rb_line1) {
            intent.putExtra(KEY_STREAMING_ADDRESS, editText.getText().toString().trim());
            intent.putExtra(KEY_PLAY_ADDRESS, editText.getText().toString().trim());
        }
        else {
            intent.putExtra(KEY_STREAMING_ADDRESS, editText.getText().toString().trim());
            intent.putExtra(KEY_PLAY_ADDRESS, editText.getText().toString().trim());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        currentPosition = position;
        if (!Utils.isNetworkConnected(this)) {
            Toast.makeText(this, "当前网络不可用.", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (Utils.getConnectedType(this)) {
            case ConnectivityManager.TYPE_MOBILE:
                Toast.makeText(this, "当前网络: mobile", Toast.LENGTH_SHORT).show();
                break;
            case ConnectivityManager.TYPE_ETHERNET:
                Toast.makeText(this, "当前网络: ehternet", Toast.LENGTH_SHORT).show();
                break;
            case ConnectivityManager.TYPE_WIFI:
                Toast.makeText(this, "当前网络: wifi", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }

        if (demoDirects != null && demoDirects.length > position && !TextUtils.isEmpty(demoDirects[position].trim())) {
            String streamId = streamIdEdtxt.getText().toString();
            if (TextUtils.isEmpty(streamId)) {
                Toast.makeText(this, "streamId is null", Toast.LENGTH_SHORT).show();
                return;
            }
            if (position == 0 || position == 1 || position == 2) { //音视频推流 权限检查
                if (permissionsChecker.lacksPermissions(liveNeedPermissions)) {
                    PermissionsActivity.startActivityForResult(this, LIVE_REQUEST_CODE, liveNeedPermissions);
                }
                else {
                    if (position == 0) {
                        MobclickAgent.onEvent(this, DemoStatistics.RTMP_CAMERA_BUTTON);
                    }
                    else if (position == 1) {
                        MobclickAgent.onEvent(this, DemoStatistics.RTMP_SCREEN_BUTTON);
                    }
                    else {
                        MobclickAgent.onEvent(this, DemoStatistics.RTMP_1V1_LINE_BUTTON);
                    }
                    startActivity(position);
                }
            }
            else if (position == 3) { //纯音频推流 权限检查
                if (permissionsChecker.lacksPermissions(liveAudioNeedPermissions)) {
                    PermissionsActivity.startActivityForResult(this, LIVE_AUDIO_REQUEST_CODE, liveAudioNeedPermissions);
                }
                else {
                    MobclickAgent.onEvent(this, DemoStatistics.RTMP_ONLY_AUDIO_BUTTON);
                    startActivity(position);
                }
            }
            else { //播放 权限检查
                if (permissionsChecker.lacksPermissions(playerNeedPermissions)) {
                    PermissionsActivity.startActivityForResult(this, PLAYER_REQUEST_CODE, playerNeedPermissions);
                }
                else {
                    MobclickAgent.onEvent(this, DemoStatistics.RTMP_PLAY_BUTTON);
                    startActivity(4);
                }
            }
        }
    }
}
