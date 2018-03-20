package com.tencent.camerademo.presenter;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.tencent.camerademo.data.CameraData;
import com.tencent.camerademo.presenter.view.CameraView;


/**
 * Created by xkazer on 2017/11/22.
 */
public class CameraHelper {
    private final static int CAMERA_WIDTH = 640;
    private final static int CAMERA_HEIGHT = 480;
    private final String TAG = "[DEV]CameraHelper";
    private CameraView cameraView;
    private CameraData[] cameraDatas;
    private int pushIndex = -1;

    public CameraHelper(Context context, CameraView view){
        cameraView = view;
        cameraDatas = new CameraData[getCameraNumber()];
        for (int i=0; i<getCameraNumber(); i++) {
            cameraDatas[i] = new CameraData();
        }
        Log.i(TAG, "CameraHelper->number: "+Camera.getNumberOfCameras());
    }

    public int getCameraNumber(){
        return Camera.getNumberOfCameras();
    }

    public void bindSurfaceView(int cameraId, SurfaceHolder surfaceHolder){
        cameraDatas[cameraId].setSurfaceHolder(surfaceHolder);
    }


    public boolean openCamera(final int cameraId){
        Log.i(TAG, "openCamera->id: "+cameraId);
        try {
            cameraDatas[cameraId].setCamera(Camera.open(cameraId));
            setCameraDisplayOrientation(cameraId, cameraDatas[cameraId].getCamera());
            Camera.Parameters parameters = cameraDatas[cameraId].getCamera().getParameters();
            parameters.setPictureSize(CAMERA_WIDTH, CAMERA_HEIGHT);
            parameters.setPreviewFormat(ImageFormat.NV21);
            cameraDatas[cameraId].getCamera().setParameters(parameters);
            cameraDatas[cameraId].getCamera().setPreviewDisplay(cameraDatas[cameraId].getSurfaceHolder());
            cameraDatas[cameraId].getCamera().setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    processCameraFrame(cameraId, data);
                }
            });
            cameraDatas[cameraId].getCamera().startPreview();
            cameraDatas[cameraId].setbEnable(true);
            return true;
        }catch (Exception e){
            Log.e(TAG, "openCamera->"+e.toString(), e);
            return false;
        }
    }

    public void onDestory(){
        for (int i=0; i<cameraDatas.length; i++){
            if (cameraDatas[i].isbEnable()){
                closeCamera(i);
            }
        }
    }

    public void closeCamera(int cameraId){
        Log.i(TAG, "closeCamera->id: "+cameraId);
        if (cameraDatas[cameraId].isbEnable()){
            cameraDatas[cameraId].getCamera().setPreviewCallback(null);
            cameraDatas[cameraId].getCamera().stopPreview();
            cameraDatas[cameraId].getCamera().release();
            cameraDatas[cameraId].setbEnable(false);
        }
    }

    public boolean switchCamera(int cameraId){
        for (int i=0; i< cameraDatas.length; i++){
            if (cameraDatas[i].isbEnable()){
                closeCamera(i);
            }
        }
        return openCamera(cameraId);
    }

    public void setPushCamera(int cameraId){
        pushIndex = cameraId;
    }

    private void setCameraDisplayOrientation (int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo (cameraId , info);
        int rotation = ((Activity)cameraView.getContext()).getWindowManager().getDefaultDisplay ().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
        } else {
            // back-facing
            result = ( info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation (result);
    }

    private void swapNV21toI420(byte[] nv21bytes, byte[] i420bytes, int width,int height){
        int nLenY = width * height;
        int nLenU = nLenY / 4;

        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            i420bytes[nLenY + i] = nv21bytes[nLenY + 2 * i + 1];
            i420bytes[nLenY + nLenU + i] = nv21bytes[nLenY + 2 * i];
        }
    }

    private void processCameraFrame(int cameraId, byte[] data){
        if (cameraId == pushIndex) {
            byte[] i420Data = new byte[data.length];
            Camera.Parameters parameters = cameraDatas[cameraId].getCamera().getParameters();
            swapNV21toI420(data, i420Data, parameters.getPreviewSize().width, parameters.getPreviewSize().height);
            cameraView.onCaptureFrameData(i420Data, i420Data.length,
                    parameters.getPreviewSize().width, parameters.getPreviewSize().height, 0);
        }
    }
}
