package chalmers.verapp;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.view.Menu;


public class MainActivity extends Activity {
	
	UsbDevice device;

	// Start create
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);	//obtaining the USB-device
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void onNewIntent(Intent i){
    	//Kolla så att rätt USB-enhet är ansluten, Denna behövs ev ej
    }
    
}
