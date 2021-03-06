package com.tencent.camerademo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;
import com.tencent.av.sdk.AVAudioCtrl;
import com.tencent.av.sdk.AVError;
import com.tencent.camerademo.encoder.CameraThread;
import com.tencent.camerademo.encoder.FileUtils;
import com.tencent.camerademo.encoder.RecordParams;
import com.tencent.camerademo.presenter.SDKHelper;
import com.tencent.camerademo.presenter.UVCCameraHelper;
import com.tencent.camerademo.presenter.view.CameraView;
import com.tencent.camerademo.presenter.view.SDKView;
import com.tencent.camerademo.util.Constants;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.view.AVRootView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.tencent.camerademo.encoder.FileUtils.ROOT_PATH;


/**
 * Created by xkazer on 2017/11/22.
 */
public class UVCActivity extends BaseActivity implements CameraView, SDKView{
    private static final String TAG = "UVCActivity";

    private SDKHelper sdkHelper;
    private UVCCameraHelper cameraHelper;
    private UVCCameraTextureView svPreviews[];
    private AVRootView avRootView;
    private EditText etAcount, etPwd, etRoomId;
    private TextView tvLog;

    private String strMsg ="";
    private CameraThread cameraThread;

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

        etAcount.setText("hunter81");
        etPwd.setText("12345678");
        etRoomId.setText("123456");

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

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        cameraThread.mHandler.sendEmptyMessage(CameraThread.MSG_CAPTURE_STOP);
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
        /*Message msg = Message.obtain();
        msg.what = CameraThread.MSG_CAPTURE_FRAME;
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.VIDEO_BYTE_ARRAY, data);
        msg.setData(bundle);
        cameraThread.mHandler.sendMessage(msg);*/
    }

    @Override
    public void onCaptureRawFrameData(byte[] data) {
        /*Message msg = Message.obtain();
        msg.what = CameraThread.MSG_CAPTURE_FRAME;
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.VIDEO_BYTE_ARRAY, data);
        msg.setData(bundle);
        cameraThread.mHandler.sendMessage(msg);*/
        cameraThread.handleCaptureFrme(data);
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

    public void onStopRecord(View view) {
        cameraThread.mHandler.sendEmptyMessage(CameraThread.MSG_CAPTURE_STOP);
    }

    public void onPlayAudio(View view) {
        String audioPath = ROOT_PATH  + "audio.pcm";
        File file = new File(audioPath);
        if(file == null){
            return;
        }
        //读取文件
        int musicLength = (int) (file.length() / 2);
        short[] music = new short[musicLength];
        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readShort();
                i++;
            }
            dis.close();
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    musicLength * 2,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(music, 0, musicLength);
            audioTrack.stop();
        } catch (Throwable t) {
            Log.e(TAG, "播放失败");
        }
    }

    private void addLog(String msg){
        strMsg += msg + "\r\n";
        tvLog.setText(strMsg);
    }

    private void processCameraOnClick(int cameraId){
        cameraHelper.setPushCamera(cameraId);

        Message msg = Message.obtain();
        msg.what = CameraThread.MSG_CAPTURE_START;
        String videoPath = ROOT_PATH+System.currentTimeMillis();
        FileUtils.createfile(ROOT_PATH+"test666.h264");
        RecordParams params = new RecordParams();
        params.setRecordPath(videoPath);
        params.setRecordDuration(0);    // 设置为0，不分割保存
        params.setVoiceClose(false);    // 不屏蔽声音
        msg.obj = params;
        cameraThread.mHandler.sendMessage(msg);
        ((AVAudioCtrl)ILiveSDK.getInstance().getAudioEngine().getAudioObj()).registAudioDataCallbackWithByteBuffer(
                AVAudioCtrl.AudioDataSourceType.AUDIO_DATA_SOURCE_MIC, mAudioDataCompleteCallbackWithByffer);

        /*String audioPath = ROOT_PATH  + "audio.pcm";
        File file = new File(audioPath);
        if (file.exists()) {
            file.delete();
            Log.e(TAG, "audio123 删除文件");
        }
        try {
            file.createNewFile();
            Log.e(TAG,"audio123 创建文件");
        } catch (IOException e) {
            Log.e(TAG,"audio123 未能创建");
            throw new IllegalStateException("audio 未能创建" + file.toString());
        }*/
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
                            /*int len = audioFrameWithByteBuffer.data.capacity();
                            byte[] audio = new byte[len];
                            Log.e(TAG, "data.remaining " + audioFrameWithByteBuffer.data.remaining());
                            if(audioFrameWithByteBuffer.data.remaining() >= len) {
                                audioFrameWithByteBuffer.data.get(audio);
                                audioFrameWithByteBuffer.data.clear();
                                //fillAudioData(audio, audio.length, audioFrameWithByteBuffer.sampleRate, audioFrameWithByteBuffer.channelNum, audioFrameWithByteBuffer.bits);
                                Message msg = Message.obtain();
                                Bundle bundle = new Bundle();
                                bundle.putByteArray("ad", audio);
                                msg.what = CameraThread.MSG_AUDIO_DATA;
                                msg.setData(bundle);
                                cameraThread.mHandler.sendMessage(msg);
                            }*/
                            //Log.e(TAG, "timestamp " + audioFrameWithByteBuffer.timeStamp);
                            /*Message msg = Message.obtain();
                            msg.what = CameraThread.MSG_AUDIO_DATA;
                            msg.obj = audioFrameWithByteBuffer;
                            cameraThread.mHandler.sendMessage(msg);*/
                            /*int len = audioFrameWithByteBuffer.data.capacity();
                            byte[] audio = new byte[len];
                            audioFrameWithByteBuffer.data.get(audio);
                            audioFrameWithByteBuffer.data.clear();*/
                            /*ByteBuffer newBuffer = ByteBuffer.allocate(audioFrameWithByteBuffer.data.capacity());
                            newBuffer = deepCopy(audioFrameWithByteBuffer.data, newBuffer);*/
                            Log.e(TAG, "thread " + Thread.currentThread().getName());
                            int len = audioFrameWithByteBuffer.data.capacity();
                            byte[] audio = new byte[len];
                            audioFrameWithByteBuffer.data.get(audio);
                            audioFrameWithByteBuffer.data.clear();
                            byte[] newAudio = new byte[len];
                            System.arraycopy(audio, 0, newAudio, 0, len);
                            cameraThread.handleAudioData(newAudio);
                        }
                    }
                    return AVError.AV_OK;
                }
            };

    public ByteBuffer deepCopy(ByteBuffer source, ByteBuffer target) {

        int sourceP = source.position();
        int sourceL = source.limit();

        if (null == target) {
            target = ByteBuffer.allocate(source.remaining());
        }
        target.put(source);
        target.flip();

        source.position(sourceP);
        source.limit(sourceL);
        return target;
    }

    private void fillAudioData(byte[] data, int len, int sampleRate, int channels, int bits) {
        ((AVAudioCtrl) ILiveSDK.getInstance().getAudioEngine().getAudioObj()).fillExternalAudioFrame(data,
                len, sampleRate, channels, bits);
    }
}
