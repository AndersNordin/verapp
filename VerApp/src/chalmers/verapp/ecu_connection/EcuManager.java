package chalmers.verapp.ecu_connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.app.Activity;
import android.hardware.usb.UsbManager;
import android.util.Log;
import chalmers.verapp.SystemInfo;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class EcuManager extends Activity {
	private UsbSerialDriver mSerialDevice;
	private UsbManager mUsbManager;
	private BlockingQueue<String> mSendQueue;
	private BlockingQueue<Byte> mReadQueue;
	private SerialInputOutputManager mSerialIoManager;
	private final ExecutorService mExecutor = Executors
			.newSingleThreadExecutor();
	private Thread mWriteThread;
	private Thread mReadThread;
	private Thread mEcuManagerThread;
	private ILogger mLogger;
	private SystemInfo mSystemInfo;
	
	private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d("", "Runner stopped.");
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
		}

	};

	
	
	public EcuManager(){
		//mSystemInfo = sysinfo;
		mSendQueue = new ArrayBlockingQueue<String>(64);
		mReadQueue = new ArrayBlockingQueue<Byte>(2048);
		mLogger = new FileLogger();
		mLogger.Open();
		
		startReadThread();
		startWriteThread();
		onDeviceStateChange();
		start("bL0002");
		
	}
	
	public void start(String ecuCommand){
		mSendQueue.add(ecuCommand);
		
		 mSerialDevice = UsbSerialProber.acquire(mUsbManager);
	        Log.d("", "Resumed, mSerialDevice=" + mSerialDevice);
	        if (mSerialDevice == null) {
	            //mTitleTextView.setText("No serial device.");
	        } else {
	            try {
	                mSerialDevice.open();
	               
	                mSerialDevice.setParameters(115200, UsbSerialDriver.DATABITS_8, 
	                         UsbSerialDriver.STOPBITS_1,
	                        UsbSerialDriver.PARITY_NONE);
	            } catch (IOException e) {
	                Log.e("", "Error setting up device: " + e.getMessage(), e);
	                //mTitleTextView.setText("Error opening device: " + e.getMessage());
	                try {
	                    mSerialDevice.close();
	                } catch (IOException e2) {
	                    // Ignore.
	                }
	                mSerialDevice = null;
	                return;
	            }
	            //mTitleTextView.setText("Serial device: " + mSerialDevice);
	        }
	        onDeviceStateChange();
		
		
	}

	private void startWriteThread(){
		mWriteThread = new Thread() {
			public void run() {
				try {
					while (!this.isInterrupted()) {
						String message = mSendQueue.take();
						char buffer[] = message.toCharArray();
						for (int i = 0; i < buffer.length; i++)
						{
							if (mSerialDevice.write(
									new byte[] { (byte) buffer[i] }, 200) > 0)
							{
								// Log success
							} else {
								// Log failed
							}
						}
					}
				} catch (InterruptedException e){
					Thread.currentThread().interrupt();
				} catch (Exception e) {
				}
			}
		};
		mWriteThread.start();
	}

	private void startReadThread(){
		mReadThread = new Thread() {
			public void run() {
				try {
					mLogger.WriteLine("Started Read Thread\n");
					ArrayList<Byte> message = new ArrayList<Byte>();
					byte lastByte = 0x00;
					while (!this.isInterrupted()) {
						
						byte buffer = mReadQueue.take();
						message.add(buffer);
						if (lastByte == 0x44 && buffer == 0x53) {
							if(message.size()==11){
							mLogger.WriteLine(message.toString() + "\n");
							message.clear();
							}
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

	private void stopWriteThread() {
		mWriteThread.interrupt();
		try {
			mWriteThread.join(2000);
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
		}
	}

	private void updateReceivedData(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			try {
				mReadQueue.put(data[i]);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
		}
	}
    private void onDeviceStateChange() {
        stopIoManager();
        stopWriteThread();
        startIoManager();
        startReadThread();
        startWriteThread();
        
    }
    
    


}
