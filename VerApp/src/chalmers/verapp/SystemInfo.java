package chalmers.verapp;

import android.content.SyncResult;

public class SystemInfo {

	/* L�gg till Longitude, Latitude, varning och om det �r n�got mer 
	 * som ska in i zip-filen h�r...
	 * Datan fr�n sensorerna kommer ocks� in h�r och in i zippen...
	 * -Jag h�mtar datan h�r och l�gger in i zippen.
	 * 
	 * Obs! tr�ds�kra..
	 */
	
	private String mLongitude;
	private String mLatitude;
	private String mWarning;
	
	private Object mLongitudeLock = new Object();
	private Object mLatitudeLock = new Object();
	private Object mWarningLock = new Object();
	
	public SystemInfo(){
		this.mLongitude = "";
		this.mLatitude = "";
		this.mWarning = "";
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
	
	
	

}
