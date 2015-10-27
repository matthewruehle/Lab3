package com.mobileproto.dabrahamsmruehle.scavengerhunt;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.Bind;
import butterknife.ButterKnife;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HUDFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HUDFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HUDFragment extends Fragment
{
    private OnFragmentInteractionListener mListener;
    public LocationManager locationManager;
    public LocationListener locationListener;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor sharedPrefsEditor;
    public boolean isAtTarget;
    public HttpHandler httpHandler;
    @Bind(R.id.curr_clue_button) Button playCurrentClue;
    @Bind(R.id.mapview) MapView mapView;
    @Bind(R.id.take_photo_button) Button takePhotoButton;
    GoogleMap map;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HUDFragment.
     */
    public static HUDFragment newInstance()
    {
        HUDFragment fragment = new HUDFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public HUDFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
//        myGoogleApiClient = new GoogleApiClient.Builder(getActivity())
//                .addApi(LocationServices.API)
//                .build();
        httpHandler = new HttpHandler(getActivity());
        httpHandler.updatePathFromServer();
        sharedPrefs = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        sharedPrefsEditor = sharedPrefs.edit();
        View view = inflater.inflate(R.layout.fragment_hud, container, false);
        ButterKnife.bind(this, view);
        playCurrentClue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((HUDFragment.OnFragmentInteractionListener) getActivity())
                        .onFragmentInteraction(Uri.parse("https://s3.amazonaws.com/olin-mobile-proto/MVI_3140.3gp")); // can be replaced with a string button or fragment now; video ID is no longer communicated via the onFragmentInteraction URI and instead uses the sharedPreferences values.

            }
        });
        takePhotoButton.setEnabled(false);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //takePhoto();
                int currentStep = sharedPrefs.getInt("current_step", 1);
                int nextStep = currentStep + 1;
                sharedPrefsEditor.putInt("current_step", nextStep); // sets the current step to be the one we want the location/video data for.
                httpHandler.updatePathFromServer(); // has the handler query the server for the latitude/longitude/videoId combos; handler then sets the sharedPreferences for these based on the "current_step" sharedpreference.
                takePhotoButton.setEnabled(false); //re-greys it out until the next time we get location info, at least.
            }
        });
//        takePhotoButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//        public void onClick(View v) {
//                Log.d("GpsVals", "TakePhoto clicked");
//
//                Location loc = LocationServices.FusedLocationApi.getLastLocation(myGoogleApiClient);
//                if (loc == null) {
//                    Log.d("GpsVals", "TakePhoto: getLastLocation returned null");
//                } else {
//                    Log.d("GpsVals", "TakePhoto onClick: lat = " + String.valueOf(loc.getLatitude()) + " long = " + String.valueOf(loc.getLongitude()));
//                }
//            }
//        });
        Log.d("GpsVals", "got to here: HUD Fragment, about to make it handle GPS");
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location)
            {
                Log.d("GpsVals", "onLocationChanged: " + String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude()));
                checkIfClose(location);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener); // Do we really need to check for these permissions if we're putting them in the manifest xml file?
        Log.d("GpsVals", "got to here: HUD Fragment, just requested location updates.");

        int checkGooglePlayServices =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(getContext());
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {
            GooglePlayServicesUtil.getErrorDialog(
                    checkGooglePlayServices, getActivity(), 1122).show();
        }

        mapView.onCreate(savedInstanceState);

        // TODO: possibly this should be getMapAsync. right now it is null.
        map = mapView.getMap();

        // TODO: THESE LINES DO NOT WORK RIGHT NOW, BUT WE NEED THEM TO INTERACT WITH THE MAP ITSELF
        // TODO: I THINK.

        // map.getUiSettings().setMyLocationButtonEnabled(true);
        // map.setMyLocationEnabled(true);

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(this.getActivity());
        return view;
    }

    public void checkIfClose(Location location)
    {
        Location destination = new Location("SERVER");
        double currentLongitude = Double.longBitsToDouble(sharedPrefs.getLong("target_longitude", 0));
        double currentLatitude = Double.longBitsToDouble(sharedPrefs.getLong("target_latitude", 0));
        float distanceThreshold = sharedPrefs.getFloat("distance_threshold", 50); // 50 meters is the default. Not sure if this is reasonable.
        destination.setLongitude(currentLongitude);
        destination.setLatitude(currentLatitude);
        float distanceToTarget = location.distanceTo(destination);
        isAtTarget = (distanceToTarget < distanceThreshold);
        takePhotoButton.setEnabled(isAtTarget);
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    /**
     * Called when the fragment is no longer in use.  This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mapView.onDestroy();
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume()
    {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri u);
    }

}
