package com.megvii.facepp.sdk.ext;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.lemon.faceu.openglfilter.common.FilterCore;
import com.lemon.faceu.openglfilter.gpuimage.base.FilterFactory;
import com.lemon.faceu.openglfilter.gpuimage.base.GPUImageFilter;
import com.lemon.faceu.openglfilter.gpuimage.base.GPUImageFilterGroupBase;
import com.lemon.faceu.openglfilter.gpuimage.base.MResFileReaderBase;
import com.lemon.faceu.openglfilter.gpuimage.changeface.ChangeFaceInfo;
import com.lemon.faceu.openglfilter.gpuimage.changeface.ChangeFaceNet;
import com.lemon.faceu.openglfilter.gpuimage.dstickers.DynamicStickerData;
import com.lemon.faceu.openglfilter.gpuimage.dstickers.DynamicStickerMulti;
import com.lemon.faceu.openglfilter.gpuimage.filtergroup.GPUImageFilterGroup;
import com.lemon.faceu.openglfilter.gpuimage.filtergroup.GPUImageMultiSectionGroup;
import com.lemon.faceu.openglfilter.gpuimage.filtergroup.MultiSectionInfo;
import com.lemon.faceu.openglfilter.gpuimage.multitriangle.DrawMultiTriangleNet;
import com.lemon.faceu.openglfilter.gpuimage.multitriangle.MultiTriangleInfo;
import com.lemon.faceu.openglfilter.gpuimage.switchface.CloneFaceFilter;
import com.lemon.faceu.openglfilter.gpuimage.switchface.SwitchFaceInfo;
import com.lemon.faceu.openglfilter.gpuimage.switchface.SwitchFaceNet;
import com.lemon.faceu.openglfilter.gpuimage.switchface.TwoPeopleSwitch;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;


/**
 * Created by michael on 2016/10/08.
 */
public class FaceuHelper {

    private static final String TAG = "FaceuHelper";

    private static String FACEU_RES_DIR;

    public final static int TYPE_CHANGE_FACE = 0;

    public final static int TYPE_DYNAMIC_STICKER = 1;

    public final static int TYPE_SWITCH_FACE = 2;

    public final static int TYPE_MULTI_SECTION = 3;

    public final static int TYPE_MULTI_TRIANGLE = 4;  // 注意强制更新的内容

    public final static int TYPE_TWO_PEOPLE_SWITCH = 5;

    public final static int TYPE_CLONE_PEOPLE_FACE = 6;

    public  static EffectItem[] sItems = new EffectItem[]{
            new EffectItem("20091_4_b.zip", 3, "animal_bearcry_b"),
           /* new EffectItem("20088_1_b.zip", 3, "animal_catfoot_b"),
            new EffectItem("20101_1.zip", 1, "animal_lujiao"),
            new EffectItem("20037_7.zip", 1, "hiphop"),
            new EffectItem("50163_1.zip", 1, "lanqiu"),
            new EffectItem("50165_1.zip", 1, "diaozhatian"),
            new EffectItem("170010_1.zip", 2, "mirrorface"),
            new EffectItem("50117_2.zip", 1, "gandong"),
            new EffectItem("50109_2.zip", 1, "weisuo"),
            new EffectItem("50204_1.zip", 1, "catking"),
            new EffectItem("50208_1.zip", 1, "menglu"),
            new EffectItem("20059_1.zip", 1, "dswd"),
            new EffectItem("50067_1.zip", 1, "maikefeng"),
            new EffectItem("50080_1.zip", 1, "dayanjing"),
            new EffectItem("30002_6.zip", 1, "discoball"),*/
    };

