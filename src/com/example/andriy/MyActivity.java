package com.example.andriy;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.*;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public class MyActivity extends Activity {
    public final static String BROADCAST_RECEIVER_ACTION = "com.example.andriy.MyActivity.pitch_values";
    public final static String X_EXTRA = "com.example.andriy.MyActivity.x_extra";
    public final static String Y_EXTRA = "com.example.andriy.MyActivity.y_extra";
    public final static String Z_EXTRA = "com.example.andriy.MyActivity.z_extra";
    public final static String TIME_EXTRA = "com.example.andriy.MyActivity.time_extra";

    private GraphicalView graphicalView;
    private XYValueSeries xSeries, ySeries, zSeries;
    XYMultipleSeriesDataset dataSet;
    XYMultipleSeriesRenderer renderer;


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BROADCAST_RECEIVER_ACTION)) {
                long time = intent.getLongExtra(TIME_EXTRA, 0);
                float x = intent.getFloatExtra(X_EXTRA, 0f)
                        , y = intent.getFloatExtra(Y_EXTRA, 0f)
                        , z = intent.getFloatExtra(Z_EXTRA, 0f);

                xSeries.add(time, x);
                ySeries.add(time, y);
                zSeries.add(time, z);

                renderer.setXAxisMin(time - 10000);
                renderer.setXAxisMax(time + 1000);

                graphicalView.repaint();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_RECEIVER_ACTION);

        registerReceiver(receiver, filter);


        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        graphicalView = ChartFactory.getLineChartView(this, getDemoDataSet(),
                getDemoRenderer());

        layout.addView(graphicalView, new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));



        Intent intent = new Intent(MyActivity.this, AccelerometerService.class);
        intent.putExtra(AccelerometerService.EXTRA_COMMAND, AccelerometerService.START_READING_DATA);
        startService(intent);
    }



    private XYMultipleSeriesRenderer getDemoRenderer() {
        renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[] {20, 30, 15, 0});

        renderer.setZoomButtonsVisible(true);

        renderer.setAntialiasing(true);

        renderer.setXAxisMin(0);
        renderer.setYAxisMin(-5);
        renderer.setYAxisMax(20);

        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(Color.BLUE);
        r.setPointStyle(PointStyle.SQUARE);
        renderer.addSeriesRenderer(r);

        r = new XYSeriesRenderer();
        r.setPointStyle(PointStyle.CIRCLE);
        r.setColor(Color.GREEN);
        r.setFillPoints(true);
        renderer.addSeriesRenderer(r);

        r = new XYSeriesRenderer();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);

        Intent intent = new Intent(MyActivity.this, AccelerometerService.class);
        intent.putExtra(AccelerometerService.EXTRA_COMMAND, AccelerometerService.STOP_READING_DATA);
        startService(intent);
    }
}
