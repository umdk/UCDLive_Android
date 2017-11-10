package com.ucloud.ulive.example.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ucloud.ulive.example.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterControllerView extends LinearLayout implements SeekBar.OnSeekBarChangeListener {
    //美颜（or 强度）
    public static int LEVEL1 = 50;//0-100
    //红润
    public static int LEVEL2 = 50;//0-100
    //明暗度
    public static int LEVEL3 = 50;//0-100

    @BindView(R.id.seek_bar_skin_beauty)
    SeekBar skinBeauty;

    @BindView(R.id.txtv_skin_beauty_progress)
    TextView skinBeautyProgress;

    @BindView(R.id.txtv_beatuy_level)
    TextView beautyLevelTxtv;

    @BindView(R.id.ll_level1)
    View level1Container;

    @BindView(R.id.seek_bar_skin_tone)
    SeekBar skinTone;

    @BindView(R.id.txtv_skin_tone_progress)
    TextView skinToneProgress;

    @BindView(R.id.ll_level2)
    View level2Container;

    @BindView(R.id.seek_bar_skin_bright)
    SeekBar skinBright;

    @BindView(R.id.txtv_skin_bright_progress)
    TextView skinBrightProgress;

    @BindView(R.id.ll_level3)
    View level3Container;


    //REMOVED
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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.bind(this);
        skinBeauty.setOnSeekBarChangeListener(this);
        skinTone.setOnSeekBarChangeListener(this);
        skinBright.setOnSeekBarChangeListener(this);
    }

    public void setListener(ProgressListener listener) {
        progressListener = listener;
    }

    public void setVisibility(int visible, int nums) {
        if (nums == 1) {
            level1Container.setVisibility(visible);
            level2Container.setVisibility(View.GONE);
            level3Container.setVisibility(View.GONE);
        }
        else {
            level1Container.setVisibility(visible);
            level2Container.setVisibility(visible);
            level3Container.setVisibility(visible);
        }
        super.setVisibility(visible);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seek_bar_skin_beauty:
                skinBeautyProgress.setText(String.valueOf(progress));
                LEVEL1 = progress;
                break;
            case R.id.seek_bar_skin_tone:
                skinToneProgress.setText(String.valueOf(progress));
                LEVEL2 = progress;
                break;
            case R.id.seek_bar_skin_bright:
                skinBeautyProgress.setText(String.valueOf(progress));
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

    public void initProgress(int ...levels) {
        if (levels.length == 1) {
            skinBeauty.setMax(100);
            skinBeauty.setProgress(levels[0]);
            skinBeautyProgress.setText(String.valueOf(levels[0]));
            beautyLevelTxtv.setText(R.string.skin_level);
        }
        else if (levels.length == 3) {
            skinBeauty.setMax(100);
            skinBeauty.setProgress(levels[0]);
            skinBeautyProgress.setText(String.valueOf(levels[0]));
            beautyLevelTxtv.setText(R.string.skin_beauty);

            skinTone.setMax(100);
            skinTone.setProgress(levels[1]);
            skinToneProgress.setText(String.valueOf(levels[1]));

            skinBright.setMax(100);
            skinBright.setProgress(levels[2]);
            skinBrightProgress.setText(String.valueOf(levels[2]));
        }
    }

    public interface ProgressListener {
        boolean onProgressChanaged(int ...level1);
    }
}
