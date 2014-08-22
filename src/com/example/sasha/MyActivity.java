package com.example.sasha;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

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
    private LinearLayout llChart, frame;
    private GraphicalView graphicalView;
    private XYMultipleSeriesDataset dataSet;
    private XYValueSeries xSeries, ySeries, zSeries;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentBasedOnLayout();
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        server = (Button) findViewById(R.id.server);
        client = (Button) findViewById(R.id.client);
        textView = (TextView) findViewById(R.id.text);
        llChart = (LinearLayout) findViewById(R.id.chart);
        frame = (LinearLayout) findViewById(R.id.frame);
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

        if (graphicalView == null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            graphicalView = ChartFactory.getLineChartView(this, getDemoDataSet(),
                    getDemoRenderer());
            layout.addView(graphicalView, new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            graphicalView.setKeepScreenOn(true);

        } else {
            graphicalView.repaint();
        }
    }


    private void setContentBasedOnLayout() {
        WindowManager winMan = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        if (winMan != null)
        {
            int orientation = winMan.getDefaultDisplay().getOrientation();

            if (orientation == 0) {
                // Portrait
                setContentView(R.layout.main);
            }
            else if (orientation == 1) {
                // Landscape
                setContentView(R.layout.main_land);
            }
        }
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
                    /*String s = textView.getText().toString();
                    s = s + intent.getStringExtra(SampleApplication.SENSOR);
                    textView.setText(s);*/
                    String data = intent.getStringExtra(SampleApplication.SENSOR);
                    String [] arr = data.split(" ", 4);

                    float time = Long.valueOf(arr[0]);
                    float x = Float.valueOf(arr[1]);
                    float y = Float.valueOf(arr[2]);
                    float z = Float.valueOf(arr[3]);

                    xSeries.add(time / 1000, x);
                    ySeries.add(time / 1000, y);
                    zSeries.add(time / 1000, z);

                    graphicalView.repaint();
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




    private XYMultipleSeriesRenderer getDemoRenderer() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[] {20, 30, 15, 0});
        org.achartengine.renderer.XYSeriesRenderer r = new org.achartengine.renderer.XYSeriesRenderer();
        r.setColor(Color.BLUE);
        r.setPointStyle(PointStyle.SQUARE);
        renderer.addSeriesRenderer(r);

        r = new org.achartengine.renderer.XYSeriesRenderer();
        r.setPointStyle(PointStyle.CIRCLE);
        r.setColor(Color.GREEN);
        r.setFillPoints(true);
        renderer.addSeriesRenderer(r);

        r = new org.achartengine.renderer.XYSeriesRenderer();
        r.setPointStyle(PointStyle.DIAMOND);
        r.setColor(Color.YELLOW);
        renderer.addSeriesRenderer(r);

        renderer.setAxesColor(Color.DKGRAY);
        renderer.setLabelsColor(Color.LTGRAY);
        return renderer;
    }


    private XYMultipleSeriesDataset getDemoDataSet(){
        dataSet = new XYMultipleSeriesDataset();

        xSeries = new XYValueSeries("X");
        ySeries = new XYValueSeries("Y");
        zSeries = new XYValueSeries("Z");

        dataSet.addSeries(xSeries);
        dataSet.addSeries(ySeries);
        dataSet.addSeries(zSeries);

        return dataSet;
    }

}
