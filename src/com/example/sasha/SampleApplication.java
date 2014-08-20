package com.example.sasha;

import android.app.Application;
import com.example.sasha.connection.ConnectionWrapper;

/**
 * @author alwx
 * @version 1.0
 */
public class SampleApplication extends Application {
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
