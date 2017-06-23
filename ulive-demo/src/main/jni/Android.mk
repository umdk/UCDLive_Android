LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
# Agora Video Engine
LOCAL_MODULE := agora-av
LOCAL_SRC_FILES := ../../../libs/$(TARGET_ARCH_ABI)/libHDACEngine.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
# Agora RTC SDK
LOCAL_MODULE := agora-rtc
LOCAL_SRC_FILES := ../../../libs/$(TARGET_ARCH_ABI)/libagora-rtc-sdk-jni.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := yuv_static
LOCAL_SRC_FILES := ../../../libs/$(TARGET_ARCH_ABI)/libyuv_static.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_SRC_FILES += jni_RemoteDataObserver.cpp
LOCAL_SRC_FILES += remote_data_observer.cpp

LOCAL_LDLIBS := -ldl -llog
LOCAL_MODULE := apm-remote-data-observer
LOCAL_STATIC_LIBRARIES += yuv_static
LOCAL_SHARED_LIBRARIES := agora-av agora-rtc
include $(BUILD_SHARED_LIBRARY)
