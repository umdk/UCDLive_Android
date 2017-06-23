package com.ucloud.ulive.example;

import com.ucloud.ulive.example.utils.StreamProfileUtil;

public class AVOption {
    public int videoFramerate = StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_CAPTURE_FPS;
    public int videoBitrate = StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_BITRATE;
    public int videoResolution = StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEO_RESOLUTION.ordinal();
    public int videoCodecType = StreamProfileUtil.AVOptionsHolder.DEFAULT_VIDEOCODEC_TYPE;
    public int videoCaptureOrientation = StreamProfileUtil.AVOptionsHolder.DEFAULT_CAPTURE_ORIENTATION;
    public final int audioBitrate = StreamProfileUtil.AVOptionsHolder.DEFAULT_AUDIO_BITRATE;
    public int audioSource = StreamProfileUtil.AVOptionsHolder.DEFAULT_AUDIO_SOURCE;
    public int audioChannels = StreamProfileUtil.AVOptionsHolder.DEFAULT_AUDIO_CHANNELS;
    public int audioSampleRate = StreamProfileUtil.AVOptionsHolder.DEFAULT_AUDIO_SAMPLERATE;
    public int videoFilterMode = StreamProfileUtil.AVOptionsHolder.DEFAULT_CAMERA_FILTER_MODE;
    public final int cameraIndex = StreamProfileUtil.AVOptionsHolder.DEFAULT_CAMERA_INDEX;
    public String streamUrl = "rtmp://publish3.cdn.ucloud.com.cn/ucloud/demo";
    public String streamId;
}
