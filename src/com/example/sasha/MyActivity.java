package com.example.sasha;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.PriorityQueue;

public class MyActivity extends Activity implements View.OnClickListener {

    private Button start, stop, server, client;
    private ServiceConnection sConn;
    private WriteService writeServise;
    private boolean bound;
    private Intent intentService;
    private BroadcastReceiver mReceiver;
    private ProgressDialog dialog;
    private TextView textView;

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
        textView = (TextView) findViewById(R.id.text);
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
        registerReceiver();
        bindService(intentService, sConn, BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
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
        if (bound) {
            switch (view.getId()) {
                case R.id.start:
                    Log.i("Loger", "start");
                    startService(new Intent(this, WriteService.class));
                        writeServise.startListening();
                    break;

                case R.id.stop:
                    Log.i("Loger", "stop");
                        writeServise.stopListening();
                    break;

                case R.id.server:
                    //startActivity(new Intent(this, ConnectActivity.class));
                    writeServise.startServer();
                    dialog = new ProgressDialog(this);
                    dialog.setMessage("Start server...");
                    dialog.show();
                    break;

                case R.id.client:
                    writeServise.connect();
                    break;

            }
        }
    }


    /**
     * Create receiver for cache data from services
     */
    private void registerReceiver() {

        IntentFilter intentFilter = new IntentFilter(SampleApplication.CONNECTED);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.hasExtra(SampleApplication.SENSOR)) {
                    String s = textView.getText().toString();
                    s = s + intent.getStringExtra(SampleApplication.SENSOR);
                    textView.setText(s);
                    return;
                }


                if (intent.hasExtra(SampleApplication.STARTED)){
                    dialog.dismiss();
                    textView.setText("Start listening... \n");
                    //startActivity(new Intent(MyActivity.this, GraphicActivity.class));
                    return;
                }

                if (intent.hasExtra(SampleApplication.DEVICE)){

                    Toast.makeText(MyActivity.this,
                            intent.getStringExtra(SampleApplication.DEVICE),
                            Toast.LENGTH_LONG).show();
                }   else {
                    Toast.makeText(MyActivity.this,
                            getString(R.string.connected),
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
    }


}
