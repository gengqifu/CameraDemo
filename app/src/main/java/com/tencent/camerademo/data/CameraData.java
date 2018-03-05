package com.tencent.camerademo.data;

import android.hardware.Camera;
import android.view.SurfaceHolder;

/**
 * Created by xkazer on 2017/11/23.
 */
public class CameraData {
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private boolean bEnable = false;

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public boolean isbEnable() {
        return bEnable;
    }

    public void setbEnable(boolean bEnable) {
        this.bEnable = bEnable;
    }
}
