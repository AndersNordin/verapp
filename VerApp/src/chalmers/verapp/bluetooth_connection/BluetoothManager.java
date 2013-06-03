package chalmers.verapp.bluetooth_connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import chalmers.verapp.SystemInfo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


public class BluetoothManager {
	private static final String TAG = "bluetoothFree2Move";

	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;
	private Thread mBtListenerThread;
	private Handler mHandler;
	private SystemInfo mSystemInfo;
	//Standard UUID, spelar ingen roll för oss.
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// MAC-address to free2Move bluetooth module
	private static String address = "00:0B:CE:0A:34:94";

	public BluetoothManager(SystemInfo systeminfo) {
		this.mSystemInfo = systeminfo;
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBTState();
	}

	public void startBTconnection(){
		BluetoothDevice device = btAdapter.getRemoteDevice(address);
		try {
			btSocket = createBluetoothSocket(device);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		btAdapter.cancelDiscovery();
	    
	    // Establish the connection.  This will block until it connects.
	    Log.d(TAG, "...Connecting...");
	    try {
	      btSocket.connect();
	      Log.d(TAG, "...Connection ok...");
	    } catch (IOException e) {
	      try {
	        btSocket.close();
	      } catch (IOException e2) {
	        
	      }
		
	    }
	    
	    try {
	        outStream = btSocket.getOutputStream();
	      } catch (IOException e) {
	        
	      }
	    StartbtLitsenerThread();

	}


	private void StartbtLitsenerThread(){


		mBtListenerThread = new Thread(){
			public void run(){
				int inputBufferSize = 1024;
				byte[] inputBuffer = new byte[inputBufferSize];
				try {
					InputStream inStream = btSocket.getInputStream();
					int bytesRead = -1;
					String inMsg = "";
					while(true){
						inMsg = "";
						bytesRead = inStream.read(inputBuffer);
						if(bytesRead != -1){
							while((bytesRead == inputBufferSize)&&(inputBuffer[inputBufferSize-1] != 0)){
								inMsg = inMsg + new String(inputBuffer,0,bytesRead);
								bytesRead = inStream.read(inputBuffer);
							}
							inMsg = inMsg + new String(inputBuffer,0,bytesRead-1);
							Log.d("MEDDELANDE FRÅN BLUETOOTH", inMsg);
							deCodeSensorsData(inMsg);

						}
					}


				} catch (IOException e) {
					Log.d("FEL FRÅN START THREAD ", e.toString());
				}

			}


		};
		mBtListenerThread.start();

	}



	public void deCodeSensorsData(String dataString){
		String[] dataList = dataString.split(",");
		Log.d("INNE I DECODERN ", dataList.toString());
		if(dataList.length>5){
			mSystemInfo.setLeftCamber(dataList[1]);
			mSystemInfo.setRightCamber(dataList[2]);
			mSystemInfo.setLeftWheelSpeed(dataList[3]);
			mSystemInfo.setRightWheelSpeed(dataList[4]);
			mSystemInfo.setSteering(dataList[5]);
			mSystemInfo.setAccelerometer(dataList[6]);


		}
		dataList = new String[0];

	}	  



	private void checkBTState() {
		if(btAdapter==null) {
			Log.d(TAG, "PROBLEM IN CheckBtState");
		} else {
			if (btAdapter.isEnabled()) {
				Log.d(TAG, "...Bluetooth ON...");
			} else {
				//Prompt user to turn on Bluetooth
				
			}
		}
	}


	private void sendData(String message) {
		byte[] msgBuffer = message.getBytes();

		Log.d(TAG, "...Send data: " + message + "...");

		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
			if (address.equals("00:0B:CE:0A:34:94"))
				msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
			msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

		}
	}

	private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
		if(Build.VERSION.SDK_INT >= 10){
			try {
				final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, MY_UUID);
			} catch (Exception e) {
				Log.e(TAG, "Could not create Insecure RFComm Connection",e);
			}
		}
		return  device.createRfcommSocketToServiceRecord(MY_UUID);
	}		  



	public void closeBTconnection(){
		if (outStream != null) {
			try {
				outStream.flush();
			} catch (IOException e) {
				Log.d(TAG, "ERROR IN CloseBtConnection...");  
			}
		}

		try     {
			btSocket.close();
		} catch (IOException e2) {
			Log.d(TAG, "ERROR IN closeBtConnection, failed to close socket");
		}
		mBtListenerThread.interrupt();
		try {
			mBtListenerThread.join(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}





}
