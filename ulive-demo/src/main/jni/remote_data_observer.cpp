#include "remote_data_observer.h"
#include <libyuv.h>
#include <pthread.h>
#include <android/log.h>
#include "include/IAgoraMediaEngine.h"
#include "include/IAgoraRtcEngine.h"
#include <vector>
using namespace std;
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

static agora::media::IVideoFrameObserver::VideoFrame remoteFrame;

static std::vector<unsigned int> remoteUids;
static uint8* dst_rgba;
static uint64 dst_rgba_length = 0;

static bool hasRemoteVideo = false;
static bool hasRemoteAudio = false;

static RemoteDataObserver_vidcallback mVidCallback = NULL;
static RemoteDataObserver_audcallback mAudCallback = NULL;
static void *mOpaque;
static RemoteDataObserver_opaque_free mOpaque_free;

#define DEBUG 0
#if DEBUG
static FILE* file;
#endif

#define LOG_TAG "remote_data_observer"
#define LOGD(...)    ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

// from android samples
/* return current time in milliseconds */
static inline double get_curtime_ms(void) {
	struct timespec res;
	clock_gettime(CLOCK_MONOTONIC, &res);
	return 1000.0 * res.tv_sec + res.tv_nsec / 1000.0 / 1000.0;
}

class AgoraAudioFrameObserver : public agora::media::IAudioFrameObserver
{
public:
	virtual bool onRecordAudioFrame(AudioFrame& audioFrame) override {
#if DEBUG
        __android_log_print(ANDROID_LOG_INFO, "onRecordAudioFrame",  "samples %d, bytesPerSample %d, channels %d, samplesPerSec %d",
                                        audioFrame.samples, audioFrame.bytesPerSample, audioFrame.channels, audioFrame.samplesPerSec);
#endif

        if (mAudCallback) {
            mAudCallback(mOpaque, (unsigned char *) audioFrame.buffer,
                         audioFrame.samples*audioFrame.bytesPerSample*audioFrame.channels,
                         audioFrame.bytesPerSample, audioFrame.samplesPerSec,
						 audioFrame.channels, get_curtime_ms(),0);
        }
		return true;
	}

	virtual bool onPlaybackAudioFrame(AudioFrame& audioFrame) override {
#if DEBUG
        LOGD("onPlaybackAudioFrame->samples %d, bytesPerSample %d, channels %d, samplesPerSec %d",
        audioFrame.samples, audioFrame.bytesPerSample, audioFrame.channels, audioFrame.samplesPerSec);
#endif

        if (mAudCallback) {
            mAudCallback(mOpaque, (unsigned char *) audioFrame.buffer,
                         audioFrame.samples*audioFrame.bytesPerSample*audioFrame.channels,
                         audioFrame.bytesPerSample, audioFrame.samplesPerSec,
						 audioFrame.channels, get_curtime_ms(),1);
        }

		return true;
	}
	virtual bool onPlaybackAudioFrameBeforeMixing(unsigned int uid, AudioFrame& audioFrame) override
	{
		return true;
	}
};

class AgoraVideoFrameObserver : public agora::media::IVideoFrameObserver
{
public:
	virtual bool onCaptureVideoFrame(VideoFrame& videoFrame) override
	{
        //LOGD("onCaptureVideoFrame->width %d, height %d, rotate %d", videoFrame.width, videoFrame.height, videoFrame.rotation);

		return true;
	}

	virtual bool onRenderVideoFrame(unsigned int uid, VideoFrame& videoFrame) override
	{
#if DEBUG
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "remote data, VIDEO(%dx%d)", videoFrame.width, videoFrame.height);
		fwrite(videoFrame.yBuffer, videoFrame.height*videoFrame.width, 1, file);
		fwrite(videoFrame.uBuffer, videoFrame.height*videoFrame.width/4, 1, file);
		fwrite(videoFrame.vBuffer, videoFrame.height*videoFrame.width/4, 1, file);
#endif

        hasRemoteVideo = true;
		int width = videoFrame.width;
		int height = videoFrame.height;

        if (width == 0 || height == 0) {
            return -1;
        }

        pthread_mutex_lock(&mutex);

		vector<unsigned int>::iterator it;
		int isContains = 0;
		for (it = remoteUids.begin(); it != remoteUids.end(); it++) {
			if ((*it) == uid) {
				isContains = 1;
				break;
			}
		}

