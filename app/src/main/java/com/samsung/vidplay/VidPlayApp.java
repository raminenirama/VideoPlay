package com.samsung.vidplay;

import android.app.Application;

import com.samsung.mars.util.Log;
import com.samsung.smesh.client.SmeshProxy;


public class VidPlayApp extends Application 
{
    private static final String LOG_PREFIX = "Sattractor";
    private static final String LOGTAG = LOG_PREFIX +".VidPlayApp";
    public static final String APP_NAME = "Sattractor";

    private SmeshProxy smeshProxy;
    private String defaultVideoFilename;
    private String featureVideoFilename;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.initialize( this, LOG_PREFIX );
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public SmeshProxy getSmeshProxy() {
        return smeshProxy;
    }
    public void setSmeshProxy( SmeshProxy smeshProxy ) {
        this.smeshProxy = smeshProxy;
    }

    public String getDefaultVideoFilename() {
        return defaultVideoFilename;
    }
    public void setDefaultVideoFilename( String filename ) {
        defaultVideoFilename = filename;
    }

    public String getFeatureVideoFilename() {
        return featureVideoFilename;
    }
    public void setFeatureVideoFilename( String filename ) {
        featureVideoFilename = filename;
    }
}
