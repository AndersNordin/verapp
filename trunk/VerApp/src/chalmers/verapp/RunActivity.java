package chalmers.verapp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import chalmers.verapp.base.BaseActivity;
import chalmers.verapp.interfaces.Constants;
import chalmers.verapp.interfaces.GPSCallback;

public class RunActivity extends BaseActivity implements GPSCallback{
	// Graphical
	private Button stopBtn, startWhenPauseBtn, incidentBtn;
	private Chronometer clockTime;	
	private TextView tvSpeed, tvLapTime1, tvLapTime2, avgSpeed;

	// GPS
	private GPSManager gpsManager = null;

	// Values 
	private long timeWhenStopped = 0;
	private boolean isTimeRunning = true;
	private String warning = "0";
	private ArrayList<Long> lapTimes = new ArrayList<Long>();
	private boolean validLap = false;
	private double totalDistance = 0;

	Timer mTimer = new Timer();
	
	// Location points	 
	private Location startPos = new Location("start position"); // first point logged
	private double bearingStart = 999.0; // Cars initial direction
	private Location secondLatestPos = new Location("Second latest point");

	// Representing the complete finish line
	private Location finishLinePointLeft = new Location("Finish Line Point to the left");
	private Location finishLinePointRight = new Location("Finish Line Point to the right");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run);

		// Screen always active
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Initiate GPS
		gpsManager = new GPSManager();
		gpsManager.startListening(getApplicationContext());
		gpsManager.setGPSCallback(this);

		// Get graphical id's
		stopBtn = (Button)findViewById(R.id.stop);
		startWhenPauseBtn = (Button)findViewById(R.id.start_when_paused);
		incidentBtn = (Button)findViewById(R.id.incident);
		clockTime = (Chronometer)findViewById(R.id.clockTime);
		tvSpeed = (TextView)findViewById(R.id.tvSpeed);
		tvLapTime1 = (TextView)findViewById(R.id.lapTime2);
		tvLapTime2 = (TextView)findViewById(R.id.lapTime3);
		avgSpeed = (TextView)findViewById(R.id.avgSpeed);

		// Pause button pushed
		startWhenPauseBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!isTimeRunning){
					// Check whether pause or start mode on button				
					clockTime.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
					clockTime.start();		
					isTimeRunning = true;
					Toast.makeText(getApplicationContext(), "Resumed", Toast.LENGTH_SHORT).show();
				}
			}
		});

		// Stop button pushed
		stopBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isTimeRunning){
					timeWhenStopped = clockTime.getBase() - SystemClock.elapsedRealtime();
					clockTime.stop();
					isTimeRunning = false;
					Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
				}
			}
		});

		// Incident button pushed
		incidentBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				warning = "1";
				Toast.makeText(getApplicationContext(), "Warning Sent", Toast.LENGTH_SHORT).show();
			}
		});

		clockTime.start();
		Toast.makeText(getApplicationContext(), "Running", Toast.LENGTH_SHORT).show();

		// Sending data with given frequency
		mTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(isTimeRunning)
					new DatabaseManager().execute("rpm", "2000", String.valueOf(13.37), String.valueOf(13.37), warning);
				warning = "0"; // reset warning				
			}
		}, 0, 15000); // Insert freq. time from settings
	}
	
	// Step 4: Get current coordinate	
	@Override
	public void onGPSUpdate(Location currentPos){
		Log.i("GPS UPDATE","(" + currentPos.getLatitude() + "," + currentPos.getLongitude() + ") Dir: " + currentPos.getBearing());

		// Step 1: Set starting coordinate
		if(startPos.getLatitude() == 0 && startPos.getLongitude() == 0)
			startPos = currentPos;

		// Step 2: Set starting bearing	
		// Step 3: Calculate finish line	
		if(currentPos.hasBearing() && bearingStart == 999.0){
			bearingStart = currentPos.getBearing();			
			setPerpendicularLine(currentPos, bearingStart);
		}

		// There are 4 cases
		boolean inter1 = false,inter2 = false, inter3 = false, inter4 = false;
		double dist = 999.0;

		// The car doesn't necessarily has to have the same angel when crossing the finish line 
		if(bearingStart+20 > currentPos.getBearing() && currentPos.getBearing() > bearingStart-20 ){
			inter1 = intersection(finishLinePointRight, (bearingStart-90), currentPos, (currentPos.getBearing() - 180));
			inter2 = intersection(finishLinePointRight , (bearingStart+90), currentPos, (currentPos.getBearing() - 180));
			inter3 = intersection(finishLinePointLeft , (bearingStart-90), currentPos, (currentPos.getBearing() - 180));
			inter4 = intersection(finishLinePointLeft , (bearingStart+90), currentPos, (currentPos.getBearing() - 180));
		}	

		// Step 6: If so... lap++;
		if((inter2 &&  inter3) || (inter1  && inter4))
			dist = distanceTo(currentPos,  startPos);


		// Ensure that the car is not passing finishLine twice on the same lap	
		Thread th = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(300000);
					validLap = true;
				}catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		th.start();

		// Step 7: Validate that the intersection was correct, which means within 10 meters
		// Step 8: Set timer to lock lap++;
		if((dist < 10) && validLap ){
			Long timeInMilliSec = (SystemClock.elapsedRealtime() - clockTime.getBase()); // in ms

			int size = lapTimes.size();
			if(size > 0){ // not first lap
				for(Long t : lapTimes)
					timeInMilliSec = timeInMilliSec - t;				
			}
			lapTimes.add(timeInMilliSec);
			size++;

			int seconds = (int) (timeInMilliSec / 1000) % 60 ;
			int minutes = (int) ((timeInMilliSec / (1000*60)) % 60);
			int hours   = (int) ((timeInMilliSec / (1000*60*60)) % 24);

			tvLapTime1.setText("Lap "+ size + ": " + hours + ":" + minutes + ":" + seconds );

			if(size > 1){
				timeInMilliSec = lapTimes.get(size-2);
				seconds = (int) (timeInMilliSec / 1000) % 60 ;
				minutes = (int) ((timeInMilliSec / (1000*60)) % 60);
				hours   = (int) ((timeInMilliSec / (1000*60*60)) % 24);

				tvLapTime2.setText("Lap "+ (size-1)+ ": " + hours + ":" + minutes + ":" + seconds);
			}					
			validLap = false;
		}

		// Display speed
		double currentSpeed = currentPos.getSpeed();
		String speedString = "" + roundDecimal(convertToKMH(currentSpeed),2);
		this.tvSpeed.setText(speedString + " km/h");

		// Average speed
		if(secondLatestPos.getLongitude() != 0 && secondLatestPos.getLatitude() != 0 && currentPos.hasBearing()){
			totalDistance = totalDistance + distanceTo(secondLatestPos, currentPos); // in meters
			double totalTime = (double)(SystemClock.elapsedRealtime() - clockTime.getBase()) / 1000; // in seconds
		
			// Display average speed			
			String avgSpeedString = "" + roundDecimal(convertToKMH(totalDistance/totalTime), 2);

			// Display color of avg speed
			if(Double.valueOf(avgSpeedString) > 25)
				avgSpeed.setTextColor(Color.GREEN);
			else
				avgSpeed.setTextColor(Color.RED);

			avgSpeed.setText("Avg Speed: " + avgSpeedString + " km/h");				
		}
		secondLatestPos = currentPos;
	}

	@Override
	protected void onDestroy(){
		gpsManager.stopListening();
		gpsManager.setGPSCallback(null);
		gpsManager = null;
		super.onDestroy();
	}

	/*
	 * speed in m/s
	 * returns in km/h
	 */
	private double convertToKMH(double speed){
		return (speed * 3.6); 
	}

	private double roundDecimal(double value, final int decimalPlace){
		BigDecimal bd = new BigDecimal(value);

		bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
		value = bd.doubleValue();

		return value;
	}

	// Sets start/finish line
	public void setPerpendicularLine(Location p1, double bearing1){		
		double bearing2 = bearing1 - 90;
		bearing1 = bearing1 + 90;

		bearing1 = Math.toRadians(bearing1);  
		bearing2 = Math.toRadians(bearing2);  
		double x = Math.toRadians(p1.getLatitude());
		double y = Math.toRadians(p1.getLongitude());

		double angularDistance = Constants.TRACK_WIDTH/Constants.EARTH_RADIUS;

		double x3 = Math.asin( Math.sin(x)*Math.cos(angularDistance) + 
				Math.cos(x)*Math.sin(angularDistance)*Math.cos(bearing1) );
		double y3 = y + Math.atan2(Math.sin(bearing1)*Math.sin(angularDistance)*Math.cos(x), 
				Math.cos(angularDistance)-Math.sin(x)*Math.sin(x3));

		double x4 = Math.asin( Math.sin(x)*Math.cos(angularDistance) + 
				Math.cos(x)*Math.sin(angularDistance)*Math.cos(bearing2) );
		double y4 = y + Math.atan2(Math.sin(bearing2)*Math.sin(angularDistance)*Math.cos(x), 
				Math.cos(angularDistance)-Math.sin(x)*Math.sin(x4));


		// normalize to -180..+180º
		y3 = (y3+3*Math.PI) % (2*Math.PI) - Math.PI;  
		y4 = (y4+3*Math.PI) % (2*Math.PI) - Math.PI;  

		x3 = Math.toDegrees(x3);
		y3 = Math.toDegrees(y3);

		x4 = Math.toDegrees(x4);
		y4 = Math.toDegrees(y4);

		finishLinePointLeft.setLatitude(x4);
		finishLinePointLeft.setLongitude(y4);
		finishLinePointRight.setLatitude(x3);
		finishLinePointRight.setLongitude(y3);
	}

	/**
	 * Returns the point of intersection of two paths defined by point and bearing
	 * See http://williams.best.vwh.net/avform.htm#Intersection
	 */
	public boolean intersection(Location f1,double brng1, Location c1, double brng2){
		double lat1 = Math.toRadians(f1.getLatitude());
		double lon1 = Math.toRadians(f1.getLongitude());
		double lat2 = Math.toRadians(c1.getLatitude());
		double lon2 = Math.toRadians(c1.getLongitude());
		double brng13 = Math.toRadians(brng1);
		double brng23 = Math.toRadians(brng2);
		double dLat = lat2-lat1;
		double dLon = lon2-lon1;

		double dist12 = 2*Math.asin( Math.sqrt( Math.sin(dLat/2)*Math.sin(dLat/2) + 
				Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2)*Math.sin(dLon/2) ) );

		if (dist12 == 0) return false;

		// initial/final bearings between points
		double brngA = Math.acos( ( Math.sin(lat2) - Math.sin(lat1)*Math.cos(dist12) ) / 
				( Math.sin(dist12)*Math.cos(lat1) ) );

		if (brngA == Double.NaN) brngA = 0;  // protect against rounding

		double brngB = Math.acos( ( Math.sin(lat1) - Math.sin(lat2)*Math.cos(dist12) ) / 
				( Math.sin(dist12)*Math.cos(lat2) ) );

		double brng12,brng21;

		if (Math.sin(lon2-lon1) > 0) {
			brng12 = brngA;
			brng21 = 2*Math.PI - brngB;
		} else {
			brng12 = 2*Math.PI - brngA;
			brng21 = brngB;
		}

		double alpha1 = (brng13 - brng12 + Math.PI) % (2*Math.PI) - Math.PI;  // angle 2-1-3
		double alpha2 = (brng21 - brng23 + Math.PI) % (2*Math.PI) - Math.PI;  // angle 1-2-3

		if (Math.sin(alpha1)==0 && Math.sin(alpha2)==0) return false;  // infinite intersections
		if (Math.sin(alpha1)*Math.sin(alpha2) < 0) return false;       // ambiguous intersection

		double alpha3 = Math.acos( -Math.cos(alpha1)*Math.cos(alpha2) + 
				Math.sin(alpha1)*Math.sin(alpha2)*Math.cos(dist12) );
		double dist13 = Math.atan2( Math.sin(dist12)*Math.sin(alpha1)*Math.sin(alpha2),Math.cos(alpha2)+Math.cos(alpha1)*Math.cos(alpha3));
		double lat3 = Math.asin( Math.sin(lat1)*Math.cos(dist13) + Math.cos(lat1)*Math.sin(dist13)*Math.cos(brng13));
		double dLon13 = Math.atan2( Math.sin(brng13)*Math.sin(dist13)*Math.cos(lat1), 
				Math.cos(dist13)-Math.sin(lat1)*Math.sin(lat3) );
		double lon3 = lon1+dLon13;

		lon3 = (lon3+3*Math.PI) % (2*Math.PI) - Math.PI;  // normalise to -180..+180º

		Location result = new Location("intersect result");
		result.setLatitude(Math.toDegrees(lat3));
		result.setLongitude(Math.toDegrees(Math.toDegrees(lon3)));

		return (result != null);
	}

	// return in meter
	public double distanceTo(Location start, Location destination){
		double lat1 = Math.toRadians(start.getLatitude());
		double lon1 = Math.toRadians(start.getLongitude());
		double lat2 = Math.toRadians(destination.getLatitude());
		double lon2 = Math.toRadians(destination.getLongitude());
		double dLat = lat2 - lat1;
		double dLon = lon2 - lon1;

		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
				Math.cos(lat1) * Math.cos(lat2) * 
				Math.sin(dLon/2) * Math.sin(dLon/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = 6371 * c;

		return d * 1000;
	}
}