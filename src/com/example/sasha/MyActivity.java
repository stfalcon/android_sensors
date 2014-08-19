package com.example.sasha;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MyActivity extends Activity implements SensorEventListener, View.OnClickListener {

    private SensorManager sensorManager;
    private ArrayList<String> arrayList = new ArrayList<String>();
    private Button start, stop;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long time = System.currentTimeMillis();
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];


            // alpha is calculated as t / (t + dT)
            // with t, the low-pass filter's time-constant
            // and dT, the event delivery rate

            final float alpha = 0.8f;

            double gravity[] = new double[3];

            gravity[0] = -9.81;
            gravity[1] = 9.81;
            gravity[2] = 0;

            double linear_acceleration[] = new double[3];

            gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

            linear_acceleration[0] = sensorEvent.values[0] - gravity[0];
            linear_acceleration[1] = sensorEvent.values[1] - gravity[1];
            linear_acceleration[2] = sensorEvent.values[2] - gravity[2];

            String data1 = time + ", " + x + "-" + y + "-" + z;

            String data2 = time + ", " + linear_acceleration[0] + " # " + linear_acceleration[1] + " # " + linear_acceleration[2];

            Log.i("Loger", "DATA1 " + data1);
            Log.i("Loger", "DATA2 " + data2);

            arrayList.add(data1);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.start:
                Log.i("Loger", "start");
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI );
                break;

            case R.id.stop:
                Log.i("Loger", "stop");
                sensorManager.unregisterListener(this);
                break;
        }
    }
}
