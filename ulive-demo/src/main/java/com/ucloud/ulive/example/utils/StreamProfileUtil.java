package com.ucloud.ulive.example.utils;

import com.ucloud.ulive.UAudioProfile;
import com.ucloud.ulive.UCameraProfile;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UStreamingProfile;
import com.ucloud.ulive.UVideoProfile;
import com.ucloud.ulive.example.AVOption;

public final class StreamProfileUtil {

    private StreamProfileUtil() {

    }

    public static class AVOptionsHolder {

        public static final int DEFAULT_CAMERA_INDEX = UCameraProfile.CAMERA_FACING_FRONT;

        public static final int DEFAULT_VIDEOCODEC_TYPE = UVideoProfile.CODEC_MODE_HARD;

        public static final int DEFAULT_CAPTURE_ORIENTATION = UVideoProfile.ORIENTATION_PORTRAIT;

        public static final int DEFAULT_VIDEO_BITRATE = UVideoProfile.VIDEO_BITRATE_MEDIUM;

        public static final int DEFAULT_VIDEO_CAPTURE_FPS = 20;

        public static final int DEFAULT_CAMERA_FILTER_MODE = UFilterProfile.FilterMode.GPU;

        public static final int DEFAULT_AUDIO_BITRATE = UAudioProfile.AUDIO_BITRATE_NORMAL;

        public static final int DEFAULT_AUDIO_CHANNELS = UAudioProfile.CHANNEL_IN_STEREO;

        public static final int DEFAULT_AUDIO_SAMPLERATE = UAudioProfile.SAMPLE_RATE_44100_HZ;

        public static final UVideoProfile.Resolution DEFAULT_VIDEO_RESOLUTION = UVideoProfile.Resolution.RATIO_AUTO;

        public static final int DEFAULT_AUDIO_SOURCE = UAudioProfile.AUDIO_SOURCE_DEFAULT;
    }

    public static UStreamingProfile buildDefault() {
        return build(StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_CAPTURE_FPS,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_BITRATE,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_RESOLUTION,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEOCODEC_TYPE,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_CAPTURE_ORIENTATION,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_AUDIO_BITRATE,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_AUDIO_CHANNELS,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_AUDIO_SAMPLERATE,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_AUDIO_SOURCE,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_CAMERA_FILTER_MODE,
                StreamProfileUtil.AVOptionsHolder.DEFAULT_CAMERA_INDEX, null);
    }

    public static UStreamingProfile build(AVOption profile) {
        return build(profile.videoFramerate,
                profile.videoBitrate,
                UVideoProfile.Resolution.valueOf(profile.videoResolution),
                profile.videoCodecType,
                profile.videoCaptureOrientation,
                profile.audioBitrate,
                profile.audioChannels,
                profile.audioSampleRate,
                profile.audioSource,
                profile.videoFilterMode,
                profile.cameraIndex,
                profile.streamUrl
                );
    }

    private static UStreamingProfile build(int fps,
                                          int videoBitrate,
                                          UVideoProfile.Resolution videoResolution,
                                          int videoCodecType,
                                          int captureOrientation,
                                          int audioBitrate, int audioChannels, int audioSampleRate, int audioSource,
                                          int videoRenderMode,
                                          int cameraIndex,
                                          String streamUrl) {
        UVideoProfile videoProfile = new UVideoProfile().fps(fps)
                .bitrate(videoBitrate) //设置初始码率 推荐设置600kbps
                .minBitrate(UVideoProfile.VIDEO_BITRATE_NORMAL) //设置最低码率 400kbps
                .maxBitrate(UVideoProfile.VIDEO_BITRATE_HIGH) //设置最高码率 800kbps 当网络发生拥塞，
                //关于最低、最高码率：(SDK内部会动态调整音视频码率)
                //1.可自行调整最低码率的值，若最小码率与初始码率相差较小，当网络拥塞时，抗抖动的能力就比较小，整体帧率会较低。
                //2.降码率，对音视频的质量肯定有影响，一般推荐设置一个您认为能够接受的最低效果的值，当网络恢复通畅时，内部会逐步稳定提高码率。
                //3.若流畅优先，视频码率最低推荐可设置200kbps，画质优先推荐成400kbps (初始码率600kbps的情况)，其它您自定义的值也适用。
                .resolution(videoResolution)
                .codecMode(videoCodecType)
                .captureOrientation(captureOrientation);

        UAudioProfile audioProfile = new UAudioProfile()
                .source(audioSource)
                .bitrate(audioBitrate) //设置初始码率 推荐设置64kbps
                .minBitrate(UAudioProfile.AUDIO_BITRATE_LOW) //设置最低码率 48kbps
                .maxBitrate(UAudioProfile.AUDIO_BITRATE_HIGH) //设置最高码率 128kbps
                .channels(audioChannels)
                .samplerate(audioSampleRate);

        UFilterProfile filterProfile = new UFilterProfile().mode(videoRenderMode);

        UCameraProfile cameraProfile = new UCameraProfile()
                .setCameraIndex(cameraIndex);

        return new UStreamingProfile.Builder()
                .setAudioProfile(audioProfile)
                .setVideoProfile(videoProfile)
                .setFilterProfile(filterProfile)
                .setCameraProfile(cameraProfile)
                .build(streamUrl);
    }
}
