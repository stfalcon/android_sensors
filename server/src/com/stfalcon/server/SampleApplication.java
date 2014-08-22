package com.stfalcon.server;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.stfalcon.server.connection.ConnectionWrapper;

/**
 * @author alwx
 * @version 1.0
 */
public class SampleApplication extends Application {
    public static final String CONNECTED = "com.example.sasha.CONNECTED";
    public static final String DATA = "com.example.sasha.DATA";
    public static final String DEVICE = "device";
    public static final String WIFI = "wifi";
    public static final String STARTED = "started";
    public static final String SENSOR = "sensor";
    private ConnectionWrapper mConnectionWrapper;
    private static SampleApplication self;
    private SharedPreferences sharedPreferences;


    public static synchronized SampleApplication getInstance() {
        return self;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (SampleApplication.class) {
            self = this;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void createConnectionWrapper(ConnectionWrapper.OnCreatedListener listener) {
        mConnectionWrapper = new ConnectionWrapper(getApplicationContext(), listener);
    }

    public ConnectionWrapper getConnectionWrapper() {
        return mConnectionWrapper;
    }

}
