package com.utrack.ramya.usertracking;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final static String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;

    protected GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Location mLastKnownLocation = null;
    private CameraPosition mCameraPosition;

    private final LatLng mDefaultLocation = new LatLng(51.5074, 0.1278);
    private static final int DEFAULT_ZOOM = 15;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;

    private static final String KEY_REQUEST_LOCATION_UPDATE="request_location_update";
    private static final String KEY_LAST_LOCATION="last_location";
    private static final String KEY_CAMERA_POSITION="camera_pos";

    private static boolean mLocationPermissionGranted = false;
    private static boolean mRequestingLocationUpdate = false;

    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private SettingsClient mSettingsClient;

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    private MyLocationCallback mLocationCallback;

    private static final int COLOR_BLUE = Color.BLUE; //0x005a47d2; purple color
    private static final int POLYLINE_STROKE_WIDTH_PX = 12;
    
    private PolylineOptions mPolylineOptions;
    private ArrayList<LatLng> mPoints;
    OA
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Update the values from the Bundle.
        if (savedInstanceState != null) {
            mRequestingLocationUpdate = savedInstanceState.getBoolean(
                    KEY_REQUEST_LOCATION_UPDATE);
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LAST_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //To receive location updates.
        mLocationCallback = new MyLocationCallback();

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        mSettingsClient = LocationServices.getSettingsClient(this);

	mPolylineOptions = new PolylineOptions();

	mPoints = new ArrrayList<LatLng>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mRequestingLocationUpdate)
            startLocationUpdates();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_REQUEST_LOCATION_UPDATE, mRequestingLocationUpdate);
        if(mMap != null){
            outState.putParcelable(KEY_CAMERA_POSITION,mMap.getCameraPosition());
            outState.putParcelable(KEY_LAST_LOCATION,mLastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getLocationPermission();
        if (mLocationPermissionGranted) {
            checkSysLocSettings();
        }
    }

    private void requestLocation() {
        getDeviceLocation();
        updateLocationUI();
        startLocationUpdates();
    }

    private void getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            Location myLocation = (Location) task.getResult();

                            if (myLocation != null) {
                                updateMap(myLocation);
                                Log.i(TAG, " My Location is retrieved ");
                            } else {
                                Log.e(TAG, "Exception: %s", task.getException());
                                displayMessage(" Current Location is not available. ");
                            }
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    private void setDefaultLocation() {
        Log.i(TAG, "Current location is null. Using defaults.");

        mMap.addMarker(new MarkerOptions().position(new LatLng(mDefaultLocation.latitude, mDefaultLocation.longitude))
                .title("London"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    /**
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    private void getLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else
            mLocationPermissionGranted = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {

        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                } else {
                    displayMessage("Close the app, can't continue without user permission");
                }
            }
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                // mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void displayMessage(String str) {
        Toast.makeText(this.getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }

    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    protected void checkSysLocSettings() {
        Task<LocationSettingsResponse> result = mSettingsClient.checkLocationSettings(mLocationSettingsRequest);

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    //All location settings are statisfied.. Initialize location request here..
                    requestLocation();

                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        MapsActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.

                            break;
                    }
                    exception.printStackTrace();
                }

            }
        });
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        try {
            // new Google API SDK v11 uses getFusedLocationProviderClient(this)
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
            mRequestingLocationUpdate = true;
        } catch (SecurityException se) {
            Log.e(TAG, " Error " + se.getMessage());
        }


    }

    public class MyLocationCallback extends LocationCallback {
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null) {

                updateMap(locationResult.getLastLocation());
            }
        }
    }

    private void updateMap(Location location) {
        LatLng mLatLng;
        if (location != null) {
            if (location != mLastKnownLocation) {
                mLatLng = new LatLng(location.getLatitude(), location.getLongitude());
		mPoints.add(mLatLng);
		/*
		 * Draw polyline if the user moves than 3 yards = 10 feet.
		 * float distance = mLastKnownLocation.distanceTo(location);
		 */
		if(mLastKnownLocation != null)
			reDrawLine();
		else 
	                mMap.addMarker(new MarkerOptions().position(mLatLng).title(" MyLocation"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        mLatLng, DEFAULT_ZOOM));
                mLastKnownLocation = location;
            } else {
                displayMessage("Location hasn't changed..");
            }
        }
    }

    private void reDrawLine() {
    
	    mMap.clear();
	    mPolylineOptions.color(COLOR_ARGB_BLUE).width(POLYLINE_STROKE_WIDTH_PX).visible(true);
	    for(int i=0; i < mPoints.size(); i++)
		    mPolylineOptions.add(mPoints.get(i));
	    mMap.addPolyline(mPolylineOptions);
    }
    /**
     * To get activity result of location services.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        requestLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        displayMessage(" Current Location is not available");
                        break;
                    default:
                        break;
                }
                break;
        }
    }

}
