package chalmers.verapp;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import chalmers.verapp.base.BaseActivity;
import chalmers.verapp.ecu_connection.EcuManagerTest;
import chalmers.verapp.interfaces.GPSCallback;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

/**
 * Main activity. Contains everything that involves run mode.
 * @author Anders Nordin, Joanna Eriksson
 */
public class RunActivity extends BaseActivity implements GPSCallback{
	// GUI
	private Chronometer clockTime = null;
	private TextView tvSpeed, tvLapTime1, tvLapTime2, avgSpeed, textSteer;
	private Button stop, start;
	private Thread distThread, logThread, newLapThread;
	private GPSManager gpsManager = null;
	private View rect, rect2;

	// steering indicator
	int nomSteer = 500; // calibrated nominal value
	int maxSteer; 
	int minSteer; 
	int diff = 300; // the amount the steering can differ around its nominal value

	// Dashboard values
	private long timeWhenStopped = 0;
	private boolean timeIsRunning = false;
	private ArrayList<Long> lapTimes = new ArrayList<Long>();
	private boolean validLap = false;
	private double totalDistance = 0;
	private String avgSpeedString;

	// EcuManager
	private UsbSerialDriver mSerialDevice;
	private UsbManager mUsbManager;
	private EcuManagerTest mEcuManger;
	private SystemInfo mSysteminfo;

