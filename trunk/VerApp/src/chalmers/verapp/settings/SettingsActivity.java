package chalmers.verapp.settings;

import chalmers.verapp.base.BaseActivity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

public class SettingsActivity extends BaseActivity implements OnPreferenceChangeListener{
	
	int freq;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);				
		getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();	
	}	

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {  
    	Intent returnIntent = new Intent();
    	freq = Integer.valueOf(newValue.toString()) * 1000; 
    	returnIntent.putExtra("freq",freq);
    	setResult(RESULT_OK,returnIntent);     
    	finish();
    	return true;
    }  
}