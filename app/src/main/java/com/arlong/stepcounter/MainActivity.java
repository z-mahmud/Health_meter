package com.arlong.stepcounter;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author  Paul Woelfel (paul@woelfel.at)
 */
public class MainActivity extends Activity implements OnClickListener{

	protected boolean running;

	protected static final String logTag = "MainActivity";
	
	protected static final String AUTO_CALIBRATION_DONE="SensorAutoCalibration";
	
	/**
	 * @uml.property  name="log"
	 * @uml.associationEnd  
	 */
	protected static final Logger log=new Logger(logTag);
	
	protected static final int REQ_PROJECT_LIST=3;
	
	/**
	 * @uml.property  name="databaseHelper"
	 * @uml.associationEnd  
	 */
	protected DatabaseHelper databaseHelper = null;



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.setLogLevelFromPreferences(this);
//		log.debug( "MainActivity onCreate");
//		Logger.i("TEST!");

		init();
	}

	protected void init() {
		log.debug( "init");

		

		setContentView(R.layout.main);

		/* First, get the Display from the WindowManager */
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

		/* Now we can retrieve all display-related infos */
		int width = display.getWidth();
		int height = display.getHeight();
		int orientation = display.getOrientation();

		log.debug( "display: " + width + "x" + height + " orientation:" + orientation);

		running = false;
		((Button) findViewById(R.id.new_project_button)).setOnClickListener(this);
		((Button) findViewById(R.id.main_quickscan_button)).setOnClickListener(this);
//		((Button) findViewById(R.id.load_project_button)).setOnClickListener(this);
//		((Button) findViewById(R.id.sample_scan_button)).setOnClickListener(this);
//		Button about_button = ((Button) findViewById(R.id.aboutButton));
//		if (about_button != null) {
//			about_button.setOnClickListener(this);
//		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.new_project_button:
			log.debug( "new project");
			//Intent npi = new Intent(this, SensorReadoutActivity.class);
			//npi.putExtra(SensorReadoutActivity.START_MODE, SensorReadoutActivity.START_NEW);
			//startActivity(npi);
			break;
//		case R.id.load_project_button:
//			log.debug( "load project");
//			Intent lpi = new Intent(this, ProjectListActivity.class);
//			
//			startActivityForResult(lpi, REQ_PROJECT_LIST);
//			
//			break;
//		case R.id.sample_scan_button:
//			log.debug( "starting sample scan activity");
//			Intent i = new Intent(this, SampleScanActivity.class);
//			startActivity(i);
//			break;
//		case R.id.aboutButton:
//			log.debug( "show About");
//			Intent aboutIntent = new Intent(this, AboutActivity.class);
//			startActivity(aboutIntent);
//			break;
			
		case R.id.main_quickscan_button:
			log.debug("Quick Scan!");
			try {
				databaseHelper= OpenHelperManager.getHelper(this, DatabaseHelper.class);
				Project p=new Project(getString(R.string.quickscan_project_name, DateFormat.getDateInstance(DateFormat.SHORT).format(new Date())));
				databaseHelper.getDao(Project.class).create(p);
				
				ProjectSite ps=new ProjectSite(p);
				ps.setTitle(getString(R.string.quickscan_project_site_name, DateFormat.getDateInstance(DateFormat.LONG).format(new Date())));
				databaseHelper.getDao(ProjectSite.class).create(ps);
				
				OpenHelperManager.releaseHelper();
				databaseHelper=null;
				
				
				Intent psIntent=new Intent(this,SensorReadoutActivity.class);
				psIntent.putExtra(SensorReadoutActivity.SITE_KEY,ps.getId());
				startActivity(psIntent);
				
			} catch (SQLException e) {
				log.error("could not create quick scan project", e);
				Toast.makeText(this, getString(R.string.main_quickscan_failed, e.getMessage()), Toast.LENGTH_LONG).show();
			}
			
			break;
		default:
			log.warn("could not identify sender = " + v.getId());
			break;
		}

	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// startActivity(getIntent());
		// finish();
		log.debug( "Config changed " + newConfig.toString());
		/* First, get the Display from the WindowManager */
		init();
	}


	@Override
	protected void onResume() {
		super.onResume();
	}



}