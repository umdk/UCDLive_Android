package com.megvii.facepp.sdk;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import com.megvii.facepp.sdk.jni.FaceApi;
import com.megvii.facepp.sdk.util.SDKUtil;

public class Facepp {
	public final static int FPP_GET_LANDMARK81 = 81;
	public final static int FPP_GET_LANDMARK106 = 106;
	public static int facePoints = FPP_GET_LANDMARK106;

	public final static int IMAGEMODE_GRAY = 0;
	public final static int IMAGEMODE_BGR = 1;
	public final static int IMAGEMODE_NV21 = 2;
	public final static int IMAGEMODE_RGBA = 3;

	private long FaceppHandle;

	public String init(Context context, byte[] model) {
		if (context == null || model == null) {
			int lastErrorCode = 1;
			return SDKUtil.getErrorType(lastErrorCode);
		}

		long handle = FaceApi.nativeInit(context, model);
		String errorType = SDKUtil.getErrorType((int) handle);
		if (errorType == null) {
			FaceppHandle = handle;
			return null;
		}

		return errorType;
	}

	public FaceppConfig getFaceppConfig() {
		int[] configs = FaceApi.nativeGetFaceppConfig(FaceppHandle);
		FaceppConfig faceppConfig = new FaceppConfig();
		faceppConfig.minFaceSize = configs[0];
		faceppConfig.rotation = configs[1];
		faceppConfig.interval = configs[2];
		faceppConfig.detectionMode = configs[3];
		faceppConfig.roi_left = configs[4];
		faceppConfig.roi_top = configs[5];
		faceppConfig.roi_right = configs[6];
		faceppConfig.roi_bottom = configs[7];
		for (int i = 0; i < configs.length; i++) {
			Log.w("ceshi", i + "===" + configs[i]);
		}
		return faceppConfig;
	}

	public void setFaceppConfig(FaceppConfig faceppConfig) {
		FaceApi.nativeSetFaceppConfig(FaceppHandle, faceppConfig.minFaceSize, faceppConfig.rotation,
				faceppConfig.interval, faceppConfig.detectionMode, faceppConfig.roi_left, faceppConfig.roi_top,
				faceppConfig.roi_right, faceppConfig.roi_bottom);
	}

	public Face[] detect(byte[] imageData, int width, int height, int imageMode) {
		int faceSize = FaceApi.nativeDetect(FaceppHandle, imageData, width, height, imageMode);
		Face[] faces = new Face[faceSize];
		for (int i = 0; i < faceSize; i++) {
			float[] points = FaceApi.nativeFaceInfo(FaceppHandle, i);
			Face face = new Face();
			loadFaceBaseInfo(face, points);
			loadFacePointsInfo(face, points, 81, 7);
			faces[i] = face;
		}
		return faces;
	}

	public void getLandMark(Face[] faces, int index, int pointNum) {
		float[] points = FaceApi.nativeLandMark(FaceppHandle, index, pointNum);
		Face face = faces[index];
		loadFacePointsInfo(face, points, pointNum, 0);
		faces[index] = face;
	}

	public void get3DPose(Face[] faces, int index) {
		float[] points = FaceApi.nativeAttribute(FaceppHandle, index);
		Face face = faces[index];
		loadFace3DPoseInfo(face, points);
		faces[index] = face;
	}

	public void getBlurness(Face face) {
	}

	public void getEyeStatus(Face face) {
	}

	public void release() {
		if (FaceppHandle == 0)
			return;
		FaceApi.nativeRelease(FaceppHandle);
		FaceppHandle = 0;
	}

	/**
	 * 获取截止日期
	 */
	public static long getApiExpication(Context context) {
		return FaceApi.nativeGetApiExpication(context);
	}

	public static String getVersion() {
		return FaceApi.nativeGetVersion();
	}

	public static long getApiName() {
		return FaceApi.nativeGetApiName();
	}
	
	private void loadFaceBaseInfo(Face face, float[] faceBaseInfo) {
		face.trackID = (int) faceBaseInfo[0];
		face.index = (int) faceBaseInfo[1];
		face.confidence = (int) faceBaseInfo[2];
		Rect rect = new Rect();
		face.rect = rect;
		rect.left = (int) faceBaseInfo[3];
		rect.top = (int) faceBaseInfo[4];
		rect.right = (int) faceBaseInfo[5];
		rect.bottom = (int) faceBaseInfo[6];
	}

	private void loadFacePointsInfo(Face face, float[] facePointsInfo, int facePoints, int offset) {
		PointF[] points = new PointF[facePoints];
		face.points = points;
		for (int j = 0; j < facePoints; j++) {
			points[j] = new PointF();
			points[j].x = facePointsInfo[offset + (j * 2)];
			points[j].y = facePointsInfo[offset + (j * 2 + 1)];
		}
	}

	private void loadFace3DPoseInfo(Face face, float[] face3DPoseInfo) {
		face.pitch = face3DPoseInfo[0];
		face.yaw = face3DPoseInfo[1];
		face.roll = face3DPoseInfo[2];
	}

	public static class Face {
		public int trackID, index;
		public float confidence;
		public float pitch, yaw, roll;
		public Rect rect;
		public PointF[] points;
	}

	public static class FaceppConfig {
		public final static int DETECTION_MODE_TRACKING = 1;
		public final static int DETECTION_MODE_NORMAL = 0;

		public int minFaceSize;
		public int rotation;
		public int interval;
		public int detectionMode;
		public int roi_left;
		public int roi_top;
		public int roi_right;
		public int roi_bottom;
	}
}