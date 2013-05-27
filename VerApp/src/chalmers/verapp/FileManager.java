package chalmers.verapp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
/**
 * Sending files from folder to web server.
 * @author Anders Nordin
 */
public class FileManager extends AsyncTask<Void, Void, Void>{
	private static final String URL = "http://www.chalmersverateam.se/verApp.php"; 

	public static final File FILES_WAITING_DIR = new File(Environment.getExternalStorageDirectory() +
			"/Android/data/com.chalmers.civinco/files/waiting/");	
	public static final File FILES_SENT_DIR = new File(Environment.getExternalStorageDirectory() +
			"/Android/data/com.chalmers.civinco/files/sent/");

	/**
	 * Check if sent files folder is created
	 */
	@Override
	protected void onPreExecute() {
		if(!FILES_SENT_DIR.exists())
			FILES_SENT_DIR.mkdirs();
	}

	// http://stackoverflow.com/questions/7943620/error-while-trying-to-upload-file-from-android
	@Override
	protected Void doInBackground(Void...voids){	
		File filesNotSent[] = FILES_WAITING_DIR.listFiles();

		int filesLeft = filesNotSent.length;	

		while(filesLeft > 0){			
			String extension = "NOT ZIP"; 		

			// Excluding folders, they will cause errors otherwise
			if(filesNotSent[filesLeft - 1].isFile())
				extension = getExtension(filesNotSent[filesLeft - 1].getName());
			else{
				filesLeft--;
				continue;				
			}

			if(extension.equals("zip") ){
				HttpURLConnection connection = null;
				DataOutputStream outputStream = null;

				String lineEnd = "\r\n";
				String twoHyphens = "--";
				String boundary = "*****";

				int bytesRead, bytesAvailable, bufferSize;
				byte[] buffer;
				int maxBufferSize =  1024*1024;
				try{					
					FileInputStream fileInputStream = new FileInputStream(filesNotSent[filesLeft - 1]);

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
							+ filesNotSent[filesLeft - 1].getPath() + "\"" + lineEnd);
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

					if(connection.getResponseCode() == 200){							
						File tempF = new File(FILES_SENT_DIR + "/" + filesNotSent[filesLeft - 1].getName());
						if (filesNotSent[filesLeft - 1].renameTo(tempF))
							filesLeft--; 							
					}
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

				File tempF = new File(FILES_SENT_DIR + "/" + filesNotSent[filesLeft - 1].getName());
				if (filesNotSent[filesLeft - 1].renameTo(tempF)){
					filesLeft--; 			
					Log.i("FILE SENT", "OK");
				}

			}else
				filesLeft--;						
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
}