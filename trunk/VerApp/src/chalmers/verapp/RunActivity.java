package chalmers.verapp;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import android.content.Context;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import chalmers.verapp.base.BaseActivity;
import chalmers.verapp.ecu_connection.EcuManagerTest;
import chalmers.verapp.interfaces.GPSCallback;

public class RunActivity extends BaseActivity implements GPSCallback{
	private Chronometer clockTime = null;
	private TextView tvSpeed, tvLapTime1, tvLapTime2, avgSpeed;
	private Button stop, start, incident, startECU, stopECU;
	private Thread distThread, logThread, newLapThread;
	private GPSManager gpsManager = null;

	// Values 
	private long timeWhenStopped = 0;
	private boolean timeIsRunning = false;
	private ArrayList<Long> lapTimes = new ArrayList<Long>();
	private boolean validLap = false;
	private double totalDistance = 0;
	
	// EcuManager
	private EditText etInputCommand;
	private UsbSerialDriver mSerialDevice;
	private UsbManager mUsbManager;
	private EcuManagerTest mEcuManger;
	private SystemInfo mSysteminfo;

	// Location points	 
	private Location _currentPos, secondLatestPos, startPos;

	// Representing the complete finish line
	/*
	private Location finishLinePointTwo = new Location("Finish Line Point to the left");
	private Location finishLinePointOne = new Location("Finish Line Point to the right");
	*/


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run);
		
		// Screen always active
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		//Systeminfo
		mSysteminfo = new SystemInfo();
		
		//Start EcuManager
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mSerialDevice = UsbSerialProber.acquire(mUsbManager);
		mEcuManger = new EcuManagerTest(mUsbManager, mSerialDevice, mSysteminfo);
		

		

		// Initiate GPS
		gpsManager = new GPSManager();
		gpsManager.startListening(getApplicationContext());
		gpsManager.setGPSCallback(this);

		// Get graphical id's
		tvSpeed = (TextView)findViewById(R.id.tvSpeed);
		tvLapTime1 = (TextView)findViewById(R.id.lapTime2);
		tvLapTime2 = (TextView)findViewById(R.id.lapTime3);
		avgSpeed = (TextView)findViewById(R.id.avgSpeed);
		stop = ((Button)findViewById(R.id.stop));
		start = ((Button)findViewById(R.id.start));
		startECU = (Button) findViewById(R.id.bStartEcu);
		stopECU = (Button) findViewById(R.id.bStopEcu);
		tvSpeed.setText("Searching for GPS signal");

		
		//runLoggingThread();
		//runDistanceThread();	
		
		startECU.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
