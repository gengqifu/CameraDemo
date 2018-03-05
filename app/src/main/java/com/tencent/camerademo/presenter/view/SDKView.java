package com.tencent.camerademo.presenter.view;

/**
 * Created by xkazer on 2017/11/22.
 */
public interface SDKView {
    void loginError(String module, int errCode, String strMsg);
    void loginSuccess();

    void enterError(String module, int errCode, String strMsg);
    void enterSuccess();

    void forceOffline(int error, String message);
}
