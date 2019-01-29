/*
 * Created on May 11, 2012
 * Author: Paul Woelfel
 * Email: frig@frig.at
 */
package com.arlong.stepcounter;

/**
 * @author Paul Woelfel (paul@woelfel.at)
 */
public class StepDetector {
	protected static final int vhSize = 6;

	protected double[] values_history = new double[vhSize];

	protected int vhPointer = 0;

	protected int accNo = 0;

	public static final int WINDOW = 5;

	
	protected double a;

	protected double peak;

	protected int stepTimeoutMS;

	protected long lastStepTs = 0;

	// last acc is low pass filtered
	protected double[] lastAcc = new double[] {0.0, 0.0, 0.0};


	protected int round = 0;
	
	
	protected boolean logSteps=true;

	
	protected long lastUpdateTimestamp=0;
	
	
	protected long lastSecond=0;
	protected int valuesPerSecond=0;

	public double stepLength = 0;

	public StepDetector(double a, double peak, int step_timeout_ms) {
		this.a = a;
		this.peak = peak;
		this.stepTimeoutMS = step_timeout_ms;
	}

	public synchronized void addSensorValues(long timestamp,float values[]) {
		// simple lowpass filter
		lastAcc[0]+=a*(values[0]-lastAcc[0]);
		lastAcc[1]+=a*(values[1]-lastAcc[1]);
		lastAcc[2]+=a*(values[2]-lastAcc[2]);
		lastUpdateTimestamp=timestamp;
		if(timestamp<lastSecond+1000){
			valuesPerSecond++;
		}else {
			if(logSteps&&Logger.isTraceEnabled())
				Logger.t(valuesPerSecond+" sensor values received in the last second");
			lastSecond=timestamp;
			valuesPerSecond=0;
		}
	}

	protected double lowpassFilter(double oldValue, double newValue) {
		return oldValue + a * (newValue - oldValue);
	}

	/**
	 * This is called every INTERVAL_MS ms from the TimerTask.
	 */
	public synchronized boolean checkForStep_old() {
		boolean ret = false;

		// Get current time for time stamps
		

		addData(lastAcc[2]);

		// Check if a step is detected upon data
		if ((lastUpdateTimestamp - lastStepTs) > stepTimeoutMS) {

			for (int t = 1; t <= WINDOW; t++) {
				if ((values_history[(vhPointer - 1 - t + vhSize + vhSize) % vhSize] - values_history[(vhPointer - 1 + vhSize) % vhSize] > peak)) {

					if(logSteps)
						Logger.i("Detected step with t = " + t + ", diff = " + peak + " < "
							+ (values_history[(vhPointer - 1 - t + vhSize + vhSize) % vhSize] - values_history[(vhPointer - 1 + vhSize) % vhSize]));
					// Set latest detected step to "now"
					lastStepTs = lastUpdateTimestamp;
					// Call algorithm for navigation/updating position
					// st.trigger(now_ms, lCompass);
//					Logger.i( "Detected step  in  round = " + round + " @ " + now_ms);
					ret = true;
					break;
				}
			}

		}
		round++;
		return ret;
	}
	double[] accold1,accold2,accnew;
	double[] peakmin = new double[]{0,0};
	double[] peakmax = new double[]{0,0};
	boolean peakmaxReady = false;
	double deltaTime = 0.15;
	double deltaA = 1.2;
	public synchronized boolean checkForStep() {
		accNo ++;
		boolean ret = false;
		double[] normAcc = new double[2];
		// Get current time for time stamps
		if (accNo >= vhSize)
			normAcc[1] = filter(norm(lastAcc));
		else
			addData(norm(lastAcc));

		normAcc[0] = lastUpdateTimestamp;

		if(accNo == 1)
			accold1 = normAcc;
		else if(accNo == 2)
			accold2 = normAcc;
		else {
			accnew = normAcc;
			if(accold1[1]<accold2[1] && accold2[1] > accnew[1]){
				if (accold2[0]-peakmin[0]> deltaTime)
					peakmax = accold2;
				peakmaxReady = true;
			}
			else if(accold1[1]>accold2[1] && accold2[1] < accnew[1]){
				if (accold2[0] - peakmax[0] >deltaTime  && peakmaxReady){
					peakmin = accold2;
					if(peakmax[1] - peakmin[1] >deltaA){
						this.stepLength = 0.5 * Math.pow(peakmax[1] - peakmin[1],0.25) ;
						ret = true;
						peakmaxReady = false;
					}
				}

			}
			accold1 = accold2;
			accold2 = accnew;
		}
		return ret;
	}

	protected void addData(double value) {
		values_history[vhPointer % vhSize] = value;
		vhPointer++;
		vhPointer = vhPointer % vhSize;
	}

	protected double norm(double[] value){
		double norm_value,sum2 = 0;
		for(int i=0;i<value.length;i++){
			sum2 += value[i]*value[i];
		}
		norm_value = Math.sqrt(sum2);
		return norm_value;
	}

	private double filter(double data){
		int count;
		double  sum=0;
		values_history[vhPointer % vhSize] = data;
		vhPointer++;
		vhPointer = vhPointer % vhSize;
		for ( count=0;count<vhSize;count++){
			sum = sum + values_history[count];
		}
		return (sum/vhSize);
	}
	/**
	 * @return the a
	 */
	public double getA() {
		return a;
	}

	/**
	 * @param a the a to set
	 */
	public void setA(double a) {
		this.a = a;
	}

	/**
	 * @return the peak
	 */
	public double getPeak() {
		return peak;
	}

	/**
	 * @param peak the peak to set
	 */
	public void setPeak(double peak) {
		this.peak = peak;
	}

	/**
	 * @return the stepTimeoutMS
	 */
	public int getStepTimeoutMS() {
		return stepTimeoutMS;
	}

	/**
	 * @param stepTimeoutMS the stepTimeoutMS to set
	 */
	public void setStepTimeoutMS(int stepTimeoutMS) {
		this.stepTimeoutMS = stepTimeoutMS;
	}

	/**
	 * @return the lastStepTs
	 */
	public long getLastStepTs() {
		return lastStepTs;
	}

	/**
	 * @return the lastAcc
	 */
	public double[] getLastAcc() {
		return lastAcc;
	}

	/**
	 * @return the round
	 */
	public int getRound() {
		return round;
	}

	/**
	 * @return the logSteps
	 */
	public boolean isLogSteps() {
		return logSteps;
	}

	/**
	 * @param logSteps the logSteps to set
	 */
	public void setLogSteps(boolean logSteps) {
		this.logSteps = logSteps;
	}

	/**
	 * @return the valuesPerSecond
	 */
	public int getValuesPerSecond() {
		return valuesPerSecond;
	}

	public double getStepLength() {
		return stepLength;
	}
}
