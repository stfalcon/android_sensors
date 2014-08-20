package com.example.sasha;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MyActivity extends Activity implements View.OnClickListener {

    private Button start, stop, server, client;
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
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        server = (Button) findViewById(R.id.server);
        client = (Button) findViewById(R.id.client);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        server.setOnClickListener(this);
        client.setOnClickListener(this);

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
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.start:
                Log.i("Loger", "start");
                startService(new Intent(this, WriteService.class));
                if (bound) {
                    writeServise.startListening();
                }
                break;

            case R.id.stop:
                Log.i("Loger", "stop");
                if (bound) {
                    writeServise.stopListening();
                }
                break;

            case R.id.server:
                startActivity(new Intent(this, ConnectActivity.class));
                break;

            case R.id.client:

                break;

        }
    }

}
