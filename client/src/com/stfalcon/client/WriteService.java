package com.stfalcon.client;


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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.stfalcon.client.connection.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by alexandr on 19.08.14.
 */
public class WriteService extends Service implements SensorEventListener {
    private static final long SENDING_DATA_INTERVAL_IN_MILLIS = 1000;

    private int NOTIFICATION = 1000;
    public WriteBinder binder = new WriteBinder();
    public static final int TYPE_A = 0;   // ACCELEROMETER
    public static final int TYPE_F = 1;   // FILTRATE_ACCELEROMETER
    public static final int TYPE_L = 2;   // LINEAR_ACCELERATION
    public static final int TYPE_G = 3;   // GRAVITY
    private float[] motion = new float[3];
    private float[] gravity = new float[3];
    private int activeSensorType;

    private SensorManager sensorManager;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = new Location("gps");
    private boolean createdConnectionWrapper = false;

    private List<String> dataToSend = new ArrayList<String>();
    private long lastSendingTime = 0l;



    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // @todo ?
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

    /**
     * Викликається при натиску на кнопку Старт. Створюється лісенер на локацію і на сенсор
     * Додає нотифікацію в статус бар
     */
    public void startListening() {
        activeSensorType = SampleApplication.getInstance().getSendedType();
        startForeground(NOTIFICATION, makeNotification());
        Log.v("Loger", "START_DONE");


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

        // @todo ?
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        if (activeSensorType == TYPE_L) {
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_UI);
        }
        if (activeSensorType == TYPE_G) {
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Викликається при натисканні на стоп. Вилучає лісенери і видаляє нотифікацію з статус бару
     */
    public void stopListening() {
        Log.v("Loger", "STOP_DONE");

        sensorManager.unregisterListener(this);

        stopForeground(true);

        if (listener != null) {
            locationManager.removeUpdates(listener);
        }
    }


    /**
     * Додає до данних координати і айді девайсу. Після чого формує пакети і періодично передає на сервер
     */
    public void sendNewData(final long time, String data, final int type) {
        if (createdConnectionWrapper) {
            if (type == activeSensorType && data != null) {
                String loc = " " + previousBestLocation.getLatitude() + " " + previousBestLocation.getLongitude();
                data = data + loc + " " + String.valueOf(previousBestLocation.getSpeed()) + "\n";
                dataToSend.add(data);

                if (time - lastSendingTime > SENDING_DATA_INTERVAL_IN_MILLIS) {
                    String stringData = "";

                    for (String string : dataToSend) {
                        stringData += string;
                    }

                    final String stringDataToSend = stringData;

                    getConnectionWrapper().send(
                            new HashMap<String, String>() {{
                                put(Communication.MESSAGE_TYPE, Communication.Connect.DATA);
                                put(Communication.Connect.DEVICE, createDeviceDescription(type));
                                put(SampleApplication.SENSOR, stringDataToSend);
                            }}
                    );

                    lastSendingTime = time;
                    dataToSend.clear();
                }
            }
        }
    }

    /**
     * Формує айдішку девайсу. Використовується для підпису на графіку і в файлі
     */
    private String createDeviceDescription(int type) {
        String stringType = "";
        switch (type) {
            case TYPE_A:
                stringType = "Accel";
                break;
            case TYPE_F:
                stringType = "Filter-Accel";
                break;
            case TYPE_G:
                stringType = "Gravity";
                break;
            case TYPE_L:
                stringType = "Linear-Accel";
                break;
        }

        String serial = Build.SERIAL;
        serial = serial.substring(serial.length() - 3);
        return Build.MODEL + "+" + serial + "-" + stringType;
    }

    /**
     * Створює нотифікацію в статус барі
     */
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

        // @todo можна забрати тайтл про файл
        if (createdConnectionWrapper) {
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

    /**
     * Викликається при надходженні нових даних сенсора. Ініціалізує передачу даних до сервера
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (lastSendingTime == 0) {
            lastSendingTime = System.currentTimeMillis();
        }

        long time = System.currentTimeMillis() - lastSendingTime;

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {


            float x = Math.round(sensorEvent.values[0]);
            float y = Math.round(sensorEvent.values[1]);
            float z = Math.round(sensorEvent.values[2]);

            // alpha is calculated as t / (t + dT)
            // with t, the low-pass filter's time-constant
            // and dT, the event delivery rate

            final float alpha = 0.8f;

            float gravity[] = new float[3];

            float linear_acceleration[] = new float[3];

            gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

            linear_acceleration[0] = Math.round(sensorEvent.values[0] - gravity[0]);
            linear_acceleration[1] = Math.round(sensorEvent.values[1] - gravity[1]);
            linear_acceleration[2] = Math.round(sensorEvent.values[2] - gravity[2]);

            String dataF = time + " " + linear_acceleration[0] + " " + linear_acceleration[1] + " " + linear_acceleration[2];
            String dataA = time + " " + x + " " + y + " " + z;

            //Log.i("Loger", dataA);

            sendNewData(System.currentTimeMillis(), dataA, WriteService.TYPE_A);
            sendNewData(System.currentTimeMillis(), dataF, WriteService.TYPE_F);


        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            float x = Math.round(sensorEvent.values[0]);
            float y = Math.round(sensorEvent.values[1]);
            float z = Math.round(sensorEvent.values[2]);

            String dataL = time + " " + x + " " + y + " " + z;

            sendNewData(System.currentTimeMillis(), dataL, WriteService.TYPE_L);

        }


        if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
            for (int i = 0; i < 3; i++) {
                gravity[i] = (float) (0.1 * sensorEvent.values[i] + 0.9 * gravity[i]);
                motion[i] = sensorEvent.values[i] - gravity[i];
            }

            float x = Math.round(motion[0]);
            float y = Math.round(motion[1]);
            float z = Math.round(motion[2]);

            String dataL = time + " " + x + " " + y + " " + z;

            sendNewData(System.currentTimeMillis(), dataL, WriteService.TYPE_G);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Для передачі даних в актівіті
     */
    public class WriteBinder extends Binder {
        public WriteService getService() {
            return WriteService.this;
        }
    }

    /**
     *
     */
    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            Log.i("Loger", "Location changed");
            previousBestLocation = loc;
            //if (isBetterLocation(loc, previousBestLocation)) previousBestLocation = loc;
        }

        public void onProviderDisabled(String provider) {
        }


        public void onProviderEnabled(String provider) {
        }


        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

    /**
     * Обробляє натискання на кнопку Коннект
     */
    public void connect() {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
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

