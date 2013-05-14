package chalmers.verapp.ecu_connection;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.hardware.usb.UsbManager;
import android.util.Log;

import chalmers.verapp.SystemInfo;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class EcuManagerTest {
	private UsbSerialDriver mSerialDevice;
	private UsbManager mUsbManager;
	private BlockingQueue<String> mSendQueue;
	private BlockingQueue<Byte> mReadQueue;
	private SerialInputOutputManager mSerialIoManager;
	private ExecutorService mExecutor = Executors
			.newSingleThreadExecutor();
	private Thread mWriteThread;
	private Thread mReadThread;
	private ILogger mLogger;
	private SystemInfo mSystemInfo;
	public Thread mEcuManagerThread;
	
	private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			mLogger.WriteLine("Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			mEcuManagerThread = new Thread(){
				public void run()
				{
					updateReceivedData(data);
				}
			};
			mEcuManagerThread.start();
//			MainActivity.this.runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					MainActivity.this.updateReceivedData(data);
//				}
//			});
		}

	};
	
	
	public EcuManagerTest(UsbManager manager, UsbSerialDriver usbSerial, SystemInfo sysinfo ){
		mUsbManager = manager;
		mSerialDevice = usbSerial;
		mSystemInfo = sysinfo;
		mSendQueue = new ArrayBlockingQueue<String>(64);
		mReadQueue = new ArrayBlockingQueue<Byte>(2048);
		mLogger = new FileLogger();
		mLogger.Open();			
		//mSendQueue.add("bL0002");
				
//		mSerialDevice = UsbSerialProber.acquire(mUsbManager);
		if (mUsbManager == null) {
        	mLogger.WriteLine("mUsbManager == null");
        }
        if (mSerialDevice == null) {
        	mLogger.WriteLine("mSerialDevice == null");
            //mTitleTextView.setText("No serial device.");
        } else {
            try {
                mSerialDevice.open();
               
                mSerialDevice.setParameters(115200, UsbSerialDriver.DATABITS_8, 
                         UsbSerialDriver.STOPBITS_1,
                        UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                mLogger.WriteLine("Error setting up device: " + e.getMessage());
                try {
                    mSerialDevice.close();
                } catch (IOException e2) {
                    mLogger.WriteLine("Exception caught in try(serialOpen) " + e2.toString());
                }
                mSerialDevice = null;
                return;
            }
            //mTitleTextView.setText("Serial device: " + mSerialDevice);
        }   
        
        
        startIoManager();
		startReadThread();
		startWriteThread();
		//startWriteThread();
       // onDeviceStateChange();
        //stopIoManager();
        //stopWriteThread();
        //startIoManager();
       // startReadThread();
       // startWriteThread();	
	}
	
	private void startWriteThread(){
		//mSendQueue.add("bL0002");
		mWriteThread = new Thread() {
			public void run() {
				try {
					while (!this.isInterrupted()) {
						mLogger.WriteLine("Inne startWrite thread");
						String message = mSendQueue.take();
						char buffer[] = message.toCharArray();
						boolean error = false;
						for (int i = 0; i < buffer.length; i++)
						{
							if (mSerialDevice.write(
									new byte[] { (byte) buffer[i] }, 200) > 0)
							{
								// Log success
								
							} else {
								error = true;
							}
						}
						if(error)
						{
							mLogger.WriteLine("WriteThread failed to write: " + message);
						}
						else
						{
							mLogger.WriteLine("WriteThread wrote: " + message);
						}
					}
				} catch (InterruptedException e){
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					mLogger.WriteLine("Exception caught in WriteThread: " + e.toString());
				}
			}
		};
		mWriteThread.start();
	}
	
	
//	private void startWriteThread(){
//		//mSendQueue.add("bL0002");
//		mWriteThread = new Thread() {
//			public void run() {
//				mLogger.WriteLine("Before try run in startWriteThread!");
//				try {
//					
//						mLogger.WriteLine("Inne startWrite thread");
//						String message = "bL0002";
//						char buffer[] = message.toCharArray();
//						boolean error = false;
//						for (int i = 0; i < buffer.length; i++)
//						{
//							if (mSerialDevice.write(
//									new byte[] { (byte) buffer[i] }, 200) > 0)
//							{
//								// Log success
//								
//							} else {
//								error = true;
//							}
//						}
//						if(error)
//						{
//							mLogger.WriteLine("WriteThread failed to write: " + message);
//						}
//						else
//						{
//							mLogger.WriteLine("WriteThread wrote: " + message);
//						}
//					
//				} catch (Exception e) {
//					mLogger.WriteLine("Exception caught in WriteThread: " + e.toString() + e.toString());
//				}
//			}
//		};
//		mWriteThread.start();
//	}

private void startReadThread(){
		
		mReadThread = new Thread() {
			public void run() {
				try {
					mLogger.WriteLine("Started Read Thread\n");
					ArrayList<Byte> message = new ArrayList<Byte>();
					ArrayList<Integer> shortMsg = new ArrayList<Integer>();
					byte lastByte = 0x00;
					
					
					while (!this.isInterrupted()) {
						
						byte buffer = mReadQueue.take();
						
						message.add(buffer);
						
						if (lastByte == 0x44 && buffer == 0x53) {
							
							if(message.size()==105){
								message.remove(0);
								message.remove(0);
								message.remove(0);
								message.remove(message.size()-1);
								message.remove(message.size()-1);
								message.remove(message.size()-1);
								message.remove(message.size()-1);
								
								while(message.size()>0){
									byte firstByte = message.remove(0);
									byte secondByte = message.remove(0);
									short temp1 = (short) firstByte;
									short temp2 = (short) secondByte;
									int val = (int) ((temp1 << 8) | (temp2 & 0xFF));
									if(val<0){
										val = 6553+val+1; 
									}
									shortMsg.add(val);
								}
								
								mLogger.WriteLine(" " +shortMsg.toString() +" sWheelSpeed," +
										"sG-force sChain sCamber sSteering "+" "+mSystemInfo.getWarning()+" "+mSystemInfo.getLongitude()+" "+mSystemInfo.getLatitude() + "\n");
								}
							shortMsg.clear();
							message.clear();
						}
						lastByte = buffer;
					}
				} catch (Exception e) {
					Thread.currentThread().interrupt();
					mLogger.WriteLine("Exception in readtread: "
							+ e.getMessage());
				}
			}
		};
		mReadThread.start();		
	}
//	private void startReadThread(){
//		mReadThread = new Thread() {
//			public void run() {
//				try {
//					mLogger.WriteLine("Started Read Thread\n");
//					ArrayList<Byte> message = new ArrayList<Byte>();
//					byte lastByte = 0x00;
//					while (!this.isInterrupted()) {
//						
//						byte buffer = mReadQueue.take();
//						message.add(buffer);
//						if (lastByte == 0x44 && buffer == 0x53) {
//							
//							mLogger.WriteLine(mSystemInfo.getLongitude() + " " + mSystemInfo.getLatitude() + " " + message.toString() + "\n");
//							message.clear();
//						}
//						lastByte = buffer;
//					}
//				} catch (Exception e) {
//					Thread.currentThread().interrupt();
//					mLogger.WriteLine("Exception in readtread: "
//							+ e.getMessage() + e.toString());
//				}
//			}
//		};
//		mReadThread.start();
//
//	}


	
	
	private void stopWriteThread() {
		mWriteThread.interrupt();
		try {
			mWriteThread.join(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void stopReadThread() {
		mReadThread.interrupt();
		try {
			mReadThread.join(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i("", "Stopping io manager ..");
			mSerialIoManager.stop();
			mSerialIoManager = null;
		}
	}

	private void startIoManager() {
		if (mSerialDevice != null) {
			Log.i("", "Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(mSerialDevice,
					mListener);
			mExecutor.submit(mSerialIoManager);
			mLogger.WriteLine("StartIOManager started");
		}
		else
		{
			mLogger.WriteLine("StartIOManager: mSerialDevice is null");
		}
	}

	private void updateReceivedData(byte[] data) {
		//mLogger.WriteLine("UpdateReceiveData");
		for (int i = 0; i < data.length; i++) {
			try {
				mReadQueue.put(data[i]);
			} catch (InterruptedException e) {
				mLogger.WriteLine("UpdateReceivedData interrupted");

			}
		}
	}
	
	public void Send(String msg)
	{
		try {
			mSendQueue.put(msg);
		} catch (Exception e) {
			mLogger.WriteLine("EcuManagerTest::Send() caught exception " + e.toString());
		}
	}
	
	public void Shutdown()
	{
		mLogger.WriteLine("EcuManagerTest Shutdown()");
		try {
			mSendQueue.put("v");
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			mLogger.WriteLine("Exception caught in shutdown()");
		}
		stopWriteThread();
		stopReadThread();
		stopIoManager();
		mLogger.Close();
		
	}
	
    private void onDeviceStateChange() {
//        stopIoManager();
//        stopWriteThread();
//        startIoManager();
//        startReadThread();
//        startWriteThread();
        
    }
	
	

}
