package chalmers.verapp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import chalmers.verapp.base.BaseActivity;
import chalmers.verapp.interfaces.Constants;
import chalmers.verapp.interfaces.GPSCallback;

public class RunActivity extends BaseActivity implements GPSCallback{
	// Graphical
	private Button stopBtn, startWhenPauseBtn, incidentBtn;
	private Chronometer clockTime;	
	private TextView tvSpeed, tvLapTime1, tvLapTime2;

	// GPS
	private GPSManager gpsManager = null;

	// Values 
	private long timeWhenStopped = 0;
	private boolean isTimeRunning = true;
	private Timer mTimer = new Timer();
	private String warning = "0";
	private ArrayList<Long> lapTimes = new ArrayList<Long>();
	private boolean clockwiseLap;
	boolean validLap = false;

	// Location points	 
	private Location startPos = new Location("start position"); // first point logged
	private double bearingStart = 999.0; // Cars initial direction

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


		// Clockwise or counterclockwise
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					clockwiseLap = true;            
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					clockwiseLap = false;
					break;
				}

				// Start time when something is picked
				clockTime.start();
				clockTime.setBase(SystemClock.elapsedRealtime());
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Is the lap runned clockwise?").setPositiveButton("Yes", dialogClickListener)
		.setNegativeButton("No", dialogClickListener).show();

		// Pause button pushed
		startWhenPauseBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!isTimeRunning){
					// Check whether pause or start mode on button				
					clockTime.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
					clockTime.start();		
					isTimeRunning = true;
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
				}
			}
		});

		// Incident button pushed
		incidentBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				warning = "1";
			}
		});


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
		// Log.d("GPS UPDATE","(" + currentPos.getLatitude() + "," + currentPos.getLongitude() + ") " + currentPos.getBearing() + " TIME: "  + DateFormat.getDateTimeInstance().format(new Date()));

		// Testing
		tvLapTime1.setText("Lap 1: " + (clockTime.getBase() - SystemClock.elapsedRealtime()));	
		tvLapTime2.setText("Lap 2: " + (clockTime.getBase() - SystemClock.elapsedRealtime()));

		// Step 1: Set starting coordinate
		if(startPos.getLatitude() == 0 && startPos.getLongitude() == 0)
			startPos = currentPos;

		// Step 2: Set starting bearing	
		// Step 3: Calculate finish line	
		if(currentPos.hasBearing() && bearingStart > 360){
			bearingStart = (currentPos.getBearing()%360);			
			setPerpendicularLine(currentPos, bearingStart);
		}

		// There are 4 cases
		Location posInter = null,posInter2 = null, posInter3 = null, posInter4 = null;
		double dist , dist2,  dist3, dist4; 
		// Distance to finish line, 999 = invalid value
		dist = dist2 = dist3 = dist4 = 999.0;

		// The car doesn't necessarily has to have the same angel when crossing the finish line 
		if(bearingStart+20 > currentPos.getBearing() && currentPos.getBearing() > bearingStart-20 ){
			posInter =  intersection(finishLinePointRight, bearingStart-90, currentPos, currentPos.getBearing()-180);
			posInter2 = intersection(finishLinePointRight , bearingStart+90, currentPos, currentPos.getBearing()-180);
			posInter3 = intersection(finishLinePointLeft , bearingStart-90, currentPos, currentPos.getBearing()-180);
			posInter4 = intersection(finishLinePointLeft , bearingStart+90, currentPos, currentPos.getBearing()-180);		
		}	

		// Step 6: If so... lap++;
		if(posInter != null){
			dist = distanceTo(currentPos,  posInter);		
		}
		else if(posInter2 != null){
			dist2 = distanceTo(currentPos,  posInter);
		}
		else if(posInter3 != null){
			dist3 = distanceTo(currentPos, posInter3);
		}
		else if(posInter4 != null){
			dist4 = distanceTo(currentPos, posInter4);
		}	



		// Ensure that the car is not passing finishLine twice on the same lap	
		Thread th = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(300000);
					validLap = true;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		th.start();

		// Step 7: Validate that the intersection was correct, which means with in 10 meters
		// Step 8: Set timer to lock lap++;
		if((dist < 10 || dist2 < 10 || dist3 < 10 || dist4 < 10) && validLap){
			Long tempTime = clockTime.getBase() - SystemClock.elapsedRealtime();
			int size = lapTimes.size();
			if(size > 0){ // not first lap
				for(Long t : lapTimes)
					tempTime = tempTime - t;				
			}
			lapTimes.add(tempTime);
			size++;

			if(size > 1)
				tvLapTime2.setText("Lap: "+ (size-1) + ": " + lapTimes.get(lapTimes.size()-2));

			tvLapTime1.setText("Lap: "+ size + ": " + lapTimes.get(lapTimes.size()-1));
			
			validLap = false;
		}

		// Display speed
		String speedString = "" + roundDecimal(convertSpeed(currentPos.getSpeed()),2);
		tvSpeed.setText(speedString + " km/h");
	}

	@Override
	protected void onDestroy(){
		gpsManager.stopListening();
		gpsManager.setGPSCallback(null);

		gpsManager = null;

		super.onDestroy();
	}

	private double convertSpeed(double speed){
		return ((speed * 3600) *  0.001); 
	}

	private double roundDecimal(double value, final int decimalPlace){
		BigDecimal bd = new BigDecimal(value);

		bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
		value = bd.doubleValue();

		return value;
	}

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


		if(clockwiseLap){ 
			finishLinePointLeft.setLatitude(x4);
			finishLinePointLeft.setLongitude(y4);
			finishLinePointRight.setLatitude(x3);
			finishLinePointRight.setLongitude(y3);
		}else{
			finishLinePointRight.setLatitude(x4);
			finishLinePointRight.setLongitude(y4);
			finishLinePointLeft.setLatitude(x3);
			finishLinePointLeft.setLongitude(y3);
		}	
	}

	/**
	 * Returns the point of intersection of two paths defined by point and bearing
	 * See http://williams.best.vwh.net/avform.htm#Intersection
	 */
	public Location intersection(Location f1,double brng1, Location c1, double brng2){
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

		if (dist12 == 0) return null;

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

		if (Math.sin(alpha1)==0 && Math.sin(alpha2)==0) return null;  // infinite intersections
		if (Math.sin(alpha1)*Math.sin(alpha2) < 0) return null;       // ambiguous intersection

		//alpha1 = Math.abs(alpha1);
		//alpha2 = Math.abs(alpha2);
		// ... Ed Williams takes abs of alpha1/alpha2, but seems to break calculation?

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

		return result;
	}

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

		return d;
	}

}

