package com.ucloud.ulive.example;

import com.ucloud.ulive.UAudioProfile;
import com.ucloud.ulive.UCameraProfile;
import com.ucloud.ulive.UFilterProfile;
import com.ucloud.ulive.UStreamingProfile;
import com.ucloud.ulive.UVideoProfile;

/**
 * Created by lw.tan on 2017/3/2.
 */

public class StreamProfileUtil {

    public static class AVOptionsHolder {

        public static int DefaultCameraIndex = UCameraProfile.CAMERA_FACING_FRONT;

        public static int DefaultVideoCodecType = UVideoProfile.CODEC_MODE_HARD;

        public static int DefaultVideoCaptureOrientation = UVideoProfile.ORIENTATION_PORTRAIT;

        public static int DefaultVideoBitrate = UVideoProfile.VIDEO_BITRATE_NORMAL;

        public static int DefaultVideoCaptureFps = 20;

        public static int DefaultVideoRenderMode = UFilterProfile.FilterMode.GPU;

        public static int DefaultAudioBitrate = UAudioProfile.AUDIO_BITRATE_NORMAL;

        public static int DefaultAudioChannels = UAudioProfile.CHANNEL_IN_STEREO;

        public static int DefaultAudioSamplerate = UAudioProfile.SAMPLE_RATE_44100_HZ;

        public static UVideoProfile.Resolution DefaultVideoResolution = UVideoProfile.Resolution.RATIO_AUTO;
    }

    public static UStreamingProfile buildDefault() {
        return build(StreamProfileUtil.AVOptionsHolder.DefaultVideoCaptureFps
                ,StreamProfileUtil.AVOptionsHolder.DefaultVideoBitrate
                ,StreamProfileUtil.AVOptionsHolder.DefaultVideoResolution
                ,StreamProfileUtil.AVOptionsHolder.DefaultVideoCodecType
                ,StreamProfileUtil.AVOptionsHolder.DefaultVideoCaptureOrientation
                ,StreamProfileUtil.AVOptionsHolder.DefaultAudioBitrate
                ,StreamProfileUtil.AVOptionsHolder.DefaultAudioChannels
                ,StreamProfileUtil.AVOptionsHolder.DefaultAudioSamplerate
                ,StreamProfileUtil.AVOptionsHolder.DefaultVideoRenderMode
                ,StreamProfileUtil.AVOptionsHolder.DefaultCameraIndex, null);
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
                profile.videoFilterMode,
                profile.cameraIndex,
                profile.streamUrl
                );
    }

    public static UStreamingProfile build(int fps,
                                          int videoBitrate,
                                          UVideoProfile.Resolution videoResolution,
                                          int videoCodecType,
                                          int captureOrientation, int audioBitrate, int audioChannels, int audioSampleRate,
                                          int videoRenderMode,
                                          int cameraIndex,
                                          String streamUrl){
        UVideoProfile videoProfile = new UVideoProfile().fps(fps)
                .bitrate(videoBitrate)
                .resolution(videoResolution)
                .codecMode(videoCodecType)
                .captureOrientation(captureOrientation);

        UAudioProfile audioProfile = new UAudioProfile()
                .bitrate(audioBitrate)
                .channels(audioChannels)
                .samplerate(audioSampleRate);

        UFilterProfile filterProfile = new UFilterProfile().mode(videoRenderMode);

        UCameraProfile cameraProfile = new UCameraProfile()
                .setCameraIndex(cameraIndex);

        UStreamingProfile streamingProfile = new UStreamingProfile.Builder()
                .setAudioProfile(audioProfile)
                .setVideoProfile(videoProfile)
                .setFilterProfile(filterProfile)
                .setCameraProfile(cameraProfile)
                .build(streamUrl);
        return streamingProfile;
    }
}