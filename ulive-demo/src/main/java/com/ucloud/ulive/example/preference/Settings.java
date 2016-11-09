package com.ucloud.ulive.example.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.ucloud.ulive.example.R;

import static com.ucloud.ulive.UVideoProfile.Resolution;

public class Settings {

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    public Settings(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getFilterType() {
        String key = "pref.filter";
        return getIntPref(key, R.string.pref_filter_default);
    }

    public int getVideoFps() {
        String key = "pref.fps";
        return getIntPref(key, R.string.pref_fps_default);
    }

    public int getVideoBitrate() {
        String key = "pref.video_bitrate";
        return getIntPref(key, R.string.pref_video_bitrate_default);
    }

    public int getVideoCaptureOrientation() {
        String key = "pref.video_capture_orientation";
        return getIntPref(key, R.string.pref_orientation_default);
    }

    /**
     * 获取推流分辨率(capture, output)
     */
    public Resolution getResolution() {
        String key = "pref.resolution";
        int resolutionId = getIntPref(key, R.string.pref_resolution_default);
        Resolution resolution = null;
        switch (resolutionId) {
            case 0:
                resolution = Resolution.RATIO_AUTO;
                break;
            case 1:
                resolution = Resolution.RATIO_4x3;
                break;
            case 2:
                resolution = Resolution.RATIO_16x9;
                break;
            case 3:
                resolution = Resolution.RATIO_16x9_LOW;
                break;
            case 4:
                resolution = Resolution.RATIO_16x9_HIGH;
                break;
            default:
                break;
        }
        return resolution;
    }

    private int getIntPref(String key, int defaultValueId) {
        String defaultValue = mContext.getResources().getString(defaultValueId);
        return Integer.parseInt(mSharedPreferences.getString(key, defaultValue));
    }

    @Override
    public String toString() {
        return "Settings{" +
                " filter type: " + getFilterType() +
                ", video frame rate: " + getVideoFps() +
                ", video bitrate: " + getVideoBitrate() +
                ", video capture orientation: " + getVideoCaptureOrientation() +
                ", resolution: " + getResolution() +
                '}';
    }

}
