package com.samsung.mars.socket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.samsung.mars.base.ChannelFactory;
import com.samsung.mars.base.Message;
import com.samsung.mars.base.Response;
import com.samsung.mars.exception.MarsException;
import com.samsung.mars.model.DeviceInfo;
import com.samsung.mars.util.Deferred;
import com.samsung.mars.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;


public class BluetoothChannel extends MultiChannel 
{
    private static final String LOGTAG = Log.setLogLevel( "Mars.BluetoothChannel", Log.DEBUG );
    private static final int REQUEST_ENABLE_BT = 66;
    
    public final static UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public final static String HEARTBEAT_TEXT = "heartbeat";
    public final static String CHARSET_NAME = "ISO-8859-1";
    
    private boolean isServerInited;
    private Map<String,BluetoothSocket> serverSockets = new HashMap<String,BluetoothSocket>();      // if client
    private Map<String,BluetoothSocket> clientSockets = new HashMap<String,BluetoothSocket>();      // if server
    private Map<String,Map<Integer,String>> chunkedMsgs = new HashMap<String,Map<Integer,String>>();  // keyed by msgId to map of msg parts keyed by part #
    private int chunkSize = 600;
    private int maxMsgSize = 1000;

    private Context context;
    private BluetoothAdapter btAdapter;
    private LocalBroadcastManager broadcastMgr;

    
    public BluetoothChannel( boolean isServer, String remoteName, String localName ) {
        super( ChannelFactory.Type.BLUETOOTH, remoteName, localName );
        setServer( isServer );
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    @Override
    public void setRemoteName( String name ) {
        remoteName = name;
    }

    @Override
    public String getLocalName() {
        if ( localName == null ) {
            localName = btAdapter.getName();
        }
        return localName;
    }

    public static String getName() {
        return BluetoothAdapter.getDefaultAdapter().getName();
    }

    public static Set<BluetoothDevice> discoverDevices()  throws MarsException {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( btAdapter == null ) {
            Log.e( LOGTAG, "initialize: Device does not support Bluetooth" );
            throw new MarsException( "Bluetooth_Not_Supported", "Device does not support Bluetooth." );
        }
        if ( !btAdapter.isEnabled() ) {
            Log.e( LOGTAG, "initialize: Bluetooth not enabled." );
            throw new MarsException( "Bluetooth_Not_Enabled", "Bluetooth is not enabled." );
        }
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        return pairedDevices;
    }

    @Override
    public Deferred<Boolean> initialize( Context context )  throws MarsException {
        this.context = context;
        final Deferred<Boolean> defer = new Deferred<Boolean>();
        broadcastMgr = LocalBroadcastManager.getInstance( context );
        getLocalName();   // to set it if null
        
        Set<BluetoothDevice> pairedDevices = discoverDevices();
        
        if ( isServer && !isServerInited ) {
            isServerInited = true;
            Thread serverThread = new Thread() {
                public void run() {
                    BluetoothServerSocket serverSocket = null;
                    try {
                        serverSocket = btAdapter.listenUsingRfcommWithServiceRecord( "service_name_for_SDP_record", DEFAULT_UUID );
                    } catch ( IOException ex ) {
                        Log.e( LOGTAG, "run: Error establishing server socket: "+ ex.getLocalizedMessage() );
                    }
                    while ( true ) {
                        Log.i( LOGTAG, "connect: before accept:................. localName: "+ localName );
                        final BluetoothSocket clientSocket;
                        try {
                            clientSocket = serverSocket.accept();
                            final BluetoothDevice clientDevice = clientSocket.getRemoteDevice();
                            String addr = clientDevice.getAddress();
                            Log.i( LOGTAG, "connect: after accept:++++++++++++++++ localName: "+ localName +", address: "+ clientDevice.getAddress() );                            
                            clientSockets.put( addr, clientSocket );
                            notifyUpdate( ACTION_MARS_CONNECTED, addr );

                            new Thread( new Runnable() {
                                @Override
                                public void ru