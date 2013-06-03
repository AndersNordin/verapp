package chalmers.verapp;

import java.util.ArrayList;

import android.util.Log;

public class SystemInfo {

	/* Lägg till Longitude, Latitude, varning och om det är något mer 
	 * som ska in i zip-filen här...
	 * Datan från sensorerna kommer också in här och in i zippen...
	 * -Jag hämtar datan här och lägger in i zippen.
	 * 
	 * Obs! trådsäkra..
	 */
	
	private String mLongitude;
	private String mLatitude;
	private String mWarning;
	//Data from sensors
	private String mLeftWheelSpeed;
	private String mRightWheelSpeed;
	private String mSteering;
	private String mLeftCamber;
	private String mRightCamber;
	private String mAccelerometer;
	
	private Object mLongitudeLock = new Object();
	private Object mLatitudeLock = new Object();
	private Object mWarningLock = new Object();
	
	private Object mLeftWheelSpeedLock = new Object();
	private Object mRightWheelSpeedLock = new Object();
	
	private Object mSteeringLock = new Object();
	
	private Object mLeftCamberLock = new Object();
	private Object mRightCamberLock = new Object();
	private Object mAccelerometerLock = new Object();
	
	public SystemInfo(){
		
		this.mLongitude = "0";
		this.mLatitude = "0";
		this.mWarning = "0";
		
		this.mLeftWheelSpeed ="0";
		this.mRightWheelSpeed = "0";
		this.mSteering = "0";
		this.mLeftCamber = "0";
		this.mRightCamber = "0";
		this.mAccelerometer = "0";
		
	}

	
	
	public String getLatitude() {
		synchronized(mLatitudeLock)
		{
			return mLatitude;
		}
	}

	public void setLatitude(String latitude) {
		synchronized(mLatitudeLock)
		{
			this.mLatitude = latitude;
		}
	}

	public String getWarning() {
		synchronized(mWarningLock)
		{
			return mWarning;
		}
	}

	public void setWarning(String warning) {
		synchronized(mWarningLock)
		{
			this.mWarning = warning;
			Log.d("::MEDDEALNDE FRÅN SYSTEMINFO: ", warning);
		}
	}

	public String getLongitude() {
		synchronized(mLongitudeLock)
		{
			return mLongitude;
		}
	}

	public void setLongitude(String longitude) {
		synchronized(mLongitudeLock)
		{
			this.mLongitude = longitude;
		}
	}
	

	
	public void setLeftWheelSpeed(String leftWheelSpeed) {
		synchronized(mLeftWheelSpeedLock)
		{
			this.mLeftWheelSpeed = leftWheelSpeed;
			Log.d("SET LeftWheelSpeed", leftWheelSpeed);
		}
	}
	

	public String getLeftWheelSpeed() {
		synchronized(mLeftWheelSpeedLock)
		{

			Log.d("GET LeftWheelSpeed ", mLeftWheelSpeed);
			return mLeftWheelSpeed;
		}
	}
	
	public void setRightWheelSpeed(String rightWheelSpeed) {
		synchronized(mRightWheelSpeedLock)
		{
			this.mLeftWheelSpeed = rightWheelSpeed;
			Log.d("RightWheelSpeed", rightWheelSpeed);
		}
	}
	

	public String getRightWheelSpeed() {
		synchronized(mRightWheelSpeedLock)
		{
			return mRightWheelSpeed;
		}
	}
	

	
	public void setSteering(String steering) {
		synchronized(mSteeringLock)
		{
			this.mSteering = steering;
		}
	}
	

	public String getSteering() {
		synchronized(mSteeringLock)
		{
			return mSteering;
		}
	}
		
	public void setLeftCamber(String leftCamber) {
		synchronized(mLeftCamberLock)
		{
			this.mLeftCamber = leftCamber;
		}
	}
	

	public String getLeftCamber() {
		synchronized(mLeftCamberLock)
		{
			return mLeftCamber;
		}
	}	

	
	public void setRightCamber(String rightCamber) {
		synchronized(mRightCamberLock)
		{
			this.mRightCamber = rightCamber;
		}
	}
	

	public String getRightCamber() {
		synchronized(mRightCamberLock)
		{
			return mRightCamber;
		}
	}
	
	
	public void setAccelerometer(String acc) {
		synchronized(mAccelerometerLock)
		{
			this.mAccelerometer = acc;
		}
	}
	

	public String getAccelerometer() {
		synchronized(mAccelerometerLock)
		{
			return mAccelerometer;
		}
	}

	
	
}