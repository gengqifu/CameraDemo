package com.tencent.camerademo.service;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;
import com.tencent.camerademo.R;
import com.tencent.camerademo.data.UVCCameraData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lqy on 2018/3/15.
 */

public class UVCCameraHelperForService {
    private final static int CAMERA_WIDTH = 480;
    private final static int CAMERA_HEIGHT = 640;
    private final String TAG = "[DEV]UVCCameraHelper";
    private CameraViewForService cameraView;
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

    public UVCCameraHelperForService(Context context, CameraViewForService view) {
        cameraView = view;
        usbMonitor = new USBMonitor(context, deviceConnectListener);
        final List<DeviceFilter> filters = DeviceFilter.getDeviceFilters(cameraView.getService(), R.xml.device_filter);
        usbMonitor.setDeviceFilter(filters);
    }

    public void onResume() {
        if(isAllSurfaceAvailable == 4) {
            usbMonitor.register();
        } else {
            cameraView.postDelay(new Runnable() {
                @Override
                public void run() {
                    onResume();
                }
            }, 1000);
        }
//        List<UsbDevice> devices = usbMonitor.getDeviceList();
//        if (null != devices) {
//            for (UsbDevice device : devices) {
/*                int index = findUVCCameraIndexByName(device.getDeviceName());
                if (-1 != index){
                    if (null != uvcCameraDatas.get(index).getUsbCtrlBlock()) {
                        continue;
                    }
                }*/
                //usbMonitor.requestPermission(device);
//            }
//        }
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
            List<Size> sizeList = uvcCameraData.getUvcCamera().getSupportedSizeList();
            int width = UVCCamera.DEFAULT_PREVIEW_WIDTH;
            int height = UVCCamera.DEFAULT_PREVIEW_HEIGHT;

            if(sizeList != null && sizeList.size() != 0){
                for(Size size : sizeList){
                    if(size.width * size.height > width * height){
                        width = size.width;
                        height = size.height;
                    }
                }
            }

            Log.e("lqy", "w:" + width + " h:" + height);
            Log.e("lqy", "autoFocus b -> " + uvcCameraData.getUvcCamera().getAutoFocus());

            if(!uvcCameraData.getUvcCamera().getAutoFocus()) {
                uvcCameraData.getUvcCamera().setAutoFocus(true);
            }
            Log.e("lqy", "autoFocus e -> " + uvcCameraData.getUvcCamera().getAutoFocus());

            uvcCameraData.getUvcCamera().open(uvcCameraData.getUsbCtrlBlock());
            uvcCameraData.getUvcCamera().setPreviewSize(width,
                    height,
                    UVCCamera.PIXEL_FORMAT_RAW,
                    1.0f);

            uvcCameraData.getUvcCamera().setPreviewDisplay(uvcCameraData.getSurface());
            //uvcCameraData.getUvcCamera().setFrameCallback(this, UVCCamera.PIXEL_FORMAT_NV21);
            uvcCameraData.getUvcCamera().setFrameCallback(new IFrameCallback() {
                @Override
                public void onFrame(ByteBuffer byteBuffer) {
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

    private volatile int isAllSurfaceAvailable = 0;

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
                isAllSurfaceAvailable++;
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

    private void processFrameData(int index, ByteBuffer frame) {
        if (index == pushIndex) {
            byte[] data = new byte[frame.capacity()];
            byte[] i420Data = new byte[frame.capacity()];
            frame.get(data, 0, data.length);
            cameraView.onCaptureRawFrameData(data);
            swapNV21toI420(data, i420Data, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
            cameraView.onCaptureFrameData(i420Data, i420Data.length,
                    UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, 0);

            // TODO: 2018/3/7 直播时拍照demo
            Log.e("lqy", "---------------------->being alive");

            if (needPhoto) {
                final byte[] nv12Data = new byte[frame.capacity()];
                swapNV21ToNV12(data, nv12Data, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isTaking = true;
                        File file = new File(Environment.getExternalStorageDirectory().toString() + "/" + System.currentTimeMillis() + ".jpg");
                        if (!file.exists()) {
                            FileOutputStream fos = null;
                            try {
                                file.createNewFile();
                                fos = new FileOutputStream(file);
                                YuvImage yuvImage = new YuvImage(nv12Data, ImageFormat.NV21, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, null);
                                yuvImage.compressToJpeg(new Rect(0, 0, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT), 100, fos);

                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (fos != null) {
                                    try {
                                        fos.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        isTaking = false;
                    }
                }).start();
            }
            needPhoto = false;
        }
    }

    private boolean needPhoto = false;
    private volatile boolean isTaking = false;
    private int i = 0;

    public void takePhoto(View view) {
        if (isTaking) {
            Toast.makeText(view.getContext(), "拍照中请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pushIndex == -1) {
            Toast.makeText(view.getContext(), "没有使用中的usb摄像头", Toast.LENGTH_SHORT).show();
            return;
        }
        uvcCameraDatas.get(pushIndex).getUvcCamera().setFocus(i);
        needPhoto = true;
        i = i + 1;
        if(i > 10){
            i = 0;
        }
    }

    private void swapNV21ToNV12(byte[] nv21bytes, byte[] nv12bytes, int width, int height) {
        if (nv21bytes == null || nv12bytes == null) return;
        int frameSize = width * height;
        int j = 0;
        System.arraycopy(nv21bytes, 0, nv12bytes, 0, frameSize);
        for (j = 0; j < frameSize / 2; j += 2) {
            nv12bytes[frameSize + j + 1] = nv21bytes[j + frameSize];
        }

        for (j = 0; j < frameSize / 2; j += 2) {
            nv12bytes[frameSize + j] = nv21bytes[j + frameSize + 1];
        }
    }

    private void swapYV12toNV12(byte[] yv12bytes, byte[] nv12bytes, int width, int height) {
        int nLenY = width * height;
        int nLenU = nLenY / 4;

        System.arraycopy(yv12bytes, 0, nv12bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            nv12bytes[nLenY + 2 * i + 1] = yv12bytes[nLenY + i];
            nv12bytes[nLenY + 2 * i] = yv12bytes[nLenY + nLenU + i];
        }
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

    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) {
        System.arraycopy(yv12bytes, 0, i420bytes, 0, width * height);
        System.arraycopy(yv12bytes, width * height + width * height / 4, i420bytes, width * height, width * height / 4);
        System.arraycopy(yv12bytes, width * height, i420bytes, width * height + width * height / 4, width * height / 4);
    }
}