		if (!isContains) {
			remoteUids.push_back(uid);
		}

		int index = 0;
		for (it = remoteUids.begin(); it != remoteUids.end(); it++) {
			if ((*it) == uid) {
				break;
			}
			index++;
		}

        int YSize = width * height;
        if (dst_rgba == NULL) {
            dst_rgba = (uint8*)malloc(4 * YSize * sizeof(uint8));
            dst_rgba_length = YSize;
        } else if (dst_rgba_length != YSize){
            free(dst_rgba);
            dst_rgba = (uint8*)malloc(4 * YSize * sizeof(uint8));
            dst_rgba_length = YSize;
        }
        int ret = libyuv::I420ToABGR((const uint8 *) videoFrame.yBuffer, videoFrame.yStride,
									 (const uint8 *) videoFrame.uBuffer, videoFrame.uStride,
									 (const uint8 *) videoFrame.vBuffer, videoFrame.vStride,
									 dst_rgba, 4 * width,
									 width, height);

        if(mVidCallback != NULL) {
            mVidCallback(uid, index, mOpaque, (unsigned char *)dst_rgba, 4 * YSize, width, height, videoFrame.rotation, get_curtime_ms());
        }
        pthread_mutex_unlock(&mutex);

		return true;
	}
};

//TODO: no use global ref
static agora::rtc::IRtcEngine* rtcEngine = NULL;
static AgoraVideoFrameObserver s_videoFrameObserver;
static AgoraAudioFrameObserver s_audioFrameObserver;

////this function will be called by agora sdk
extern "C" int __attribute__((visibility("default"))) loadAgoraRtcEnginePlugin(agora::rtc::IRtcEngine* engine)
{
    LOGD("loadAgoraRtcEnginePlugin");
	rtcEngine = engine;
#if DEBUG
	file = fopen("/sdcard/test.yuv", "w+");
#endif
	return 0;
}

extern "C" void __attribute__((visibility("default"))) unloadAgoraRtcEnginePlugin(agora::rtc::IRtcEngine* engine)
{
    LOGD("unloadAgoraRtcEnginePlugin");
#if DEBUG
	fclose(file);
#endif
	rtcEngine = NULL;
}

RemoteDataObserver::RemoteDataObserver(void)
{
}

RemoteDataObserver::~RemoteDataObserver(void)
{
}

void RemoteDataObserver::enableObserver(bool enable)
{
	if (!rtcEngine)
        return;

	pthread_mutex_lock(&mutex);

	agora::util::AutoPtr<agora::media::IMediaEngine> mediaEngine;
	mediaEngine.queryInterface(*rtcEngine, agora::rtc::AGORA_IID_MEDIA_ENGINE);
	if (mediaEngine) {
		if (enable) {
			//TODO
			mediaEngine->registerVideoFrameObserver(&s_videoFrameObserver);
			mediaEngine->registerAudioFrameObserver(&s_audioFrameObserver);
		} else {
			mediaEngine->registerVideoFrameObserver(NULL);
			mediaEngine->registerAudioFrameObserver(NULL);
		}
	}
    LOGD("PreProcessing enbale %d", enable);

    if (enable) {
        remoteUids.clear();
    }
    else {
        if (dst_rgba) {
            free(dst_rgba);
            dst_rgba = NULL;
        }
        remoteUids.clear();
		hasRemoteVideo = false;
        hasRemoteAudio = false;
    }

	pthread_mutex_unlock(&mutex);
}

void RemoteDataObserver::resetRemoteUid(unsigned int uid) {
	vector<unsigned int>::iterator it;
	for(it = remoteUids.begin(); it!=remoteUids.end(); ++it) {
		if(*it == uid) {
			remoteUids.erase(it);
			break;
		}
	}
    LOGD("resetRemoteUid->uid = %d", uid);
}

void RemoteDataObserver::set_callback(RemoteDataObserver_vidcallback vcb,
                                      RemoteDataObserver_audcallback acb,
									  void *opaque, RemoteDataObserver_opaque_free opaque_free) {
	mVidCallback = vcb;
    mAudCallback = acb;
	mOpaque = opaque;
	mOpaque_free = opaque_free;
}
