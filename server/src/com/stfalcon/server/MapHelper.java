package com.stfalcon.server;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by alexandr on 22.08.14.
 */
public class MapHelper {

    private Activity activity;
    private GoogleMap googleMap;

    private float green_pin = 13;
    private float yellow_pin = 20;

    public MapHelper(Activity activity){
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
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setMyLocationEnabled(true);

        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

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

        googleMap.addMarker(options);

    }
}
