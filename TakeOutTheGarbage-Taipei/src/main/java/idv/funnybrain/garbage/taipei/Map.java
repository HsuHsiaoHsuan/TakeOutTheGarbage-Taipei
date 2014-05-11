package idv.funnybrain.garbage.taipei;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Freeman on 2014/3/20.
 */
public class Map extends SherlockFragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, android.support.v4.app.LoaderManager.LoaderCallbacks<List<GarbageObject>>,
        LocationListener {
    private static final boolean D = true;
    private static final String TAG = "Map";

    private GoogleMap mMap;

    private final int garbageLoaderId = 0;

    //private LocationClient mLocationClient;

    private SpinnerAdapter spinnerAdapter;

    private ActionBar.OnNavigationListener navigationListener;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;
    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;
    // Handle to SharedPreferences for this app
    SharedPreferences mPrefs;
    // Handle to a SharedPreferences editor
    SharedPreferences.Editor mEditor;
    /*
     * Note if updates have been turned on. Starts out as "false"; is set to "true" in the
     * method handleRequestSuccess of LocationUpdateReceiver.
     *
     */
    boolean mUpdatesRequested = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.map);

        getSupportActionBar().setTitle("");

        spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.distict_taipei_multi_lan, android.R.layout.simple_spinner_dropdown_item);

        navigationListener = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                System.out.println("select: " + itemPosition);
                if(itemPosition == 0) {
                    (new Utils()).setSelectedDist("");
                } else
                if(itemPosition == 1) {
                    final Location location = mLocationClient.getLastLocation();
                    if(mLocationClient.isConnected() && (location != null)) {
                        if(D) { Log.d(TAG, "onNavigationItemSelected: " + location.getLatitude() + "," + location.getLongitude()); }
                    } else {
                        if(D) { Log.d(TAG, "no Connection!"); }
                        // no location
                        return true;
                    }
                    final LatLng nowLatLng = new LatLng(Double.valueOf(location.getLatitude()), Double.valueOf(location.getLongitude()));
                    (new Utils()).setSelectedDist("");
                    LinearLayout dialog = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_picker_distance, null);
                    final NumberPicker np_1 = (NumberPicker) dialog.findViewById(R.id.picker_num_1);
                    final NumberPicker np_2 = (NumberPicker) dialog.findViewById(R.id.picker_num_2);
                    final NumberPicker np_3 = (NumberPicker) dialog.findViewById(R.id.picker_num_3);
                    final Button dist = (Button) dialog.findViewById(R.id.bt_distance);
                    np_1.setMinValue(0);
                    np_2.setMinValue(0);
                    np_3.setMinValue(0);
                    np_1.setMaxValue(9);
                    np_2.setMaxValue(9);
                    np_3.setMaxValue(9);
                    dist.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(dist.getText().toString().equals(getString(R.string.km))) {
                                dist.setText(R.string.m);
                                np_1.setVisibility(View.VISIBLE);
                                np_1.setVisibility(View.VISIBLE);

                            } else {
                                dist.setText(R.string.km);
                                np_1.setValue(0);
                                np_1.setVisibility(View.GONE);
                                np_2.setValue(0);
                                np_2.setVisibility(View.GONE);
                            }
                        }
                    });

                    new AlertDialog.Builder(Map.this).setView(dialog)
                                                     .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                         @Override
                                                         public void onClick(DialogInterface dialog, int which) {
                                                             mMap.clear();
                                                             int range = np_1.getValue() * 100 + np_2.getValue() * 10 + np_3.getValue();
                                                             if(D) { Log.d(TAG, "distance: " + range + " " + dist.getText()); }

                                                             int range_tmp = range;
                                                             if(dist.getText().equals(getString(R.string.km))) {
                                                                 range_tmp *= 1000;
                                                             }

                                                             Circle circle = mMap.addCircle(new CircleOptions()
                                                                     .center(nowLatLng)
                                                                     .radius(range_tmp)
                                                                     .strokeColor(Color.BLACK)
                                                                     .strokeWidth(1)
                                                                     .fillColor(Color.argb(30, 0, 255, 0)));
                                                             mMap.moveCamera(CameraUpdateFactory.newLatLng(nowLatLng));
                                                             resetListMode(range + " " + dist.getText());
                                                             new RangeDownloadTask().execute(String.valueOf(range_tmp));
                                                         }
                                                     })
                                                     .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                         @Override
                                                         public void onClick(DialogInterface dialog, int which) {
                                                         }
                                                     })
                                                     .show();
                } else {
                    String[] district_in_TC = getResources().getStringArray(R.array.distict_taipei);
                    String selected = district_in_TC[itemPosition-2];
                    if(D) { Log.d(TAG, "onNavigationItemSelected: " + selected); }
                    (new Utils()).setSelectedDist(selected);
                    getSupportLoaderManager().restartLoader(garbageLoaderId, null, Map.this).forceLoad();
                    Log.d(TAG, "hi: " + spinnerAdapter.getItem(0).toString());
                    //getActionBar().setListNavigationCallbacks(spinnerAdapter, this);
                    resetListMode(selected);
                    String[] lat_array = getResources().getStringArray(R.array.district_taipei_latitude);
                    String[] lng_array = getResources().getStringArray(R.array.district_taipei_longtitude);
                    if(D) { Log.d(TAG, "selected LatLng: " + lat_array[itemPosition-2] + ", " + lng_array[itemPosition-2]); }
                    LatLng tmpLatLng = new LatLng(Double.valueOf(lat_array[itemPosition-2]),
                                                  Double.valueOf(lng_array[itemPosition-2]));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(tmpLatLng, 13.5f));
                    (new Utils()).setSelectedDist(selected);
                }
                return true;
            }
        };

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        //getActionBar().setListNavigationCallbacks(spinnerAdapter, navigationListener);
        resetListMode();

        getSupportLoaderManager().initLoader(garbageLoaderId, null, this).forceLoad();
