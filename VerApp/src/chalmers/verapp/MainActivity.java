package chalmers.verapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import chalmers.verapp.base.BaseActivity;

public class MainActivity extends BaseActivity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		
		
		turnOnGPSRequest();	

		// Create listener
		OnClickListener listener = new OnClickListener(){
			@Override
			public void onClick(View v){			
					Intent intent = new Intent(MainActivity.this, RunActivity.class);
					startActivity(intent);				
			}			
		};
		
		// Identify start button
		Button btn = (Button) findViewById(R.id.launch);
		// Connect listener and button
		btn.setOnClickListener(listener);

		// Exit correctly
		if( getIntent().getBooleanExtra("Exit me", false)){
			finish();
			return;
		}
	}

	/**
	 * This functions checks if the gps is enabled.
	 * If it's not the user is prompted and redirected to turn it on.
	 */
	private void turnOnGPSRequest(){
		LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
		if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String message = "Enable either GPS or any other location"
					+ " for better performance";
			builder.setMessage(message)
			.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface d, int id) {
					startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					d.dismiss();
				}
			})
			.setNegativeButton("No",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface d, int id) {
					d.cancel();
				}
			});
			builder.create().show();
		}
	}
}