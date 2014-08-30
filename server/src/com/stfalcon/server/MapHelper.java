package com.stfalcon.server;

import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by alexandr on 22.08.14.
 */
public class MapHelper {
    private static final double MIN_LOCATION_DIFFERENCE = 0.000001;

    private MyActivity activity;
    private GoogleMap googleMap;
    private SeekBar seekBar;
    private ArrayList<Marker> markers = new ArrayList<Marker>();

    public double green_pin = 13;
    public double yellow_pin = green_pin * 1.5;

    public MapHelper(MyActivity activity) {
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

        showValues((int) green_pin);
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


    public void addPoint(double lat, double lon, float pit, double speed, boolean newPoint) {
        // && speed > activity.MIN_DELTA_SPEED
        if (!newPoint || needAddMarker(lat, lon)) {

            MarkerOptions options = new MarkerOptions();
            options.position(new LatLng(lat, lon));

            if (pit < green_pin) {
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.green_pin));
            }

            if (pit >= green_pin && pit <= yellow_pin) {
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_pin));
            }

            if (pit > yellow_pin) {
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.red_pin));
            }

            options.title(String.valueOf(pit) + " " + String.valueOf(speed));

            Marker marker = googleMap.addMarker(options);
            markers.add(marker);

        }
    }

    private boolean needAddMarker(double lat, double lon) {
        if (markers.isEmpty()) {
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
        int count = markers.size();

        Marker marker;

        for (int i = 0; i < count; i++){
            marker = markers.get(i);
            try {
                String[] arr = marker.getTitle().split(" ", 2);
                float pit = Float.valueOf(arr[0]);

                if (pit < green_pin){
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.green_pin));
                }

                if (pit >= green_pin && pit <= yellow_pin){
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_pin));
                }

                if (pit > yellow_pin){
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.red_pin));
                }

            } catch (NumberFormatException e){
                e.printStackTrace();
            }
        }
    }


    private void showValues(int green_pin) {
        this.green_pin = green_pin;
        yellow_pin = green_pin * 1.5;

        ((TextView) activity.findViewById(R.id.green)).setText("< " + green_pin);
        ((TextView) activity.findViewById(R.id.yellow)).setText("> " + green_pin + " <" + yellow_pin);
        ((TextView) activity.findViewById(R.id.red)).setText("> " + yellow_pin);

    }

    public void clearMarkers() {
        googleMap.clear();

        markers.clear();
    }




    public void snapshotMap() {
        GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
            Bitmap bitmap;

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                // TODO Auto-generated method stub
                bitmap = snapshot;
                try {
                    File directory = new File("/sdcard/AccelData/ScreenShots/");
                    directory.mkdirs();

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String time = simpleDateFormat.format(System.currentTimeMillis());

                    File jpegPictureFile = new File("/sdcard/AccelData/ScreenShots/" + time + "_graph.jpeg");
                    jpegPictureFile.createNewFile();
                    FileOutputStream pictureOutputStream = new FileOutputStream(jpegPictureFile);

                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, pictureOutputStream);

                    MediaStore.Images.Media.insertImage(activity.getContentResolver(), jpegPictureFile.getPath(), null
                            , "Graph Screen Shot");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        googleMap.snapshot(callback);

    }

}
