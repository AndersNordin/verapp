package chalmers.verapp.ecu_connection;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import chalmers.verapp.interfaces.ILogger;

import android.os.Environment;


public class FileLogger implements ILogger {

	private final int MaxCount = 10240;
	private File mBackupPath = null;
	private BufferedOutputStream mOutputFile = null;
	private int mCurrentCount = 0;
	private String mOutputPath = "";
	private int fileNr = 0;
	
	
	public FileLogger(){
		mBackupPath = new File(Environment.getExternalStorageDirectory() + "/Android/data/com.chalmers.civinco/files" );
		
	}

	@Override
	public void Open() {
		mCurrentCount = 0;
		fileNr++;
		if(!mBackupPath.exists())
		{
			mBackupPath.mkdirs();
		}
		

		try {
			String timeStamp = DateFormat.getDateInstance().format(new Date());
			mOutputPath = mBackupPath.getPath()+ "/datalog " + timeStamp +"fileNr-"+fileNr +".txt";
		
			mOutputFile = new BufferedOutputStream(new FileOutputStream(mOutputPath,true));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void Close() {
		try {
			mOutputFile.flush();
			mOutputFile.close();
			ZipUtility zipUtility = new ZipUtility(mOutputPath, mOutputPath + ".zip", true);
			zipUtility.Zip();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
	}

	@Override
	public synchronized void WriteLine(String text) {
		try {
			text = text.replaceAll(","," ");
			text = text.replace("[","");
			text = text.replace("]","");
			
			mCurrentCount += text.length();
			mOutputFile.write(text.getBytes());
			
			if(mCurrentCount >= MaxCount)
			{
				Close();
				ZipUtility zipUtility = new ZipUtility(mOutputPath, mOutputPath + ".zip", true);
				zipUtility.Zip();
				Open();
			}
			mOutputFile.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}