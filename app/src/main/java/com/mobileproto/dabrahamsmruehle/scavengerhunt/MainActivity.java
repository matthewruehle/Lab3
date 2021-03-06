package com.mobileproto.dabrahamsmruehle.scavengerhunt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity implements
        StartMenuFragment.OnFragmentInteractionListener,
        AWS_Video.OnFragmentInteractionListener,
        HUDFragment.OnFragmentInteractionListener
{

    private static final String DEBUG_TAG = "Activity_Debug";

    FragmentManager manager;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private Uri uri;
    private static final String OUTPUT_FILE_URI_KEY = "outputUri";
    public SharedPreferences sharedPrefs;
    public SharedPreferences.Editor sharedPrefsEditor;
    public HttpHandler httpHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        manager = getSupportFragmentManager();
        setContentView(R.layout.activity_main);
        switchFragment(StartMenuFragment.newInstance());
        sharedPrefs = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        sharedPrefsEditor = sharedPrefs.edit();
        httpHandler = new HttpHandler(this);

        if (savedInstanceState != null)
        {
            uri = savedInstanceState.getParcelable(OUTPUT_FILE_URI_KEY);
        }

    }

    private void switchFragment(Fragment f)
    {
        FragmentTransaction transaction = manager.beginTransaction();
        // addToBackStack requires a String object for some reason. I'm a rebel
        // and gave it null.
        transaction.replace(R.id.container, f).addToBackStack(null);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String buttonName)
    {
        if (buttonName == "continue_button")
        {
            switchFragment(HUDFragment.newInstance());
        } else if (buttonName == "take_photo_button")
        {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            uri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri); // set the image file name

            // start the image capture Intent
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        } else if (buttonName == "play_clue_button")
        {
            AWS_Video vid = new AWS_Video();
            switchFragment(vid);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putParcelable(OUTPUT_FILE_URI_KEY, uri);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(DEBUG_TAG, "Intent received");
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Image saved to:\n" +
                        uri.toString(), Toast.LENGTH_LONG).show();
                int currentStep = sharedPrefs.getInt("current_step", 1);
                int nextStep = currentStep + 1;
                sharedPrefsEditor.putInt("current_step", nextStep); // sets the current step to be the one we want the location/video data for.
                sharedPrefsEditor.commit();
                httpHandler.updatePathFromServer(); // has the handler query the server for the latitude/longitude/videoId combos; handler then sets the sharedPreferences for these based on the "current_step" sharedpreference.
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }
    }

}
