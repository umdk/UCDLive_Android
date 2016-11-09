package com.megvii.facepp.sdk.jni;

import android.content.Context;

public class FaceApi {

	public static native long nativeInit(Context context, byte[] data);
	
	public static native int[] nativeGetFaceppConfig(long handle);
	
	public static native int nativeSetFaceppConfig(long handle, int minFaceSize, int rotation, int interval,
			int detectionMode, int roi_left, int roi_top, int roi_right, int roi_bottom);
	
	public static native int nativeDetect(long handle, byte[] imageData, int width, int height, int imageMode);
	
	public static native float[] nativeFaceInfo(long handle, int index);
	
	public static native float[] nativeLandMark(long handle, int index, int pointNum);
	
	public static native float[] nativeAttribute(long handle, int index);

	public static native long nativeGetApiExpication(Context context);
	
	public static native void nativeRelease(long handle);

	public static native String nativeGetVersion();
	
	public static native long nativeGetApiName();

	static {
		System.loadLibrary("MegviiFacepp-0.2.0");
		System.loadLibrary("MegviiFacepp-jni-0.2.0");
	}

}
