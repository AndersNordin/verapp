package chalmers.verapp.base;

import chalmers.verapp.MainActivity;
import chalmers.verapp.R;
import chalmers.verapp.settings.SettingsActivity;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.Menu;
import android.view.MenuItem;

public class BaseActivity extends Activity {



	private static final int RESULT_SETTINGS = 1;

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
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					Intent intent = new Intent(BaseActivity.this, MainActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					intent.putExtra("Exit me", true);
					startActivity(intent);
					finish();
					break;
				}
			}
		};


		switch (item.getItemId()){
		case R.id.menu_settings:
			// Start settings 
			Intent i = new Intent(BaseActivity.this, SettingsActivity.class);
			startActivityForResult(i, RESULT_SETTINGS);
			break;

		case R.id.menu_exit:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to exit?").setPositiveButton("Yes", dialogClickListener)
			.setNegativeButton("No", dialogClickListener).show();
			break;
		} 
		return true;    	
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case RESULT_SETTINGS:
			SettingsActivity.choseFreq();
			break;
		}
	}

	@Override
	public void onBackPressed (){
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					finish();
					break;
				}
			}
		};		

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to go back?").setPositiveButton("Yes", dialogClickListener)
		.setNegativeButton("No", dialogClickListener).show();

	}

}
