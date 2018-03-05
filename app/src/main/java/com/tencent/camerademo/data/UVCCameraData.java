package com.tencent.camerademo.data;

import android.view.Surface;

import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.CameraViewInterface;

/**
 * Created by xkazer on 2017/11/23.
 */
public class UVCCameraData {
    private String name;
    private UVCCamera uvcCamera;
    private Surface surface;
    private USBMonitor.UsbControlBlock usbCtrlBlock;
    private CameraViewInterface cameraViewInterface;
    private boolean isEnable = false;

    public UVCCameraData() {
        name = "";
        uvcCamera = new UVCCamera();
        usbCtrlBlock = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UVCCamera getUvcCamera() {
        return uvcCamera;
    }

    public void setUvcCamera(UVCCamera uvcCamera) {
        this.uvcCamera = uvcCamera;
    }

    public Surface getSurface() {
        return surface;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
    }

    public USBMonitor.UsbControlBlock getUsbCtrlBlock() {
        return usbCtrlBlock;
    }

    public void setUsbCtrlBlock(USBMonitor.UsbControlBlock usbCtrlBlock) {
        this.usbCtrlBlock = usbCtrlBlock;
    }

    public CameraViewInterface getCameraViewInterface() {
        return cameraViewInterface;
    }

    public void setCameraViewInterface(CameraViewInterface cameraViewInterface) {
        this.cameraViewInterface = cameraViewInterface;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean bEnable) {
        this.isEnable = bEnable;
    }

    @Override
    public String toString() {
        return "UVCCameraData{" +
                "name='" + name + '\'' +
                ", uvcCamera=" + uvcCamera +
                ", surface=" + surface +
                ", usbCtrlBlock=" + usbCtrlBlock +
                ", cameraViewInterface=" + cameraViewInterface +
                ", isEnable=" + isEnable +
                '}';
    }
}
