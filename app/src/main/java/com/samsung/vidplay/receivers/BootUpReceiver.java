package com.samsung.vidplay.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.samsung.mars.util.Log;


public class BootUpReceiver extends BroadcastReceiver 
{
    private static final String LOGTAG = Log.setLogLevel( "VidPlay.BootUpReceiver", Log.DEBUG );
    
    private Context context;
    
    @Override
    public void onReceive( final Context context, Intent intent ) {
        this.context = context;
        Log.w( LOGTAG, "onReceive: ==============================" );
        
        try {
            dumpBuildInfo();
            if ( "ODROID".equals( Build.BRAND ) ) {
                Intent actIntent = new Intent( "com.samsung.VidPlayApp.ACTION" );
                actIntent.setPackage( "com.samsung.vidplay" );
                actIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                context.startActivity( actIntent );
            }
        }
        catch ( Exception ex ) {
            Log.e( LOGTAG, "onReceive: "+ ex.getLocalizedMessage(), ex );
        }
        Log.w( LOGTAG, "onReceive: ============================== done" );
    }
    
    public static void dumpBuildInfo() {
        Log.i( LOGTAG, "dumpBuildInfo: brand: "+ Build.BRAND +", manufacture: "+ Build.MANUFACTURER );
        Log.i( LOGTAG, "dumpBuildInfo: hardware: "+ Build.HARDWARE +", board: "+ Build.BOARD );
        Log.i( LOGTAG, "dumpBuildInfo: product: "+ Build.PRODUCT +", model: "+ Build.MODEL );
        Log.i( LOGTAG, "dumpBuildInfo: device: "+ Build.DEVICE +", type: "+ Build.TYPE );
    }
    
}
