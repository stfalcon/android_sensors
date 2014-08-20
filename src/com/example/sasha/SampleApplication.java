package com.example.sasha;

import android.app.Application;
import com.example.sasha.connection.ConnectionWrapper;

/**
 * @author alwx
 * @version 1.0
 */
public class SampleApplication extends Application {
    public static final String CONNECTED = "com.example.sasha.CONNECTED";
    public static final String DATA = "com.example.sasha.DATA";
    public static final String DEVICE = "device";
    public static final String STARTED = "started";
    public static final String SENSOR = "sensor";
    private ConnectionWrapper mConnectionWrapper;
    private static SampleApplication self;


    public static synchronized SampleApplication getInstance() {
        return self;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (SampleApplication.class) {
            self = this;
        }
    }

    public void createConnectionWrapper(ConnectionWrapper.OnCreatedListener listener) {
        mConnectionWrapper = new ConnectionWrapper(getApplicationContext(), listener);
    }

    public ConnectionWrapper getConnectionWrapper() {
        return mConnectionWrapper;
    }


}
