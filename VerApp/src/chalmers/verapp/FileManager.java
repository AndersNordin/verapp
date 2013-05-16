package chalmers.verapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
/**
 * Sending files from folder to web server.
 * @author Anders Nordin
 */
public class FileManager extends AsyncTask<String[], Void, Void>{
	private static final String URL = "http://www.chalmersverateam.se/verApp.php"; 

	public static final File FILE_HISTORY_DIR = new File(Environment.getExternalStorageDirectory() +
			"/Android/data/com.chalmers.civinco/files/history/");	
	public static final File FILE_LOGS_DIR = new File(Environment.getExternalStorageDirectory() +
			"/Android/data/com.chalmers.civinco/files/");
	public static final File FILE_HISTORY = new File(FILE_HISTORY_DIR, "file_history.txt");


	/**
	 * List all files present in the folder
	 */
	@Override
	protected void onPreExecute() {
		if(!FILE_HISTORY_DIR.exists())
			FILE_HISTORY_DIR.mkdirs();
	}

	// http://stackoverflow.com/questions/7943620/error-while-trying-to-upload-file-from-android
	@Override
	protected Void doInBackground(String[]...listOfFiles){	
		Log.i("SIZE",""+ listOfFiles.length );
		HashMap<String, String> sentFiles = readFromFile();
		for (int i = 0; i < listOfFiles.length; i++) {
			// Exclude directories
			String extension = "NOT ZIP";
			
			// Excluding folders, they will cause errors otherwise
			if(new File(FILE_LOGS_DIR + "/" + listOfFiles[i]).isFile())
				extension = getExtension(listOfFiles[i].toString());			
			
			if(sentFiles.get(listOfFiles[i]) == null && extension.equals("zip") ){
				addToFileHistory(listOfFiles[i].toString());
				
				HttpURLConnection connection = null;
				DataOutputStream outputStream = null;

				String lineEnd = "\r\n";
				String twoHyphens = "--";
				String boundary = "*****";

				int bytesRead, bytesAvailable, bufferSize;
				byte[] buffer;
				int maxBufferSize = 1073741824; // 1gb
				try{

					FileInputStream fileInputStream = new FileInputStream(new File(FILE_LOGS_DIR + "/" + listOfFiles[i]));

					URL url = new URL(URL);

					connection = (HttpURLConnection) url.openConnection();
					connection.setDoInput(true);
					connection.setDoOutput(true);
					connection.setUseCaches(false);
					connection.setRequestProperty("Connection", "Keep-Alive");
					connection.setRequestProperty("Content-Type",
							"multipart/form-data;boundary=" + boundary);

					outputStream = new DataOutputStream(connection.getOutputStream());
					outputStream.writeBytes(twoHyphens + boundary + lineEnd);
					outputStream
					.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
							+FILE_LOGS_DIR + "/" + listOfFiles[i] + "\"" + lineEnd);
					outputStream.writeBytes(lineEnd);

					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					buffer = new byte[bufferSize];

					bytesRead = fileInputStream.read(buffer, 0, bufferSize);

					while (bytesRead > 0) {
						outputStream.write(buffer, 0, bufferSize);
						bytesAvailable = fileInputStream.available();
						bufferSize = Math.min(bytesAvailable, maxBufferSize);
						bytesRead = fileInputStream.read(buffer, 0, bufferSize);
					}

					outputStream.writeBytes(lineEnd);
					outputStream.writeBytes(twoHyphens + boundary + twoHyphens
							+ lineEnd);

					String serverResponseMessage = connection.getResponseMessage();
					int serverResponseCode = connection.getResponseCode();

					Log.i("RESPONSE",serverResponseCode +  serverResponseMessage);

					fileInputStream.close();
					outputStream.flush();
					outputStream.close();
				}catch (FileNotFoundException e){
					e.printStackTrace();
				}catch (MalformedURLException e) {
					e.printStackTrace();
				}catch (IOException e) {
					e.printStackTrace();
				}
			}	
		}
		return null;
	}	
	
	/**
	 * Cuts out extension from file
	 * @param s full file name
	 * @return extension(.zip,.txt etc)
	 */
	public static String getExtension(String s) {
		String ext = null;
		int i = s.lastIndexOf('.');
		
		if (i > 0 &&  i < s.length() - 1) {
			ext = s.substring(i+1).toLowerCase();
		}
		return ext;
	}

	/**
	 * Adding file to file history
	 * @param text is file name
	 */
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

	/**
	 * Creates a hashmap of all files in the directory
	 * @return hashmap with all files in folder
	 */
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