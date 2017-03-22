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

/**
 * Created by lw.tan on 2017/3/10.
 */

public class FilterControllerView extends LinearLayout implements SeekBar.OnSeekBarChangeListener {

    public static int level1 = 60;

    public static int level2 = 26;

    public static int level3 = 15;

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

    private Listener mListener;

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

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seek_bar_skin_blur:
                skinBlurProgress.setText(String.valueOf(progress));
                level1 = progress;
                break;
            case R.id.seek_bar_skin_whitening:
                skinWhiteningProgress.setText(String.valueOf(progress));
                level2 = progress;
                break;
            case R.id.seek_bar_skin_ruddy:
                skinRuddyProgress.setText(String.valueOf(progress));
                level3 = progress;
                break;
        }
        if (mListener != null) {
            mListener.onProgressChanaged(level1, level2, level3);
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

    public interface Listener {
        public boolean onProgressChanaged(int level1, int level2, int level3);
    }
}
