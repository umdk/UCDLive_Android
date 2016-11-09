package com.ucloud.ulive.example;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ucloud.ulive.example.permission.PermissionsActivity;
import com.ucloud.ulive.example.permission.PermissionsChecker;
import com.ucloud.ulive.example.play.VideoActivity;
import com.ucloud.ulive.example.preference.Settings;
import com.ucloud.ulive.example.preference.SettingsActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_RTMP_ADDRESS = "rtmp_address";

    String streamId = "ucloud_test";

    private int default_index = 0;

    String[] publishUrls = {
            "rtmp://publish3.cdn.ucloud.com.cn/ucloud/%s",
            "rtmp://publish3.usmtd.ucloud.com.cn/live/%s"};

    String[] playUrls = {
            "http://vlive3.rtmp.cdn.ucloud.com.cn/ucloud/%s.flv",
            "http://rtmp3.usmtd.ucloud.com.cn/live/%s.flv"
    };

    private EditText mUrlEdtx;

    private Settings mSettings;

    private static final int REQUEST_CODE = 200;

    private PermissionsChecker mPermissionsChecker; //for android target version >=23

    private Button mLine1Btn;

    private Button mLine2Btn;

    String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        streamId = (int) Math.floor((new Random().nextDouble() * 10000.0 + 10000.0)) + "";
        mPermissionsChecker = new PermissionsChecker(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mUrlEdtx = (EditText) findViewById(R.id.stream_id_et);
        mUrlEdtx.setText(streamId);
        mLine1Btn = (Button) findViewById(R.id.btn_line1);
        mLine2Btn = (Button) findViewById(R.id.btn_line2);
        mSettings = new Settings(this);
        if (default_index == 0) {
            mLine1Btn.setAlpha(1.0f);
            mLine2Btn.setAlpha(0.3f);
        } else {
            mLine1Btn.setAlpha(0.3f);
            mLine2Btn.setAlpha(1.0f);
        }
    }

    public void onPlayBtnClick(View view) {
        String streamId = mUrlEdtx.getText().toString();
        if (TextUtils.isEmpty(streamId)) {
            Toast.makeText(this, "stream id is null", Toast.LENGTH_SHORT).show();
            return;
        }
        startPlayActivity(String.format(playUrls[default_index], streamId));
    }

    public void onLine1BtnClick(View view) {
        default_index = 0;
        mLine1Btn.setAlpha(1.0f);
        mLine2Btn.setAlpha(0.3f);
    }

    public void onLine2BtnClick(View view) {
        default_index = 1;
        mLine1Btn.setAlpha(0.3f);
        mLine2Btn.setAlpha(1.0f);
    }

    public void onPublishBtnClick(View view) {
        String streamId = mUrlEdtx.getText().toString().trim();
        if (TextUtils.isEmpty(streamId)) {
            Toast.makeText(this, "stream id is null", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mPermissionsChecker.lacksPermissions(permissions)) {
            startPermissionsActivity();
        } else {
            startStreamActivity(String.format(publishUrls[default_index], streamId));
        }
    }

    private void startStreamActivity(String url) {
        Intent intent;
        intent = new Intent(this, PublishDemo.class);
        intent.putExtra(MainActivity.EXTRA_RTMP_ADDRESS, url);
        startActivity(intent);
    }

    private void startPlayActivity(String url) {
        Intent intent;
        intent = new Intent(this, VideoActivity.class);
        intent.putExtra(MainActivity.EXTRA_RTMP_ADDRESS, url);
        startActivity(intent);
    }

    private void startPermissionsActivity() {
        PermissionsActivity.startActivityForResult(this, REQUEST_CODE, permissions);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (!mPermissionsChecker.lacksPermissions(permissions)) {
                startStreamActivity(String.format(publishUrls[default_index], streamId));
            }
        }
    }
}