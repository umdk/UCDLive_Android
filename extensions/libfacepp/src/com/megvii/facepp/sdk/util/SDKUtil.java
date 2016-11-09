package com.megvii.facepp.sdk.util;

import java.util.regex.Pattern;

public class SDKUtil {
	
	public static boolean isNumeric(String str){
	    Pattern pattern = Pattern.compile("[0-9]*");  
	    return pattern.matcher(str).matches();     
	}
	
	public static String getErrorType(int retCode){
		switch (retCode) {
		case 0:
			return "MG_RETCODE_OK";
		case -1:
			return "MG_RETCODE_FAILED";
		case 1:
			return "MG_RETCODE_INVALID_ARGUMENT";
		case 2:
			return "MG_RETCODE_INVALID_HANDLE";
		case 3:
			return "MG_RETCODE_INDEX_OUT_OF_RANGE";
		case 101:
			return "MG_RETCODE_EXPIRE";
		case 102:
			return "MG_RETCODE_INVALID_BUNDLEID";
		case 103:
			return "MG_RETCODE_INVALID_LICENSE";
		case 104:
			return "MG_RETCODE_INVALID_MODEL";
		}
		
		return null;
	}
}
