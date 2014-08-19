package com.example.andriy;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
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
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

public class MyActivity extends Activity{
    private boolean isReading = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        isReading = isMyServiceRunning();

        final Button button = (Button) findViewById(R.id.button);
        button.setText(isReading ? R.string.stop : R.string.start);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isReading)
                    isReading = false;
                else
                    isReading = true;

                Intent intent = new Intent(MyActivity.this, AccelerometerService.class);
                intent.putExtra(AccelerometerService.EXTRA_COMMAND
                        , isReading ? AccelerometerService.START_READING_DATA : AccelerometerService.STOP_READING_DATA);
                startService(intent);

                button.setText(isReading ? R.string.stop: R.string.start);
            }
        });
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AccelerometerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
