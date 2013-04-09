package chalmers.verapp;

import java.math.*;
import chalmers.verapp.interfaces.Constants;
import chalmers.verapp.interfaces.GPSCallback;
import android.location.*;
import android.os.Bundle;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

public class RunActivity extends Activity implements GPSCallback{
	private Button start, stop;
	private Chronometer clockTime;	
	private TextView tvSpeed;

	private GPSManager gpsManager = null;
	private double speed = 0.0;
	private int measurement_index = Constants.INDEX_KM;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run);
		
		// Disable landscape mode
		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		gpsManager = new GPSManager();
		gpsManager.startListening(getApplicationContext());
		gpsManager.setGPSCallback(this);


		// start = (Button)findViewById(R.id.start);
		stop = (Button)findViewById(R.id.stop);
		clockTime = (Chronometer)findViewById(R.id.clockTime);
		tvSpeed = (TextView)findViewById(R.id.tvSpeed);

		clockTime.start();
		
		Log.d("start", "start");

		/*start.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				timer.start();
			}
		});*/
		
		stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
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

