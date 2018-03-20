package com.tencent.camerademo.service;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by lqy on 2018/3/15.
 */

public class ServiceStartActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, UVCService.class));
//        startService(new Intent(this, UVCService.InnerService.class));
        finish();
    }
}
