package chalmers.verapp;

import android.content.SyncResult;

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
