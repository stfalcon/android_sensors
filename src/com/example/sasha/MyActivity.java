package com.example.sasha;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class MyActivity extends Activity implements SensorEventListener, View.OnClickListener {

    private SensorManager sensorManager;
    private Button start, stop;
    private ServiceConnection sConn;
    private WriteService writeServise;
    private boolean bound;
    private Intent intentService;
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

        intentService = new Intent(this, WriteService.class);

        //Connect location service
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d("Loger", "MainActivity onServiceConnected");
                writeServise = ((WriteService.WriteBinder) binder).getService();
                bound = true;
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d("Loger", "MainActivity onServiceDisconnected");
                bound = false;
            }
        };

    }


    @Override
    public void onStart() {
        super.onStart();
        bindService(intentService, sConn, BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!bound) return;
        unbindService(sConn);
        bound = false;
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

            float gravity[] = new float[3];

            float linear_acceleration[] = new float[3];

            gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

            linear_acceleration[0] = sensorEvent.values[0] - gravity[0];
            linear_acceleration[1] = sensorEvent.values[1] - gravity[1];
            linear_acceleration[2] = sensorEvent.values[2] - gravity[2];

            String dataA = "T" + time + " " + linear_acceleration[0] + " " + linear_acceleration[1] + " " + linear_acceleration[2];
            String dataS = "T" + time + " " + x + " " + y + " " + z;
            String dataSA = "T" + time + " " + x + " " + linear_acceleration[0] + " " + y + " " + linear_acceleration[1] + " " + z + " " + linear_acceleration[2];
            /*Log.i("Loger", "A " + dataA);
            Log.i("Loger", "S " + dataS);*/
            Log.i("Loger", "SA " + dataSA);

            if (bound){
                writeServise.writeNewData(dataA, WriteService.TYPE_A);
                writeServise.writeNewData(dataS, WriteService.TYPE_S);
            }

        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            long time = System.currentTimeMillis();
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            String dataL = "T" + time + " " + x + " " + y + " " + z;

            if (bound){
                writeServise.writeNewData(dataL, WriteService.TYPE_L);
            }


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
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_UI );
                startService(new Intent(this, WriteService.class));
                if (bound){
                    writeServise.startListening();
                }
                break;

            case R.id.stop:
                Log.i("Loger", "stop");
                sensorManager.unregisterListener(this);
                if (bound){
                    writeServise.stopListening();
                }
                break;

        }
    }
}
