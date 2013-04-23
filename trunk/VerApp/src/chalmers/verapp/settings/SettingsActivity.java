package chalmers.verapp.settings;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction()
		.replace(android.R.id.content, new SettingsFragment()).commit();

	}

	public static void choseFreq() {
		//SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		//setLogFreq(sharedPrefs.getString("frequency", "NULL"));	//Inte bra att funktionen har string som parameter, ska vara int! Skapa metoden setLogFreq där frekvensvariabeln används.Det här behöver även skrivas om lite grann. Kanske ha någon form av case sats för att settings ska kunna utökas senare.  

	}

}
