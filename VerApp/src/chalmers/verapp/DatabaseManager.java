package chalmers.verapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;

public class DatabaseManager extends AsyncTask<String, Void, Void>{

	@Override
	/*
	 * Param1=signalName,Param2=signalValue,Param3=lon, Param4=lat,Param5=warning
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	protected Void doInBackground(String... params) {
		
		String signalName = params[0];
		String signalValue = params[1];
		String lon = params[2];
		String lat = params[3];
		String warning = params[4];
		
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		
		HttpPost httppost = new HttpPost("http://chalmersverateam.se/verapp/dbManager.php");  
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

		try {
			// Test values, these should contain the real data		
			nameValuePairs.add(new BasicNameValuePair("signalName", signalName));
			nameValuePairs.add(new BasicNameValuePair("signalValue", signalValue));
			nameValuePairs.add(new BasicNameValuePair("long", lon));
			nameValuePairs.add(new BasicNameValuePair("lat", lat));
			nameValuePairs.add(new BasicNameValuePair("warning", warning));
			
			/* Log.d("signalName", signalName);
			Log.d("signalValue", signalValue);
			Log.d("long", lon);
			Log.d("lat", lat);
			Log.d("warning", warning); */

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			/* HttpResponse response =*/ httpclient.execute(httppost);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
		
	}	
}
