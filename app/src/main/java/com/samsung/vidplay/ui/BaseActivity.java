package com.samsung.vidplay.ui;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;

import com.samsung.smesh.client.SmeshProxy;
import com.samsung.vidplay.VidPlayApp;

public class BaseActivity extends AppCompatActivity
{
    protected VidPlayApp getApp() {
        return (VidPlayApp) getApplication();
    }

    public SmeshProxy getSmeshProxy() {
        return getApp().getSmeshProxy();
    }
    public void setSmeshProxy( SmeshProxy smeshProxy ) {
        getApp().setSmeshProxy( smeshProxy );
    }

}
