package chalmers.verapp.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import chalmers.verapp.R;

public class SettingsFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.settings);		
		
		OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {				
				OnPreferenceChangeListener listener = ( OnPreferenceChangeListener) getActivity();
				listener.onPreferenceChange(preference, newValue);
				return true;
			}
		};
		
		ListPreference p = (ListPreference)getPreferenceManager().findPreference("frequency");		
		p.setOnPreferenceChangeListener(onPreferenceChangeListener);
	}

}
