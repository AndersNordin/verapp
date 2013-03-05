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

public class DatabaseManager extends AsyncTask<Void,Void,Void>{

	@Override
	protected Void doInBackground(Void... arg0) {	
		// Create a new HttpClient and Post HeaderO
		HttpClient httpclient = new DefaultHttpClient();
		
		// computer in school, local IP
		HttpPost httppost = new HttpPost("http://129.16.21.10/verapp/dbManager.php");  
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);

		try {
			// Test values, these should contain the real data		
			nameValuePairs.add(new BasicNameValuePair("signalName", "RPM"));
			nameValuePairs.add(new BasicNameValuePair("signalValue", "13.37"));
			nameValuePairs.add(new BasicNameValuePair("long", "13.37"));
			nameValuePairs.add(new BasicNameValuePair("lat", "13.37"));
			nameValuePairs.add(new BasicNameValuePair("warning", "0"));

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			/* HttpResponse response = */httpclient.execute(httppost);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null; 
	}
	
}