	// Locations 
	private Location _currentPos, secondLatestPos, startPos;
	private String[] listOfFiles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run);
		
		// Initiate values
		maxSteer = nomSteer + diff;
		minSteer = nomSteer - diff;		

		// Screen always active
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
		tvSpeed.setText("Loading GPS");
		rect = (View) findViewById(R.id.myRectangleView);
		rect2 = (View) findViewById(R.id.myRectangleView2);
		textSteer = (TextView) findViewById(R.id.dispSteer);

		// runLoggingThread();
		runDistanceThread();
	}

	public void ButtonOnClick(View v) {
		switch(v.getId()){
		case R.id.stop:
			if(timeIsRunning){
				mEcuManger.Send("v"); // end logging
				mEcuManger.Shutdown();		
				timeWhenStopped = clockTime.getBase() - SystemClock.elapsedRealtime();
				clockTime.stop();
				
				timeIsRunning = false;
				stop.setEnabled(false);
				start.setEnabled(true);
				Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
				printEndRace();
			}	        
			break;
		case R.id.incident:
			mSysteminfo.setWarning("1");
			Toast.makeText(getApplicationContext(), "Warning Sent", Toast.LENGTH_SHORT).show();
			break;
		case R.id.start:
			if(!timeIsRunning){
				mEcuManger.Send("bL0030"); // Start logging cmd
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
		}
	}

/*	private void runLoggingThread(){
		logThread = new Thread(){
			@Override
			public void run() {
				while (true) {
					if(timeIsRunning)							
						
					mSysteminfo.setWarning("0");
				}
			}		
		};
		logThread.start();	
	}*/

	@Override
	/**
	 * When GPS Coordinate is updated this function is triggered
	 * with the latest coordinate
	 */
	public void onGPSUpdate(Location currentPos){	
		_currentPos = currentPos;
		
		Log.i("GPS UPDATE","(" + currentPos.getLatitude() + "," + currentPos.getLongitude() + ") Dir: " + currentPos.getBearing());

		// Initial
		if(clockTime == null){
			clockTime = (Chronometer)findViewById(R.id.clockTime);
			tvSpeed.setText("GPS signal OK");
			start.setEnabled(true);
		}	

		// Rest of the code is only performed when time is running
		if(timeIsRunning){			
			mSysteminfo.setLatitude(""+currentPos.getLatitude());
			mSysteminfo.setLatitude(""+currentPos.getLongitude());
			
			if(startPos == null)
				startPos = currentPos;

			// Ensure that the car is not passing finishLine twice on the same lap	
			newLapThread = new Thread() {
				public void run() {
					try {
						Thread.sleep(15000); 
						validLap = true;
					}catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			};
			newLapThread.start();
			
			if((currentPos.distanceTo(startPos) < 10) && validLap){
				Long timeInMilliSec = (SystemClock.elapsedRealtime() - clockTime.getBase()); // in ms

				int size = lapTimes.size();
				if(size > 0){ // not first lap
					for(Long t : lapTimes)
						timeInMilliSec = timeInMilliSec - t;				
				}
				lapTimes.add(timeInMilliSec);
				size++;

				tvLapTime1.setText("Lap "+ size + ": " + createTimeString(timeInMilliSec));

				if(size > 1){
					timeInMilliSec = lapTimes.get(size-2);
					tvLapTime2.setText("Lap "+ (size-1)+ ": " + createTimeString(timeInMilliSec));
				}					
				validLap = false;
			}

			// Display speed
			double currentSpeed = _currentPos.getSpeed();
			String speedString = "" + roundDecimal(convertToKMH(currentSpeed),2);
			this.tvSpeed.setText(speedString + " km/h");
			
			avgSpeedString = "" + roundDecimal(convertToKMH(totalDistance/((double)(SystemClock.elapsedRealtime() - clockTime.getBase()) / 1000)), 2);

			// Set color for avgSpeed textview
			if(Double.valueOf(avgSpeedString) > 25)
				avgSpeed.setTextColor(Color.GREEN);
			else
				avgSpeed.setTextColor(Color.RED);

			avgSpeed.setText("Avg Speed: " + avgSpeedString + " km/h");		

			if(secondLatestPos == null && timeIsRunning)
				secondLatestPos = currentPos;
		}
	}

	/**
	 * @param time
	 * @return String as format hh:mm:ss
	 */
	private String createTimeString(Long time){
		int seconds = (int) (time / 1000) % 60 ;
		int minutes = (int) ((time/ (1000*60)) % 60);
		int hours   = (int) ((time / (1000*60*60)) % 24);

		return(hours + ":" + minutes + ":" + seconds );
	}

	/**
	 * Run a thread for calculating distance to calculate lap time
	 */
	private void runDistanceThread() {
		distThread = new Thread() {
			public void run() {
				while (true) {
					try {
						if(_currentPos != null && secondLatestPos != null && timeIsRunning && _currentPos.hasSpeed()){
							totalDistance = totalDistance + secondLatestPos.distanceTo(_currentPos); // in meters
							Log.i("TOTAL DISTANCE", "" + totalDistance);							
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
			e.printStackTrace();
		}
		closeDistThread();
		// closeLogThread();	
	}

	private void closeDistThread(){
		distThread.interrupt();
		try {
			distThread.join(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	/*private void closeLogThread(){
		logThread.interrupt();
		try {
			logThread.join(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}*/

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
	 * Prints out final stats
	 */
	private void printEndRace(){
		// Where to display it, set context
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder((Context)this);
		
		alertDialogBuilder.setTitle("Do you want to end race?");

		// Compose the message string
		String summaryString = "Summary \n\n" + 
				"Total time: " + createTimeString(SystemClock.elapsedRealtime() - clockTime.getBase()) + "\n"	+	
				"Avg Speed: " + avgSpeedString + " km/h \n" +
				"Laps: " + lapTimes.size() + "\n";
		
		int lapNr = 1;
		for(Long l : lapTimes){
			summaryString += "Lap " + lapNr + ": " + createTimeString(l);
			lapNr++;
		}

		if(lapNr == 1)
			summaryString += "No laps completed";

		alertDialogBuilder
		.setMessage(summaryString)
		.setCancelable(false)
		.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				callRealEnd();
			}
		})
		.setNegativeButton("No",new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				dialog.cancel();
			}
		});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	/**
	 * Changes the steering indicator according to the logged value.
	 * @param steer, the steering
	 */
	public void theRectangle(int steer) {
		RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams)rect2.getLayoutParams();

		RelativeLayout.LayoutParams t = (RelativeLayout.LayoutParams)textSteer.getLayoutParams();

		// setting the width of the bar
		rect2.getLayoutParams().width = Math.abs(steer - nomSteer) * (rect.getLayoutParams().width) / (maxSteer - minSteer);

		// displaying the steering
		textSteer.setText(Integer.toString(steer));

		// the driver is turning left
		if (steer < nomSteer) {
			params2.leftMargin = rect.getLayoutParams().width / 2 - rect2.getLayoutParams().width;
			t.leftMargin = 0;
		}

		// the driver is turning right
		else {
			params2.leftMargin = rect.getLayoutParams().width / 2;
			t.leftMargin = rect2.getLayoutParams().width - textSteer.getWidth();
		}
		GradientDrawable sd = (GradientDrawable) rect2.getBackground().mutate();

		// the steering is outside the accepted interval => red color
		if (Math.abs(steer - nomSteer) > diff / 3)
			sd.setColor(0xffff0000);
		// the steering is within the accepted interval => green color
		else
			sd.setColor(0xff00ff00);

		sd.invalidateSelf();
	}
}