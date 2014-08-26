package com.stfalcon.server;

import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;

/**
 * Created by alexandr on 22.08.14.
 */
public class MapHelper {
    private static final double MIN_LOCATION_DIFFERENCE = 0.01;

    private MyActivity activity;
    private GoogleMap googleMap;
    private SeekBar seekBar;
    private ArrayList<Marker> markers = new ArrayList<Marker>();

    public double green_pin = 13;
    public double yellow_pin = green_pin * 1.5;

    public MapHelper(MyActivity activity){
           this.activity = activity;
    }



    public void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) activity.getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(activity.getApplicationContext(),
                        "Map failed to create", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
        }
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.setMyLocationEnabled(true);

        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

            }
        });

        showValues((int)green_pin);
        seekBar = (SeekBar) activity.findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                showValues(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                repaintMarkers();
            }
        });


    }


    public void addPoint(double lat, double lon , float pit, double speed, boolean newPoint){

        if (!newPoint || needAddMarker(lat, lon)){

            MarkerOptions options =  new MarkerOptions();
            options.position(new LatLng(lat, lon));

            if (pit < green_pin){
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.green_pin));
            }

            if (pit >= green_pin && pit <= yellow_pin){
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_pin));
            }

            if (pit > yellow_pin){
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.red_pin));
            }

            options.title(String.valueOf(pit) + "|" + String.valueOf(speed));

            Marker marker = googleMap.addMarker(options);
            markers.add(marker);
        }
    }

    private boolean needAddMarker(double lat, double lon) {
        if (markers.isEmpty()){
            return true;
        } else {

            Marker lastMarker = markers.get(markers.size() - 1);

            double lastLat = lastMarker.getPosition().latitude;
            double lastLon = lastMarker.getPosition().longitude;

            if (Math.abs(lastLat - lat) > MIN_LOCATION_DIFFERENCE
                    && Math.abs(lastLon - lon) > MIN_LOCATION_DIFFERENCE)
                return true;
            else
                return false;
        }
    }


    private void repaintMarkers(){
        googleMap.clear();

        ArrayList<Marker> copyMarkers = new ArrayList<Marker>(markers);

        markers.clear();

        for (Marker marker : copyMarkers){
            try {
            String[] arr = marker.getTitle().split("|", 2);
            addPoint(marker.getPosition().latitude,
                    marker.getPosition().longitude,
                    Float.valueOf(arr[0]),
                    Double.valueOf(arr[1])
                    , false);
            } catch (NumberFormatException e){
                e.printStackTrace();
            }
        }

    }


    private void showValues(int green_pin){
        this.green_pin = green_pin;
        yellow_pin = green_pin * 1.5;

        ((TextView)activity.findViewById(R.id.green)).setText("< " + green_pin);
        ((TextView)activity.findViewById(R.id.yellow)).setText("> " + green_pin + " <" + yellow_pin);
        ((TextView)activity.findViewById(R.id.red)).setText("> " + yellow_pin);
    }
}
