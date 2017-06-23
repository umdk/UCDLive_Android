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

        public static final int DEFAULT_VIDEO_BITRATE = UVideoProfile.VIDEO_BITRATE_NORMAL;

        public static final int DEFAULT_VIDEO_CAPTURE_FPS = 20;

        public static final int DEFAULT_CAMERA_FILTER_MODE = UFilterProfile.FilterMode.GPU;

        public static final int DEFAULT_AUDIO_BITRATE = UAudioProfile.AUDIO_BITRATE_NORMAL;

        public static final int DEFAULT_AUDIO_CHANNELS = UAudioProfile.CHANNEL_IN_STEREO;

        public static final int DEFAULT_AUDIO_SAMPLERATE = UAudioProfile.SAMPLE_RATE_44100_HZ;

        public static final UVideoProfile.Resolution DEFAULT_VIDEO_RESOLUTION = UVideoProfile.Resolution.RATIO_AUTO;

        public static final int DEFAULT_AUDIO_SOURCE = UAudioProfile.AUDIO_SOURCE_MIC;
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
                .bitrate(videoBitrate)
                .resolution(videoResolution)
                .codecMode(videoCodecType)
                .captureOrientation(captureOrientation);

        UAudioProfile audioProfile = new UAudioProfile()
                .source(audioSource)
                .bitrate(audioBitrate)
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
