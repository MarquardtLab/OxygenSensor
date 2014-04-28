// Oxygen Sensor
// Main Activity
//
// This is the main activity for the oxygen sensor app
// All of the data processing is done through here and all of the
// fragments are controlled from here

package com.example.oxygensensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.example.oxygensensor.BluetoothConnectionService;
import com.example.oxygensensor.CalGraphView.CalGraphThread;
import com.example.oxygensensor.GraphView.GraphThread;
import com.example.oxygensensor.SubGraphView.SubGraphThread;
import com.example.oxygensensor.adapter.TabsPagerAdapter;
import com.example.oxygensensor.R;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint("NewApi")
public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
	
	// Adjust to Preference
	public static final int CAL_SAMPLES = 20;
	public static final int MAX_SAMPLES = 50;

	// Debugging
	private static final String TAG = "MainActivity";
	private static final boolean D = true;
	
	// UI
	private ViewPager viewPager;
	private TabsPagerAdapter mAdapter;
	private ActionBar actionBar;
	// Tab titles
	private String[] tabs = { "Data Collection", "Calibration" };
	
	// Bluetooth
	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	private BluetoothConnectionService btService = null;
	private String mConnectedDeviceName = null;
	
	// Bluetooth connection service data handles
	private static final int REQUEST_BLUETOOTH_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;	
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;	
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Data transfer values
	private static final byte[] GET_DATA_ARRAY = {'m'};
	private static final int BUFFER_SIZE = 80;
	private static final char SOP = '<';
	private static final char EOP = '>';
	
	// Graph view data handles
	public static final String INDEX = "current_index";
	public static final String REFERENCE = "data_reference";
	public static final String SAMPLE = "data_sample";
	public static final String MAX = "data_max";
	public static final String MODEL = "draw_model";
	public static final String ONESITE = "one_site";
	public static final String TWOSITE = "two_site";
	public static final String O2SAMPLE = "O2_sample";
	public static final String O2REFERENCE = "O2_reference";
	public static final String O2SENT = "O2_sent";
	public static final String O2COLLECTED = "O2_collected";
	public static final String AIRSAMPLE = "air_sample";
	public static final String AIRREFERENCE = "air_reference";
	public static final String AIRSENT = "air_sent";
	public static final String AIRCOLLECTED = "air_collected";
	public static final String N2SAMPLE = "N2_sample";
	public static final String N2REFERENCE = "N2_reference";
	public static final String N2SENT = "N2_sent";
	public static final String O2CONCENTRATION = "oxygen_concentration";
	public static final String MINC = "minimum_concentration";
	public static final String MAXC = "maximum_concentration";
	public static final String SLOPE = "slope";
	public static final String INTERCEPT = "intercept";
	public static final String A = "a";
	public static final String B = "b";
	public static final String C = "c";
	public static final String CALN2 = "calibrated_N2";
	public static final String CALAIR = "calibrated_Air";
	public static final String CALO2 = "calibrated_O2";
	public static final String CALMIN = "cal_min";
	public static final String CALMAX = "cal_max";
	public static final String MODELMIN = "model_min";
	public static final String MODELMAX = "model_max";
	public static final String RSQUARED = "coefficient_of_determination";
	public static final String ROOTMEANSQUARE = "root_mean_square";
	
	// Graph View
	// Main
	private GraphThread mMainGraphThread;
	private GraphView mMainGraphView;
	private Handler mMainGraphHandler;
	// Sub
	private SubGraphThread mSubGraphThread;
	private SubGraphView mSubGraphView;
	private Handler mSubGraphHandler;
	// Cal
	private CalGraphThread mCalGraphThread;
	private CalGraphView mCalGraphView;
	private Handler mCalGraphHandler;

	// Dropbox
	private static final String APP_KEY = "lj7lnjkqfht1tw4";
	private static final String APP_SECRET = "up3pufx5fbp85ee";
	private static final AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	
	private DropboxAPI<AndroidAuthSession> mDBApi;
	
	// Sensor Variables
	private long sampleRate;
	private boolean isSending;
	private char[] buffer = null;
	private int bufferIndex = 0;
	private boolean started = false;
	private boolean ended = false;
	private String received;
	private int currentIndex;
	private int dataMax;
	private float[] dataReference = new float[MAX_SAMPLES];
	private float[] dataSample = new float[MAX_SAMPLES];
	private float[] dataO2Ref = new float[MAX_SAMPLES];
	private float[] dataO2 = new float[MAX_SAMPLES];
	private float[] dataAirRef = new float[MAX_SAMPLES];
	private float[] dataAir = new float[MAX_SAMPLES];
	private float[] dataN2Ref = new float[MAX_SAMPLES];
	private float[] dataN2 = new float[MAX_SAMPLES];
	
	// Modeling variables
	private float[] calO2;
	private float[] calAir;
	private float[] calN2;
	private float[] oxygenConcentration;
	private float minC, maxC;
	private float I0;
	private float ref;
	private float intercept;
	private float slope;
	private float a, b, c;
	private boolean O2Collected;
	private boolean airCollected;
	private boolean N2Collected;
	private boolean drawModel;
	private boolean isOneSite;
	private boolean isTwoSite;
	
	// Data recoding variables
	private boolean isRecording;	
	private Calendar calendar;
	private FileOutputStream out;
	private int count;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(D) Log.d(TAG, "~~~ On CREATE ~~~");
		
		setContentView(R.layout.activity_main);

		// Initialization
		viewPager = (ViewPager) findViewById(R.id.pager);
		actionBar = getActionBar();
		mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(mAdapter);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);		

		// Adding Tabs
		for (String tab_name : tabs) {
			actionBar.addTab(actionBar.newTab().setText(tab_name)
					.setTabListener(this));
		}
		
		
		/**
		 * on swiping the viewpager make respective tab selected
		 * */
		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				// on changing the page
				// make respected tab selected
				actionBar.setSelectedNavigationItem(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

		
			
		
	}
	
	// Returns the current index of the data
	public int getCurrentIndex() {
		return currentIndex;
	}
	
	// Returns an array of the data from the reference sensor
	public float[] getDataReference() {
		return dataReference;
	}
	
	// Returns an array of the data from the sample sensor
	public float[] getDataSample() {
		return dataSample;
	}
	
	// Creates a new Bluetooth connection service which handles all Bluetooth communication
	public void connectBluetooth(View view) {
		boolean connected = ((ToggleButton) view).isChecked();
		
		if (connected) {
			if(!mBluetoothAdapter.isEnabled()) {
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			} else {
				if (D) Log.d(TAG, "Begin DeviceListFragment");
				btService = new BluetoothConnectionService(this, mHandler);
				Intent intent = new Intent();
				intent.setClass(this, DeviceListFragment.class);
				startActivityForResult(intent, REQUEST_BLUETOOTH_DEVICE);
				if (D) Log.d(TAG, "DeviceListFragment completed");
			}
		} else {
			btService.stop();
		}
			
	}
	
	// Connects to device chosen from list
	private void connectDevice(Intent data, boolean secure) {

		if (D) Log.d(TAG, "connectDevice()");
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListFragment.EXTRA_DEVICE_ADDRESS);

		if (D) Log.d(TAG, "Device address:" + address);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
		if (D) Log.d(TAG, "Device:" + device);
        btService.connect(device);
    }
	
	// Reads in the user provided sample rate and updates the current value
	public void updateSampleRate(View view) {
		if (btService == null || btService.getState() != BluetoothConnectionService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        } else {
        	EditText editText = (EditText) findViewById(R.id.edit_samplerate);
        	String text = editText.getText().toString().trim();
        	// text must be a decimal number with an optional decimal point: 1, 1.0, 1., .1 are all valid
			if (text.matches("\\d+(\\.(\\d+)?)?") || text.matches("(\\d+)?\\.\\d+")) {
				sampleRate = (long) (Float.parseFloat(text) * 1000);
				timerHandler.removeCallbacks(sendMessage);
				timerHandler.post(sendMessage);
				isSending = true;
				if (D) Log.d(TAG, "Sending at " + sampleRate + "ms");
			} else {
				Toast.makeText(getApplicationContext(), "Invalid Sample Rate", Toast.LENGTH_SHORT).show();
			}
        }
		// hides the keyboard when update button is pressed
		hideKeyboard();
	}
	
	// When stop button pressed, stops requesting new samples
	public void stopCollecting(View view) {
		timerHandler.removeCallbacks(sendMessage);
		isSending = false;
		if (D) Log.d(TAG, "Stopped Sending");
	}
	
	// Sends request for a new sample, called at the sample rate
	Handler timerHandler = new Handler();
	Runnable sendMessage = new Runnable() {
		@Override
		public void run() {
			// Prompt sensor for a measurement
			btService.write(GET_DATA_ARRAY);
			// Calls it's own runnable with a delay of sample rate
			timerHandler.postDelayed(this, sampleRate);
		}
	};
	
	// When Collect button is pressed, saves the data of the specified type
	public void collectData(View view) {
		// Stop showing oxygen concentration data
		drawModel = false;
		
		// If enough samples have been taken, execute code
		if (currentIndex == MAX_SAMPLES - 1) {
			// Determine which gas type has been checked
			boolean O2Checked = ((RadioButton) findViewById(R.id.radioO2)).isChecked();
			boolean AirChecked = ((RadioButton) findViewById(R.id.radioAir)).isChecked();
			boolean N2Checked = ((RadioButton) findViewById(R.id.radioN2)).isChecked();
			
			// Get thread and handler for calibration view
			if (mCalGraphView == null) {
				mCalGraphView = (CalGraphView) findViewById(R.id.CalGraph);
				mCalGraphThread = mCalGraphView.getThread();
				mCalGraphHandler = mCalGraphThread.getHandler();
			}

			// Send appropriate collected data to calibration view
			Message msgCal = mCalGraphHandler.obtainMessage(CalGraphView.MESSAGE_DATA);
			Bundle bundleCal = new Bundle();
			bundleCal.putBoolean(MODEL, drawModel);
			boolean sendO2 = false;
			boolean sendAir = false;
			boolean sendN2 = false;
			if (O2Checked) {
				dataO2 = Arrays.copyOfRange(dataSample, MAX_SAMPLES / 2 - CAL_SAMPLES / 2 - 1, (MAX_SAMPLES / 2 + CAL_SAMPLES / 2 - 1));
				dataO2Ref = Arrays.copyOfRange(dataReference, MAX_SAMPLES / 2 - CAL_SAMPLES / 2 - 1, (MAX_SAMPLES / 2 + CAL_SAMPLES / 2 - 1));				
				O2Collected = true;
				sendO2 = true;
				bundleCal.putFloatArray(O2SAMPLE, dataO2);
				bundleCal.putFloatArray(O2REFERENCE, dataO2Ref);			
			} else if (AirChecked) {
				dataAir = Arrays.copyOfRange(dataSample, MAX_SAMPLES / 2 - CAL_SAMPLES / 2 - 1, (MAX_SAMPLES / 2 + CAL_SAMPLES / 2 - 1));
				dataAirRef = Arrays.copyOfRange(dataReference, MAX_SAMPLES / 2 - CAL_SAMPLES / 2 - 1, (MAX_SAMPLES / 2 + CAL_SAMPLES / 2 - 1));	
				airCollected = true;
				sendAir = true;
				bundleCal.putFloatArray(AIRSAMPLE, dataAir);
				bundleCal.putFloatArray(AIRREFERENCE, dataAirRef);
			} else if (N2Checked) {
				dataN2 = Arrays.copyOfRange(dataSample, MAX_SAMPLES / 2 - CAL_SAMPLES / 2 - 1, (MAX_SAMPLES / 2 + CAL_SAMPLES / 2 - 1));
				dataN2Ref = Arrays.copyOfRange(dataReference, MAX_SAMPLES / 2 - CAL_SAMPLES / 2 - 1, (MAX_SAMPLES / 2 + CAL_SAMPLES / 2 - 1));	
				N2Collected = true;
				sendN2 = true;
				bundleCal.putFloatArray(N2SAMPLE, dataN2);
				bundleCal.putFloatArray(N2REFERENCE, dataN2Ref);
			}
			bundleCal.putBoolean(O2SENT, sendO2);
			bundleCal.putBoolean(AIRSENT, sendAir);
			bundleCal.putBoolean(N2SENT, sendN2);
			bundleCal.putInt(MAX, dataMax);
			bundleCal.putBoolean(MODEL, drawModel);
			msgCal.setData(bundleCal);
			mCalGraphHandler.sendMessage(msgCal);	
		}
	}
	
	// Builds the model to calculate O2 concentration from intensity data
	// (I0/Ref0)/(I/Ref)=slope*[C]+intercept --> [C]=[(I0/Ref0)/(I/Ref)-intercept]/slope
	public void createModel(View view) {
		// Determine which type of model is selected
	    isOneSite = ((RadioButton) findViewById(R.id.radioOneSite)).isChecked();
		isTwoSite = ((RadioButton) findViewById(R.id.radioTwoSite)).isChecked();
		drawModel = false;
		
		// Only execute if model type is compatible with collected data
		if (N2Collected && (((airCollected || O2Collected) && isOneSite) || (airCollected && O2Collected && isTwoSite))) {
			Message msgCal = mCalGraphHandler.obtainMessage(CalGraphView.MESSAGE_DATA);
		    Bundle bundleCal = new Bundle();
		    bundleCal.putBoolean(AIRCOLLECTED, airCollected);
		    bundleCal.putBoolean(O2COLLECTED, O2Collected);
		    
		    // Initialize model variables
			minC = 0;
			maxC = 100;
			float sumN2 = 0;
			float sumRef = 0;
			float modelMin = 0;
			float modelMax = 0;
			float calMax = 0;
			float calMin = 0;
			calN2 = new float[CAL_SAMPLES];
			if (O2Collected) {
				calO2 = new float[CAL_SAMPLES];
			}
			if (airCollected) {
				calAir = new float[CAL_SAMPLES];
			}
			oxygenConcentration = new float[MAX_SAMPLES];
			
			// Calibrate data
			for (int i = 0; i < CAL_SAMPLES; i++) {
				sumN2 += dataN2[i];
				sumRef += dataN2Ref[i];
			}
			I0 = sumN2 / CAL_SAMPLES;
			ref = sumRef / CAL_SAMPLES;
			for (int i = 0; i < CAL_SAMPLES; i++) {
				calN2[i] = (I0 / ref) / (dataN2[i] / dataN2Ref[i]);
				if (O2Collected) {
					calO2[i] = (I0 / ref) / (dataO2[i] / dataO2Ref[i]);
				}
				if (airCollected) {
					calAir[i] = (I0 / ref) / (dataAir[i] / dataAirRef[i]);
				}
			}
			
			// Calculate values required for both model types
			float sumY = getSumY();
		    float sumX = getSumX();
		    float sumX2 = getSumX2();
		    float sumXY = getSumXY();
		    float count;
		    if (airCollected && O2Collected) {
		      count = 3 * CAL_SAMPLES;
		    } else {
		      count = 2 * CAL_SAMPLES;
		    }
		    float expectedO2 = 0;
		    float expectedAir = 0;
		    float expectedN2 = 0;
			bundleCal.putBoolean(ONESITE, isOneSite);
			bundleCal.putBoolean(TWOSITE, isTwoSite);
			
			// Create one site model
		    if (isOneSite) {
		      // http://people.hofstra.edu/stefan_waner/realworld/calctopic1/regression.html
		      isTwoSite = false;
		      slope = (count * sumXY - sumX * sumY) / (count * sumX2 - sumX * sumX);
		      intercept = (sumY - slope * sumX) / count;
		      expectedO2 = slope * 100 + intercept;
		      expectedAir = slope * 21 + intercept;
		      expectedN2 = slope * 0 + intercept;
		      modelMin = intercept;
		      modelMax = slope * 100 + intercept;
		      calMax = getCalMax(modelMax);
		      calMin = getCalMin(modelMin, calMax);
		      isOneSite = true;
		      drawModel = true;
		      bundleCal.putBoolean(MODEL, drawModel);
		      bundleCal.putFloat(SLOPE, slope);
			  bundleCal.putFloat(INTERCEPT, intercept);
		    } 
		    // Create two site model
		    else if (isTwoSite && (airCollected && O2Collected)) {
		      // -http://www.codeproject.com/Articles/63170/Least-Squares-Regression-for-Quadratic-Curve-Fitti
		      // -Characterization of a dissolved oxygen sensor made of plastic optical fiver coated with
		      // ruthenium-incorporated solgel
		      // -Photophysics and Photochemistry of Oxygen Sensors Based on Luminescent Transition-Metal Complexes
		      // -Characterization of ormosil film for dissolved oxygen-sensing
		      isOneSite = false;
		      float sumX4 = getSumX4();
		      float sumX3 = getSumX3();
		      float sumX2Y = getSumX2Y();
		      a = (sumX2Y * (sumX2 * count - sumX * sumX) - 
		        sumXY * (sumX3 * count - sumX * sumX2) + 
		        sumY * (sumX3 * sumX - sumX2 * sumX2))
		        /
		        (sumX4 * (sumX2 * count - sumX * sumX) -
		          sumX3 * (sumX3 * count - sumX * sumX2) + 
		          sumX2 * (sumX3 * sumX - sumX2 * sumX2));
		      b = (sumX4 * (sumXY * count - sumY * sumX) - 
		        sumX3 * (sumX2Y * count - sumY * sumX2) + 
		        sumX2 * (sumX2Y * sumX - sumXY * sumX2))
		        /
		        (sumX4 * (sumX2 * count - sumX * sumX) - 
		          sumX3 * (sumX3 * count - sumX * sumX2) + 
		          sumX2 * (sumX3 * sumX - sumX2 * sumX2));
		      c = (sumX4 * (sumX2 * sumY - sumX * sumXY) - 
		        sumX3 * (sumX3 * sumY - sumX * sumX2Y) + 
		        sumX2 * (sumX3 * sumXY - sumX2 * sumX2Y))
		        /
		        (sumX4 * (sumX2 * count - sumX * sumX) - 
		          sumX3 * (sumX3 * count - sumX * sumX2) + 
		          sumX2 * (sumX3 * sumX - sumX2 * sumX2));
		      expectedO2 = (float) (a * Math.pow(100, 2) + b * 100 + c);
		      expectedAir = (float) (a * Math.pow(21, 2) + b * 21 + c);
		      expectedN2 = (float) (a * Math.pow(0, 2) + b * 0 + c);
		      modelMin = c;
		      modelMax = (float) (a * Math.pow(100, 2) + b * 100 + c);
		      calMax = getCalMax(modelMax);
		      calMin = getCalMin(modelMin, calMax);
		      isTwoSite = true;    
		      drawModel = true;
		      bundleCal.putBoolean(MODEL, drawModel);
		      bundleCal.putFloat(A, a);
			  bundleCal.putFloat(B, b);
			  bundleCal.putFloat(C, c);
		    }
		    
		    // Calculate various fit statistics
		    float mean = sumY / count;
		    float SSres = 0;
		    float SStot = 0;
		    float sumSquares = 0;
		    if (O2Collected && airCollected) {
		      for (int i = 0; i < CAL_SAMPLES; i++) {
		        SSres += Math.pow(calO2[i] - expectedO2, 2) + 
		          Math.pow(calAir[i] - expectedAir, 2) + 
		          Math.pow(calN2[i] - expectedN2, 2);
		        SStot += Math.pow(calO2[i] - mean, 2) + Math.pow(calAir[i] - mean, 2) + Math.pow(calN2[i] - mean, 2);
		        sumSquares += Math.pow(calO2[i], 2) + Math.pow(calAir[i], 2) + Math.pow(calN2[i], 2);
		      }
		    } else if (O2Collected) {
		      for (int i = 0; i < CAL_SAMPLES; i++) {
		        SSres += Math.pow(calO2[i] - expectedO2, 2) + 
		          Math.pow(calN2[i] - expectedN2, 2);
		        SStot += Math.pow(calO2[i] - mean, 2) + Math.pow(calN2[i] - mean, 2);
		        sumSquares += Math.pow(calO2[i], 2) + Math.pow(calN2[i], 2);
		      }
		    } else if (airCollected) {
		      for (int i = 0; i < CAL_SAMPLES; i++) {
		        SSres += Math.pow(calAir[i] - expectedAir, 2) + 
		          Math.pow(calN2[i] - expectedN2, 2);
		        SStot += Math.pow(calAir[i] - mean, 2) + Math.pow(calN2[i] - mean, 2);
		        sumSquares += Math.pow(calAir[i], 2) + Math.pow(calN2[i], 2);
		      }
		    }
		    float RSquared = 1 - SSres / SStot;
		    float RMS = (float) Math.sqrt((1 / count) * sumSquares);
		    bundleCal.putFloatArray(CALN2, calN2);
			if (O2Collected) {
				bundleCal.putFloatArray(CALO2, calO2);
			}
			if (airCollected) {
				bundleCal.putFloatArray(CALAIR, calAir);
			}
			bundleCal.putFloat(RSQUARED, RSquared);
			bundleCal.putFloat(ROOTMEANSQUARE, RMS);
		    bundleCal.putFloat(CALMIN, calMin);
		    bundleCal.putFloat(CALMAX, calMax);
		    bundleCal.putFloat(MODELMIN, modelMin);
		    bundleCal.putFloat(MODELMAX, modelMax);
		    msgCal.setData(bundleCal);
		    mCalGraphHandler.sendMessage(msgCal);
		}
	}

	//Returns the sum of all the calibration concentrations
	private float getSumX() {
		float sumX = 0;
		for (int i = 0; i < CAL_SAMPLES; i++) {
			if (O2Collected) {
				sumX += 100;
			}
			if (airCollected) {
				sumX += 21;
			}
		}
		return sumX;
	}
	
	//Returns the sum of all the calibration intensities
	private float getSumY() {
		float sumY = 0;
		for (int i = 0; i < CAL_SAMPLES; i++) {
			sumY += calN2[i];
			if (O2Collected) {
				sumY += calO2[i];
			}
			if (airCollected) {
				sumY += calAir[i];
			}
		}
		return sumY;
	}
	
	//Returns the sum of all the calibration concentrations squared
	private float getSumX2() {
		float sumX2 = 0;
		for (int i = 0; i < CAL_SAMPLES; i++) {
			if (O2Collected) {
				sumX2 += Math.pow(100, 2);
			}
			if (airCollected) {
				sumX2 += Math.pow(21, 2);
			}
		}
		return sumX2;
	}
	
	//Returns the sum of all the calibration concentrations cubed
	private float getSumX3() {
		float sumX3 = 0;
		for (int i = 0; i < CAL_SAMPLES; i++) {
			sumX3 += Math.pow(100, 3)+ Math.pow(21, 3);
		}
		return sumX3;
	}
	
	//Returns the sum of all the calibration concentrations to the fourth
	private float getSumX4() {
		float sumX4 = 0;
		for (int i = 0; i < CAL_SAMPLES; i++) {
			sumX4 += Math.pow(100, 4)+ Math.pow(21, 4);
		}
		return sumX4;
	}
	
	//Returns the sum of all the calibration concentrations times intensities
	private float getSumXY() {
		float sumXY = 0;
		for (int i = 0; i < CAL_SAMPLES; i++) {
			if (O2Collected) {
				sumXY += 100 * calO2[i];
			}
			if (airCollected) {
				sumXY += 21 * calAir[i];
			}
		}
		return sumXY;
	}
	
	//Returns the sum of all the calibration concentrations squared times intensities
	private float getSumX2Y() {
		float sumX2Y = 0;
		for (int i = 0; i < CAL_SAMPLES; i++) {
			sumX2Y += Math.pow(100, 2) * calO2[i] + Math.pow(21, 2) * calAir[i];
		}
		return sumX2Y;
	}
	
	// Returns the maximum value for the calibration graph
	private float getCalMax(float modelMax) {
		float calMax = 0;
		if (modelMax > calMax) {
			calMax = modelMax;
		}
		for (int i = 0; i < CAL_SAMPLES; i++) {
			calMax = Math.max(calN2[i], calMax);
			if (O2Collected) { 
				calMax = Math.max(calO2[i], calMax);
			}
			if (airCollected) { 
				calMax = Math.max(calAir[i], calMax);
			}
		}
		return calMax;
	}

	// Returns the minimum value for the calibration graph
	private float getCalMin(float modelMin, float calMax) {
		float calMin = 0;
		// Adjusts graph and draws axis if intercept is less than 0
		if (modelMin < 0) {
			calMin = modelMin;
		}
		if (modelMin > 0.4 * calMax) {
			calMin = calMax;
		for (int i = 0; i < CAL_SAMPLES; i++) {
			calMin = Math.min(calN2[i], calMin);
			if (O2Collected) { 
				calMin = Math.min(calO2[i], calMin);
			}
			if (airCollected) { 
				calMin = Math.min(calAir[i], calMin);
			}
		}
		calMin = Math.min(calMin, modelMin);
		}
		return calMin;
	}
	
	// Takes in the data from the Bluetooth connection service
	// sets the current data
	public String[] messageProcessing(byte[] data) {
		if (isSending) {
			for (int i = 0; i < data.length; i++) {
				char currentByte = (char) data[i];
				// Start of a data package
				if (currentByte == SOP) {
					buffer = new char[BUFFER_SIZE];
					bufferIndex = 0;
					buffer[bufferIndex] = 0;
					started = true;
					ended = false;
				// End of a data package
				} else if (currentByte == EOP) {
					ended = true;
					break;
				// Store all incoming data in a buffer
				} else {
					if (bufferIndex < BUFFER_SIZE -1) {
						try {
							buffer[bufferIndex] = currentByte;
							bufferIndex++;
							buffer[bufferIndex] = '\0';
						} catch (NullPointerException e) {
							buffer = new char[BUFFER_SIZE];
							bufferIndex = 0;
							buffer[bufferIndex] = '\0';
						}
					}
				}
			}
					
			// When the whole data packet is received
			if (started && ended) {
				received = new String(buffer);
				received = received.trim();
				if (D) Log.d(TAG, received);
				String[] currentData = received.split(",");
				started = false;
				ended = false;
				bufferIndex = 0;
				buffer[bufferIndex] = '\0';
				return currentData;
			}
		}
		return null;
	}
	
	// Takes in the current data and adds it to the data arrays to be graphed
	// During calibration, graphs sensor intensity data
	// After a model is built, graphs oxygen concentration
	public void processData(String[] currentData) {
		float referenceDatum = Math.abs(Float.parseFloat(currentData[0]));
		float sampleDatum = Math.abs(Float.parseFloat(currentData[1]));
		if (Math.max(referenceDatum, sampleDatum) > dataMax) {
			dataMax = (int) Math.ceil(Math.max(referenceDatum, sampleDatum));
		}
		dataReference[currentIndex] = referenceDatum;
		dataSample[currentIndex] = sampleDatum;
		// Iterate current index up till the max samples stored at a time
		if (currentIndex < MAX_SAMPLES - 1) {
			currentIndex++;
		// Once max samples have been stored, shifts data for each new sample
		} else {
			for (int i = 0; i < MAX_SAMPLES - 1; i++) {
				dataReference[i] = dataReference[i + 1];
				dataSample[i] = dataSample[i + 1];
			}
			currentIndex = MAX_SAMPLES - 1;
		}
		
		// If model was built, calculate oxygen concentration
		if (drawModel) {
			float concentration = 0;
			for (int i = 0; i < MAX_SAMPLES; i++) {
				if (isOneSite) {
					concentration = ((I0 / ref) / (dataSample[i] / dataReference[i]) - intercept) / slope;
				} else if (isTwoSite) {
					float discriminant = (float) (Math.pow(b, 2) - 4 * a * (c - (I0 / ref) / (dataSample[i] / dataReference[i])));
					if (discriminant >= 0) {
						concentration = (float) ((-b + Math.sqrt(discriminant)) / (2 * a));
					} else {
						concentration = 100;
					}
				}
				minC = Math.min(concentration, minC);
				maxC = Math.max(concentration, maxC);
				oxygenConcentration[i] = concentration;
			}
			// If recording, save each sample's reference, sample, and oxygen concentration data along with current time
			if (isRecording) {
				calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH) + 1;
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				int second = calendar.get(Calendar.SECOND);
				count++;
				try {
					out.write((year + "/" + month + "/" + day + " " + hour + ":" + minute + ":" +
					          second + "," + count +"," + dataReference[currentIndex] + "," + dataSample[currentIndex] +
					          "," + oxygenConcentration[currentIndex] + "%\n").getBytes());
				} catch (IOException e) {
					if (D) Log.e(TAG, "Writing data failed");
					e.printStackTrace();
				}
			}
		}
		
		if (mMainGraphView == null) {
			createGraphViewHandles();
		}
		// Sends the current information to the graphing thread
		try {
			Message msgMain = mMainGraphHandler.obtainMessage(GraphView.MESSAGE_DATA);
			Bundle bundleMain = new Bundle();
			bundleMain.putInt(INDEX, currentIndex);
			bundleMain.putFloatArray(REFERENCE, dataReference);
			bundleMain.putFloatArray(SAMPLE, dataSample);
			bundleMain.putInt(MAX, dataMax);
			bundleMain.putBoolean(MODEL, drawModel);
			if (drawModel) {
				bundleMain.putFloatArray(O2CONCENTRATION, oxygenConcentration);
				bundleMain.putFloat(MINC, minC);
				bundleMain.putFloat(MAXC, maxC);
			}
			msgMain.setData(bundleMain);
			mMainGraphHandler.sendMessage(msgMain);
			
			Message msgSub = mSubGraphHandler.obtainMessage(SubGraphView.MESSAGE_DATA);
			Bundle bundleSub = new Bundle();
			bundleSub.putInt(INDEX, currentIndex);
			bundleSub.putFloatArray(REFERENCE, dataReference);
			bundleSub.putFloatArray(SAMPLE, dataSample);
			bundleSub.putInt(MAX, dataMax);
			bundleSub.putBoolean(MODEL, drawModel);
			if (drawModel) {
				bundleSub.putFloatArray(O2CONCENTRATION, oxygenConcentration);
				bundleSub.putFloat(MINC, minC);
				bundleSub.putFloat(MAXC, maxC);
			}
			msgSub.setData(bundleSub);
			mSubGraphHandler.sendMessage(msgSub);
		} catch (NullPointerException e) {
			if (D) Log.e(TAG, "GraphView Handles not created", e);
			createGraphViewHandles();
		}
	}
	
	// When record button is pressed, writes data to CSV file
	public void recordData(View view) {		
		if (drawModel && !isRecording) {
			try {
				// gets the text input by the user to use as a file name
				EditText editText = (EditText) findViewById(R.id.edit_filename);
	        	String filename = editText.getText().toString().trim();
	        	// creates an "OxygenSensorData" directory in the device
				File directory = new File(Environment.getExternalStorageDirectory() + "/Oxygen Sensor Data/");
				if (!directory.mkdirs()) {
					Log.e(TAG, "Directory not created");
				}
				// Get current time to include in file name
				Calendar calendar = Calendar.getInstance();
				int year = calendar.get(Calendar.YEAR);
				int month = calendar.get(Calendar.MONTH) + 1;
				int day = calendar.get(Calendar.DAY_OF_MONTH);
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				int minute = calendar.get(Calendar.MINUTE);
				int second = calendar.get(Calendar.SECOND);
				
				// create a new file to store the recorded data
				File file = new File(directory, year + "-" + month + "-" + day + "-" + hour + "-" + minute + "-" + second + "-" 
									 + filename + ".csv");
				// create output stream for the file
				out = new FileOutputStream(file);
				isRecording = true;
				if (D) Log.d(TAG, "Recording Data");
				if (D) Log.d(TAG, file.getPath());
				
				// write's file header
				out.write((year + "/" + month + "/" + day + " Oxygen Sensor Data\n").getBytes());
				if (isOneSite) {
					out.write(("One site model: I0/I = " + slope + " * [C] " + intercept + "\n").getBytes());
				} else {
		            out.write(("Two site model: I0/I = " + a + " * [C]^2 + " + b  + " * [C] + " + c + "\n").getBytes());
				}
	            out.write(("I0 = " + I0 + " Ref0 = " + ref + "\n").getBytes());				
				out.write("Date/Time,Count,Reference Intensity,Sample Intensity,Concentration\n".getBytes());
				count = 0;
			} catch (Exception e) {
				Log.e(TAG, "File Output Error");
				e.printStackTrace();
			}
		} else if (isRecording) {
			try {
				// must flush and close to complete writing to file
				out.flush();
				out.close();
				isRecording = false;
				if (D) Log.d(TAG, "Recording Data Stopped");
			} catch (IOException e) {
				Log.e(TAG, "File Output Error");
				e.printStackTrace();
			}
		}
		// Hides the keyboard when record button is pressed
		hideKeyboard();
	}
	
	// Handler handles the messages between activities
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch(msg.arg1) {
				case BluetoothConnectionService.STATE_CONNECTED:
					Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();					
					break;
				case BluetoothConnectionService.STATE_CONNECTING:
					break;
				case BluetoothConnectionService.STATE_NONE:
					break;
				}
			case MESSAGE_WRITE:
				//if (D) Log.d(TAG, "Sent: " + msg.obj);
				break;
			case MESSAGE_READ:
				//if (D) Log.d(TAG, msg.obj.toString().trim());
				byte[] readBuf = (byte[]) msg.obj;
				String[] curData = messageProcessing(readBuf);
				if (curData != null) {
					processData(curData);
				}
				// Alternate AsyncTask, needs work
				//new ProcessMessage().execute(readBuf);
				break;
			case MESSAGE_DEVICE_NAME:
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	// When activities are concluded, takes action based on result
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D) Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
        case REQUEST_BLUETOOTH_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
        case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				Intent intent = new Intent();
				intent.setClass(this, DeviceListFragment.class);
				startActivityForResult(intent, REQUEST_BLUETOOTH_DEVICE);
			} else {
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
		
    }
	
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// on tab selected
		// show respective fragment view
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		hideKeyboard();
	}
	
	// Function to hide the soft keyboard
	public void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
	}
	
	// Function that creates handles for the graphing thread
	private void createGraphViewHandles() {
		// Main Graph
		mMainGraphView = (GraphView) findViewById(R.id.MainGraph);
		if (D) Log.d(TAG, "Created GraphView Handle");
		mMainGraphThread = mMainGraphView.getThread();

		if (D) Log.d(TAG, "Created GraphThread Handle");
		mMainGraphHandler = mMainGraphThread.getHandler();

		if (D) Log.d(TAG, "Created GraphThread Handler Handle");
		
		// Sub Graph
		mSubGraphView = (SubGraphView) findViewById(R.id.SubGraph);
		mSubGraphThread = mSubGraphView.getThread();
		mSubGraphHandler = mSubGraphThread.getHandler();
	}
	
	private String[] getKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
        	String[] ret = new String[2];
        	ret[0] = key;
        	ret[1] = secret;
        	return ret;
        } else {
        	return null;
        }
    }
	
	private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
    }
	/*
	// Process Message in AsyncTask, doesn't work, isn't used
	private class ProcessMessage extends AsyncTask<byte[], Void, String> {
		protected String doInBackground(byte[]... data) {
			int bufferIndex = 0;
			char[] buffer = null;
			boolean started = false;
			boolean ended = false;
			String result = null;
			for (int i = 0; i < data[0].length; i++) {
				char currentByte = (char) data[0][i];
				if (currentByte == SOP) {
					buffer = new char[BUFFER_SIZE];
					bufferIndex = 0;
					buffer[bufferIndex] = '\0';
					started = true;
					ended = false;
				} else if (currentByte == EOP) {
					ended = true;
					break;
				} else {
					if (bufferIndex < BUFFER_SIZE - 1) {
						try {
							buffer[bufferIndex] = currentByte;
							bufferIndex++;
							buffer[bufferIndex] = '\0';
						} catch (NullPointerException e) {
							if (D) Log.e(TAG, "Buffer Error", e);
							buffer = new char[BUFFER_SIZE];
							bufferIndex = 0;
							buffer[bufferIndex] = '\0';  
						}
					}
				}
			}
			if (started && ended) {
			      result = new String(buffer);
			      result = received.trim();
			      String[] currentData = received.split(",");
			      started = false;
			      ended = false;
			      bufferIndex = 0;
			      buffer[bufferIndex] = '\0';
			      return result;
			}
			return result;
		}
		
		protected void onPostExecute(String result) {
			received = result;
		    if (D) Log.d(TAG, received);
		}
	}*/
}