    public static void deleteDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteDirectory(child);
            }
        }
        fileOrDirectory.delete();
    }

    public static void init(final Context context) {
        FACEU_RES_DIR =  MiscUtils.getSdCardPath() + File.separator + "UCloud" + File.separator + context.getPackageName() + File.separator + "faceu";
        String olderPath = MiscUtils.getSdCardPath() + File.separator + "UCloud" + File.separator + "Faceu";
        File f = new File(olderPath);
        if (f.exists()) {
            deleteDirectory(f);
        }
        FilterCore.initialize(context, null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (EffectItem item : sItems) {
                    unzipAsset(context, item.name, item.unzipPath);
                }
            }
        }).start();
    }

    private static void unzipAsset(Context context, String assetName, String unzipDirName) {
        String unzipPath = FACEU_RES_DIR + "/" + unzipDirName;
        if (MiscUtils.isFileExist(unzipPath)) {
            Log.i(TAG, "faceu resources already exist");
            return;
        } else {
            Log.i(TAG, "faceu resources unzip path = " + unzipPath);
            MiscUtils.mkdirs(FACEU_RES_DIR);
        }

        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        Map<String, ArrayList<MResFileReaderBase.FileItem>> dirItems = null;
        try {
            inputStream = assetManager.open(assetName);
            dirItems = MResFileReaderBase.getFileListFromZip(inputStream);
            Log.i(TAG, "faceu get file list succeed.");
        } catch (IOException e) {
            Log.i(TAG, "faceu get file list failed.");
            e.printStackTrace();
            return;
        } finally {
            MiscUtils.safeClose(inputStream);
        }

        // 文件流需要打开两次,因为读了两次
        try {
            inputStream = assetManager.open(assetName);
            if (dirItems != null) {
                MResFileReaderBase.unzipToAFile(inputStream, new File(FACEU_RES_DIR), dirItems);
            }
            Log.i(TAG, "faceu unzip succeed.");
        } catch (IOException e) {
            Log.e(TAG, "faceu unzip failed.");
            e.printStackTrace();
        } finally {
            MiscUtils.safeClose(inputStream);
        }
    }

    public static class EffectItem {
        public String name;
        public int type;
        public String unzipPath;

        public EffectItem(String name, int type, String unzipPath) {
            this.name = name;
            this.type = type;
            this.unzipPath = unzipPath;
        }
    }

    public static GPUImageFilterGroupBase getFaceuFilter(int index) {
        EffectItem item = sItems[index];
        GPUImageFilterGroupBase filterGroup = parseEffect(item.type, FACEU_RES_DIR + "/" + item.unzipPath);
        return filterGroup;
    }

    private static GPUImageFilterGroupBase parseEffect(int type, String unzipPath) {
        GPUImageFilterGroupBase groupBase = new GPUImageFilterGroup();
        groupBase.addFilter(new GPUImageFilter());
        try {
            if (type == TYPE_CHANGE_FACE) {
                ChangeFaceInfo changeFaceInfo = FilterFactory.readChangeFaceInfo(unzipPath);
                groupBase.addFilter(new ChangeFaceNet(unzipPath, changeFaceInfo));
            } else if (type == TYPE_DYNAMIC_STICKER) {
                DynamicStickerData data = FilterFactory.readDynamicStickerData(unzipPath);
                groupBase.addFilter(new DynamicStickerMulti(unzipPath, data));
            } else if (type == TYPE_SWITCH_FACE) {
                SwitchFaceInfo switchFaceInfo = FilterFactory.readSwitchFaceData(unzipPath);
                groupBase.addFilter(new SwitchFaceNet(unzipPath, switchFaceInfo));
            } else if (type == TYPE_MULTI_SECTION) {
                MultiSectionInfo multiSectionInfo = FilterFactory.readMultiSectionData(unzipPath);
                groupBase = new GPUImageMultiSectionGroup(unzipPath, multiSectionInfo);
                groupBase.addFilter(new GPUImageFilter());
            } else if (type == TYPE_MULTI_TRIANGLE) {
                MultiTriangleInfo info = FilterFactory.readMultiTriangleInfo(unzipPath);
                groupBase.addFilter(new DrawMultiTriangleNet(unzipPath, info));
            } else if (type == TYPE_TWO_PEOPLE_SWITCH) {
                groupBase.addFilter(new TwoPeopleSwitch());
            } else if (type == TYPE_CLONE_PEOPLE_FACE) {
                groupBase.addFilter(new CloneFaceFilter());
            }
        } catch (IOException e) {
            Log.e(TAG, "read effect filter data failed, " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "parse effect filter data failed, " + e.getMessage());
        }

        return groupBase;
    }
}