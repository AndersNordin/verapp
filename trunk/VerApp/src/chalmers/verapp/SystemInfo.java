package chalmers.verapp;

public class SystemInfo {

	/* L�gg till Longitude, Latitude, varning och om det �r n�got mer 
	 * som ska in i zip-filen h�r...
	 * Datan fr�n sensorerna kommer ocks� in h�r och in i zippen...
	 * -Jag h�mtar datan h�r och l�gger in i zippen.
	 * 
	 * Obs! tr�ds�kra..
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
