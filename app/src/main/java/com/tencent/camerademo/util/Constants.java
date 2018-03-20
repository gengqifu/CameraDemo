package com.tencent.camerademo.util;

import android.os.Environment;

import java.io.File;

/**
 * Created by gengqifu on 2018/3/20.
 */

public class Constants {
    public static final String VIDEO_BYTE_ARRAY = "ba";
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator;
}