//				
				mEcuManger.Send("bL0030");
			}
		});
		stopECU.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mEcuManger.Send("v");
				mEcuManger.Shutdown();
				

				
			}
		});
		
	}

	public void ButtonOnClick(View v) {
		switch(v.getId()){
		case R.id.stop:
			if(timeIsRunning){
				timeWhenStopped = clockTime.getBase() - SystemClock.elapsedRealtime();
				clockTime.stop();
				timeIsRunning = false;
				stop.setEnabled(false);
				start.setEnabled(true);
			
				
				Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
			}	        
			break;
		case R.id.incident:
			mSysteminfo.setWarning("1");
			Toast.makeText(getApplicationContext(), "Warning Sent", Toast.LENGTH_SHORT).show();
			break;
		case R.id.start:
			if(!timeIsRunning){						
				clockTime.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
				clockTime.start();		
				timeIsRunning = true;
				stop.setEnabled(true);
				start.setEnabled(false);
			
				if(timeWhenStopped == 0){
					Toast.makeText(getApplicationContext(), "Started", Toast.LENGTH_SHORT).show();
					start.setText("Resume");
				}
				else
					Toast.makeText(getApplicationContext(), "Resumed", Toast.LENGTH_SHORT).show();
			}

//		case R.id.bStartEcuManager:
//			mSerialDevice = UsbSerialProber.acquire(mUsbManager);
//			mEcuManger = new EcuManagerTest(mUsbManager, mSerialDevice);
//			mEcuManger.Send(etInputCommand.getText().toString());
//			clockTime.start();
//			runLoggingThread();
//			runDistanceThread();
//			break;
		}
	}

	/**
	 * A fix for Ice Cream Sandwhich and lower. Start load class in UI Thread
	 */
	private void runLoggingThread(){		
		logThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// if(timeIsRunning)									
									new FileManager().execute(); // Send files
								mSysteminfo.setWarning("0");			
							}
						});
						//Thread.sleep(frequency);
					
				}
			}		
		};
		logThread.start();		
	}

	@Override
	/**
	 * When GPS Coordinate is updated this function is triggered
	 * with the latest coordinate
	 */
	public void onGPSUpdate(Location currentPos){	
		if(timeIsRunning){
			_currentPos = currentPos;
			mSysteminfo.setLatitude(""+currentPos.getLatitude());
			mSysteminfo.setLatitude(""+currentPos.getLongitude());
		}

		if(clockTime == null){
			clockTime = (Chronometer)findViewById(R.id.clockTime);
			tvSpeed.setText("GPS signal OK");
			((Button)findViewById(R.id.start)).setEnabled(true);
			mSysteminfo.setWarning("0");
		}

		Log.i("GPS UPDATE","(" + currentPos.getLatitude() + "," + currentPos.getLongitude() + ") Dir: " + currentPos.getBearing());

		// Step 1: Set starting coordinate
		if(startPos == null)
			startPos = currentPos;

		/*// Step 2: Set starting bearing	
		// Step 3: Calculate finish line	
		if(currentPos.hasBearing() && bearingStart == 999.0){
			bearingStart = currentPos.getBearing();			
			setPerpendicularLine(currentPos, bearingStart);
		}

		// There are 4 cases
		boolean inter1 = false,inter2 = false, inter3 = false, inter4 = false;

		// The car doesn't necessarily has to have the same angel when crossing the finish line 
		if(startPos.getBearing()+20 > currentPos.getBearing() && currentPos.bearingTo(startPos) > startPos.getBearing()-20 ){
			inter1 = intersection(finishLinePointOne, (bearingStart-90), currentPos, (currentPos.getBearing()));
			inter2 = intersection(finishLinePointOne , (bearingStart+90), currentPos, (currentPos.getBearing()));
			inter3 = intersection(finishLinePointTwo , (bearingStart-90), currentPos, (currentPos.getBearing()));
			inter4 = intersection(finishLinePointTwo , (bearingStart+90), currentPos, (currentPos.getBearing()));

			Log.i("Possible Intersection", "Has it really crossed the finishLine?");
		}	

		// Step 6: If so... lap++;
		if((inter2 &&  inter3) || (inter1  && inter4)){
			Log.i("The distance is:", "" + currentPos.distanceTo(startPos));
		} */

		// Ensure that the car is not passing finishLine twice on the same lap	
//		newLapThread = new Thread() {
//			public void run() {
//				try {
//					Thread.sleep(300000); 
//					validLap = true;
//				}catch (InterruptedException e) {
//					throw new RuntimeException(e);
//				}
//			}
//		};
//		
//		newLapThread.start();		

		// Step 7: Validate that the intersection was correct, which means within 10 meters
		// Step 8: Set timer to lock lap++;
		if((currentPos.distanceTo(startPos) < 10) && validLap){
			Log.i("NEW LAP", "LAP++");

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
		if(timeIsRunning){
			double currentSpeed = currentPos.getSpeed();
			String speedString = "" + roundDecimal(convertToKMH(currentSpeed),2);
			this.tvSpeed.setText(speedString + " km/h");
		}

		// Display average speed
		String avgSpeedString = "" + roundDecimal(convertToKMH(totalDistance/((double)(SystemClock.elapsedRealtime() - clockTime.getBase()) / 1000)), 2);

		// Display color of average speed
		if(Double.valueOf(avgSpeedString) > 25)
			avgSpeed.setTextColor(Color.GREEN);
		else
			avgSpeed.setTextColor(Color.RED);

		avgSpeed.setText("Avg Speed: " + avgSpeedString + " km/h");		

		if(secondLatestPos == null && timeIsRunning)
			secondLatestPos = _currentPos;
	}

	/**
	 * Run a thread for calculating distance to calculate lap time
	 */
	private void runDistanceThread() {
		distThread = new Thread() {
			public void run() {
				while (true) {
					try {
						if(_currentPos != null && secondLatestPos != null && timeIsRunning){
							if(secondLatestPos.getLongitude() != 0 && secondLatestPos.getLatitude() != 0 && _currentPos.hasSpeed()){
								totalDistance = totalDistance + secondLatestPos.distanceTo(_currentPos); // in meters
								Log.i("TOTAL DISTANCE", "" + totalDistance);
							}	
							secondLatestPos = _currentPos;
							Thread.sleep(2000);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};		
		distThread.start();
	}

	@Override
	/**
	 * When Run Activity is destroyed
	 * Send callback for GPS 
	 */
	protected void onDestroy(){
		gpsManager.stopListening();
		gpsManager.setGPSCallback(null);
		gpsManager = null;
		mEcuManger.Shutdown();
		try {
			mSerialDevice.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		mEcuManger.Shutdown();
		try {
			mSerialDevice.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		shutdown();		
	}
	
	private void closeDistThread(){
		distThread.interrupt();
		try {
			distThread.join(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void closeNewLapThread(){
		logThread.interrupt();
		try {
			logThread.join(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void closeLogThread(){
		distThread.interrupt();
		try {
			distThread.join(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void shutdown(){
		closeDistThread();
		closeLogThread();
		closeNewLapThread();
	}

	/**
	 * Converts speed from meter per second to kilometer per hour
	 * @param speed given speed in m/s
	 * @return speed in km/h
	 */
	private double convertToKMH(double speed){
		return (speed * 3.6); 
	}

	/**
	 * Rounds value to given decimal
	 * @param value, given number to modify
	 * @param decimalPlace, how many decimal
	 * @return the number as a double
	 */
	private double roundDecimal(double value, final int decimalPlace){
		BigDecimal bd = new BigDecimal(value);

		bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
		value = bd.doubleValue();

		return value;
	}

	/**
	 * Takes start location and sets two perpendicular points 
	 * according to the start point. +/- 90 degrees
	 * @param p1, initial starting point
	 * @param bearing1, initial direction
	 */
	/* NOT NECESSARY 
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


		// normalize to -180..+180�
		y3 = (y3+3*Math.PI) % (2*Math.PI) - Math.PI;  
		y4 = (y4+3*Math.PI) % (2*Math.PI) - Math.PI;  

		x3 = Math.toDegrees(x3);
		y3 = Math.toDegrees(y3);

		x4 = Math.toDegrees(x4);
		y4 = Math.toDegrees(y4);

		finishLinePointTwo.setLatitude(x4);
		finishLinePointTwo.setLongitude(y4);
		finishLinePointOne.setLatitude(x3);
		finishLinePointOne.setLongitude(y3);
	}
	 */	

	/**
	 * Checks if a line is intersecting with another
	 * See http://williams.best.vwh.net/avform.htm#Intersection
	 * @param f1, current point
	 * @param brng1, current direction
	 * @param c1, destination point
	 * @param brng2, destination direction
	 * @return true if they are intersection, otherwise false
	 */
	/* NOT NECESSARY 
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

		lon3 = (lon3+3*Math.PI) % (2*Math.PI) - Math.PI;  // normalise to -180..+180�

		Location result = new Location("intersect result");
		result.setLatitude(Math.toDegrees(lat3));
		result.setLongitude(Math.toDegrees(Math.toDegrees(lon3)));

		return (result != null);
	}	*/
}