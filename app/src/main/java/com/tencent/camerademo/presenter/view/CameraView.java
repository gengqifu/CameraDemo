package com.tencent.camerademo.presenter.view;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;

import java.nio.ByteBuffer;

/**
 * Created by xkazer on 2017/11/22.
 */
public interface CameraView {
    //Activity getActivity();
    Context getContext();

    void onCaptureFrameData(byte[] data, int length, int width, int heigth, int angle);
    void onCaptureRawFrameData(byte[] data);
}
