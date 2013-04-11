package chalmers.verapp;

import java.math.*;

import chalmers.verapp.base.BaseActivity;
import chalmers.verapp.interfaces.Constants;
import chalmers.verapp.interfaces.GPSCallback;
import android.location.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

public class RunActivity extends BaseActivity implements GPSCallback{
	private Button stopBtn, startWhenPauseBtn, incidentBtn;
	private Chronometer clockTime;	
	private TextView tvSpeed;
	private GPSManager gpsManager = null;
	private double speed = 0.0;
	private int measurement_index = Constants.INDEX_KM;
	private long timeWhenStopped = 0;
	private boolean isTimeRunning = true;

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
				
			}
		});
		
		
	}

	@Override
	public void onGPSUpdate(Location location) 
	{
		String lat = "" + location.getLatitude();
		String lon = "" + location.getLongitude();
		speed = location.getSpeed();

		Log.d("lat", lat);
		Log.d("long", lon);
		Log.d("long", " " + speed);

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

