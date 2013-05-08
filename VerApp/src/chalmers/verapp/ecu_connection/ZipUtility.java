package chalmers.verapp.ecu_connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.util.Log;

public class ZipUtility {

	boolean mRunInBackground = false;
	String mFilePath;
	String mZipFile;
	Thread mThread = null;

	public ZipUtility(String file, String zipFile, boolean runInBackground) {
		mFilePath = file;
		mZipFile = zipFile;
		mRunInBackground = runInBackground;
	}

	public boolean Zip() {
		try {
			if (mRunInBackground) {
				mThread = new Thread() {
					public void run() {
						startZip(mFilePath, mZipFile);
					}
				};
				mThread.start();
				return true;
			} else {
				return startZip(mFilePath, mZipFile);
			}
		} catch (Exception e) {
			Log.v("Zip", "Exception caught in Zip() " + e.toString());
			return false;
		}
	}

	private boolean startZip(String file, String zipFile) {
		final int BUFFER = 2048;
		try {
			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(zipFile);

			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
					dest));

			byte data[] = new byte[BUFFER];

			Log.v("Zip", "Adding: " + file);
			FileInputStream fi = new FileInputStream(file);
			origin = new BufferedInputStream(fi, BUFFER);
			ZipEntry entry = new ZipEntry(
					file.substring(file.lastIndexOf("/") + 1));
			out.putNextEntry(entry);
			int count;
			while ((count = origin.read(data, 0, BUFFER)) != -1) {
				out.write(data, 0, count);
			}
			origin.close();

			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
