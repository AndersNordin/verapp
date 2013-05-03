package chalmers.verapp;

public class SystemInfo {

	/* Lägg till Longitude, Latitude, varning och om det är något mer 
	 * som ska in i zip-filen här...
	 * Datan från sensorerna kommer också in här och in i zippen...
	 * -Jag hämtar datan här och lägger in i zippen.
	 * 
	 * Obs! trådsäkra..
	 */
	
	public String longitude;
	public String latitude;
	public String warning;
	

	public SystemInfo(){
		this.longitude = "";
		this.latitude = "";
		this.warning = "";
	}
	
	
	public synchronized String getLatitude() {
		return latitude;
	}

	public synchronized void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public synchronized String getWarning() {
		return warning;
	}

	public synchronized void setWarning(String warning) {
		this.warning = warning;
	}

	public synchronized String getLongitude() {
		return longitude;
	}

	public synchronized void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	
	
	

}
