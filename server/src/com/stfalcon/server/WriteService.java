package com.stfalcon.server;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.stfalcon.server.connection.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by alexandr on 19.08.14.
 */
public class WriteService extends Service {
    private static final int THREE_MINUTES = 1000 * 60 * 3;
    private int NOTIFICATION = 1000;
    public WriteBinder binder = new WriteBinder();


    private boolean createdConnectionWrapper = false;

    @Override
    public void onCreate() {
        super.onCreate();

        SampleApplication.getInstance().createConnectionWrapper(
                new ConnectionWrapper.OnCreatedListener() {
                    @Override
                    public void onCreated() {
                        createdConnectionWrapper = true;
                    }
                }
        );
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public class WriteBinder extends Binder {
        public WriteService getService() {
            return WriteService.this;
        }
    }


    // WIFI CONNECTION

    /**
     *
     */
    public void startServer() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int intaddr = wifi.getConnectionInfo().getIpAddress();

        if (wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED || intaddr == 0) {
            Intent intentTracking = new Intent(SampleApplication.CONNECTED);
            intentTracking.putExtra(SampleApplication.WIFI, true);
            LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);
        } else {
            getConnectionWrapper().stopNetworkDiscovery();
            getConnectionWrapper().startServer();
            getConnectionWrapper().setHandler(mServerHandler);

            Intent intentTracking = new Intent(SampleApplication.CONNECTED);
            intentTracking.putExtra(SampleApplication.STARTED, true);
            LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);
        }
    }

    /**
     *
     */
    public void connect() {
        if (createdConnectionWrapper) {
            getConnectionWrapper().findServers(new NetworkDiscovery.OnFoundListener() {
                @Override
                public void onFound(javax.jmdns.ServiceInfo info) {
                    if (info != null && info.getInet4Addresses().length > 0) {
                        getConnectionWrapper().stopNetworkDiscovery();
                        getConnectionWrapper().connectToServer(
                                info.getInet4Addresses()[0],
                                info.getPort(),
                                mConnectionListener
                        );
                        getConnectionWrapper().setHandler(mClientHandler);
                    }
                }
            });
        }
    }


    /**
     *
     */
    private Connection.ConnectionListener mConnectionListener = new Connection.ConnectionListener() {
        @Override
        public void onConnection() {
            getConnectionWrapper().send(
                    new HashMap<String, String>() {{
                        put(Communication.MESSAGE_TYPE, Communication.Connect.DEVICE);
                        put(Communication.Connect.DEVICE, Build.MODEL);
                    }}
            );
        }
    };

    /**
     *
     */
    private Handler mServerHandler = new MessageHandler() {
        @Override
        public void onMessage(String type, JSONObject message) {
            try {


                if (type.equals(Communication.Connect.DATA)) {

                    //Log.d("Loger", "mServerHandler have data");
                    final String deviceFrom = message.getString(Communication.Connect.DEVICE);
                    final String data = message.getString(SampleApplication.SENSOR);

                    Intent intentTracking = new Intent(SampleApplication.CONNECTED);
                    intentTracking.putExtra(SampleApplication.DEVICE, "Device: " + deviceFrom);
                    intentTracking.putExtra(SampleApplication.SENSOR, data);
                    LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);

                }


                if (type.equals(Communication.Connect.DEVICE)) {
                    final String deviceFrom = message.getString(Communication.Connect.DEVICE);

                    Intent intentTracking = new Intent(SampleApplication.CONNECTED);
                    intentTracking.putExtra(SampleApplication.DEVICE, "Device: " + deviceFrom);
                    LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);

                    getConnectionWrapper().send(
                            new HashMap<String, String>() {{
                                put(Communication.MESSAGE_TYPE, Communication.ConnectSuccess.TYPE);
                            }}
                    );
                }


            } catch (JSONException e) {
                Log.d("Loger", "JSON parsing exception: " + e);
            }
        }
    };

    private Handler mClientHandler = new MessageHandler() {
        @Override
        public void onMessage(String type, JSONObject message) {
            if (type.equals(Communication.ConnectSuccess.TYPE)) {
                Intent intentTracking = new Intent(SampleApplication.CONNECTED);
                LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);
                createdConnectionWrapper = true;
            }
        }
    };


    @Override
    public void onDestroy() {
        getConnectionWrapper().reset();
        super.onDestroy();
    }

    private ConnectionWrapper getConnectionWrapper() {
        return SampleApplication.getInstance().getConnectionWrapper();
    }
}

