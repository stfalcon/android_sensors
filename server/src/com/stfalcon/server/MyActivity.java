package com.stfalcon.server;

import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;

public class MyActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private final static int MIN_VALUES_COUNT_PER_SECOND = 5;
    private final static int MAX_VALUES_COUNT_PER_SECOND = 30;
    private final static int MILLISECONDS_BEFORE_REFRESH_GRAPHS = 30;

    private Button  server;
    private ServiceConnection sConn;
    private WriteService writeServise;
    private boolean bound = false;
    private Intent intentService;
    private BroadcastReceiver mReceiver;
    private TextView textView, tvFilterValue;
    private LinearLayout llChart;
    private boolean pause = false;

    private int filterValuePerSecond = 15; //in seconds
    private long lastUpdatedTime = System.currentTimeMillis(), updateInterval = 200;

    private CheckBox cbX, cbY, cbZ, cbSqrt;

    ArrayList<DeviceGraphInformation> devices = new ArrayList<DeviceGraphInformation>();
    private GraphicalView graphicalView;
    private XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentBasedOnLayout();
        server = (Button) findViewById(R.id.server);
        textView = (TextView) findViewById(R.id.text);
        llChart = (LinearLayout) findViewById(R.id.chart);
        server.setOnClickListener(this);

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

        findViewById(R.id.clear).setOnClickListener(this);
        findViewById(R.id.pause).setOnClickListener(this);
        findViewById(R.id.plus).setOnClickListener(this);
        findViewById(R.id.minus).setOnClickListener(this);
        findViewById(R.id.screen_shot).setOnClickListener(this);

        tvFilterValue = (TextView) findViewById(R.id.filter_value);

        updateFilterValue();
    }

    private void updateFilterValue(){
        updateInterval = 1000 / filterValuePerSecond;
        tvFilterValue.setText(String.valueOf(filterValuePerSecond));
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

    private void clearGraph(){
        if (graphicalView.isChartDrawn()){
            renderer.removeAllRenderers();
            dataSet.clear();
            devices.clear();
            graphicalView.repaint();
        }
    }

    private void setContentBasedOnLayout() {
        WindowManager winMan = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        if (winMan != null) {
            int orientation = winMan.getDefaultDisplay().getOrientation();

            if (orientation == 0) {
                // Portrait
                setContentView(R.layout.main_land);
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

                case R.id.server:
                    //startActivity(new Intent(this, ConnectActivity.class));
                    writeServise.startServer();
                    break;

                case R.id.pause:
                    if (pause){
                        pause = false;
                    } else {
                        pause = true;
                    }

                    ((Button) findViewById(R.id.pause)).setText(pause ? "Start" : "Pause");
                    break;
                case R.id.clear:
                    clearGraph();
                    break;


                case R.id.minus:
                    if (filterValuePerSecond > MIN_VALUES_COUNT_PER_SECOND){
                        filterValuePerSecond--;
                        updateFilterValue();
                    }
                    break;
                case R.id.plus:
                    if (filterValuePerSecond <MAX_VALUES_COUNT_PER_SECOND){
                        filterValuePerSecond++;
                        updateFilterValue();
                    }
                    break;

                case R.id.screen_shot:
                    makeScreenShot();
                    break;
            }
        }
    }

    private void makeScreenShot() {
        Bitmap bitmap = graphicalView.toBitmap();

        try {
            File directory = new File("/sdcard/AccelData/ScreenShots/");
            directory.mkdirs();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = simpleDateFormat.format(System.currentTimeMillis());

            File jpegPictureFile = new File("/sdcard/AccelData/ScreenShots/" + time + "_graph.jpeg");
            jpegPictureFile.createNewFile();
            FileOutputStream pictureOutputStream = new FileOutputStream(jpegPictureFile);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, pictureOutputStream);

            MediaStore.Images.Media.insertImage(getContentResolver(), jpegPictureFile.getPath(), null
                    , "Graph Screen Shot");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

                if (intent.hasExtra(SampleApplication.WIFI)) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    return;
                }


                if (lastUpdatedTime + updateInterval > System.currentTimeMillis()){
                    return;
                } else {
                    lastUpdatedTime = System.currentTimeMillis();
                }


                if (intent.hasExtra(SampleApplication.SENSOR)) {
                    /*String s = textView.getText().toString();
                    s = s + intent.getStringExtra(SampleApplication.SENSOR);
                    textView.setText(s);*/

                    String device = intent.getStringExtra(SampleApplication.DEVICE);

                    String data = intent.getStringExtra(SampleApplication.SENSOR);
                    String[] arr = data.split(" ", 4);

                    //long time = Long.valueOf(arr[0]);
                    long time = System.currentTimeMillis();
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

                    if (information.xSeries.getMaxX() + 1000 < time)  {
                        createAndAddXSeriesAndRenderer(information);
                    }
                    if (information.ySeries.getMaxX() + 1000 < time)
                        createAndAddYSeriesAndRenderer(information);
                    if (information.zSeries.getMaxX() + 1000 < time)
                        createAndAddZSeriesAndRenderer(information);
                    if (information.sqrSeries.getMaxX() + 1000 < time)
                        information.xSeries.clearAnnotations();
                        createAndAddSqrSeriesAndRenderer(information);


                    information.xSeries.add(time, x);
                    information.ySeries.add(time, y);
                    information.zSeries.add(time, z);
                    information.sqrSeries.add(time, Math.sqrt(x * x + y * y + z * z));

                    if (!pause){
                        renderer.setXAxisMin(System.currentTimeMillis() - 10000);
                        renderer.setXAxisMax(System.currentTimeMillis() + 500);
                        graphicalView.repaint();
                    }
                    return;
                }


                if (intent.hasExtra(SampleApplication.STARTED)) {
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
        renderer.setPointSize(2f);
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
        try {
            createAndAddXSeriesAndRenderer(information);
            createAndAddYSeriesAndRenderer(information);
            createAndAddZSeriesAndRenderer(information);
            createAndAddSqrSeriesAndRenderer(information);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createAndAddSqrSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(getRandomColor());
        r.setPointStyle(PointStyle.CIRCLE);
        r.setFillPoints(true);
        r.setLineWidth(3f);

        information.sqrSeriesRenderer = r;


        XYValueSeries sqrSeries = new XYValueSeries(information.device + "-sqr");

        information.sqrSeries = sqrSeries;
        if (cbSqrt.isChecked()){
            renderer.addSeriesRenderer(information.sqrSeriesRenderer);
            dataSet.addSeries(devices.size(), sqrSeries);
        }
    }

    private void createAndAddZSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(getRandomColor());
        r.setPointStyle(PointStyle.DIAMOND);
        r.setFillPoints(true);
        r.setLineWidth(3f);

        information.zSeriesRenderer = r;

        XYValueSeries zSeries = new XYValueSeries(information.device + "-Z");
        information.zSeries = zSeries;

        if (cbZ.isChecked()){
            renderer.addSeriesRenderer(information.zSeriesRenderer);
            dataSet.addSeries(devices.size(), zSeries);
        }
    }

    private void createAndAddYSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setPointStyle(PointStyle.TRIANGLE);
        r.setFillPoints(true);
        r.setColor(getRandomColor());
        r.setLineWidth(3f);

        information.ySeriesRenderer = r;

        XYValueSeries ySeries = new XYValueSeries(information.device + "-Y");
        information.ySeries = ySeries;
        if (cbY.isChecked()){
            renderer.addSeriesRenderer(information.ySeriesRenderer);
            dataSet.addSeries(devices.size(), ySeries);
        }
    }

    private void createAndAddXSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(getRandomColor());
        r.setPointStyle(PointStyle.SQUARE);
        r.setFillPoints(true);
        r.setLineWidth(3f);

        information.xSeriesRenderer = r;

        XYValueSeries xSeries = new XYValueSeries(information.device + "-X");
        information.xSeries = xSeries;

        if (cbX.isChecked()){
            renderer.addSeriesRenderer(information.xSeriesRenderer);
            dataSet.addSeries(devices.size(), xSeries);
        }
    }

    public int getRandomColor(){
        Random rand = new Random();
        // Java 'Color' class takes 3 floats, from 0 to 1.
        int r = rand.nextInt(255);
        int g = rand.nextInt(255);
        int b = rand.nextInt(255);
        return new Color().rgb(r, g, b);
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
