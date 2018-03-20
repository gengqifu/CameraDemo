package com.tencent.camerademo.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;
import com.tencent.av.sdk.AVAudioCtrl;
import com.tencent.av.sdk.AVError;
import com.tencent.camerademo.R;
import com.tencent.camerademo.encoder.CameraThread;
import com.tencent.camerademo.encoder.FileUtils;
import com.tencent.camerademo.encoder.RecordParams;
import com.tencent.camerademo.presenter.SDKHelper;
import com.tencent.camerademo.presenter.view.CameraView;
import com.tencent.camerademo.presenter.view.SDKView;
import com.tencent.camerademo.util.Constants;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.view.AVRootView;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.tencent.camerademo.util.Constants.ROOT_PATH;

/**
 * Created by lqy on 2018/3/15.
 */

public class UVCService extends Service implements CameraViewForService, SDKView, View.OnClickListener, CameraView {

    public static final String TAG = UVCService.class.getName();

    private static final int UVC_SERVICE_ID = 999;

    private static final int MIN_WINDOW_SIZE = 1;

    private SDKHelper mSDKHelper;
    private UVCCameraHelperForService mUVCCameraHelper;

    private int requestCode = 1;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) rootView.getLayoutParams();
            layoutParams.width = mWidth;
            layoutParams.height = mHeight;
            mWindowManager.updateViewLayout(rootView, layoutParams);
        }
    };

    private void initialNotification() {
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        Intent intent = new Intent();
        intent.setAction(TAG);
        builder.setContentIntent(PendingIntent.getBroadcast(getApplicationContext(), requestCode++, intent, FLAG_UPDATE_CURRENT))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("直播中")
                .setContentTitle("后台直播");

        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }
        startForeground(UVC_SERVICE_ID, notification);
    }

    WindowManager mWindowManager;
    private View rootView;
    private UVCCameraTextureView svPreviews[];
    private AVRootView avRootView;
    private EditText etAcount, etPwd, etRoomId;
    private TextView tvLog;
    private Button loginBtn, createBtn, photoBtn, minBtn, exitBtn, stopBtn;

    private int mWidth, mHeight;
    private CameraThread cameraThread;


    private void initialWindow() {
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        assert mWindowManager != null;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mWidth = displayMetrics.widthPixels;
        mHeight = displayMetrics.heightPixels;

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                mWidth, mHeight,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSPARENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        rootView = LayoutInflater.from(this).inflate(R.layout.window_uvc_service, null);

        tvLog = (TextView) rootView.findViewById(R.id.uvc_service_window_tv_log);
        etAcount = (EditText) rootView.findViewById(R.id.uvc_service_window_et_account);
        etPwd = (EditText) rootView.findViewById(R.id.uvc_service_window_et_pwd);
        etRoomId = (EditText) rootView.findViewById(R.id.uvc_service_window_et_roomid);
        avRootView = (AVRootView) rootView.findViewById(R.id.uvc_service_window_avrv);
        loginBtn = (Button) rootView.findViewById(R.id.uvc_service_window_btn_login);
        createBtn = (Button) rootView.findViewById(R.id.uvc_service_window_btn_enter);
        photoBtn = (Button) rootView.findViewById(R.id.uvc_service_window_btn_photo);
        minBtn = (Button) rootView.findViewById(R.id.uvc_service_window_btn_min);
        exitBtn = (Button) rootView.findViewById(R.id.uvc_service_window_btn_exit);
        stopBtn = (Button) rootView.findViewById(R.id.uvc_service_window_btn_stop);

        loginBtn.setOnClickListener(this);
        createBtn.setOnClickListener(this);
        photoBtn.setOnClickListener(this);
        minBtn.setOnClickListener(this);
        exitBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);

        etAcount.setText("hunter81");
        etPwd.setText("12345678");
        etRoomId.setText("123456");

        svPreviews = new UVCCameraTextureView[4];
        svPreviews[0] = (UVCCameraTextureView) rootView.findViewById(R.id.uvc_service_window_sv_camera0);
        svPreviews[1] = (UVCCameraTextureView) rootView.findViewById(R.id.uvc_service_window_sv_camera1);
        svPreviews[2] = (UVCCameraTextureView) rootView.findViewById(R.id.uvc_service_window_sv_camera2);
        svPreviews[3] = (UVCCameraTextureView) rootView.findViewById(R.id.uvc_service_window_sv_camera3);
        for (int i = 0; i < 4; i++) {
            final int cameraId = i;
            mUVCCameraHelper.bindViewInterface(i, svPreviews[i]);
            svPreviews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processCameraOnClick(cameraId);
                }
            });
        }

        mWindowManager.addView(rootView, layoutParams);

        cameraThread = new CameraThread(CameraThread.CameraHandler.class, this, 2, 640, 480, 0, UVCCamera.DEFAULT_BANDWIDTH,
                new CameraThread.OnEncodeResultListener() {

                    @Override
                    public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                        // type = 0,aac格式音频流
                        // type = 1,h264格式视频流
                        if(type == 1){
                            FileUtils.putFileStream(data,offset,length);
                        }
                    }

                    @Override
                    public void onRecordResult(String videoPath) {
                        showShortMsg(videoPath);
                    }
                });
        cameraThread.start();
    }

    private void processCameraOnClick(int cameraId) {
        mUVCCameraHelper.setPushCamera(cameraId);
        Message msg = Message.obtain();
        msg.what = CameraThread.MSG_CAPTURE_START;
        String videoPath = ROOT_PATH + System.currentTimeMillis();
        FileUtils.createfile(FileUtils.ROOT_PATH+"test666.h264");
        RecordParams params = new RecordParams();
        params.setRecordPath(videoPath);
        params.setRecordDuration(0);    // 设置为0，不分割保存
        params.setVoiceClose(false);    // 不屏蔽声音
        msg.obj = params;
        cameraThread.mHandler.sendMessage(msg);
        ((AVAudioCtrl) ILiveSDK.getInstance().getAudioEngine().getAudioObj()).registAudioDataCallbackWithByteBuffer(
                AVAudioCtrl.AudioDataSourceType.AUDIO_DATA_SOURCE_MIC, mAudioDataCompleteCallbackWithByffer);
    }

    private AVAudioCtrl.RegistAudioDataCompleteCallbackWithByteBuffer mAudioDataCompleteCallbackWithByffer =
            new AVAudioCtrl.RegistAudioDataCompleteCallbackWithByteBuffer() {
                @Override
                public int onComplete(AVAudioCtrl.AudioFrameWithByteBuffer audioFrameWithByteBuffer, int srcType) {
                    if (srcType== AVAudioCtrl.AudioDataSourceType.AUDIO_DATA_SOURCE_MIC) {
                        synchronized (this) {
                            /*************************************************
                             将要混入的音频数据写入audioFrameWithByteBuffer中
                             *************************************************/
                            Log.e(TAG, "timestamp " + audioFrameWithByteBuffer.timeStamp);
                            Message msg = Message.obtain();
                            msg.what = CameraThread.MSG_AUDIO_DATA;
                            msg.obj = audioFrameWithByteBuffer;
                            cameraThread.mHandler.sendMessage(msg);
                        }
                    }
                    return AVError.AV_OK;
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();

        mUVCCameraHelper = new UVCCameraHelperForService(this, this);
        mSDKHelper = new SDKHelper(this, this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(TAG);
        registerReceiver(mIntentReceiver, filter);

        initialWindow();
        initialNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mUVCCameraHelper.onResume();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraThread.mHandler.sendEmptyMessage(CameraThread.MSG_CAPTURE_STOP);
        stopForeground(true);
        unregisterReceiver(mIntentReceiver);
        mUVCCameraHelper.onPause();
        mSDKHelper.onDestory();
        mUVCCameraHelper.onDestory();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        switch (level) {
            case TRIM_MEMORY_UI_HIDDEN: {
                Log.e("lqy", "TRIM_MEMORY_UI_HIDDEN");
            }
            break;
            case TRIM_MEMORY_RUNNING_CRITICAL: {
                Log.e("lqy", "TRIM_MEMORY_RUNNING_CRITICAL");
            }
            break;
            case TRIM_MEMORY_RUNNING_LOW: {
                Log.e("lqy", "TRIM_MEMORY_RUNNING_LOW");
            }
            break;
            case TRIM_MEMORY_RUNNING_MODERATE: {
                Log.e("lqy", "TRIM_MEMORY_RUNNING_MODERATE");
            }
            break;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new RuntimeException();
    }

    @Override
    public void loginError(String module, int errCode, String strMsg) {
        Toast.makeText(this, "loginError->" + module + "|" + errCode + "|" + strMsg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void loginSuccess() {
        etAcount.setEnabled(false);
        etPwd.setEnabled(false);
        loginBtn.setEnabled(false);
        mSDKHelper.initAVRootView(avRootView);
    }

    @Override
    public void enterError(String module, int errCode, String strMsg) {
        Toast.makeText(this, "enterError->" + module + "|" + errCode + "|" + strMsg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void enterSuccess() {
        etRoomId.setEnabled(false);
        createBtn.setEnabled(false);
    }

    @Override
    public void forceOffline(int error, String message) {
        Toast.makeText(this, "loginError->" + error + "|" + message, Toast.LENGTH_SHORT).show();
        etAcount.setEnabled(true);
        etPwd.setEnabled(true);
        loginBtn.setEnabled(true);
        etRoomId.setEnabled(true);
        createBtn.setEnabled(true);
    }

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCaptureFrameData(byte[] data, int length, int width, int heigth, int angle) {
        if (mSDKHelper.isEnter()) {
            mSDKHelper.fillCameraData(data, length, width, heigth, angle);
        }
        Message msg = Message.obtain();
        msg.what = CameraThread.MSG_CAPTURE_FRAME;
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.VIDEO_BYTE_ARRAY, data);
        msg.setData(bundle);
        cameraThread.mHandler.sendMessage(msg);
    }

    @Override
    public void postDelay(Runnable runnable, long time) {
        if (rootView != null) {
            rootView.postDelayed(runnable, time);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.uvc_service_window_btn_login: {
                mSDKHelper.login(etAcount.getText().toString(), etPwd.getText().toString());
            }
            break;
            case R.id.uvc_service_window_btn_enter: {
                mSDKHelper.enterRoom(Integer.valueOf(etRoomId.getText().toString()));
            }
            break;
            case R.id.uvc_service_window_btn_photo: {
                mUVCCameraHelper.takePhoto(photoBtn);
            }
            break;
            case R.id.uvc_service_window_btn_min: {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) rootView.getLayoutParams();
                layoutParams.width = MIN_WINDOW_SIZE;
                layoutParams.height = MIN_WINDOW_SIZE;
                mWindowManager.updateViewLayout(rootView, layoutParams);
            }
            break;
            case R.id.uvc_service_window_btn_exit: {
                mWindowManager.removeView(rootView);
                stopSelf();
            }
            break;
            case R.id.uvc_service_window_btn_stop: {
                stopRecord();
            }
            break;
            default:
                break;
        }
    }

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void stopRecord() {
        cameraThread.mHandler.sendEmptyMessage(CameraThread.MSG_CAPTURE_STOP);
    }
}
