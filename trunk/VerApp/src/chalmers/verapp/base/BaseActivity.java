package chalmers.verapp.base;

import chalmers.verapp.MainActivity;
import chalmers.verapp.R;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
		
		
		//Code for: Are you sure you want to exit?
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(DialogInterface.BUTTON_POSITIVE == which){			    
					 Intent intent = new Intent(BaseActivity.this, MainActivity.class);
					    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					    intent.putExtra("Exit me", true);
					    startActivity(intent);
					    finish();
					
				}
			}			
		};

		switch (item.getItemId()){
		case R.id.menu_settings:
			// Start settings 
			break;

		case R.id.menu_exit:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
			.setNegativeButton("No", dialogClickListener).show();
			break;			   	
		}
		return true; 
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case 0:
			finish();
			break;
		}
	}

}
