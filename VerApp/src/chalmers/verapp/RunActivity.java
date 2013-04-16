package chalmers.verapp;

import java.math.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import chalmers.verapp.base.BaseActivity;
import chalmers.verapp.interfaces.Constants;
import chalmers.verapp.interfaces.GPSCallback;
import android.location.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

public class RunActivity extends BaseActivity implements GPSCallback{
	private Button stopBtn, startWhenPauseBtn, incidentBtn;
	private Chronometer clockTime;	
	private TextView tvSpeed, tvLapTime;
	private GPSManager gpsManager = null;
	private double speed = 0.0;
	private int measurement_index = Constants.INDEX_KM;
	private long timeWhenStopped = 0;
	private boolean isTimeRunning = true;
	private Timer mTimer = new Timer();
	private String warning = "0";
	private double lat, lon, startLon = 0, startLat = 0, secondLon, secondLat;
	private ArrayList<Long> lapTimes = new ArrayList<Long>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run);

		// Screen always active
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		gpsManager = new GPSManager();
		gpsManager.startListening(getApplicationContext());
		gpsManager.setGPSCallback(this);

		stopBtn = (Button)findViewById(R.id.stop);
		startWhenPauseBtn = (Button)findViewById(R.id.start_when_paused);
		incidentBtn = (Button)findViewById(R.id.incident);
		clockTime = (Chronometer)findViewById(R.id.clockTime);
		tvSpeed = (TextView)findViewById(R.id.tvSpeed);
		tvLapTime = (TextView)findViewById(R.id.lapTime);

		clockTime.start();

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

		incidentBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				warning = "1";
			}
		});

		mTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				new DatabaseManager().execute("rpm", "2000", String.valueOf(lat), String.valueOf(lon), warning);
				warning = "0";
			}
		}, 0, 15000); // Insert refresh time from settings


	}

	@Override
	public void onGPSUpdate(Location location){
		
		
		lat = location.getLatitude();
		lon = location.getLongitude();
		


		if(startLon == lon && startLat == lat){ // If crossing the start line
			int size = lapTimes.size();

			if(size > 0){				
				long latestLapTime = clockTime.getBase() - SystemClock.elapsedRealtime();
				for(int i = 0; i < size; i++)
					latestLapTime = latestLapTime - lapTimes.get(size-i);

				lapTimes.add(latestLapTime);
				tvLapTime.setText("Last Lap: " + String.valueOf(latestLapTime));
			}else{ // first lap
				lapTimes.add(lapTimes.get(0));
				tvLapTime.setText("Last Lap: " + String.valueOf(lapTimes.get(0)));
			}

		}

		// Set starting position
		if(startLon == 0 && startLat == 0){
			startLon = lon;
			startLat = lat;
		}
		
		// Set direction position, second point
		if(startLon != 0 && secondLon == 0){
			
			
		}

		
		speed = location.getSpeed();		

		String speedString = "" + roundDecimal(convertSpeed(speed),2);
		String unitString = measurementUnitString(measurement_index);

		tvSpeed = (TextView) findViewById(R.id.tvSpeed);
		tvSpeed.setText(speedString + " " + unitString);	
	}

	@Override
	protected void onDestroy() {
		gpsManager.stopListening();
		gpsManager.setGPSCallback(null);

		gpsManager = null;

		super.onDestroy();
	}

	private double convertSpeed(double speed){
		return ((speed * Constants.HOUR_MULTIPLIER) * Constants.UNIT_MULTIPLIERS[measurement_index]); 
	}

	private String measurementUnitString(int unitIndex){
		String string = "";

		switch(unitIndex)
		{
		case Constants.INDEX_KM:string = "km/h";	break;
		case Constants.INDEX_MILES:	string = "mi/h";	break;
		}

		return string;
	}

	private double roundDecimal(double value, final int decimalPlace)
	{
		BigDecimal bd = new BigDecimal(value);

		bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
		value = bd.doubleValue();

		return value;
	}
}

