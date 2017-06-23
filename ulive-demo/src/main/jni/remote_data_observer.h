#ifndef UCLOUDSTREAMERANDROIDSDK_REMOTE_DATA_OBSERVER_H
#define UCLOUDSTREAMERANDROIDSDK_REMOTE_DATA_OBSERVER_H

#include <jni.h>

class AgoraAudioFrameObserver;
class AgoraVideoFrameObserver;

typedef void (*RemoteDataObserver_vidcallback)(void *, unsigned char *, int, int, int, int, double);
typedef void (*RemoteDataObserver_audcallback)(void *, unsigned char *, int, int, int, int, double, bool);
typedef void (*RemoteDataObserver_opaque_free)(void *);

class RemoteDataObserver{
public:
    RemoteDataObserver(void);
    ~RemoteDataObserver();

    void enableObserver(bool enable);

    void resetRemoteUid();

    void set_callback(RemoteDataObserver_vidcallback vcb, RemoteDataObserver_audcallback acb,
                      void *opaque, RemoteDataObserver_opaque_free opaque_free);

protected:
};

#endif