//        SlidingMenu slidingMenu = new SlidingMenu(this);
//        slidingMenu.setMode(SlidingMenu.LEFT);
//        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
//        slidingMenu.setShadowDrawable(R.drawable.drawer_shadow);
//        slidingMenu.setBehindOffset(90);
//        slidingMenu.setFadeDegree(0.35f);
//        slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
//        slidingMenu.setMenu(R.layout.map_sliding_menu_left);

        // Create a new global location parameters object
        // mLocationRequest = LocationRequest.create();
        /*
         * Set the update interval
         */
        // mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
        // Use high accuracy
        // mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the interval ceiling to one minute
        // mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        // Note that location updates are off until the user turns them on
        mUpdatesRequested = false;
        // Open Shared Preferences
        mPrefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        // Get an editor
        mEditor = mPrefs.edit();
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
    }

    private void resetListMode() {
        getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, navigationListener);
    }

    private void resetListMode(String input) {
        ArrayList<String> tmpList = new ArrayList<String>();
        tmpList.add(input);
        String[] tmpArray = getResources().getStringArray(R.array.distict_taipei_multi_lan);
        for(int x=1; x<tmpArray.length; x++) {
            tmpList.add(tmpArray[x]);
        }
        tmpList.toArray(tmpArray);
        ArrayAdapter<String> tmpAdapter = new ArrayAdapter<String>(Map.this, android.R.layout.simple_spinner_dropdown_item, tmpList);
        getSupportActionBar().setListNavigationCallbacks(tmpAdapter, navigationListener);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        MenuItem menu_startTime = menu.add(0, 0, 0, "StartTime");
        MenuItem menu_startQuery = menu.add(0, 1, 1, "QUERY");
        MenuItem menu_endTime = menu.add(0, 2, 2, "EndTime");

        menu_startTime.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu_startQuery.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu_endTime.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu_startTime.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DialogFragment newFragment = new TimePickerFragment();
                newFragment.show(getSupportFragmentManager(), "Start Time");
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onStop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }
        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onPause() {
        // Save the current setting for updates
        mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, mUpdatesRequested);
        mEditor.commit();

        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

        if(! (new Utils()).getSelectedDist().equals("")) {
            getSupportLoaderManager().restartLoader(garbageLoaderId, null, this).forceLoad();
        }

    }

    private void setUpMapIfNeeded() {
        if(mMap == null) {
//            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            FragmentManager fm = getSupportFragmentManager();
            if(fm == null) { Log.d(TAG, "----> fragment manager null"); }
            SupportMapFragment mf = (SupportMapFragment) fm.findFragmentById(R.id.map);
            if(mf == null) { Log.d(TAG, "----> support map fragment null"); }
            //mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            mMap = mf.getMap();
            if(mMap == null) { Log.d(TAG, "----> google map null"); }
            if(mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // 捷運大直站 （預設）
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(25.079224, 121.547041), 11.0f));
    }

    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case SherlockFragmentActivity.RESULT_OK:

                        // Log the result
                        //Log.d(LocationUtils.TAG, getString(R.string.resolved));

                        // Display the result
                        //mConnectionState.setText(R.string.connected);
                        //mConnectionStatus.setText(R.string.resolved);
                        break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        //Log.d(LocationUtils.TAG, getString(R.string.no_resolution));

                        // Display the result
                        //mConnectionState.setText(R.string.disconnected);
                        //mConnectionStatus.setText(R.string.no_resolution);

                        break;
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                //Log.d(LocationUtils.TAG, getString(R.string.unknown_activity_request_code, requestCode));

                break;
        }
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            //Log.d(LocationUtils.TAG, getString(R.string.play_services_available));
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), LocationUtils.TAG);
            }
            return false;
        }
    }

    /**
     * Invoked by the "Get Location" button.
     *
     * Calls getLastLocation() to get the current location
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void getLocation(View v) {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            // Display the current location in the UI
            //mLatLng.setText(LocationUtils.getLatLng(this, currentLocation));
        }
    }

    /**
     * Invoked by the "Start Updates" button
     * Sends a request to start location updates
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void startUpdates(View v) {
        mUpdatesRequested = true;

        if (servicesConnected()) {
            startPeriodicUpdates();
        }
    }

    /**
     * Invoked by the "Stop Updates" button
     * Sends a request to remove location updates
     * request them.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void stopUpdates(View v) {
        if(D) { Log.d(TAG, "stopUpdates"); }
        mUpdatesRequested = false;

        if (servicesConnected()) {
            stopPeriodicUpdates();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if(D) { Log.d(TAG, "onConnected"); }
        //mConnectionStatus.setText(R.string.connected);

        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
    }

    @Override
    public void onDisconnected() {
        if(D) { Log.d(TAG, "onDisconnected"); }
        //mConnectionStatus.setText(R.string.disconnected);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(D) { Log.d(TAG, "onConnectionFailed"); }
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }


    @Override
    public Loader<List<GarbageObject>> onCreateLoader(int id, Bundle args) {
        if(D) { Log.d(TAG, "onCreateLoader"); }
        return new GarbageCursorLoader(this);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<List<GarbageObject>> loader, List<GarbageObject> data) {
        if(D) { Log.d(TAG, "onLoadFinished: " + data.size()); }
        if(data.size() == 0) {

        }
        mMap.clear();
        for (GarbageObject go : data) {
            if(D) {
                Log.d(TAG, "onLoadFinished: " + go.getADDRESS() + " " + go.getLAT() + "~" + go.getLNG());
            }
            LatLng tmpLatLng = new LatLng(Double.valueOf(go.getLAT()), Double.valueOf(go.getLNG()));
            String notice = "";
            if(go.getNOTICE() != null) { notice = go.getNOTICE(); }

            mMap.addMarker(new MarkerOptions().position(tmpLatLng)
                    .title(go.getADDRESS())
                    .snippet(go.getSTART_TIME() + " ~ " + go.getEND_TIME() + "\n" + notice)
                    .icon(BitmapDescriptorFactory.defaultMarker())
                    .draggable(false));
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom());
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<List<GarbageObject>> loader) {
        if(D) { Log.d(TAG, "onLoaderReset"); }
    }

    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {
        if(D) { Log.d(TAG, "onLocationChanged"); }
        // Report to the UI that the location was updated
        //mConnectionStatus.setText(R.string.location_updated);

        // In the UI, set the latitude and longitude to the value received
        //mLatLng.setText(LocationUtils.getLatLng(this, location));
    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {
        if(D) { Log.d(TAG, "startPeriodicUpdates"); }
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        //mConnectionState.setText(R.string.location_requested);
    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        if(D) { Log.d(TAG, "setPeriodicUpdates"); }
        mLocationClient.removeLocationUpdates(this);
        //mConnectionState.setText(R.string.location_updates_stopped);
    }

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {
        if(D) { Log.d(TAG, "showErrorDialog"); }

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), LocationUtils.TAG);
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private class RangeDownloadTask extends AsyncTask<String, Void, List<GarbageObject>> {
        @Override
        protected List<GarbageObject> doInBackground(String... params) {
            List<GarbageObject> result = new ArrayList<GarbageObject>();
            int radius = Integer.valueOf(params[0]);
            DBHelperTaipei dbHelperTaipei = new DBHelperTaipei(Map.this);
            //dbHelperTaipei
            Location lastLocation = mLocationClient.getLastLocation();
            if (mLocationClient.isConnected() && (lastLocation != null)) {
                result = (new DBHelperTaipei(Map.this)).queryGarbageByDistance(lastLocation.getLatitude(), lastLocation.getLongitude(), radius);
            } else {
                return null;
            }
            return result;
        }


        @Override
        protected void onPostExecute(List<GarbageObject> garbageObjects) {
            super.onPostExecute(garbageObjects);

            if((garbageObjects != null) && garbageObjects.size() > 0) {
                if(D) { Log.d(TAG, "RangeDownloadTask: onPostExecute, garbageObjects != null, count:" + garbageObjects.size() ); }
                //mMap.clear();
                for(GarbageObject gb : garbageObjects) {
                    if(D) { Log.d(TAG, "GarbageObject, Lat: " + gb.getLAT() + ", Lng: " + gb.getLNG()); }
                    LatLng tmpLatLng = new LatLng(Double.valueOf(gb.getLAT()), Double.valueOf(gb.getLNG()));
                    String notice = "";
                    if(gb.getNOTICE() != null) { notice += gb.getNOTICE(); }
                    mMap.addMarker(new MarkerOptions().position(tmpLatLng)
                                                      .title(gb.getADDRESS())
                                                      .snippet(gb.getSTART_TIME() + " ~ " + gb.getEND_TIME() + "\n" + notice)
                                                      .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                                                      .draggable(false));

//                    mMap.addMarker(new MarkerOptions().position(new LatLng(25.03265, 121.518223))
//                            .title("test")
//                            .snippet("test")
//                            .icon(BitmapDescriptorFactory.defaultMarker())
//                            .draggable(false));
                }

            }
        }
    }
}
