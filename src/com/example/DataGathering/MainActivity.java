package com.example.DataGathering;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.*;
import java.util.*;

/** @noinspection FieldCanBeLocal, ResultOfMethodCallIgnored, Convert2Lambda */
public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private File file;
    private File path;
    private PowerManager.WakeLock wakeLock;
    private int typeOfRecording;
    private NumberPicker np;
    private List<View> beforeAndAfterRecordingElements;
    private List<View> duringRecordingElements;
    //Global parameters
    private int START_TO_GO_DELAY = 10000;
    private int NUMBERPICKER_MINIMUM_VALUE = 15;
    private int NUMBERPICKER_MAXIMUM_VALUE = 60;
    private int NUMBERPICKER_DEFAULT_VALUE = 30;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Setup Lists
        beforeAndAfterRecordingElements = new ArrayList<>();
        duringRecordingElements = new ArrayList<>();

        //Setup Accelerometer
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Setup wakelock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pwl");
        //Setup number picker
        np = (NumberPicker) findViewById(R.id.numberPicker1);
        np.setMinValue(NUMBERPICKER_MINIMUM_VALUE);
        np.setMaxValue(NUMBERPICKER_MAXIMUM_VALUE);
        np.setValue(NUMBERPICKER_DEFAULT_VALUE);
        np.setWrapSelectorWheel(false);
        beforeAndAfterRecordingElements.add(np);
        //Setup buttons
        Button b1 = (Button) findViewById(R.id.button1);
        beforeAndAfterRecordingElements.add(b1);
        Button b2 = (Button) findViewById(R.id.button2);
        beforeAndAfterRecordingElements.add(b2);
        Button b3 = (Button) findViewById(R.id.button3);
        beforeAndAfterRecordingElements.add(b3);
        //Setup textviews
        TextView t1 = (TextView) findViewById(R.id.textview1);
        beforeAndAfterRecordingElements.add(t1);
        TextView t2 = (TextView) findViewById(R.id.textview2);
        beforeAndAfterRecordingElements.add(t2);

        TextView t3 = (TextView) findViewById(R.id.textview3);
        duringRecordingElements.add(t3);
        t3.setVisibility(View.INVISIBLE);
    }

    public void startStillRecording(View v){
        typeOfRecording = 1;
        setupRecording();
    }

    public void startLowenergyRecording(View v){
        typeOfRecording = 2;
        setupRecording();
    }

    public void startHighenergyRecording(View v){
        typeOfRecording = 3;
        setupRecording();
    }

    private void setupRecording(){
        wakeLock.acquire();
        hideButtonsAndShowTextView();
        int time = (int) System.currentTimeMillis();
        path = getDownloadsDir();
        file = new File(path+"/test"+time+".arff");

        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if(isSdReadable()) {
                FileOutputStream fos = new FileOutputStream(file,true);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.append("@RELATION action \r\n");
                osw.append("@ATTRIBUTE  x       NUMERIC \r\n");
                osw.append("@ATTRIBUTE  y       NUMERIC \r\n");
                osw.append("@ATTRIBUTE  z       NUMERIC \r\n");
                osw.append("@ATTRIBUTE  class   {still, lowenergy, highenergy} \r\n");
                osw.append("@DATA \r\n");
                osw.flush();
                osw.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                playSound(R.raw.gogogo);
                mSensorManager.registerListener(getEventListener(), mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                Timer timer = new Timer();

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        playSound(R.raw.thatsprettymuchit);
                        runOnUiThread(new Runnable() {
                                          @Override
                                          public void run() {
                                              hideTextViewAndDisplayButtons();
                                          }
                                      });
                        mSensorManager.unregisterListener(getEventListener());
                        getWakeLock().release();
                    }
                }, np.getValue()*1000);
            }
        }, START_TO_GO_DELAY);
    }

    private Activity getMainActivity() {
        return this;
    }

    private void hideTextViewAndDisplayButtons() {
        for(View v : beforeAndAfterRecordingElements)
            v.setVisibility(View.VISIBLE);
        for(View v : duringRecordingElements)
            v.setVisibility(View.INVISIBLE);
    }

    private void hideButtonsAndShowTextView() {
        for(View v : beforeAndAfterRecordingElements)
            v.setVisibility(View.INVISIBLE);
        for(View v : duringRecordingElements)
            v.setVisibility(View.VISIBLE);
    }

    public PowerManager.WakeLock getWakeLock(){
        return this.wakeLock;
    }

    public void playSound(int uri){
        MediaPlayer mp = MediaPlayer.create(getActivityContext(), uri);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
    }

    public SensorEventListener getEventListener(){
        return this;
    }

    public Context getActivityContext(){
        return this;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() != Sensor.TYPE_ACCELEROMETER){
            return;
        }

        String output = getOutput(event);

        try {
            if(isSdReadable()) {
                FileOutputStream fos = new FileOutputStream(file,true);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                osw.append(output);
                osw.append("\r \n");
                osw.flush();
                osw.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getOutput(SensorEvent event) {
        String res = "";
        if (typeOfRecording == 1)
            res = event.values[0] + "," + event.values[1] + "," + event.values[2] + ",still";
        else if (typeOfRecording == 2)
            res = event.values[0] + "," + event.values[1] + "," + event.values[2] + ",lowenergy";
        else if(typeOfRecording == 3)
            res = event.values[0] + "," + event.values[1] + "," + event.values[2] + ",highenergy";
        return res;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private File getDownloadsDir(){
        File file = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)));
        if (!file.mkdirs()){
            Log.i("file not present", "the file was not present");
        }
        return file;
    }

    private boolean isSdReadable() {

        boolean mExternalStorageAvailable = false;
        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mExternalStorageAvailable = true;
                Log.i("isSdReadable", "External storage card is readable.");
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                Log.i("isSdReadable", "External storage card is readable.");
                mExternalStorageAvailable = true;
            } else {
                mExternalStorageAvailable = false;
                Log.i("isSdReadable", "External storage card is not readable");
            }
        } catch (Exception ex) {
        }
        return mExternalStorageAvailable;
    }
}
