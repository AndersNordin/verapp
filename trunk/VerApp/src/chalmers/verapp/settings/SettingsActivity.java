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
		//setLogFreq(sharedPrefs.getString("frequency", "NULL"));	//Inte bra att funktionen har string som parameter, ska vara int! Skapa metoden setLogFreq d�r frekvensvariabeln anv�nds.Det h�r beh�ver �ven skrivas om lite grann. Kanske ha n�gon form av case sats f�r att settings ska kunna ut�kas senare.  

	}

}
