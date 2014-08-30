package com.example.andriy;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

/**
 * Created by user on 19.08.14.
 */
public class AccelerometerService extends Service implements SensorEventListener {
    public static final String EXTRA_COMMAND = "command";

    public static final int START_READING_DATA = 0;
    public static final int STOP_READING_DATA = 1;

    private SensorManager sensorManager;
    private OutputStream fo;
    private long startTime;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int commandId = intent.getIntExtra(EXTRA_COMMAND, START_READING_DATA);

        switch (commandId) {
            case STOP_READING_DATA:
                stopReadingData();

                stopSelf();
                break;
            case START_READING_DATA:
            default:
                startReadingData();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopReadingData() {
        closeTextFile();

        if (sensorManager != null)
            sensorManager.unregisterListener(this);
    }

    // 1408458990703 -0.060 4.523 8.920
    private void startReadingData() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (sensorAccelerometer != null) {
            createTextFile();

            sensorManager.registerListener(this, sensorAccelerometer
                    , SensorManager.SENSOR_DELAY_NORMAL);
        } else
            Toast.makeText(this, getString(R.string.no_accelerometer)
                    , Toast.LENGTH_LONG).show();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        long time = System.currentTimeMillis();
        float x, y, z;

        float[] values = event.values;

        x = values[0];
        y = values[1];
        z = values[2];


        // write data to file
        String data = time + "\u0020" + String.format("%.3f", x) + "\u0020"
                + String.format("%.3f", y) + "\u0020" + String.format("%.3f", z) + "\n";

        writeToTextFile(data);


        //send data to Activity for displaying in fraph
        Intent intent = new Intent(MyActivity.BROADCAST_RECEIVER_ACTION);

        intent.putExtra(MyActivity.X_EXTRA, x);
        intent.putExtra(MyActivity.Y_EXTRA, y);
        intent.putExtra(MyActivity.Z_EXTRA, z);

        intent.putExtra(MyActivity.TIME_EXTRA, time - startTime);

        sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void createTextFile() {
        startTime = System.currentTimeMillis();

        String fileName = "acc_" + new SimpleDateFormat("d_MM_yyy_HH:mm:ss").format(startTime) + ".txt";

        File textFile = new File(Environment.getExternalStorageDirectory() + File.separator + fileName);
        try {
            textFile.createNewFile();

            if (textFile.exists()) {
                fo = new FileOutputStream(textFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToTextFile(String line) {
        if (fo != null)
            try {
                fo.write(line.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    private void closeTextFile() {
        try {
            if (fo != null) {
                fo.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
