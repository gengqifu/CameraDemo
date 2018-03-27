package com.tencent.camerademo.presenter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;
import com.tencent.camerademo.R;
import com.tencent.camerademo.data.UVCCameraData;
import com.tencent.camerademo.presenter.view.CameraView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xkazer on 2017/11/23.
 */
public class UVCCameraHelper {
    private final static int CAMERA_WIDTH = 480;
    private final static int CAMERA_HEIGHT = 640;
    private final String TAG = "[DEV]UVCCameraHelper";
    private CameraView cameraView;
    private USBMonitor usbMonitor;
    private int pushIndex = -1;

    private ArrayList<UVCCameraData> uvcCameraDatas = new ArrayList<>();

    private final USBMonitor.OnDeviceConnectListener deviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice usbDevice) {
            Log.v(TAG, "onAttach->" + usbDevice.getDeviceName());
            usbMonitor.requestPermission(usbDevice);
        }

        @Override
        public void onDettach(UsbDevice usbDevice) {
            Log.v(TAG, "onDettach->" + usbDevice.getDeviceName());
        }

        @Override
        public void onConnect(UsbDevice usbDevice, final USBMonitor.UsbControlBlock usbControlBlock, boolean b) {
            Log.v(TAG, "onConnect->" + usbDevice.getDeviceName());
            int index = findUVCCameraIndexByName(usbDevice.getDeviceName());
            if (-1 == index) {
                index = findUVCCameraIndexByName("");
            }
            if (-1 != index) {
                UVCCameraData uvcCameraData = uvcCameraDatas.get(index);
                uvcCameraData.setName(usbDevice.getDeviceName());
                if (null == uvcCameraData.getUsbCtrlBlock()) {
                    uvcCameraData.setUsbCtrlBlock(usbControlBlock);
                    Log.i(TAG, "onConnect->add uvc camera: " + usbDevice.getDeviceName() + "/" + index);
                    Log.v(TAG, "onConnect->" + index + ":" + uvcCameraData.toString());
                    openCamera(index);
                }
            }
        }

        @Override
        public void onDisconnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock) {
            Log.v(TAG, "onDisconnect->" + usbDevice.getDeviceName());
            int index = findUVCCameraIndexByName(usbDevice.getDeviceName());
            if (-1 != index) {
                realseUVCCamera(uvcCameraDatas.get(index));
            }
        }

        @Override
        public void onCancel(UsbDevice usbDevice) {
            Log.v(TAG, "onCancel->" + usbDevice.getDeviceName());
        }
    };

    public UVCCameraHelper(Context context, CameraView view) {
        cameraView = view;
        usbMonitor = new USBMonitor(context, deviceConnectListener);
        final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(cameraView.getContext(), R.xml.device_filter);
        usbMonitor.setDeviceFilter(filters);
    }

    public void onResume() {
        usbMonitor.register();
        List<UsbDevice> devices = usbMonitor.getDeviceList();
        if (null != devices) {
            for (UsbDevice device : devices) {
/*                int index = findUVCCameraIndexByName(device.getDeviceName());
                if (-1 != index){
                    if (null != uvcCameraDatas.get(index).getUsbCtrlBlock()) {
                        continue;
                    }
                }*/
                //usbMonitor.requestPermission(device);
            }
        }
    }

    public void onPause() {
        usbMonitor.unregister();
    }

    public int getCameraNumber() {
        return usbMonitor.getDeviceCount();
    }

    public boolean openCamera(final int index) {
        UVCCameraData uvcCameraData = uvcCameraDatas.get(index);
        try {
            uvcCameraData.getUvcCamera().open(uvcCameraData.getUsbCtrlBlock());
            uvcCameraData.getUvcCamera().setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH,
                    UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                    UVCCamera.PIXEL_FORMAT_RAW,
                    1.0f);

            uvcCameraData.getUvcCamera().setPreviewDisplay(uvcCameraData.getSurface());
            //uvcCameraData.getUvcCamera().setFrameCallback(this, UVCCamera.PIXEL_FORMAT_NV21);
            uvcCameraData.getUvcCamera().setFrameCallback(new IFrameCallback() {
                @Override
                public void onFrame(ByteBuffer byteBuffer) {
                    /*int len = byteBuffer.capacity();
                    byte[] yuv = new byte[len];
                    byte[] newYuv = new byte[yuv.length];
                    System.arraycopy(yuv, 0, newYuv, 0, yuv.length);
                    ByteBuffer newFrame = ByteBuffer.wrap(yuv);
                    cameraView.onCaptureFrame(byteBuffer);*/
                    processFrameData(index, byteBuffer);
                }
            }, UVCCamera.PIXEL_FORMAT_NV21);
            uvcCameraData.getUvcCamera().startPreview();
            uvcCameraData.setEnable(true);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "openCamera->failed: " + e.toString());
            return false;
        }
    }

    public void bindViewInterface(final int index, UVCCameraTextureView uvcCameraTextureView) {
        while (uvcCameraDatas.size() - 1 < index) {
            uvcCameraDatas.add(new UVCCameraData());
        }
        uvcCameraTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                UVCCameraData uvcCameraData = uvcCameraDatas.get(index);
                uvcCameraData.setSurface(new Surface(surfaceTexture));
                //openCamera(index);
                Log.v(TAG, "onSurfaceTextureAvailable->" + index + ":" + uvcCameraData.toString());
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        uvcCameraTextureView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH * 1.0f / UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        uvcCameraDatas.get(index).setCameraViewInterface(uvcCameraTextureView);
    }

    public void setPushCamera(int cameraId) {
        pushIndex = cameraId;
    }

    public void onDestory() {
        for (int i = 0; i < uvcCameraDatas.size(); i++) {
            UVCCameraData uvcCameraData = uvcCameraDatas.get(i);
            if (uvcCameraData.isEnable()) {
                realseUVCCamera(uvcCameraData);
            }
        }
    }

    private int findUVCCameraIndexByName(String name) {
        for (int i = 0; i < uvcCameraDatas.size(); i++) {
            if (uvcCameraDatas.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private void realseUVCCamera(UVCCameraData uvcCameraData) {
        if (uvcCameraData.isEnable()) {
            uvcCameraData.getUvcCamera().setFrameCallback(null, UVCCamera.PIXEL_FORMAT_NV21);
            uvcCameraData.getUvcCamera().close();
            uvcCameraData.setEnable(false);
        }
        if (null != uvcCameraData.getSurface()) {
            uvcCameraData.getSurface().release();
            uvcCameraData.setSurface(null);
        }
        uvcCameraData.setUsbCtrlBlock(null);
    }

    private void swapNV21toI420(byte[] nv21bytes, byte[] i420bytes, int width, int height) {
        int nLenY = width * height;
        int nLenU = nLenY / 4;

        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            i420bytes[nLenY + i] = nv21bytes[nLenY + 2 * i];
            i420bytes[nLenY + nLenU + i] = nv21bytes[nLenY + 2 * i + 1];
        }
    }

    private void processFrameData(int index, ByteBuffer frame) {
        if (index == pushIndex) {
            byte[] data = new byte[frame.capacity()];
            byte[] i420Data = new byte[frame.capacity()];
            frame.get(data, 0, data.length);
            cameraView.onCaptureRawFrameData(data);
            swapNV21toI420(data, i420Data, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
            cameraView.onCaptureFrameData(i420Data, i420Data.length,
                    UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, 0);
        }
    }
}
