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
import android.widget.*;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;

public class MyActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private final static int MIN_VALUES_COUNT_PER_SECOND = 5;
    private final static int MAX_VALUES_COUNT_PER_SECOND = 30;
    private final static int MILLISECONDS_BEFORE_REFRESH_GRAPHS = 30;
    public final static int MIN_DELTA_SPEED = 5;
    private Button server, showMap, showConsole, writeToFile;
    private ServiceConnection sConn;
    private WriteService writeServise;
    private boolean bound = false, write = false;
    private Intent intentService;
    private BroadcastReceiver mReceiver;
    private TextView textView, tvFilterValue, tvFrequency;
    private LinearLayout llChart;
    private boolean pause = false;
    private MapHelper mapHelper;
    public View mapFragment;
    public TextView tvSpeed;
    public RelativeLayout rlSpeed;
    private LinearLayout llConsole;
    private TextView tvConsole;
    private View mDecorView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private int filterValuePerSecond = 15, counter = 0; //in seconds

    private CheckBox rbX, rbY, rbZ, rbSqrt, rbLFF, cbAuto;
    private RadioGroup radioGroup;
    private SeekBar seekBarFrequency, seekBarSensativity;
    private float frequency;
    private float speed = 0f;

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
        //full screen
        getActionBar().hide();

        setContentView(R.layout.main_land);
        mDecorView = getWindow().getDecorView();

        mapHelper = new MapHelper(this);

        server = (Button) findViewById(R.id.server);
        showMap = (Button) findViewById(R.id.show_map);
        showConsole = (Button) findViewById(R.id.show_console);
        writeToFile = (Button) findViewById(R.id.write);
        textView = (TextView) findViewById(R.id.text);
        llChart = (LinearLayout) findViewById(R.id.chart);
        mapFragment = (View) findViewById(R.id.map);
        mapFragment.setVisibility(View.GONE);
        tvSpeed = (TextView) findViewById(R.id.speed);
        rlSpeed = (RelativeLayout) findViewById(R.id.rl_speed);
        llConsole = (LinearLayout) findViewById(R.id.ll_console);
        tvConsole = (TextView) findViewById(R.id.tv_console);
        cbAuto = (CheckBox) findViewById(R.id.cb_auto);
        seekBarSensativity = (SeekBar) findViewById(R.id.seek_bar);

        server.setOnClickListener(this);
        showMap.setOnClickListener(this);
        showConsole.setOnClickListener(this);
        writeToFile.setOnClickListener(this);
        findViewById(R.id.clear).setOnClickListener(this);
        findViewById(R.id.pause).setOnClickListener(this);
        findViewById(R.id.plus).setOnClickListener(this);
        findViewById(R.id.minus).setOnClickListener(this);
        findViewById(R.id.screen_shot).setOnClickListener(this);
        findViewById(R.id.show_console).setOnClickListener(this);


        radioGroup = (RadioGroup) findViewById(R.id.radio_group);

        rbX = (CheckBox) findViewById(R.id.rb_x);
        rbY = (CheckBox) findViewById(R.id.rb_y);
        rbZ = (CheckBox) findViewById(R.id.rb_z);
        rbSqrt = (CheckBox) findViewById(R.id.rb_sqrt);
        rbLFF = (CheckBox) findViewById(R.id.rb_lff);

        rbX.setOnCheckedChangeListener(this);
        rbY.setOnCheckedChangeListener(this);
        rbZ.setOnCheckedChangeListener(this);
        rbSqrt.setOnCheckedChangeListener(this);
        rbLFF.setOnCheckedChangeListener(this);

        rbLFF.performClick();


        tvFrequency = (TextView) findViewById(R.id.tv_frequency);
        tvFilterValue = (TextView) findViewById(R.id.filter_value);

        seekBarFrequency = (SeekBar) findViewById(R.id.seek_bar_frequency);
        seekBarFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frequency = progress;
                tvFrequency.setText(String.valueOf(frequency));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        frequency = seekBarFrequency.getProgress();
        tvFrequency.setText(String.valueOf(frequency));

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
        updateFilterValue();
    }





    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            if (hasFocus) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }



    private void updateFilterValue() {
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

        mapHelper.initilizeMap();

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



    private void clearGraph() {
        if (graphicalView.isChartDrawn()) {
            renderer.removeAllRenderers();
            dataSet.clear();
            devices.clear();
            graphicalView.repaint();
        }
    }




    @Override
    public void onClick(View view) {
        if (bound) {
            switch (view.getId()) {

                case R.id.server:
                    writeServise.startServer();
                    break;

                case R.id.show_map:
                    if (mapFragment.getVisibility() == View.VISIBLE) {
                        mapFragment.setVisibility(View.GONE);
                        showMap.setText("Show Map");
                    } else {
                        mapFragment.setVisibility(View.VISIBLE);
                        showMap.setText("Hide Map");
                    }
                    break;

                case R.id.pause:
                    if (pause) {
                        pause = false;
                    } else {
                        pause = true;
                    }

                    ((Button) findViewById(R.id.pause)).setText(pause ? "Start" : "Pause");
                    break;
                case R.id.clear:
                    clearGraph();

                    mapHelper.clearMarkers();
                    break;


                case R.id.minus:
                    if (filterValuePerSecond > MIN_VALUES_COUNT_PER_SECOND) {
                        filterValuePerSecond--;
                        updateFilterValue();
                    }
                    break;
                case R.id.plus:
                    if (filterValuePerSecond < MAX_VALUES_COUNT_PER_SECOND) {
                        filterValuePerSecond++;
                        updateFilterValue();
                    }
                    break;

                case R.id.screen_shot:
                    makeScreenShot();
                    break;

                case R.id.show_console:
                    if (llConsole.getVisibility() == View.VISIBLE) {
                        llConsole.setVisibility(View.GONE);
                        showConsole.setText("Show Console");
                    } else {
                        llConsole.setVisibility(View.VISIBLE);
                        showConsole.setText("Hide Console");
                    }
                    break;

                case R.id.write:
                    if (!write) {
                        writeToFile.setText("Stop write");
                        write = true;
                    } else {
                        writeToFile.setText("Write to file");
                        write = false;
                        if (bound) {
                            writeServise.stopWriteToFile();
                        }
                    }
                    break;
            }
        }
    }




    private void makeScreenShot() {
        Bitmap bitmap;
        if (mapFragment.getVisibility() == View.VISIBLE) {
            mapHelper.snapshotMap();
            return;
        } else {
            bitmap = graphicalView.toBitmap();
        }


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


                if (intent.hasExtra(SampleApplication.SENSOR)) {

                    String device = intent.getStringExtra(SampleApplication.DEVICE);


                    DeviceGraphInformation information = findDeviceOnGraph(device);
                    if (information == null) {
                        //add new device
                        information = new DeviceGraphInformation(device);
                        createSeriesAndRendersForNewDevice(information);

                        devices.add(information);

                        if (write && bound) {
                            writeServise.createFileToWrite(getModel(device));
                            String dataToWrite = "time" + "\t\t\t\t\t\t\t\t\t\t\t" + "x" + "\t\t\t\t\t" + "y" + "\t\t\t\t" + "z" + "\t\t\t\t" + "sqr" +
                                    "\t\t\t\t\t\t\t" + "lat" + "\t\t\t\t\t\t" + "lon" + "\t\t\t\t\t" + "speed" + "\n";
                            writeServise.writeToFile(getModel(device), dataToWrite);
                        }
                    }

                    long currentTime = System.currentTimeMillis();


                    if (pause) {
                        information.xSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.ySeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.zSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.sqrSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        return;
                    }


                    String allData = intent.getStringExtra(SampleApplication.SENSOR);
                    tvConsole.setText(allData);
                    String[] datas = allData.split("\n");


                    long sendingTime;

                    String data = datas[datas.length - 1];
                    String[] arr = data.split(" ", 7);
                    long lastTime = Long.valueOf(arr[0]);
                    sendingTime = currentTime - lastTime;

                    for (String currentData : datas) {
                        arr = currentData.split(" ", 7);
                        long readDataTime = Long.valueOf(arr[0]);
                        float x = Float.valueOf(arr[1]);
                        float y = Float.valueOf(arr[2]);
                        float z = Float.valueOf(arr[3]);
                        float sqr = (float) Math.sqrt(x * x + y * y + z * z);

                        long graphTime = sendingTime + readDataTime;

                        showSpeed(arr[6]);

                        //TODO:
                        float lff;

                        if (information.lffSeries.getItemCount() != 0) {
                            float lastLFF = (float) information.lffSeries.getY(information.lffSeries.getItemCount() - 1);
                            long lastLFFTime = (long) information.lffSeries.getMaxX();
                            double green = mapHelper.green_pin * frequency / 100;

                            if (graphTime - lastLFFTime > frequency
                                    && Math.abs(sqr - lastLFF) > green) {
                                lff = sqr;
                            } else {
                                lff = lastLFF;
                            }
                        } else {
                            lff = sqr;
                        }

                        try {
                            double lat, lon, speed;
                            lat = Double.valueOf(arr[4]);
                            lon = Double.valueOf(arr[5]);
                            speed = Double.valueOf(arr[6]);

                            float pit;

                            switch (radioGroup.getCheckedRadioButtonId()) {
                                case R.id.rb_x:
                                    pit = x;
                                    break;
                                case R.id.rb_y:
                                    pit = y;
                                    break;
                                case R.id.rb_z:
                                    pit = z;
                                    break;
                                case R.id.rb_lff:
                                    pit = lff;
                                    break;
                                case R.id.rb_sqrt:
                                default:
                                    pit = sqr;
                                    break;

                            }

                            validatePit(pit);

                            if (write && bound) {

                                String time = simpleDateFormat.format(System.currentTimeMillis());
                                String dataToWrite = time + "\t\t\t" + x + "\t\t\t" + y + "\t\t\t" + z + "\t\t\t" + sqr +
                                        "\t\t\t" + lat + "\t\t\t" + lon + "\t\t\t" + speed + "\n";
                                writeServise.writeToFile(getModel(device), dataToWrite);
                            }

                            mapHelper.addPoint(lat, lon, pit, speed, true);

                            /*if (information.xSeries.getItemCount() == 0){
                                for (int i = 0; i < 1000; i++){
                                    double demoLat, demoLon, demoSpeed;
                                    float demoPit;

                                    demoLat = lat + new Random().nextDouble() / 100;
                                    demoLon = lon + new Random().nextDouble() / 100;
                                    demoSpeed = speed + new Random(i).nextDouble() * 30;
                                    demoPit = new Random().nextInt(30);

                                    mapHelper.addPoint(demoLat, demoLon, demoPit, demoSpeed, false);
                                }
                            }*/

                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }

                        if (information.xSeries.getMaxX() + 1000 < graphTime) {

                            information.xSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                            information.ySeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                            information.zSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                            information.sqrSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                            information.lffSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        }

                        information.xSeries.add(graphTime, x);
                        information.ySeries.add(graphTime, y);
                        information.zSeries.add(graphTime, z);
                        information.sqrSeries.add(graphTime, sqr);

                        //TODO:
                        information.lffSeries.add(graphTime, lff);

                    }


                    renderer.setXAxisMin(System.currentTimeMillis() - 10000);
                    renderer.setXAxisMax(System.currentTimeMillis() + 500);
                    graphicalView.repaint();

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





    private void showSpeed(String speed) {
        tvSpeed.setText(speed.substring(0, speed.indexOf(".") + 2));
        this.speed = Float.valueOf(speed);
        if (cbAuto.isChecked() && this.speed > MIN_DELTA_SPEED) {
            seekBarSensativity.setProgress((int) (mapHelper.green_pin - (mapHelper.green_pin / this.speed)));
        }
    }





    private void validatePit(float pit) {

        if (counter >= 1 && pit <= mapHelper.yellow_pin) {
            counter--;
            return;
        }

        if (pit < mapHelper.green_pin) {
            rlSpeed.setBackgroundResource(R.drawable.circle_green);
            counter = 1;
        }

        if (pit >= mapHelper.green_pin && pit <= mapHelper.yellow_pin) {
            rlSpeed.setBackgroundResource(R.drawable.circle_yellow);
            counter = 15;
        }

        if (pit > mapHelper.yellow_pin) {
            rlSpeed.setBackgroundResource(R.drawable.circle_red);
            counter = 15;
        }
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

        renderer.setZoomButtonsVisible(false);
        renderer.setAntialiasing(true);

        renderer.setXAxisMin(0);
        renderer.setYAxisMin(-5);
        renderer.setYAxisMax(20);

        renderer.setAxesColor(Color.DKGRAY);
        renderer.setLabelsColor(Color.BLACK);
        renderer.setYLabelsColor(0, Color.GREEN);

        return renderer;
    }





    private XYMultipleSeriesDataset getDemoDataSet() {
        dataSet = new XYMultipleSeriesDataset();
        return dataSet;
    }




    private void createSeriesAndRendersForNewDevice(DeviceGraphInformation information) {
        try {
            createAndAddXSeriesAndRenderer(information);
            createAndAddYSeriesAndRenderer(information);
            createAndAddZSeriesAndRenderer(information);
            createAndAddSqrSeriesAndRenderer(information);
            createAndAddLFFSeriesAndRenderer(information);
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
        if (rbSqrt.isChecked()) {
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

        if (rbZ.isChecked()) {
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
        if (rbY.isChecked()) {
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

        if (rbX.isChecked()) {
            renderer.addSeriesRenderer(information.xSeriesRenderer);
            dataSet.addSeries(devices.size(), xSeries);
        }
    }

    private void createAndAddLFFSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(getRandomColor());
        r.setPointStyle(PointStyle.POINT);
        r.setFillPoints(true);
        r.setLineWidth(3f);

        information.lffSeriesRenderer = r;

        XYValueSeries lffSeries = new XYValueSeries(information.device + "-LFF");
        information.lffSeries = lffSeries;

        if (rbX.isChecked()) {
            renderer.addSeriesRenderer(information.lffSeriesRenderer);
            dataSet.addSeries(devices.size(), lffSeries);
        }
    }

    public int getRandomColor() {
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
                case R.id.rb_lff:
                    if (isChecked) {
                        seekBarFrequency.setVisibility(View.VISIBLE);
                        tvFrequency.setVisibility(View.VISIBLE);
                    } else {
                        seekBarFrequency.setVisibility(View.GONE);
                        tvFrequency.setVisibility(View.GONE);
                    }

                    series = information.lffSeries;
                    seriesRenderer = information.lffSeriesRenderer;
                    break;

                case R.id.rb_sqrt:
                    series = information.sqrSeries;
                    seriesRenderer = information.sqrSeriesRenderer;
                    break;

                default:
                    series = information.lffSeries;
                    seriesRenderer = information.lffSeriesRenderer;
                    break;
            }

            if (isChecked) {
                renderer.addSeriesRenderer(seriesRenderer);
                dataSet.addSeries(series);
            } else {
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
        private XYValueSeries lffSeries;

        private org.achartengine.renderer.XYSeriesRenderer xSeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer ySeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer zSeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer sqrSeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer lffSeriesRenderer;

        public DeviceGraphInformation(String device) {
            this.device = device;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DeviceGraphInformation)
                return this.device.equals(((DeviceGraphInformation) o).device);
            else
                return false;
        }
    }


    private String getModel(String device) {
        return device.substring(0, device.indexOf("-"));
    }

}
