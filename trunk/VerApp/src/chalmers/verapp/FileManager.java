package chalmers.verapp;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class FileManager extends AsyncTask<Void, Void, Void>{
	private static final String URL = "www.chalmersverateam.se/zipParser.php"; 

	public static final File FILE_HISTORY_DIR = new File(Environment.getExternalStorageDirectory() +
			"/Android/data/com.chalmers.civinco/files/history/");	
	public static final File FILE_LOGS_DIR = new File(Environment.getExternalStorageDirectory() +
			"/Android/data/com.chalmers.civinco/files/");
	public static final File FILE_HISTORY = new File(FILE_HISTORY_DIR, "file_history.txt");
	private File[] listOfFiles;
	
	/**
	 * List all files present in the folder
	 */
	@Override
	protected void onPreExecute() {
		if(!FILE_HISTORY_DIR.exists())
			FILE_HISTORY_DIR.mkdirs();
		
		listOfFiles = FILE_LOGS_DIR.listFiles();
	}

	@Override
	protected Void doInBackground(Void...voids){	
		HashMap<String, String> sentFiles = readFromFile();

		for (int i = 0; i < listOfFiles.length; i++) {
			
				// Exclude directories
				String extension = "NOT ZIP";
				if(listOfFiles[i].isFile())
					extension = getExtension(listOfFiles[i]);

			if(sentFiles.get(listOfFiles[i].getName()) == null && extension.equals("zip") ){
					addToFileHistory(listOfFiles[i].getName());
					Log.i("SENDING ", listOfFiles[i].getName());
					/*try {
						 HttpClient httpClient = new DefaultHttpClient();						
						 HttpPost httpPost = new HttpPost(URL);

						InputStreamEntity reqEntity = new InputStreamEntity(
								new FileInputStream(listOfFiles[i]), -1);

						reqEntity.setContentType("binary/octet-stream");					
						reqEntity.setChunked(true); // Send in multiple parts if needed
						httpPost.setEntity(reqEntity);
						HttpResponse response = httpClient.execute(httpPost);
					} catch (Exception e) {
						e.printStackTrace();
					}*/
				}		
		} 
		return null;
	}	
	
	public static String getExtension(File f) {
	    String ext = null;
	    String s = f.getName();
	    int i = s.lastIndexOf('.');

	    if (i > 0 &&  i < s.length() - 1) {
	        ext = s.substring(i+1).toLowerCase();
	    }
	    return ext;
	}



	public void addToFileHistory(String text){		
		try{			
			FileOutputStream f1 = new FileOutputStream(FILE_HISTORY,true);
			PrintStream p = new PrintStream(f1);
			p.print(text + "\n");
			p.close();
			f1.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   
	}

	public HashMap<String, String> readFromFile(){
		HashMap<String, String> textLines = new HashMap<String, String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(FILE_HISTORY));
			String line;

			while ((line = br.readLine()) != null) 		    	
				textLines.put(line, "Sent");	    
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return textLines;
	}
}