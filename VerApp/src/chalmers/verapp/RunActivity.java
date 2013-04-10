package chalmers.verapp;

import java.math.*;
import chalmers.verapp.interfaces.Constants;
import chalmers.verapp.interfaces.GPSCallback;
import android.location.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

public class RunActivity extends Activity implements GPSCallback{
	private Button stopBtn, pauseBtn;
	private Chronometer clockTime;	
	private TextView tvSpeed;
	private GPSManager gpsManager = null;
	private double speed = 0.0;
	private int measurement_index = Constants.INDEX_KM;
	private long timeWhenStopped = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run);

		// Disable landscape mode
		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Screen always active
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		gpsManager = new GPSManager();
		gpsManager.startListening(getApplicationContext());
		gpsManager.setGPSCallback(this);

		stopBtn = (Button)findViewById(R.id.stop);
		pauseBtn = (Button)findViewById(R.id.pause);
		clockTime = (Chronometer)findViewById(R.id.clockTime);
		tvSpeed = (TextView)findViewById(R.id.tvSpeed);		

		clockTime.start();

		pauseBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Check whether pause or start mode on button				
				if(timeWhenStopped > 0){
					pauseBtn.setText(getResources().getString(R.string.pause_button));
					clockTime.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
					clockTime.start();
					/* clockTime.setBase(SystemClock.elapsedRealtime());
					timeWhenStopped = 0; */
				}else{
					
					clockTime.stop();
				}
			}
		});

		stopBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				timeWhenStopped = clockTime.getBase() - SystemClock.elapsedRealtime();
				clockTime.stop();
			}
		});
	}

	@Override
	public void onGPSUpdate(Location location) 
	{
		Log.d("gps", "UPDATED");

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
		case Constants.INDEX_KM:		string = "km/h";	break;
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

