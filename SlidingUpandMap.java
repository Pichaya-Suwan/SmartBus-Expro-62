package com.example.smartbusmaptest;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.View;
import android.widget.Button;


import androidx.annotation.NonNull;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;



public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private LatLng currentlatLng;
    private static final String TAG = "BusStop";


    private LocationManager locationManager;
    private LocationListener locationListener;

    private final long MIN_TIME = 100;
    private final float MIN_DIST = 0;

    private Button button_exp;
    private BottomSheetBehavior mBottomSheetBehavior;
    private View bottomSheet;

    boolean onStart = true;
    Marker m = null;

    //private LatLng currentlatLng = new LatLng(13.7525, 100.494167);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        bottomSheet = findViewById(R.id.bottom_sheet);
        button_exp = (Button) findViewById(R.id.button_expand);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        button_exp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        //---------------------------------------------------------------------------------
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //private final LatLng current_locat;

        currentlatLng = new LatLng(13.7525, 100.494167);



    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlatLng, 5));


        //final Handler handler = new Handler();

                locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        //Log.d(TAG, "run: hello testtset123456");
                        //current_locat = new LatLng(location.getLatitude(), location.getLongitude());
                        currentlatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        if(m == null){
                            MarkerOptions marker = new MarkerOptions().position(currentlatLng);
                            marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_my_locat_test_foreground));
                            m = mMap.addMarker(marker);
                        }
                        //Log.d(TAG, "onLocationChanged: " + location.getLatitude() + "  " + location.getLongitude());
                        if(onStart){
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentlatLng,15));
                            onStart = false;
                        }
                        else{
                            m.setPosition(currentlatLng);
                        }

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }


                };

                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                try {
                    //Log.d(TAG, "run: hello testtset123456");
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
                }
                catch (SecurityException e){
                    e.printStackTrace();
                }

    /*
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentlatLng,15));
            }
        }, 100);
*/


        //LatLng shinjuku_station = new LatLng(35.689578, 139.700555);
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(shinjuku_station));

    /*
        // Add a marker in Sydney and move the camera
        LatLng shinjuku_station = new LatLng(35.689578, 139.700555);

        MarkerOptions marker = new MarkerOptions().position(shinjuku_station);
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_stop_100px));

        mMap.addMarker(marker);
        //mMap.addMarker(new MarkerOptions().position(shinjuku_station).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(shinjuku_station));

    */
        //LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //LatLng shinjuku_station = new LatLng(35.689578, 139.700555);


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Bus_Stop")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //Log.d(TAG, document.getId() + " => " + document.getData());
                                //Log.d(TAG, document.getId() + " => " + document.getGeoPoint("bs_locat").getLatitude());
                                //Log.d(TAG, document.getId() + " => " + document.getGeoPoint("bs_locat").getLongitude());
                                LatLng bus_stop = new LatLng((document.getGeoPoint("bs_locat").getLatitude()), (document.getGeoPoint("bs_locat").getLongitude()));
                                MarkerOptions marker = new MarkerOptions().position(bus_stop);
                                marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bus_stop_test222_foreground));
                                mMap.addMarker(marker);

                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });


        public void clickExpand(){

        }
}
