package com.example.sasha;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.wifi.WifiManager;
import android.os.*;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.example.sasha.connection.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by alexandr on 19.08.14.
 */
public class WriteService extends Service implements SensorEventListener {
    private static final int THREE_MINUTES = 1000 * 60 * 3;
    private int NOTIFICATION = 1000;
    private OutputStreamWriter outputStreamWriterA;
    private OutputStreamWriter outputStreamWriterS;
    private OutputStreamWriter outputStreamWriterL;
    private OutputStreamWriter outputStreamWriterGPS;
    public WriteBinder binder = new WriteBinder();
    public static final int TYPE_A = 0;   // ACCELEROMETER
    public static final int TYPE_F = 1;   // FILTRATE_ACCELEROMETER
    public static final int TYPE_L = 2;   // LINEAR_ACCELERATION

    private SensorManager sensorManager;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;
    private boolean createdConnectionWrapper = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

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

    public void startListening() {
        //startService(new Intent(this, WriteService.class));
        startForeground(NOTIFICATION, makeNotification());
        Log.v("Loger", "START_DONE");

        try {

            File directory = new File("/sdcard/AccelData/");
            directory.mkdirs();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = simpleDateFormat.format(System.currentTimeMillis());

            File myFileA = new File("/sdcard/AccelData/" + time + "Data_ACCELEROMETER.txt");
            myFileA.createNewFile();
            FileOutputStream fOutA = new FileOutputStream(myFileA);

            File myFileS = new File("/sdcard/AccelData/" + time + "Data_FILTRATE_ACCELEROMETER.txt");
            myFileS.createNewFile();
            FileOutputStream fOutS = new FileOutputStream(myFileS);

            File myFileL = new File("/sdcard/AccelData/" + time + "Data_LINEAR_ACCELERATION.txt");
            myFileL.createNewFile();
            FileOutputStream fOutL = new FileOutputStream(myFileL);

            File myFileGPS = new File("/sdcard/AccelData/" + time + "Data_GPS.txt");
            myFileGPS.createNewFile();
            FileOutputStream fOutGPS = new FileOutputStream(myFileGPS);

            outputStreamWriterA = new OutputStreamWriter(fOutA);
            outputStreamWriterS = new OutputStreamWriter(fOutS);
            outputStreamWriterL = new OutputStreamWriter(fOutL);
            outputStreamWriterGPS = new OutputStreamWriter(fOutGPS);
        } catch (IOException e) {
            e.printStackTrace();
        }


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);


        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_UI);
    }


    public void stopListening() {
        Log.v("Loger", "STOP_DONE");

        sensorManager.unregisterListener(this);

        stopForeground(true);

        try {
            outputStreamWriterA.close();
            outputStreamWriterS.close();
            outputStreamWriterL.close();
            outputStreamWriterGPS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (listener != null) {
            locationManager.removeUpdates(listener);
        }
    }


    /**
     *
     */
    public void writeNewData(long time, final String data, int type) {

        if(createdConnectionWrapper){
             if (type == TYPE_L) {
                 getConnectionWrapper().send(
                         new HashMap<String, String>() {{
                             put(Communication.MESSAGE_TYPE, Communication.Connect.DATA);
                             put(Communication.Connect.DEVICE, Build.MODEL + Build.SERIAL);
                             put(SampleApplication.SENSOR, data);
                         }}
                 );
             }
        } else {

        if (previousBestLocation != null) {
            String loc = " lat" + previousBestLocation.getLatitude() + " " + "lon" + previousBestLocation.getLongitude();
            try {
                String location = String.valueOf(time) + loc + "\n";
                outputStreamWriterGPS.write(location);
                switch (type) {
                    case TYPE_A:
                        outputStreamWriterA.write(data);
                        break;
                    case TYPE_F:
                        outputStreamWriterS.write(data);
                        break;
                    case TYPE_L:
                        outputStreamWriterL.write(data);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        }
    }


    public Notification makeNotification() {

        Intent intent = new Intent(this, MyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_sys_upload)
                        .setContentTitle(getString(R.string.app_name))
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(contentIntent);

        if (createdConnectionWrapper){
            mBuilder.setContentText(getString(R.string.send));
        } else {
            mBuilder.setContentText(getString(R.string.write));
        }

        mBuilder.setAutoCancel(true);
        return mBuilder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long time = System.currentTimeMillis();
            float x = round(sensorEvent.values[0], 3);
            float y = round(sensorEvent.values[1], 3);
            float z = round(sensorEvent.values[2], 3);


            // alpha is calculated as t / (t + dT)
            // with t, the low-pass filter's time-constant
            // and dT, the event delivery rate

            final float alpha = 0.8f;

            float gravity[] = new float[3];

            float linear_acceleration[] = new float[3];

            gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

            linear_acceleration[0] = round(sensorEvent.values[0] - gravity[0], 3);
            linear_acceleration[1] = round(sensorEvent.values[1] - gravity[1], 3);
            linear_acceleration[2] = round(sensorEvent.values[2] - gravity[2], 3);

            String dataF = time + " " + linear_acceleration[0] + " " + linear_acceleration[1] + " " + linear_acceleration[2] + "\n";
            String dataA = time + " " + x + " " + y + " " + z + "\n";

            //Log.i("Loger", dataA);

            writeNewData(time, dataA, WriteService.TYPE_A);
            writeNewData(time, dataF, WriteService.TYPE_F);


        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            long time = System.currentTimeMillis();
            float x = round(sensorEvent.values[0], 3);
            float y = round(sensorEvent.values[1], 3);
            float z = round(sensorEvent.values[2], 3);

            String dataL = time + " " + x + " " + y + " " + z + "\n";

            writeNewData(time, dataL, WriteService.TYPE_L);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public class WriteBinder extends Binder {
        public WriteService getService() {
            return WriteService.this;
        }
    }


    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            Log.i("Loger", "Location changed");
            isBetterLocation(loc, previousBestLocation);
        }

        public void onProviderDisabled(String provider) {
        }


        public void onProviderEnabled(String provider) {
        }


        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }


    /**
     * @param location
     * @param currentBestLocation
     * @return
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            previousBestLocation = location;
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > THREE_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -THREE_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }


    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


    private static float round(float number, int scale) {
        int pow = 10;
        for (int i = 1; i < scale; i++)
            pow *= 10;
        float tmp = number * pow;
        return (float) (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) / pow;
    }




    // WIFI CONNECTION

    /**
     *
     */
    public void startServer(){
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            int intaddr = wifi.getConnectionInfo().getIpAddress();

            if (wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED || intaddr == 0) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } else {
                getConnectionWrapper().stopNetworkDiscovery();
                getConnectionWrapper().startServer();
                getConnectionWrapper().setHandler(mServerHandler);
            }
        Intent intentTracking = new Intent(SampleApplication.CONNECTED);
        intentTracking.putExtra(SampleApplication.STARTED, true);
        LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);
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

