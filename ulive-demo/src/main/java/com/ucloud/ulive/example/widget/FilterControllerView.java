package com.ucloud.ulive.example.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ucloud.ulive.example.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FilterControllerView extends LinearLayout implements SeekBar.OnSeekBarChangeListener {

    public static int LEVEL1 = 60;

    public static int LEVEL2 = 26;

    public static int LEVEL3 = 15;

    @Bind(R.id.seek_bar_skin_blur)
    SeekBar skinBlur;

    @Bind(R.id.txtv_skin_blur_progress)
    TextView skinBlurProgress;

    @Bind(R.id.seek_bar_skin_ruddy)
    SeekBar skinRuddy;

    @Bind(R.id.txtv_skin_ruddy_progress)
    TextView skinRuddyProgress;

    @Bind(R.id.seek_bar_skin_whitening)
    SeekBar skinWhitening;

    @Bind(R.id.txtv_skin_whitening_progress)
    TextView skinWhiteningProgress;

    private ProgressListener progressListener;

    public FilterControllerView(Context context) {
        super(context);
    }

    public FilterControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FilterControllerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FilterControllerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
        skinBlur.setOnSeekBarChangeListener(this);
        skinRuddy.setOnSeekBarChangeListener(this);
        skinWhitening.setOnSeekBarChangeListener(this);
    }

    public void setListener(ProgressListener listener) {
        progressListener = listener;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seek_bar_skin_blur:
                skinBlurProgress.setText(String.valueOf(progress));
                LEVEL1 = progress;
                break;
            case R.id.seek_bar_skin_whitening:
                skinWhiteningProgress.setText(String.valueOf(progress));
                LEVEL2 = progress;
                break;
            case R.id.seek_bar_skin_ruddy:
                skinRuddyProgress.setText(String.valueOf(progress));
                LEVEL3 = progress;
                break;
            default:
                break;
        }
        if (progressListener != null) {
            progressListener.onProgressChanaged(LEVEL1, LEVEL2, LEVEL3);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void initProgress(int level1, int level2, int level3) {
        skinBlur.setMax(100);
        skinWhitening.setMax(100);
        skinRuddy.setMax(100);

        skinBlur.setProgress(level1);
        skinWhitening.setProgress(level2);
        skinRuddy.setProgress(level3);

        skinBlurProgress.setText(String.valueOf(level1));
        skinWhiteningProgress.setText(String.valueOf(level2));
        skinRuddyProgress.setText(String.valueOf(level3));
    }

    public interface ProgressListener {
        boolean onProgressChanaged(int level1, int level2, int level3);
    }
}
