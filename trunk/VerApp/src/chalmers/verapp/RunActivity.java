package chalmers.verapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import chalmers.verapp.base.BaseActivity;
import chalmers.verapp.ecu_connection.FileLogger;
import chalmers.verapp.interfaces.GPSCallback;
import chalmers.verapp.interfaces.ILogger;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

/**
 * Contains everything that involves run mode.
 * 
 * @author Anders Nordin, Joanna Eriksson
 */
public class RunActivity extends BaseActivity implements GPSCallback,
		SteeringChangeListener {
	// GUI
	private Chronometer clockTime = null;
	private TextView tvSpeed, tvLapTime1, tvLapTime2, avgSpeed;
	private Button stop, start;
	private Thread distThread, newLapThread;
	private GPSManager gpsManager = null;
	private View rect, rect2;

	// steering indicator
	int nomSteer = 500; // calibrated nominal value
	int maxSteer;
	int minSteer;
	int diff = 300; // the amount the steering can differ around its nominal
					// value

	// Dashboard values
	private long timeWhenStopped = 0;
	private boolean timeIsRunning = false;
	private ArrayList<Long> lapTimes = new ArrayList<Long>();
	private boolean validLap = false;
	private double totalDistance = 0;
	private String avgSpeedString;

	// EcuManager
	/*
	 * private UsbSerialDriver mSerialDevice; private UsbManager mUsbManager;
	 * private EcuManagerTest mEcuManger; private SystemInfo mSysteminfo;
	 */

	// Locations
	private Location _currentPos, secondLatestPos, startPos;

	private FileObserver observer;

	private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {
		@Override
		public void onRunError(Exception e) {
			mLogger.WriteLine("Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			mEcuManagerThread = new Thread() {
				public void run() {
					updateReceivedData(data);
				}
			};
			mEcuManagerThread.start();
		}
	};

	private Button startECU, stopECU;
	private SteeringChangeListener steeringChangeListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_run);

		// INIT ECU
		mLogger.Open();

		if (mUsbManager == null) {
			mLogger.WriteLine("mUsbManager == null");
		}
		if (mSerialDevice == null) {
			mLogger.WriteLine("mSerialDevice == null");
		} else {
			try {
				mSerialDevice.open();
				mSerialDevice
						.setParameters(115200, UsbSerialDriver.DATABITS_8,
								UsbSerialDriver.STOPBITS_1,
								UsbSerialDriver.PARITY_NONE);
			} catch (IOException e) {
				mLogger.WriteLine("Error setting up device: " + e.getMessage());
				try {
					mSerialDevice.close();
				} catch (IOException e2) {
					mLogger.WriteLine("Exception caught in try(serialOpen) "
							+ e2.toString());
				}
				mSerialDevice = null;
				return;
			}
		}
		startIoManager();
		startReadThread();
		startWriteThread();

		maxSteer = nomSteer + diff;
		minSteer = nomSteer - diff;

		// Screen always active
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Start EcuManager
		// mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		// mSerialDevice = UsbSerialProber.acquire(mUsbManager);
		// mEcuManger = new EcuManagerTest(mUsbManager, mSerialDevice,
		// mSysteminfo);

		// Initiate GPS
		gpsManager = new GPSManager();
		gpsManager.startListening(getApplicationContext());
		gpsManager.setGPSCallback(this);

		// Get graphical id's
		tvSpeed = (TextView) findViewById(R.id.tvSpeed);
		tvLapTime1 = (TextView) findViewById(R.id.lapTime2);
		tvLapTime2 = (TextView) findViewById(R.id.lapTime3);
		avgSpeed = (TextView) findViewById(R.id.avgSpeed);
		stop = ((Button) findViewById(R.id.stop));
		start = ((Button) findViewById(R.id.start));
		tvSpeed.setText("Loading GPS");
		rect = (View) findViewById(R.id.myRectangleView);
		rect2 = (View) findViewById(R.id.myRectangleView2);
		startECU = (Button) findViewById(R.id.ecuStart);
		stopECU = (Button) findViewById(R.id.ecuStop);
		// runDistanceThread();

		startECU.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Send("bL0030"); // Start logging cmd
				Toast.makeText(getApplicationContext(), "ECU START",
						Toast.LENGTH_SHORT).show();

			}
		});
		stopECU.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Send("v");
				Toast.makeText(getApplicationContext(), "ECU Stopped",
						Toast.LENGTH_SHORT).show();

			}
		});

		// Observing waiting folder
		observer = new FileObserver(FileManager.FILES_WAITING_DIR.getPath()) { // set
																				// up
																				// a
																				// file
																				// observer
																				// to
																				// watch
																				// this
																				// directory
																				// on
																				// sd
																				// card
			@Override
			public void onEvent(int event, String file) {
				if (event == FileObserver.CLOSE_WRITE) {
					addToLog(event + " " + file);
					checkItOut();
				}
			}
		};
		observer.startWatching();

		// Possible to run without GPS
		LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			start.setEnabled(true);

		mSystemInfo.setSteeringChangeListener(this);

	}

	private void checkItOut() {
		new FileManager().execute();
	}

	// Temporary for bug testing
	private void addToLog(String text) {
		try {
			File createLog = new File(Environment.getExternalStorageDirectory()
					+ "/Android/data/com.chalmers.civinco/files/",
					"createLog.txt");
			FileOutputStream f2 = new FileOutputStream(createLog, true);
			PrintStream p = new PrintStream(f2);
			Time now = new Time();
			now.setToNow();
			String time = now.hour + ":" + now.minute + ":" + now.second;
			p.print(time + ": " + text + "\n");
			p.close();
			f2.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void ButtonOnClick(View v) {
		switch (v.getId()) {
		case R.id.stop:
			if (timeIsRunning) {
				Shutdown();
				timeWhenStopped = clockTime.getBase()
						- SystemClock.elapsedRealtime();
				clockTime.stop();
				timeIsRunning = false;
				stop.setEnabled(false);
				start.setEnabled(true);
				Toast.makeText(getApplicationContext(), "Stopped",
						Toast.LENGTH_SHORT).show();
				printEndRace();
			}
			break;
		case R.id.incident:
			mSystemInfo.setWarning("1");
			Toast.makeText(getApplicationContext(), "Warning Sent",
					Toast.LENGTH_SHORT).show();
			break;
		case R.id.start:
			if (!timeIsRunning) {
				Send("bL0030"); // Start logging cmd
				clockTime.setBase(SystemClock.elapsedRealtime()
						+ timeWhenStopped);
				clockTime.start();
				timeIsRunning = true;
				stop.setEnabled(true);
				start.setEnabled(false);

				if (timeWhenStopped == 0) {
					Toast.makeText(getApplicationContext(), "Started",
							Toast.LENGTH_SHORT).show();
					start.setText("Resume");
				} else
					Toast.makeText(getApplicationContext(), "Resumed",
							Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	/**
	 * When GPS Coordinate is updated this function is triggered
	 * with the latest coordinate
	 */
	public void onGPSUpdate(Location currentPos) {
		_currentPos = currentPos;

		Log.i("GPS UPDATE",
				"(" + currentPos.getLatitude() + ","
						+ currentPos.getLongitude() + ") Dir: "
						+ currentPos.getBearing());

		// Initial
		if (clockTime == null) {
			clockTime = (Chronometer) findViewById(R.id.clockTime);
			tvSpeed.setText("GPS signal OK");
			start.setEnabled(true);
		}

		// Rest of the code is only performed when time is running
		if (timeIsRunning) {
			mSystemInfo.setLatitude("" + currentPos.getLatitude());
			mSystemInfo.setLatitude("" + currentPos.getLongitude());

			if (startPos == null) {
				startPos = currentPos;
				// runNewLapThread();
			}

			float dist = currentPos.distanceTo(startPos);
			Log.i("Distance To", "" + dist);
			Log.i("Valid Lap", "" + validLap);

			if ((currentPos.distanceTo(startPos) < 10) && validLap) {
				// runNewLapThread();
				validLap = false;

				Log.i("NEW LAP", "+++");
				Long timeInMilliSec = (SystemClock.elapsedRealtime() - clockTime
						.getBase()); // in ms

				int size = lapTimes.size();
				if (size > 0) { // not first lap
					for (Long t : lapTimes)
						timeInMilliSec = timeInMilliSec - t;
				}
				lapTimes.add(timeInMilliSec);
				size++;

				tvLapTime1.setText("Lap " + size + ": "
						+ createTimeString(timeInMilliSec));

				if (size > 1) {
					timeInMilliSec = lapTimes.get(size - 2);
					tvLapTime2.setText("Lap " + (size - 1) + ": "
							+ createTimeString(timeInMilliSec));
				}

			}

			// Display speed
			double currentSpeed = _currentPos.getSpeed();
			String speedString = ""
					+ roundDecimal(convertToKMH(currentSpeed), 2);
			this.tvSpeed.setText(speedString + " km/h");

			avgSpeedString = ""
					+ roundDecimal(
							convertToKMH(totalDistance
									/ ((double) (SystemClock.elapsedRealtime() - clockTime
											.getBase()) / 1000)),
							2);

			// Set color for avgSpeed textview
			if (Double.valueOf(avgSpeedString) > 25)
				avgSpeed.setTextColor(Color.GREEN);
			else
				avgSpeed.setTextColor(Color.RED);

			avgSpeed.setText("Avg Speed: " + avgSpeedString + " km/h");

			if (secondLatestPos == null && timeIsRunning)
				secondLatestPos = currentPos;
		}
	}

	/**
	 * @param time
	 * @return String as format hh:mm:ss
	 */
	private String createTimeString(Long time) {
		int seconds = (int) (time / 1000) % 60;
		int minutes = (int) ((time / (1000 * 60)) % 60);
		int hours = (int) ((time / (1000 * 60 * 60)) % 24);

		return (hours + ":" + minutes + ":" + seconds);
	}

	/**
	 * Run a thread for calculating distance to calculate lap time
	 */
	private void runDistanceThread() {
		distThread = new Thread() {
			public void run() {
				while (true) {
					try {
						if (_currentPos != null && secondLatestPos != null
								&& timeIsRunning && _currentPos.hasSpeed()) {
							totalDistance = totalDistance
									+ secondLatestPos.distanceTo(_currentPos); // in
																				// meters
							Log.i("TOTAL DISTANCE", "" + totalDistance);
							secondLatestPos = _currentPos;
							Thread.sleep(2000);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		distThread.start();
	}

	/**
	 * Ensure that the car is not passing finishLine twice on the same lap.
	 * Therefore lock it for a couple of minutes.
	 */
	private void runNewLapThread() {
		newLapThread = new Thread() {
			public void run() {
				try {
					Log.i("NEW LAP THREAD", "INSIDE");
					Thread.sleep(10000);
					validLap = true;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		};
		newLapThread.start();
	}

	// @Override
	/**
	 * When Run Activity is destroyed Send callback for GPS
	 */
	protected void onDestroy() {
		gpsManager.stopListening();
		gpsManager.setGPSCallback(null);
		gpsManager = null;
		// ButtonOnClick(stop);
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		gpsManager.stopListening();
		gpsManager.setGPSCallback(null);
		gpsManager = null;
		// ButtonOnClick(stop);
		super.onBackPressed();
	}

	/*
	 * private void closeDistThread(){ distThread.interrupt(); try {
	 * distThread.join(2000); } catch (InterruptedException e) {
	 * e.printStackTrace(); } }
	 */

	/**
	 * Converts speed from meter per second to kilometer per hour
	 * 
	 * @param speed
	 *            given speed in m/s
	 * @return speed in km/h
	 */
	private double convertToKMH(double speed) {
		return (speed * 3.6);
	}

	/**
	 * Rounds value to given decimal
	 * 
	 * @param value
	 *            , given number to modify
	 * @param decimalPlace
	 *            , how many decimal
	 * @return the number as a double
	 */
	private double roundDecimal(double value, final int decimalPlace) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
		value = bd.doubleValue();

		return value;
	}

	/**
	 * Prints out final stats
	 */
	private void printEndRace() {
		// Where to display it, set context
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				(Context) this);

		alertDialogBuilder.setTitle("Do you want to end race?");

		// Compose the message string
		String summaryString = "Summary \n\n"
				+ "Total time: "
				+ createTimeString(SystemClock.elapsedRealtime()
						- clockTime.getBase()) + "\n" + "Avg Speed: "
				+ avgSpeedString + " km/h \n" + "Laps: " + lapTimes.size()
				+ "\n";

		int lapNr = 1;
		for (Long l : lapTimes) {
			summaryString += "Lap " + lapNr + ": " + createTimeString(l) + "\n";
			lapNr++;
		}

		if (lapNr == 1)
			summaryString += "No laps completed";

		alertDialogBuilder
				.setMessage(summaryString)
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								callRealEnd();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	/**
	 * Changes the steering indicator according to the logged value.
	 * 
	 * @param steer
	 *            , the steering
	 */
	public void theRectangle(int steer) {

		RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) rect2
				.getLayoutParams();

		// setting the width of the bar

		rect2.getLayoutParams().width = Math.abs(steer - nomSteer)
				* (rect.getLayoutParams().width) / (maxSteer - minSteer);

		// the driver is turning left
		if (steer < nomSteer) {
			params2.leftMargin = rect.getLayoutParams().width / 2
					- rect2.getLayoutParams().width;

		}

		// the driver is turning right
		else {
			params2.leftMargin = rect.getLayoutParams().width / 2;

		}
		GradientDrawable sd = (GradientDrawable) rect2.getBackground().mutate();

		// the steering is outside the accepted interval => red color
		if (Math.abs(steer - nomSteer) > diff / 3)
			sd.setColor(0xffff0000);
		// the steering is within the accepted interval => green color
		else
			sd.setColor(0xff00ff00);

		sd.invalidateSelf();
	}

	public void onSteeringChanged(int s) {
		theRectangle(s);

	}

	/********************************* ECU MANAGER **************************************/
	private void startWriteThread() {
		mWriteThread = new Thread() {
			public void run() {
				try {
					while (!this.isInterrupted()) {
						mLogger.WriteLine("Inne startWrite thread");
						String message = mSendQueue.take();
						char buffer[] = message.toCharArray();
						boolean error = false;
						for (int i = 0; i < buffer.length; i++) {
							if (mSerialDevice.write(
									new byte[] { (byte) buffer[i] }, 200) > 0)
								;
							// Log success
							else
								error = true;

						}
						if (error)
							mLogger.WriteLine("WriteThread failed to write: "
									+ message);
						else
							mLogger.WriteLine("WriteThread wrote: " + message);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					mLogger.WriteLine("Exception caught in WriteThread: "
							+ e.toString());
				}
			}
		};
		mWriteThread.start();
	}

	private void startReadThread() {
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

							if (message.size() == 105) {
								message.remove(0);
								message.remove(0);
								message.remove(0);
								message.remove(message.size() - 1);
								message.remove(message.size() - 1);
								message.remove(message.size() - 1);
								message.remove(message.size() - 1);

								while (message.size() > 0) {
									byte firstByte = message.remove(0);
									byte secondByte = message.remove(0);
									short temp1 = (short) firstByte;
									short temp2 = (short) secondByte;
									int val = (int) ((temp1 << 8) | (temp2 & 0xFF));
									if (val < 0) {
										val = 6553 + val + 1;
									}
									shortMsg.add(val);
								}

								mLogger.WriteLine(" " + shortMsg.toString()
										+ "  10" + "  11  12  13  14" + "  "
										+ mSystemInfo.getLongitude() + "  "
										+ mSystemInfo.getLatitude() + "  "
										+ mSystemInfo.getWarning()
										+ "  10000\n");
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

	private void stopWriteThread() {
		mWriteThread.interrupt();
		try {
			mWriteThread.join(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void stopReadThread() {
		mReadThread.interrupt();
		try {
			mReadThread.join(2000);
		} catch (InterruptedException e) {
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
		} else {
			mLogger.WriteLine("StartIOManager: mSerialDevice is null");
		}
	}

	private void updateReceivedData(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			try {
				mReadQueue.put(data[i]);
			} catch (InterruptedException e) {
				mLogger.WriteLine("UpdateReceivedData interrupted");

			}
		}
	}

	public void Send(String msg) {
		try {
			mSendQueue.put(msg);
		} catch (Exception e) {
			mLogger.WriteLine("EcuManagerTest::Send() caught exception "
					+ e.toString());
		}
	}

	public void Shutdown() {
		mLogger.WriteLine("EcuManagerTest Shutdown()");
		try {
			mSendQueue.put("v");
			Thread.sleep(200);
		} catch (InterruptedException e) {
			mLogger.WriteLine("Exception caught in shutdown()");
		}
		stopWriteThread();
		stopReadThread();
		stopIoManager();
		mLogger.Close();
	}

}