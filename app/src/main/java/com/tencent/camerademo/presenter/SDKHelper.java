package com.tencent.camerademo.presenter;

import android.content.Context;
import android.util.Log;

import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.camerademo.presenter.view.SDKView;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.adapter.CommonConstants;
import com.tencent.ilivesdk.core.ILiveLog;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomConfig;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.core.ILiveRoomOption;
import com.tencent.ilivesdk.view.AVRootView;

/**
 * Created by xkazer on 2017/11/22.
 */
public class SDKHelper implements ILiveLoginManager.TILVBStatusListener {
    private final static String TAG = "[DEV]SDKHelper";
    private SDKView sdkView;
    private Context context;
    private boolean isLogin = false;
    private boolean isEnter = false;

    public SDKHelper(SDKView view, Context ctx){
        context = ctx;
        sdkView = view;
    }

    @Override
    public void onForceOffline(int error, String message) {
        sdkView.forceOffline(error, message);
    }

    public void initAVRootView(AVRootView avRootView){
        ILiveRoomManager.getInstance().initAvRootView(avRootView);
    }

    public void onDestory(){
        ILiveRoomManager.getInstance().onDestory();
    }

    public void login(String account, String password){
        ILiveSDK.getInstance().initSdk(context, 1400028096, 11851);
        ILiveRoomManager.getInstance().init(new ILiveRoomConfig());
        ILiveLoginManager.getInstance().setUserStatusListener(this);
        ILiveLoginManager.getInstance().tlsLoginAll(account, password, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                isLogin = true;
                sdkView.loginSuccess();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                sdkView.loginError(module, errCode, errMsg);
            }
        });
    }

    public void enterRoom(int roomid){
        ILiveRoomOption option = new ILiveRoomOption("")
                .imsupport(false)
                .autoCamera(false)
                .autoMic(false);
        ILiveRoomManager.getInstance().joinRoom(roomid, option, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                isEnter = true;
                sdkView.enterSuccess();
                ((AVVideoCtrl)ILiveSDK.getInstance().getVideoEngine().getVideoObj()).enableExternalCapture(true,
                        true, new AVVideoCtrl.EnableExternalCaptureCompleteCallback(){
                            @Override
                            protected void onComplete(boolean enable, int result) {
                                super.onComplete(enable, result);
                                Log.v(TAG, "enableExternalCapture->result: "+enable+", "+result);
                            }
                        });
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                sdkView.enterError(module, errCode, errMsg);
            }
        });
    }

    public boolean isLogin() {
        return isLogin;
    }

    public boolean isEnter() {
        return isEnter;
    }

    public void fillCameraData(byte[] data, int length, int width, int height, int angle){
        //Log.d(TAG, "fillCameraData->"+length+", "+width+"*"+height+", "+angle);
        ((AVVideoCtrl)ILiveSDK.getInstance().getVideoEngine().getVideoObj()).fillExternalCaptureFrame(data,
                length, 0, width, height, angle, AVVideoCtrl.EXTERNAL_FORMAT_I420, CommonConstants.Const_VideoType_Camera);
    }
}
