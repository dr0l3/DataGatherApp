package com.example.DataGathering;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import java.io.*;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private File file;
    private File path;
    private int START_TO_GO_DELAY = 10000;
    private int RECORD_DURATION = 60000;
    //private boolean isStarted;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void toggleRecording(View v){
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
                osw.append("@ATTRIBUTE  class   {standing, walking, sitting} \r\n");
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
                        mSensorManager.unregisterListener(getEventListener());
                    }
                }, RECORD_DURATION);
            }
        }, START_TO_GO_DELAY);
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

        String output = event.values[0] + "," + event.values[1] + "," + event.values[2] + ",class";

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
