package com.example.smartbusmaptest;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;

import androidx.fragment.app.FragmentActivity;

import com.example.smartbusmaptest.MapD.DirectionsParser;
import com.example.smartbusmaptest.MapD.TaskLoadedCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.google.firebase.database.FirebaseDatabase;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng currentlatLng;
    private static final String TAG = "BusStop";


    private LocationManager locationManager;
    private LocationListener locationListener;

    private final long MIN_TIME = 100;
    private final float MIN_DIST = 0;

    //-----------------------------------------------------------------------------------------------------------------------------------------------
    private Button button_exp, bus1, bus2, bus3, cleanL;
    private BottomSheetBehavior mBottomSheetBehavior;
    private View bottomSheet;
    private MarkerOptions place1,place2;
    private static final int LOCATION_REQUEST = 500;
    private ArrayList<MarkerOptions> bStop = new ArrayList<MarkerOptions>(5);
    private TextView text;
    private Double dist;
    private int tbArrive;
    private String distance;
    private ArrayList<Polyline> points = new ArrayList();
    private Timer timeBtn1,timeBtn2,timeBtn3,timeCoppy;
    private TimerTask timeTaskBtn1,timeTaskBtn2,timeTaskBtn3,timerTaskCoppy;

    //-----------------------------------------------------------------------------------------------------------------------------------------------
    // Route
    String[] route = {"13.12399372712117%2C100.91892480446943","13.124009181197483%2C100.91890827313979"};

    boolean onStart = true;
    Marker m = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //private final LatLng current_locat;
        currentlatLng = new LatLng(13.7525, 100.494167);

        //-------------------------------------------Sliding up-----------------------------------------------//

        bottomSheet = findViewById(R.id.bottom_sheet);
        button_exp = (Button) findViewById(R.id.button_expand);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        button_exp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        //-----------------------------------------Direction-------------------------------------------------//

        //*********************************************************************************************************************
        //Main Route

        /*String url = "https://maps.googleapis.com/maps/api/directions/json?origin=13.12399372712117%2C100.91892480446943
        &destination=13.124009181197483%2C100.91890827313979
        &mode=walking
        &waypoints=13.117695964354631%2C100.92047562299139
        %7C13.115360658190903%2C100.92288873795512
        %7C13.117698833139764%2C100.92043178084396
        %7Cvia:13.1192273%2C100.9188043
        %7Cvia:13.1202636%2C100.9185936
        %7Cvia:13.12392712071409%2C100.9187391934786
        &key=AIzaSyDldobztiCXc6nAE-xlOPQXNF6c_Vvhkfc";*/

        // We assume speed of bus is 30 km/h
        //*********************************************************************************************************************
        // Clear button will reset all things on display
        cleanL = findViewById(R.id.clearL);
        cleanL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timeCoppy != null & timerTaskCoppy != null){
                    timeCoppy.cancel();
                    timerTaskCoppy.cancel();
                    for (Polyline line : points){
                        line.remove();
                    }
                    points.clear();
                    text.setText("Choose the Bus stop\n\nDistance : 0 \nTime : 0 ");
                }
            }
        });

        bus1 = findViewById(R.id.bus1);
        text = findViewById(R.id.show_up);
        bus1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Update the locate of bus with timer
                if(timerTaskCoppy != null & timeCoppy != null){
                    timeCoppy.cancel();
                    timerTaskCoppy.cancel();
                }
                if(timeBtn1 != null){
                    timeCoppy.cancel();
                }
                timeBtn1 = new Timer();
                timeTaskBtn1 = new TimerTask() {
                    @Override
                    public void run() {
                        place1 = bStop.get(1); // ---get bus stop --- //
                        String url = getRequestUrl(place1.getPosition(), place2.getPosition());

                        // Make lines from each locate and Calculate distance and time to arrive
                        TaskRequestDirections taskRequestDirections = new TaskRequestDirections(); taskRequestDirections.execute(url);

                        //text.setText("Distance : "+ distance + " km.\n\nTime : "+timeBusArrive+ " s");
                        if(dist != null){
                            if(dist >= 1000){
                                if(tbArrive < 60)
                                    text.setText("Bus Stop 1\n\nDistance : "+ Math.round(dist/1000) + " km.\nTime : "+tbArrive+ " s.");
                                else if(tbArrive >= 60)
                                    text.setText("Bus Stop 1\n\nDistance : "+ Math.round(dist/1000) + " km.\nTime : "+(tbArrive/60)+ " m.");

                            }
                            else if(dist < 1000){
                                if(tbArrive < 60)
                                    text.setText("Bus Stop 1\n\nDistance : "+ Math.round(dist) + " meter\nTime : "+tbArrive+ " s.");
                                else if(tbArrive >= 60)
                                    text.setText("Bus Stop 1\n\nDistance : "+ Math.round(dist) + " meter\nTime : "+(tbArrive/60)+ " m.");

                            }
                        }
                    }
                };
                timeBtn1.schedule(timeTaskBtn1,1000,2000);
                timeCoppy = timeBtn1;
                timerTaskCoppy = timeTaskBtn1;
            }
        });

        bus2 = findViewById(R.id.bus2);
        bus2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timerTaskCoppy != null & timeCoppy != null){
                    timeCoppy.cancel();
                    timerTaskCoppy.cancel();
                }
                if(timeBtn2 != null){
                    timeBtn2.cancel();
                }
                timeBtn2 = new Timer();
                timeTaskBtn2 = new TimerTask() {
                    @Override
                    public void run() {
                        place1 = bStop.get(0); // ---get bus stop --- //
                        String url = getRequestUrl(place1.getPosition(), place2.getPosition());

                        // Make lines from each locate and Calculate distance and time to arrive
                        TaskRequestDirections taskRequestDirections = new TaskRequestDirections(); taskRequestDirections.execute(url);

                        if(dist != null){
                            if(dist >= 1000){
                                if(tbArrive < 60)
                                    text.setText("Bus Stop 2\n\nDistance : "+ Math.round(dist/1000) + " km.\nTime : "+tbArrive+ " s.");
                                else if(tbArrive >= 60)
                                    text.setText("Bus Stop 2\n\nDistance : "+ Math.round(dist/1000) + " km.\nTime : "+(tbArrive/60)+ " m.");

                            }
                            else if(dist < 1000){
                                if(tbArrive < 60)
                                    text.setText("Bus Stop 2\n\nDistance : "+ Math.round(dist) + " meter\nTime : "+tbArrive+ " s.");
                                else if(tbArrive >= 60)
                                    text.setText("Bus Stop 2\n\nDistance : "+ Math.round(dist) + " meter\nTime : "+(tbArrive/60)+ " m.");

                            }
                        }
                    }
                };
                timeBtn2.schedule(timeTaskBtn2,1000,2000);
                timeCoppy = timeBtn2;
                timerTaskCoppy = timeTaskBtn2;

            }
        });

        bus3 = findViewById(R.id.bus3);
        bus3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timerTaskCoppy != null & timeCoppy != null){
                    timeCoppy.cancel();
                    timerTaskCoppy.cancel();
                }
                if(timeBtn3 != null){
                    timeBtn3.cancel();
                }
                timeBtn3 = new Timer();
                timeTaskBtn3 = new TimerTask() {
                    @Override
                    public void run() {
                        place1 = bStop.get(2); // ---get bus stop --- //
                        String url = getRequestUrl(place1.getPosition(), place2.getPosition());

                        // Make lines from each locate and Calculate distance and time to arrive
                        TaskRequestDirections taskRequestDirections = new TaskRequestDirections(); taskRequestDirections.execute(url);

                        if(dist != null){
                            if(dist >= 1000){
                                if(tbArrive < 60)
                                    text.setText("Bus Stop 3\n\nDistance : "+ Math.round(dist/1000) + " km.\nTime : "+tbArrive+ " s.");
                                else if(tbArrive >= 60)
                                    text.setText("Bus Stop 3\n\nDistance : "+ Math.round(dist/1000) + " km.\nTime : "+(tbArrive/60)+ " m.");

                            }
                            else if(dist < 1000){
                                if(tbArrive < 60)
                                    text.setText("Bus Stop 3\n\nDistance : "+ Math.round(dist) + " meter\nTime : "+tbArrive+ " s.");
                                else if(tbArrive >= 60)
                                    text.setText("Bus Stop 3\n\nDistance : "+ Math.round(dist) + " meter\nTime : "+(tbArrive/60)+ " m.");

                            }
                        }
                    }
                };
                timeBtn3.schedule(timeTaskBtn3,1000,2000);
                timeCoppy = timeBtn3;
                timerTaskCoppy = timeTaskBtn2;

            }
        });
        //-----------------------------------------------------------------------------------------------------
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

    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlatLng, 5));

        //---------------------
        Log.d("mylog", "Added Markers");

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //currentlatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.setMyLocationEnabled(true);

                /*if (m == null) {
                    MarkerOptions marker = new MarkerOptions().position(currentlatLng);
                    // take the locate on advice
                    // place2 = new MarkerOptions().position(currentlatLng);

                    marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_my_locat_test_foreground));
                    m = mMap.addMarker(marker);
                }
                if (onStart) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentlatLng, 15));
                    onStart = false;
                } else {
                    m.setPosition(currentlatLng);
                    // place2 = new MarkerOptions().position(currentlatLng);
                }*/
            }

            @Override
            public void onProviderDisabled(String provider) { }

            @Override
            public void onProviderEnabled(String provider) { }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

        };

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        /** Firebase marker bus stop
         * Add marker that is Bus Stop**/
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Bus_Stop")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int a = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                LatLng bus_stop = new LatLng((document.getGeoPoint("bs_locat").getLatitude()), (document.getGeoPoint("bs_locat").getLongitude()));
                                MarkerOptions marker = new MarkerOptions().position(bus_stop);
                                marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bus_locat_test_foreground));

                                mMap.addMarker(marker);
                                bStop.add(marker);
                                MarkerOptions b = bStop.get(a);
                                System.out.println(b.getPosition()+" "+a);
                                a++;


                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });

        /** Firebase Realtime Database Fetch Bus_Location
         *   implementation 'com.google.firebase:firebase-database:19.2.1' **/

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("bus_1/LatLng");

        myRef.addValueEventListener(new ValueEventListener() {
            Marker busMarker = null;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                //Log.d(TAG, "Value is: " + value);
                String[] latLngOfValue = value.split("/");
                //Log.d(TAG, "Value Lat is: " + latLngOfValue[0]);
                //Log.d(TAG, "Value Lng is: " + latLngOfValue[1]);

                Double dLat = Double.parseDouble(latLngOfValue[0]);
                Double dLng = Double.parseDouble(latLngOfValue[1]);

                Log.d(TAG, "Value dLat is: " + dLat);
                Log.d(TAG, "Value dLng is: " + dLng);

                LatLng busLocat = new LatLng(dLat, dLng);

                if(busMarker == null){
                    MarkerOptions marker = new MarkerOptions().position(busLocat);

                    //take the location from databases
                    place2 = new MarkerOptions().position(busLocat);

                    marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bus_locat_new_oldtone_foreground));
                    busMarker = mMap.addMarker(marker);
                }
                else{
                    busMarker.setPosition(busLocat);
                    place2 = new MarkerOptions().position(busLocat);
                }

            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private String getRequestUrl(LatLng origin, LatLng dest) {
        //Value of origin
        String str_org = "origin=" + origin.latitude + "," + origin.longitude;
        //Value of destination
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        //Set value enable the sensor
        String sensor = "sensor=false";
        //Mode for find direction
        String mode = "mode=walking";
        //Build the full param
        String param = str_org + "&" + str_dest + "&" + sensor + "&" + mode;
        //Output format
        String output = "json";
        //Create url to request
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param + "&key=" + getString(R.string.google_maps_key);
        System.out.println(url);
        return url;
    }

    private String requestDirection(String reqUrl) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            //Get the response result
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
                break;
        }
    }
    //Get the url map
    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Parse json here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }
    // Make JSON
    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> route1 = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                route1 = directionsParser.parse(jsonObject);

                //-------------------------------------------------------------------------------------------------------------//
                // Calculate diatance and Time to arrive
                JSONArray array = jsonObject.getJSONArray("routes");

                JSONObject routes = array.getJSONObject(0);

                JSONArray legs = routes.getJSONArray("legs");

                JSONObject steps = legs.getJSONObject(0);
                JSONObject distance1 = steps.getJSONObject("distance");

                // To calculate and get distance
                Log.i("Distance", distance1.toString());

                dist = Double.parseDouble(distance1.getString("text").replaceAll("[^\\.0123456789]",""));
                if(dist < 5)
                    dist = dist*1000;
                distance = distance1.getString("text");
                System.out.println(distance);

                // Bus speed is 30 km convert ro ms equal 8.333334
                tbArrive = (int)Math.round(dist/8.3);

                //--------------------------------------------------------------------------------------------------------------//


            } catch (JSONException e) {
                e.printStackTrace();
            }
            return route1;
        }
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            //Get list route and display it into the map
            ArrayList locate = new ArrayList();
            if(points != null){
                for (Polyline line : points){
                    line.remove();
                }
                points.clear();
            }

            for (List<HashMap<String, String>> path : lists) {

                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));
                    locate.add(new LatLng(lat, lon));
                }
                points.add(mMap.addPolyline(new PolylineOptions().addAll(locate).width(10).color(Color.DKGRAY)));

            }
        }
    }
}
