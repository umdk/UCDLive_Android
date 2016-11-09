package com.megvii.facepp.sdk.ext;

import android.os.Environment;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Created by michael on 2016/10/08.
 */
public class MiscUtils {

    public static String getSdCardPath() {
        String path = "";
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File dir = Environment.getExternalStorageDirectory();
            path = dir != null ? dir.getAbsolutePath() : "";
        }
        return path;
    }

    public static boolean isFileExist(String filePath) {
        return new File(filePath).exists();
    }

    public static boolean mkdirs(String dir) {
        File file = new File(dir);
        if (file.exists()) {
            return file.isDirectory();
        }

        return file.mkdirs();
    }

    public static boolean safeClose(Closeable closeable) {
        if(null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }
}
