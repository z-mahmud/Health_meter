/*
 * Created on May 16, 2012
 * Author: Paul Woelfel
 * Email: frig@frig.at
 */
package com.arlong.stepcounter;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


/**
 * @author Paul Woelfel (paul@woelfel.at)
 */
public class CompassSensorWatcher implements SensorEventListener {

	protected SensorManager sensorManager;

	protected Sensor compass;

	protected Sensor accelerometer;

	protected Sensor gyroscope;

	protected Context context;

	protected Madgwick madgwick = new Madgwick();

	float[] inR = new float[16];

	float[] I = new float[16];

	float[] gravity = new float[3] ;

	float[] geomag = new float[3];

	float[] gyrodata = new float[3];

	float[] orientVals = new float[3];

	float azimuth = 0;

	float angle = 0;
	float initAngle = 0;

	float[] qinit = new float[4];

	private long lastTime = 0;
	private long currentTime = 0;

//	String azimuthText = "";

	int minX = 0, minY = 0, maxX = 0, maxY = 0, centerX = 0, centerY = 0, width = 0, height = 0;

	float l = 0.3f;
	
	protected CompassListener listener;

	protected float lastAzimuth = 0f;
	
	

	public CompassSensorWatcher(Context context,CompassListener cl,float lowpassFilter) {
		this.context = context;
		this.listener=cl;
		this.l=lowpassFilter;
		
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		compass = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		this.gravity = null; this.geomag = null; this.gyrodata = null;

		try {
			sensorManager.registerListener(this, compass, SensorManager.SENSOR_DELAY_UI);
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
			sensorManager.registerListener(this,gyroscope,SensorManager.SENSOR_DELAY_NORMAL);
		} catch (Exception e) {
			Logger.e("could not register listener", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {

		// Logger.d("sensor changed "+event);
		// we use TYPE_MAGNETIC_FIELD to get changes in the direction, but use SensorManager to get directions
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
			return;

		// Gets the value of the sensor that has been changed
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			gravity = event.values.clone();
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			geomag = event.values.clone();
			break;
		case Sensor.TYPE_GYROSCOPE:
			gyrodata = event.values.clone();
			currentTime = event.timestamp;
			break;
		}
		if (!SensorReadoutActivity.getInitAngle) {
			// If gravity and geomag have values then find rotation matrix
			if (gravity != null && geomag != null) {

				// checks that the rotation matrix is found
				boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
				if (success) {
					SensorManager.getOrientation(inR, orientVals);
					/*for(int i=0;i<100;i++){
						qinit = madgwick.MadgwickAHRSupdate(0.0f,0.0f,0.0f,gravity[0],gravity[1],gravity[2],geomag[0],geomag[1],geomag[2]);
					}
					orientVals = madgwick.quatern2euler(madgwick.quaternConj(qinit));
				    //madgwick.setQ(qinit);
					angle = (float) ToolBox.normalizeAngle(orientVals[2]);*/
					angle = (float) ToolBox.normalizeAngle(orientVals[0]);
					azimuth = (float) Math.toDegrees(angle);
					initAngle = angle;
					lowPassFilter();

					angle = (float) Math.toRadians(azimuth);

//				azimuthText = getAzimuthLetter(azimuth) + " " + Integer.toString((int) azimuth) + "°";
					//qinit = madgwick.quaternConj(madgwick.euler2quatern(orientVals));

					//madgwick.setQ(qinit);
					lastTime = event.timestamp;
					//getInitAngle =true;
					gyrodata = null;
					if (listener != null) {
						listener.onCompassChanged(azimuth, angle, getAzimuthLetter(azimuth));
					}
				}
			}
		}
		else{
			if(gyrodata!=null && gravity!=null){
				float NS2S=1.0f/1000000000.0f;//纳秒转为秒
				float a = 1.0f/(currentTime-lastTime)/NS2S;
				madgwick.setSampleFreq(a);
				lastTime = currentTime;
				float[] euler = madgwick.quatern2euler(madgwick.quaternConj(madgwick.MadgwickAHRSupdateIMU(gyrodata[0], gyrodata[1], gyrodata[2],gravity[0],gravity[1],gravity[2])));
				gyrodata = null;
				angle = (float) ToolBox.normalizeAngle(initAngle-euler[2]);
				//angle = euler[2];
				azimuth = (float) Math.toDegrees(angle);

				//lowPassFilter();

				angle = (float) Math.toRadians(azimuth);

//				azimuthText = getAzimuthLetter(azimuth) + " " + Integer.toString((int) azimuth) + "°";

				if (listener != null) {
					listener.onCompassChanged(azimuth, angle, getAzimuthLetter(azimuth));
				}
			}
		}
	}
	
	public void stop(){
		try {
			sensorManager.unregisterListener(this);
		} catch (Exception e) {
			Logger.w("could not unregister listener", e);
		}
	}

	public String getAzimuthLetter(float azimuth) {
		String letter = "";
		int a = (int) azimuth;

		if (a < 23 || a >= 315) {
			letter = "N";
		} else if (a < 45 + 23) {
			letter = "NO";
		} else if (a < 90 + 23) {
			letter = "O";
		} else if (a < 135 + 23) {
			letter = "SO";
		} else if (a < (180 + 23)) {
			letter = "S";
		} else if (a < (225 + 23)) {
			letter = "SW";
		} else if (a < (270 + 23)) {
			letter = "W";
		} else {
			letter = "NW";
		}

		return letter;
	}

	protected void lowPassFilter() {
		// lowpass filter
		float dazimuth = azimuth -lastAzimuth;

//		// if the angle changes more than 180°, we want to change direction and follow the shorter angle
		if (dazimuth > 180) {
			// change to range -180 to 0
			dazimuth = (float) (dazimuth - 360f);
		} else if (dazimuth < -180) {
			// change to range 0 to 180
			dazimuth = (float) (360f + dazimuth);
		}
		// lowpass filter
		azimuth = lastAzimuth+ dazimuth*l;
		
		azimuth%=360;
		
		if(azimuth<0){
			azimuth+=360;
		}
		
		lastAzimuth=azimuth;
		
//		lastAzimuth=azimuth=ToolBox.lowpassFilter(lastAzimuth, azimuth, l);
		
//		oldValue + filter * (newValue - oldValue);

	}

}
