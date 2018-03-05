package com.tencent.camerademo.presenter.view;

import android.app.Activity;
import android.hardware.Camera;

/**
 * Created by xkazer on 2017/11/22.
 */
public interface CameraView {
    Activity getActivity();

    void onCaptureFrameData(byte[] data, int length, int width, int heigth, int angle);
}
