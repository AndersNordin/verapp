package chalmers.verapp;


import android.util.Log;

public class SystemInfo {
	/*
	 * Lägg till Longitude, Latitude, varning och om det är något mer som ska in
	 * i zip-filen här... Datan från sensorerna kommer också in här och in i
	 * zippen... Jag hämtar datan här och lägger in i zippen.
	 */

	private String mLongitude = "";
	private String mLatitude = "";
	private String mWarning = "";
	private int steering;
	private int prevSteering;

	private Object mLongitudeLock = new Object();
	private Object mLatitudeLock = new Object();
	private Object mWarningLock = new Object();
	private SteeringChangeListener steeringChangeListener;

	public SystemInfo() {
		this.mLongitude = "42.1100";
		this.mLatitude = "11.420000";
		this.mWarning = "0";
	}

	public String getLatitude() {
		synchronized (mLatitudeLock) {
			return mLatitude;
		}
	}

	public void setLatitude(String latitude) {
		synchronized (mLatitudeLock) {
			this.mLatitude = latitude;
		}
	}

	public String getWarning() {
		synchronized (mWarningLock) {
			return mWarning;
		}
	}

	public void setWarning(String warning) {
		synchronized (mWarningLock) {
			this.mWarning = warning;
		}
	}

	public String getLongitude() {
		synchronized (mLongitudeLock) {
			return mLongitude;
		}
	}

	public void setLongitude(String longitude) {
		synchronized (mLongitudeLock) {
			this.mLongitude = longitude;
		}
	}

	public int getSteering() {
		return steering;
	}

	public int getPrevSteering() {
		return prevSteering;
	}

	public void setSteering(int s) {

		prevSteering = 0;
		steering = s;
		if (this.steeringChangeListener != null && (steering != prevSteering)) {

			this.steeringChangeListener.onSteeringChanged(steering);

		}

	}

	public void setSteeringChangeListener(
			SteeringChangeListener steeringChangeListener) {
		Log.e("HALLÅ", "LYSSNARE SKAPAS");

		this.steeringChangeListener = steeringChangeListener;

	}
	/*
	 * public void steeringChanged() { if (this.steeringChangeListener != null)
	 * { Log.e("HALLÅ", "Lyssnare reagerar 1"); if (steering != prevSteering) {
	 * Log.e("HALLÅ", "Lyssnare reagerar 2");
	 * this.steeringChangeListener.onSteeringChanged(steering); } } }
	 */

}