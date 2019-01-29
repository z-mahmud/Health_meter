package com.example.jdolan.step_counter;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdolan on 26/04/16.
 */
public class Activity extends ListActivity {
    private CountDataSource datasource;
    public EditText userText = null;

    
    private GoogleApiClient client;
    MySQLiteHelper myDB;
    MainActivity MA;
    ListView list2;
    int ress;
    private Cursor res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Button Back = (Button) findViewById(R.id.button4);
        Button Delete = (Button) findViewById(R.id.b_delete);
        myDB = new MySQLiteHelper(this);

        final ArrayList<String> list = new ArrayList<String>();

        list.add ("DATE       :   STEPS");

        list2 = (ListView) findViewById(android.R.id.list);
        Cursor res = getAllData();


        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainPage(v);
            }
        });

    }


    public Cursor getAllData() {
        Cursor res = myDB.databaseToString();
        if(res.getCount() == 0){
            showMessage("Error", "No Data Found");
            return null;
        }

        StringBuffer buffer = new StringBuffer();
        while (res.moveToNext()){
            buffer.append(res.getString(0) + " " );
            buffer.append(res.getString(1) + " ");
            buffer.append(res.getString(2) + "\n");
        }
        return res;
    }

    public void showMessage(String title, String Message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    private void openDB(){
        myDB = new MySQLiteHelper(this);

    }






     public void populateListView() {



         String[] values =  new String[] { myDB.COLUMN_ID,myDB.COLUMN_STARTTIME,myDB.COLUMN_COMMENT
        };
        SimpleCursorAdapter myCursorAdapter;
    }


    public void MainPage(View v1) {
        Intent A2Intent = new Intent(v1.getContext(), MainActivity.class);
        startActivity(A2Intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                " Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.jdolan.step_counter/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                " Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.jdolan.step_counter/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}