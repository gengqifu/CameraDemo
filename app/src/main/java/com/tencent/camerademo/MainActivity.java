package com.tencent.camerademo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.camerademo.presenter.CameraHelper;
import com.tencent.camerademo.presenter.SDKHelper;
import com.tencent.camerademo.presenter.view.CameraView;
import com.tencent.camerademo.presenter.view.SDKView;
import com.tencent.ilivesdk.view.AVRootView;

import java.nio.ByteBuffer;


/**
 * Created by xkazer on 2017/11/22.
 */
public class MainActivity extends Activity implements CameraView, SDKView{

    private SDKHelper sdkHelper;
    private CameraHelper cameraHelper;
    private SurfaceView svPreviews[];
    private AVRootView avRootView;
    private EditText etAcount, etPwd, etRoomId;
    private TextView tvLog;

    private String strMsg ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraHelper = new CameraHelper(this, this);
        sdkHelper = new SDKHelper(this, this);

        tvLog = (TextView)findViewById(R.id.tv_log);
        etAcount = (EditText)findViewById(R.id.et_account);
        etPwd = (EditText)findViewById(R.id.et_pwd);
        etRoomId = (EditText)findViewById(R.id.et_roomid);
        avRootView = (AVRootView)findViewById(R.id.av_root_view);
        avRootView.setLocalFullScreen(false);

        etAcount.setText("guest");
        etPwd.setText("12345678");
        etRoomId.setText("2548");

        svPreviews = new SurfaceView[4];
        svPreviews[0] = (SurfaceView)findViewById(R.id.sv_camera0);
        svPreviews[1] = (SurfaceView)findViewById(R.id.sv_camera1);
        svPreviews[2] = (SurfaceView)findViewById(R.id.sv_camera2);
        svPreviews[3] = (SurfaceView)findViewById(R.id.sv_camera3);
        int number = cameraHelper.getCameraNumber()>4 ? 4 : cameraHelper.getCameraNumber();
        for (int i=0; i<number; i++){
            final int cameraId = i;
            SurfaceHolder surfaceHolder = svPreviews[i].getHolder();
            svPreviews[cameraId].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processCameraOnClick(cameraId);
                }
            });
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    long curMills = System.currentTimeMillis();
                    cameraHelper.bindSurfaceView(cameraId, holder);
                    if (cameraHelper.openCamera(cameraId)){
                        addLog("openCamera->"+cameraId+"...OK...cost:"+(System.currentTimeMillis()-curMills)+"ms");
                    }else{
                        addLog("openCamera->"+cameraId+"...FAIL...cost:"+(System.currentTimeMillis()-curMills)+"ms");
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    cameraHelper.closeCamera(cameraId);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sdkHelper.onDestory();
        cameraHelper.onDestory();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCaptureFrameData(byte[] data, int length, int width, int heigth, int angle) {
        if (sdkHelper.isEnter()){
            sdkHelper.fillCameraData(data, length, width, heigth, angle);
        }
    }

    @Override
    public void onCaptureRawFrameData(byte[] data) {

    }

    @Override
    public void loginError(String module, int errCode, String strMsg) {
        Toast.makeText(this, "loginError->"+module+"|"+errCode+"|"+strMsg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void forceOffline(int error, String message) {
        Toast.makeText(this, "loginError->"+error+"|"+strMsg, Toast.LENGTH_SHORT).show();
        etAcount.setEnabled(true);
        etPwd.setEnabled(true);
        findViewById(R.id.btn_login).setEnabled(true);
        etRoomId.setEnabled(true);
        findViewById(R.id.btn_enter).setEnabled(true);
    }

    @Override
    public void loginSuccess() {
        etAcount.setEnabled(false);
        etPwd.setEnabled(false);
        findViewById(R.id.btn_login).setEnabled(false);
        sdkHelper.initAVRootView(avRootView);
    }

    @Override
    public void enterError(String module, int errCode, String strMsg) {
        Toast.makeText(this, "enterError->"+module+"|"+errCode+"|"+strMsg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void enterSuccess() {
        etRoomId.setEnabled(false);
        findViewById(R.id.btn_enter).setEnabled(false);
    }

    public void onLogin(View view){
        sdkHelper.login(etAcount.getText().toString(), etPwd.getText().toString());
    }

    public void onEnter(View view){
        sdkHelper.enterRoom(Integer.valueOf(etRoomId.getText().toString()));
    }

    public void onUVC(View view){
        finish();
        startActivity(new Intent(this, UVCActivity.class));
    }

    private void addLog(String msg){
        strMsg += msg + "\r\n";
        tvLog.setText(strMsg);
    }

    private void processCameraOnClick(int cameraId){
        if (!sdkHelper.isEnter()){
            long curMills = System.currentTimeMillis();
            if (cameraHelper.switchCamera(cameraId)){
                addLog("switchCamera->"+cameraId+"...OK...cost:"+(System.currentTimeMillis()-curMills)+"ms");
            }else{
                addLog("switchCamera->"+cameraId+"...FAIL...cost:"+(System.currentTimeMillis()-curMills)+"ms");
            }
        }else{
            cameraHelper.setPushCamera(cameraId);
        }
    }
}
