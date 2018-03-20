package com.tencent.camerademo.service;

import android.app.Service;

/**
 * Created by lqy on 2018/3/15.
 */

public interface CameraViewForService {
    Service getService();

    void onCaptureFrameData(byte[] data, int length, int width, int heigth, int angle);

    void postDelay(Runnable runnable, long time);
}
