package chalmers.verapp.base;

import chalmers.verapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.Menu;
import android.view.MenuItem;

public class BaseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Disable landscape mode
		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public boolean onMenuItemSelected(int featureId, MenuItem item){
		switch (item.getItemId()){

		case R.id.menu_settings:
			 // Start settings 
			 // Intent i = new Intent(this, SettingsActivity.class);
			 // startActivityForResult(i, RESULT_SETTINGS);
			break;

		case R.id.menu_exit:
			this.finish();
			break;
		} 
		return true;    	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			// When case is settings. Enter settings
		}
	}

}
