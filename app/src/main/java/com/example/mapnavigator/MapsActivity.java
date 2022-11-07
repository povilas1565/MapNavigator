package com.example.mapnavigator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.menus.ToolbarSettings;
import com.example.tracecallbacks.TaskCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.mapnavigator.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, TaskCallback {

    private SupportMapFragment mapFragment;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient client;
    private LocationManager locationManager;
    private Marker currentLocationMarker;
    private String route_mode;
    private String distance_unit;
    
    private final int LOCATION_REFRESH_TIME = 10000; //10 seconds to update
    private final int LOCATION_REFRESH_DISTANCE = 500; //500 meters to update
    

    ConcurrentHashMap<Marker, Integer> markers;
    ConcurrentHashMap<Polyline, Integer> polylines;
    int markerCount;

    public MapsActivity(){
        currentLocationMarker = null;
        markers = new ConcurrentHashMap<>();
        polylines = new ConcurrentHashMap<>();
        markerCount = 1;
        route_mode = "driving";
        distance_unit = "metric";
    }
    
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        client = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        
        //Check if the user has granted the appropriate permissions
        if(ActivityCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE,
                    location -> this.getCurrentLocation()
            );
        }
        else {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 44);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(googleMap -> {
            googleMap.getUiSettings().setZoomControlsEnabled(true);

            googleMap.setOnMapClickListener(latLng -> {
                Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng).title(""));

                markers.put(marker, markerCount++);

                getRoute(currentLocationMarker, marker, route_mode);
            });
        });
        
        //this.setButtons();
        //this.setSpinners();
        this.setToolbar();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {}
    
    /**
     * Runs when a user selects whether or not he wants to grant a location permission for the App.
     * @param requestCode The location permission request code
     * @param permissions The permissions requested
     * @param grantResults The result of a user's choice
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 44){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                this.getCurrentLocation();
            }
        }
    }
    
    private void getCurrentLocation(){
        @SuppressLint("MissingPermission")
        Task<Location> task = client.getLastLocation();

        task.addOnSuccessListener(location -> {
            if(location != null){
                mapFragment.getMapAsync(googleMap -> {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    currentLocationMarker = googleMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 10));
                });
            }
        });
    }

    /**
     * Get the two positions and the mode, and construct the URL for the API call.
     * @param sourcePos The position of the beginning marker
     * @param destinationPos The position of the destination marker
     * @param mode The form of traversal {Driving, Walking}
     * @return The URL
     */
    private String getRouteUrl(LatLng sourcePos,
                               LatLng destinationPos,
                               String mode) {
    
        return "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                sourcePos.latitude + "," + sourcePos.longitude + "&destination=" + destinationPos.latitude + "," + destinationPos.longitude  +
                "&mode=" + mode + "&key=" + getString(R.string.API_KEY);
    }
    
    private String getDistanceMatrixUrl(LatLng sourcePos,
                                        LatLng destinationPos,
                                        String mode) {
        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                sourcePos.latitude + "," + sourcePos.longitude + "&destinations=" +
                destinationPos.latitude + "," + destinationPos.longitude  +
                "&mode=" + mode + "&units=" + distance_unit +
                "&key=" + getString(R.string.API_KEY);
        
        return url;
    }
    /**
     * Get the URL and initiate the API call.
     * @param source The source marker
     * @param destination The destination marker
     * @param mode The form of traversal {Driving, Walking}
     */
    private void getRoute(Marker source,
                          Marker destination,
                          String mode){
        if(source == null || destination == null){
            return;
        }
        
        String route_url = getRouteUrl(source.getPosition(), destination.getPosition(), mode);
        String distance_matrix_url = getDistanceMatrixUrl(source.getPosition(), destination.getPosition(), mode);

        DirectionsAPICaller route_API_call = new DirectionsAPICaller(MapsActivity.this, new RouteParser());
        DirectionsAPICaller distance_matrix_API_call = new DirectionsAPICaller(MapsActivity.this, new DistanceMatrixParser());
        
        route_API_call.execute(route_url);
        distance_matrix_API_call.execute(distance_matrix_url);

        Log.d("MapsActivity::getRoute", route_url);
        Log.d("MapsActivity::getRoute", distance_matrix_url);
    }
    
    /**
     * Returns the polylines when the route API call is finished executing.
     * @param polylineProps
     */
    @Override
    public void onRouteDone(Object... polylineProps){
        mapFragment.getMapAsync(googleMap -> {
            polylines.put(googleMap.addPolyline((PolylineOptions) polylineProps[0]), 1);
        });
    }
    
    /**
     * Returns the time and distance for each marker route drawn.
     * @param values
     */
    @Override
    public void onDistanceMatrixDone(Object... values) {
        Map<String, String> distance_duration = (Map<String, String>)values[0];
        String text = distance_duration.get("distance") + " in " + distance_duration.get("duration");
        
        Toast.makeText(MapsActivity.this, text, Toast.LENGTH_LONG).show();
    }
    
    /*
    private void setButtons(){
        final Button clear_map_btn = findViewById(R.id.clear_map_btn);
        ClearMapBtn clear_map = new ClearMapBtn();
        
        clear_map.clearMap(clear_map_btn, markers, polylines);
    }
    
    //TODO: Refactor into Spinner class
    private void setSpinners(){
        final Spinner travel_method_dropdown = findViewById(R.id.travel_method_menu_spinner);
        
        String[] travel_methods = {"Driving", "Walking", "Bicycling", "Transit"};
    
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MapsActivity.this,
                R.layout.travel_method_list, travel_methods);
    
        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        travel_method_dropdown.setAdapter(adapter);
        travel_method_dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
        
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                route_mode = adapterView.getItemAtPosition(i).toString();
                Toast.makeText(MapsActivity.this, route_mode, Toast.LENGTH_SHORT).show();
                route_mode = route_mode.toLowerCase();
                System.out.printf("%s\n", route_mode);
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                route_mode = "driving";
            }
        });
    }
    
     */
    
    /**
     * Set the toolbar view.
     */
    private void setToolbar(){
        Toolbar main_toolbar = findViewById(R.id.main_toolbar);
        main_toolbar.setTitle(R.string.app_name);
        setSupportActionBar(main_toolbar);
    }
    
    /**
     * Create an intent when a user selects the options menu in the top toolbar menu.
     * @param menu The list of options upon selecting the top right menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater settings = getMenuInflater();
        settings.inflate(R.menu.toolbar_menu, menu);
        
        return settings != null;
    }
    
    
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent intent = result.getData();
                
                if(intent != null){
                    distance_unit = intent.getStringExtra("unit");
                    Log.d("unit:", distance_unit);
                }
            }
    );
    
    /**
     * Runs when the user selects an option from any of the top right menu options
     * @param item The item that was selected, such as "Settings"
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        
        switch(item.getItemId()){
            case R.id.main_toolbar_settings: {
                Intent settings_menu = new Intent(MapsActivity.this, ToolbarSettings.class);
                settings_menu.putExtra("unit", distance_unit);
                activityResultLauncher.launch(settings_menu);
            }
            
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }
}