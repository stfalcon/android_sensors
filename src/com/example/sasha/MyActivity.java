package com.example.sasha;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;

public class MyActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

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
    XYMultipleSeriesRenderer renderer;

    private CheckBox cbX, cbY, cbZ, cbSqrt;

    ArrayList<DeviceGraphInformation> devices = new ArrayList<DeviceGraphInformation>();

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

        if (winMan != null) {
            int orientation = winMan.getDefaultDisplay().getOrientation();

            if (orientation == 0) {
                // Portrait
                setContentView(R.layout.main);
            } else if (orientation == 1) {
                // Landscape
                setContentView(R.layout.main_land);

                cbX = (CheckBox) findViewById(R.id.rb_x);
                cbY = (CheckBox) findViewById(R.id.rb_y);
                cbZ = (CheckBox) findViewById(R.id.rb_z);
                cbSqrt = (CheckBox) findViewById(R.id.rb_sqrt);

                cbX.setOnCheckedChangeListener(this);
                cbY.setOnCheckedChangeListener(this);
                cbZ.setOnCheckedChangeListener(this);
                cbSqrt.setOnCheckedChangeListener(this);
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

                    String device = intent.getStringExtra(SampleApplication.DEVICE);

                    String data = intent.getStringExtra(SampleApplication.SENSOR);
                    String[] arr = data.split(" ", 4);

                    long time = Long.valueOf(arr[0]);
                    float x = Float.valueOf(arr[1]);
                    float y = Float.valueOf(arr[2]);
                    float z = Float.valueOf(arr[3]);


                    DeviceGraphInformation information = findDeviceOnGraph(device);
                    if (information == null) {
                        //add new device
                        information = new DeviceGraphInformation(device);
                        createSeriesAndRendersForNewDevice(information);

                        devices.add(information);
                    }

                    information.xSeries.add(time, x);
                    information.ySeries.add(time, y);
                    information.zSeries.add(time, z);
                    information.sqrSeries.add(time, Math.sqrt(x * x + y * y + z * z));

                    renderer.setXAxisMin(time - 10000);
                    renderer.setXAxisMax(time + 100);

                    graphicalView.repaint();
                    return;
                }


                if (intent.hasExtra(SampleApplication.STARTED)) {
                    dialog.dismiss();
                    textView.setText("Start listening... \n");
                    //startActivity(new Intent(MyActivity.this, GraphicActivity.class));
                    return;
                }

                if (intent.hasExtra(SampleApplication.DEVICE)) {

                    Toast.makeText(MyActivity.this,
                            intent.getStringExtra(SampleApplication.DEVICE),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MyActivity.this,
                            getString(R.string.connected),
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
    }

    private DeviceGraphInformation findDeviceOnGraph(String device) {
        for (DeviceGraphInformation information : devices)
            if (information.device.equals(device))
                return information;

        return null;
    }


    private XYMultipleSeriesRenderer getDemoRenderer() {
        renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[]{20, 30, 15, 0});

        renderer.setZoomButtonsVisible(true);
        renderer.setAntialiasing(true);

        renderer.setXAxisMin(0);
        renderer.setYAxisMin(-5);
        renderer.setYAxisMax(20);

        renderer.setAxesColor(Color.DKGRAY);
        renderer.setLabelsColor(Color.LTGRAY);

        return renderer;
    }


    private XYMultipleSeriesDataset getDemoDataSet() {
        dataSet = new XYMultipleSeriesDataset();
        return dataSet;
    }

    private void createSeriesAndRendersForNewDevice(DeviceGraphInformation information){
        org.achartengine.renderer.XYSeriesRenderer r = new org.achartengine.renderer.XYSeriesRenderer();
        r.setColor(Color.BLUE + 100 * devices.size());
        r.setPointStyle(PointStyle.SQUARE);

        information.xSeriesRenderer = r;

        r = new org.achartengine.renderer.XYSeriesRenderer();
        r.setPointStyle(PointStyle.CIRCLE);
        r.setColor(Color.GREEN + 100 * devices.size());
        r.setFillPoints(true);

        information.ySeriesRenderer = r;

        r = new org.achartengine.renderer.XYSeriesRenderer();
        r.setPointStyle(PointStyle.DIAMOND);
        r.setColor(Color.YELLOW + 100 * devices.size());

        information.zSeriesRenderer = r;

        r = new org.achartengine.renderer.XYSeriesRenderer();
        r.setPointStyle(PointStyle.TRIANGLE);
        r.setColor(Color.RED + 100 * devices.size());

        information.sqrSeriesRenderer = r;


        String name = information.device;

        XYValueSeries xSeries = new XYValueSeries(name + "-X");
        XYValueSeries ySeries = new XYValueSeries(name + "-Y");
        XYValueSeries zSeries = new XYValueSeries(name + "-Z");
        XYValueSeries sqrSeries = new XYValueSeries(name + "-sqr");

        information.xSeries = xSeries;
        information.ySeries = ySeries;
        information.zSeries = zSeries;
        information.sqrSeries = sqrSeries;

        if (cbX.isChecked()){
            renderer.addSeriesRenderer(information.xSeriesRenderer);
            dataSet.addSeries(devices.size(), xSeries);
        }
        if (cbY.isChecked()){
            renderer.addSeriesRenderer(information.ySeriesRenderer);
            dataSet.addSeries(devices.size(), ySeries);
        }
        if (cbZ.isChecked()){
            renderer.addSeriesRenderer(information.zSeriesRenderer);
            dataSet.addSeries(devices.size(), zSeries);
        }
        if (cbSqrt.isChecked()){
            renderer.addSeriesRenderer(information.sqrSeriesRenderer);
            dataSet.addSeries(devices.size(), sqrSeries);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        for (DeviceGraphInformation information : devices) {
            XYValueSeries series;
            XYSeriesRenderer seriesRenderer;

            switch (buttonView.getId()) {
                case R.id.rb_x:
                    series = information.xSeries;
                    seriesRenderer = information.xSeriesRenderer;
                    break;
                case R.id.rb_y:
                    series = information.ySeries;
                    seriesRenderer = information.ySeriesRenderer;
                    break;
                case R.id.rb_z:
                    series = information.zSeries;
                    seriesRenderer = information.zSeriesRenderer;
                    break;
                case R.id.rb_sqrt:
                default:
                    series = information.sqrSeries;
                    seriesRenderer = information.sqrSeriesRenderer;
                    break;
            }

            if (isChecked){
                renderer.addSeriesRenderer(seriesRenderer);
                dataSet.addSeries(series);
            }
            else {
                renderer.removeSeriesRenderer(seriesRenderer);
                dataSet.removeSeries(series);
            }

            graphicalView.repaint();
        }
    }

    private class DeviceGraphInformation {
        private String device;
        private XYValueSeries xSeries;
        private XYValueSeries ySeries;
        private XYValueSeries zSeries;
        private XYValueSeries sqrSeries;

        private org.achartengine.renderer.XYSeriesRenderer xSeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer ySeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer zSeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer sqrSeriesRenderer;

        public DeviceGraphInformation(String device) {
            this.device = device;
        }

        public String getDevice() {
            return device;
        }

        public XYValueSeries getxSeries() {
            return xSeries;
        }

        public XYValueSeries getySeries() {
            return ySeries;
        }

        public XYValueSeries getzSeries() {
            return zSeries;
        }

        public XYValueSeries getSqrSeries() {
            return sqrSeries;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DeviceGraphInformation)
                return this.device.equals(((DeviceGraphInformation) o).device);
            else
                return false;
        }
    }
}
