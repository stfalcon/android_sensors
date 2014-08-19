package com.example.andriy;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

public class MyActivity extends Activity implements SensorEventListener {
    /**
     * Called when the activity is first created.
     */

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private boolean isReading = false;

    private File textFile;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        final Button button = (Button) findViewById(R.id.button);
        button.setText(isReading ? R.string.stop : R.string.start);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isReading) {
                    sensorManager.unregisterListener(MyActivity.this);
                    button.setText(R.string.start);
                    isReading = false;
                } else if (sensorAccelerometer != null) {
                    sensorManager.registerListener(MyActivity.this, sensorAccelerometer
                            , SensorManager.SENSOR_DELAY_NORMAL);
                    button.setText(R.string.stop);
                    isReading = true;
                } else
                    Toast.makeText(MyActivity.this, getString(R.string.no_accelerometer), Toast.LENGTH_LONG).show();
            }
        });
    }
/*
    private void createTextFile() {
        textFile = new File(Environment.getExternalStorageDirectory() + File.separator + "test.txt");
        textFile.createNewFile();
        String[] data1={"1","1","0","0"};

        if(textFile.exists()){
            OutputStream fo = new FileOutputStream(textFile);
            fo.write(data1);
            fo.close();


        }
    }*/

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long time = System.currentTimeMillis();
        float x, y, z;

        float[] values = event.values;

        x = values[0];
        y = values[1];
        z = values[2];

        Log.i("logerr", "time = " + time + "; x = " + x + "; y = " + y + "; z = " + z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
