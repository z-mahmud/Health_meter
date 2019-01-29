package com.example.jdolan.step_counter;

import android.app.AlertDialog;
import android.app.FragmentHostCallback;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.hardware.*;
import android.text.format.Time;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.example.jdolan.step_counter.MySQLiteHelper;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.jdolan.step_counter.CountDataSource;

public class MainActivity extends AppCompatActivity  implements SensorEventListener {

    MySQLiteHelper myDB;
    private CountDataSource datasource;

    private SensorManager sensorManager;
    private Sensor stepDetectorSensor;
    //private Sensor accelerometer;
    //private Sensor magnetometer;
    public int stepCount = 0;
    public float Avg = 0;
    public int TotalCount = 0;
    public int NumOfActivities = 0;
    public float TempTotalCount = 0;
    public float TempNumOfActivities = 0;

    Button Start, History;

    TextView Count,Total_Count, Average;
    boolean activityRunning;
    public boolean active = false;
    public boolean isInserted =false;
    String StartTime2 = "";
    int[] NumSteps2 = {0};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        History = (Button) findViewById(R.id.b_History);
        Total_Count = (TextView) findViewById(R.id.Total_Count);
        Count = (TextView) findViewById(R.id.Current_Count);
        Average = (TextView) findViewById(R.id.Average);
        myDB = new MySQLiteHelper(this);

        TextView tv = (TextView) findViewById(R.id.Current_Count);
        tv.setText(Integer.toString(stepCount));

        UpdateCounters();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        History.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                History(v);
            }
        });


        final Button b_Start = (Button) findViewById(R.id.b_Start);
        if (b_Start != null) {

            b_Start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!active) {
                        b_Start.setText("Finish");
                        b_Start.setBackgroundColor(Color.parseColor("#F44336"));
                        sensorManager.registerListener(MainActivity.this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
                        StartTime2 = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
                        active = true;
                        View b = findViewById(R.id.b_History);
                        b.setVisibility(View.GONE);

                    }

                    else {
                        b_Start.setText("Start");
                        b_Start.setBackgroundColor(Color.parseColor("#A4C639"));
                        NumSteps2[0] = stepCount;

                        sensorManager.unregisterListener(MainActivity.this, stepDetectorSensor);
                        isInserted = myDB.insertData(StartTime2, NumSteps2[0]);
                        if (isInserted = true)
                            Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Failure", Toast.LENGTH_SHORT).show();

                        active = false;
                        UpdateCounters();
                        View b = findViewById(R.id.b_History);
                        b.setVisibility(View.VISIBLE);
                    }
                    stepCount = 0;
                    TextView tv = (TextView) findViewById(R.id.Current_Count);
                    tv.setText(Integer.toString(stepCount));

                }
            });


        }

    }



    public void UpdateCounters(){
        TotalCount = myDB.getTotalSteps();
        TextView tv = (TextView) findViewById(R.id.Total_Count);
        tv.setText(Integer.toString(TotalCount));

        NumOfActivities = myDB.getRunCount();
        TextView tv1 = (TextView) findViewById(R.id.No_Activities);
        tv1.setText(Integer.toString(NumOfActivities));


        if (NumOfActivities == 0){
            Avg = 0;
        }
        else {
            TempNumOfActivities = NumOfActivities;
            TempTotalCount = TotalCount;
            Avg = TempTotalCount / TempNumOfActivities;
            String str = String.format("%1.2f", Avg);
            Avg = Float.valueOf(str);
            TextView tv2 = (TextView) findViewById(R.id.Average);
            tv2.setText(Float.toString(Avg));
        }
    }



    public void History(View v1) {
        Intent A2Intent = new Intent (v1.getContext(), Activity.class);
        startActivity(A2Intent);
    }


    @Override
    protected void onResume() {
        super.onResume();
        activityRunning = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (activityRunning & (active == true)) {
            stepCount++;
            TextView tv = (TextView) findViewById(R.id.Current_Count);
            tv.setText(Integer.toString(stepCount));
        }

    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onStart() {
        super.onStart();

        Action viewAction2 = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,

                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.jdolan.step_counter/http/host/path")
        );
    }

    @Override
    public void onStop() {
        super.onStop();

        Action viewAction2 = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,

                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.jdolan.step_counter/http/host/path")
        );
    }
}

