package com.stfalcon.server;

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


    public void addPoint(double lat, double lon , float pit){

        MarkerOptions options =  new MarkerOptions();
        options.position(new LatLng(lat, lon));

        if (pit < green_pin){
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.green_pin));
        }

        if (pit <= green_pin && pit <= yellow_pin){
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_pin));
        }

        if (pit > yellow_pin){
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.red_pin));
        }

        options.title(String.valueOf(pit));

        Marker marker = googleMap.addMarker(options);
        markers.add(marker);
    }



    private void repaintMarkers(){

        googleMap.clear();

        int count = markers.size();
        for (int i = 0; i < count; i++){
            addPoint(markers.get(i).getPosition().latitude,
                    markers.get(i).getPosition().longitude,
                    Float.valueOf(markers.get(i).getTitle()));
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
