package com.tencent.camerademo;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.widget.UVCCameraTextureView;
import com.tencent.camerademo.presenter.SDKHelper;
import com.tencent.camerademo.presenter.UVCCameraHelper;
import com.tencent.camerademo.presenter.view.CameraView;
import com.tencent.camerademo.presenter.view.SDKView;
import com.tencent.ilivesdk.view.AVRootView;


/**
 * Created by xkazer on 2017/11/22.
 */
public class UVCActivity extends BaseActivity implements CameraView, SDKView{
    private SDKHelper sdkHelper;
    private UVCCameraHelper cameraHelper;
    private UVCCameraTextureView svPreviews[];
    private AVRootView avRootView;
    private EditText etAcount, etPwd, etRoomId;
    private TextView tvLog;

    private String strMsg ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvc);

        cameraHelper = new UVCCameraHelper(this, this);
        sdkHelper = new SDKHelper(this, this);

        tvLog = (TextView)findViewById(R.id.tv_log);
        etAcount = (EditText)findViewById(R.id.et_account);
        etPwd = (EditText)findViewById(R.id.et_pwd);
        etRoomId = (EditText)findViewById(R.id.et_roomid);
        avRootView = (AVRootView)findViewById(R.id.av_root_view);

        etAcount.setText("guest");
        etPwd.setText("12345678");
        etRoomId.setText("2548");

        svPreviews = new UVCCameraTextureView[4];
        svPreviews[0] = (UVCCameraTextureView)findViewById(R.id.sv_camera0);
        svPreviews[1] = (UVCCameraTextureView)findViewById(R.id.sv_camera1);
        svPreviews[2] = (UVCCameraTextureView)findViewById(R.id.sv_camera2);
        svPreviews[3] = (UVCCameraTextureView)findViewById(R.id.sv_camera3);
        for (int i=0; i<4; i++){
            final int cameraId = i;
            cameraHelper.bindViewInterface(i, svPreviews[i]);
            svPreviews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processCameraOnClick(cameraId);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraHelper.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraHelper.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sdkHelper.onDestory();
        cameraHelper.onDestory();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void onCaptureFrameData(byte[] data, int length, int width, int heigth, int angle) {
        if (sdkHelper.isEnter()){
            sdkHelper.fillCameraData(data, length, width, heigth, angle);
        }
    }

    @Override
    public void loginError(String module, int errCode, String strMsg) {
        Toast.makeText(this, "loginError->"+module+"|"+errCode+"|"+strMsg, Toast.LENGTH_SHORT).show();
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
    public void forceOffline(int error, String message) {
        Toast.makeText(this, "loginError->"+error+"|"+strMsg, Toast.LENGTH_SHORT).show();
        etAcount.setEnabled(true);
        etPwd.setEnabled(true);
        findViewById(R.id.btn_login).setEnabled(true);
        etRoomId.setEnabled(true);
        findViewById(R.id.btn_enter).setEnabled(true);
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

    private void addLog(String msg){
        strMsg += msg + "\r\n";
        tvLog.setText(strMsg);
    }

    private void processCameraOnClick(int cameraId){
        cameraHelper.setPushCamera(cameraId);
    }
}
