package chalmers.verapp.base;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import chalmers.verapp.MainActivity;
import chalmers.verapp.R;
import chalmers.verapp.SystemInfo;
import chalmers.verapp.ecu_connection.FileLogger;
import chalmers.verapp.interfaces.ILogger;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * This is the base class. The foundation of all activities.
 * @author Anders Nordin
 */
public class BaseActivity extends Activity {

	final static int RESULT_SETTINGS = 1;
	protected int frequency = 5000; // in ms
	
	//ECU
	protected UsbSerialDriver mSerialDevice;
	protected UsbManager mUsbManager;
	protected BlockingQueue<String> mSendQueue;
	protected BlockingQueue<Byte> mReadQueue;
	protected SerialInputOutputManager mSerialIoManager;
	protected ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	protected Thread mWriteThread;
	protected Thread mReadThread;
	protected ILogger mLogger;
	protected SystemInfo mSystemInfo;
	protected Thread mEcuManagerThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Disable landscape mode
		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mSerialDevice = UsbSerialProber.acquire(mUsbManager);
		mSystemInfo = new SystemInfo();
		mSendQueue = new ArrayBlockingQueue<String>(64);
		mReadQueue = new ArrayBlockingQueue<Byte>(2048);
		mLogger = new FileLogger();
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
					System.exit(0);  // Kill all active threads
					break;
				}
			}
		};

		switch (item.getItemId()){
		case R.id.menu_reset:
			Intent intent = new Intent(BaseActivity.this, MainActivity.class);
			startActivity(intent);	
			return true;
			/* case R.id.menu_settings:
			startActivityForResult(new Intent(BaseActivity.this, SettingsActivity.class), RESULT_SETTINGS);
			return true; */

		case R.id.menu_exit:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to exit?").setPositiveButton("Yes", dialogClickListener)
			.setNegativeButton("No", dialogClickListener).show();
			return true;

		default:
			Toast.makeText(getApplicationContext(), "Nothing To Display", Toast.LENGTH_SHORT).show();
			return true;
		}  	
	}

	@Override
	public void onBackPressed (){
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					callRealEnd();
					break;
				}
			}
		};		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to go back?").setPositiveButton("Yes", dialogClickListener)
		.setNegativeButton("No", dialogClickListener).show();	
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RESULT_SETTINGS && resultCode == RESULT_OK) {  
			frequency=data.getIntExtra("freq", 5000); 
			Toast.makeText(getApplicationContext(), "Log Interval: " + (frequency/1000) + "s", Toast.LENGTH_SHORT).show();
		}
	}

	public void callRealEnd(){
		BaseActivity.super.onBackPressed();
	}
}