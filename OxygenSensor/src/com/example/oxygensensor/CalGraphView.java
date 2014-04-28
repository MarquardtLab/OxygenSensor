// Oxygen Sensor
// CalGraphView
//
// This class defines the CalGraphView which is a custom surface view
// that is placed on the bottom of the Calibration tab
// The CalGraphView displays the collected data points during calibration
// After a model is created, it shows the O2 model

package com.example.oxygensensor;

import java.math.BigDecimal;
import java.math.MathContext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CalGraphView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "CalGraphView";
	private static final boolean D = true;
	
	class CalGraphThread extends Thread {
		private final long FPS = 24;
		private SurfaceHolder mSurfaceHolder;
		private boolean running = false;
		
		public CalGraphThread(SurfaceHolder surfaceHolder, Context context) {
			mSurfaceHolder = surfaceHolder;
		}

		// Receives the calibration data from the main activity
		@SuppressLint("HandlerLeak")
		private Handler mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MESSAGE_DATA:
					drawModel = msg.getData().getBoolean(MainActivity.MODEL);
					if (drawModel) {
						calN2 = msg.getData().getFloatArray(MainActivity.CALN2);
						airCollected = msg.getData().getBoolean(MainActivity.AIRCOLLECTED);
						O2Collected = msg.getData().getBoolean(MainActivity.O2COLLECTED);
						if (airCollected) {
							calAir = msg.getData().getFloatArray(MainActivity.CALAIR);
						}
						if (O2Collected) {
							calO2 = msg.getData().getFloatArray(MainActivity.CALO2);
						}
						isOneSite = msg.getData().getBoolean(MainActivity.ONESITE);
						isTwoSite = msg.getData().getBoolean(MainActivity.TWOSITE);
						if (isOneSite) {
							calMin = msg.getData().getFloat(MainActivity.CALMIN);
							calMax = msg.getData().getFloat(MainActivity.CALMAX);
							modelMin = msg.getData().getFloat(MainActivity.MODELMIN);
							modelMax = msg.getData().getFloat(MainActivity.MODELMAX);
							slope = msg.getData().getFloat(MainActivity.SLOPE);
							intercept = msg.getData().getFloat(MainActivity.INTERCEPT);
						} else if (isTwoSite) {
							a = msg.getData().getFloat(MainActivity.A);
							b = msg.getData().getFloat(MainActivity.B);
							c = msg.getData().getFloat(MainActivity.C);
						}
						RSquared = msg.getData().getFloat(MainActivity.RSQUARED);
						RMS = msg.getData().getFloat(MainActivity.ROOTMEANSQUARE);
					} else { // Calibration data
						O2Sent = msg.getData().getBoolean(MainActivity.O2SENT);
						airSent = msg.getData().getBoolean(MainActivity.AIRSENT);
						N2Sent = msg.getData().getBoolean(MainActivity.N2SENT);
						if (O2Sent) {
							dataO2 = msg.getData().getFloatArray(MainActivity.O2SAMPLE);
						}
						if (airSent) {
							dataAir = msg.getData().getFloatArray(MainActivity.AIRSAMPLE);
						}
						if (N2Sent) {
							dataN2 = msg.getData().getFloatArray(MainActivity.N2SAMPLE);
						}
						maxY = msg.getData().getInt(MainActivity.MAX);
					}
					break;
				}
			}
		};
		
		public Handler getHandler() {
			return mHandler;
		}
		
		public void setRunning(boolean run) {
			running = run;
		}
		
		// This method runs while the surface view is active
		@Override
		public void run() {
			long ticksPS = 1000 / FPS;
			long startTime;
			long sleepTime;
			while (running) {
				Canvas c = null;
				startTime = System.currentTimeMillis();
				try {
					c = mSurfaceHolder.lockCanvas();
					synchronized (mSurfaceHolder) {
						if (running) {
							doDraw(c);
						}
					}
				} finally {
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
				sleepTime = ticksPS-(System.currentTimeMillis() - startTime);
				try {
					if (sleepTime > 0) {
						sleep(sleepTime);
					} else {
						sleep(10);
					}
				} catch (Exception e) {
					if (D) Log.e(TAG, "Error sleeping", e);
				}
			}
		}
		
		// This method defines what to draw on the surface view
		public void doDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE);
			Paint paint = new Paint();
			paint.setColor(Color.BLACK);
			paint.setTextSize(20);
			// Draw the axis of the calibration graph
			canvas.drawLine(getLeft() + getWidth() / 9, 0, getLeft() + getWidth() / 9, getHeight() - getHeight() / 4, paint);
			canvas.drawLine(getLeft() + getWidth() / 9, getHeight() - getHeight() / 4, getRight(), getHeight() - getHeight() / 4, paint);
			canvas.drawText("[C]", (getRight() - getWidth() / 9) / 2, (getHeight() - 60), paint);
			canvas.drawText("0%", map(0, 0, 100, getWidth() / 9, getRight()), (getHeight() - 100), paint);
			canvas.drawText("21%", map(21, 0, 100, getWidth() / 9, getRight()), (getHeight() - 100), paint);
			canvas.drawText("100%", map(100, 0, 100, getWidth() / 9, getRight()) - getWidth() / 1600, (getHeight() - 100), paint);
			
			if (drawModel) {
				canvas.drawText("I0/I", getLeft() + getWidth() / 160, (getHeight() - getHeight() / 4) / 2, paint);
				canvas.drawText("" + roundToThreeSigFigs(modelMin), 40,
						(getHeight() - getHeight() / 4) - map(modelMin, calMin, calMax, 0, (getHeight() - getHeight() / 4)), paint);
				canvas.drawText("" + roundToThreeSigFigs(modelMax), 40,
						(getHeight() - getHeight() / 4) - map(modelMax, calMin, calMax, 0, (getHeight() - getHeight() / 4)), paint);
				
				// Draw x axis if the model minimum is less than zero
				if (modelMin < 0) {
				canvas.drawLine(getLeft() + getWidth() / 9, 
					    (getHeight() - getHeight() / 4) - map(0, calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
					    getRight(), 
					    (getHeight() - getHeight() / 4) - map(0, calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
					    paint);
				}
				// Draw one site model
				if (isOneSite) {
					canvas.drawText("Ksv: " + slope, getWidth() / 9, 30, paint);
					canvas.drawText("Intercept: " + intercept, getWidth() / 9, 60, paint);
					canvas.drawText("R^2: " + RSquared, getWidth() / 9, getWidth() / 9, paint);
					canvas.drawText("RMS: " + RMS, getWidth() / 9, getHeight() / 4, paint);			
					canvas.drawLine(getLeft() + getWidth() / 9, 
								    (getHeight() - getHeight() / 4) - map(modelMin, calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
								    getRight(), 
								    (getHeight() - getHeight() / 4) - map(modelMax, calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
								    paint);
				// Draw two site model
				} else if (isTwoSite) {
					canvas.drawText("I0/I = " + a + " * [C]^2 + " + b + " * [C] + " + c, 
							getLeft() + getWidth() / 9, 
							30, 
							paint);
					canvas.drawText("R^2: " + RSquared, getWidth() / 9, 60, paint);
					canvas.drawText("RMS: " + RMS, getWidth() / 9, getWidth() / 9, paint);
					for (int i = 0; i < 100; i++) {
						canvas.drawLine(getLeft() + getWidth() / 9 + (float) ((getWidth() - getWidth() / 9) / 100) * i, 
								(getHeight() - getHeight() / 4) - map(a * (float) Math.pow(i, 2) + b * i + c, calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
								getLeft() + getWidth() / 9 + (float) ((getWidth() - getWidth() / 9) / 100) * (i + 1), 
								(getHeight() - getHeight() / 4) - map(a * (float) Math.pow((i + 1), 2) + b * (i + 1) + c, calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
								paint);
					}
				}
				paint.setColor(Color.rgb(200, 100, 0));
				for (int i = 0; i < MainActivity.CAL_SAMPLES; i++) {
					if (O2Collected) {
						canvas.drawCircle(getRight() - getWidth() / 160, 
								  (getHeight() - getHeight() / 4) - map(calO2[i], calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
								  getWidth() / 160, 
								  paint);
					}
					if (airCollected) {
						canvas.drawCircle((float) ((getWidth() - getWidth() / 9) * 0.21 + getWidth() / 9), 
								  (getHeight() - getHeight() / 4) - map(calAir[i], calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
								  getWidth() / 160, 
								  paint);
					}
					canvas.drawCircle(getLeft() + 95, 
							  (getHeight() - getHeight() / 4) - map(calN2[i], calMin, calMax, 0, (getHeight() - getHeight() / 4)), 
							  getWidth() / 160, 
							  paint);
				}
			} else { // Draw Calibration
				canvas.drawText("Intensity", getLeft() + getWidth() / 160, (getHeight() - getHeight() / 4) / 2, paint);
				paint.setColor(Color.rgb(200, 100, 0));
				for (int i = 0; i < MainActivity.CAL_SAMPLES; i++) {
					if (dataO2 != null) {
						canvas.drawCircle(getRight() - getWidth() / 160, 
										  (getHeight() - getHeight() / 4) - map(dataO2[i], 0, maxY, 0, (getHeight() - getHeight() / 4)), 
										  getWidth() / 160, 
										  paint);
					}
					if (dataAir != null) {
						canvas.drawCircle((float) ((getWidth() - getWidth() / 9) * 0.21 + getWidth() / 9), 
										  (getHeight() - getHeight() / 4) - map(dataAir[i], 0, maxY, 0, (getHeight() - getHeight() / 4)), 
										  getWidth() / 160, 
										  paint);
					}
					if (dataN2 != null) {
						canvas.drawCircle(getLeft() + 95, 
										  (getHeight() - getHeight() / 4) - map(dataN2[i], 0, maxY, 0, (getHeight() - getHeight() / 4)), 
										  getWidth() / 160, 
										  paint);
					}
				}
			}
		}
		
		// maps a value from a given range to a new range
		private float map(float val, float inMin, float inMax, float outMin, float outMax) {
			return (val - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
		}
		
		// rounds a float to three significant figures
		private float roundToThreeSigFigs(float in) {
			BigDecimal bd = new BigDecimal(in);
			bd = bd.round(new MathContext(3));
			return bd.floatValue();
		}
	}
	
	public static final int MESSAGE_DATA = 1;

	private CalGraphThread thread;
	

	private float maxY;
	private boolean drawModel;
	private boolean O2Sent;
	private boolean airSent;
	private boolean N2Sent;
	private boolean isOneSite;
	private boolean isTwoSite;
	private boolean airCollected;
	private boolean O2Collected;
	private float[] dataO2;
	private float[] dataAir;
	private float[] dataN2;
	private float[] calO2;
	private float[] calAir;
	private float[] calN2;
	private float slope;
	private float intercept;
	private float a;
	private float b;
	private float c;
	private float calMin;
	private float calMax;
	private float modelMin;
	private float modelMax;
	private float RSquared;
	private float RMS;
	
	public CalGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
		
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		thread = new CalGraphThread(holder, context);
		if (D) Log.d(TAG, "GraphThread created");
	}
	
	public CalGraphThread getThread() {
		return thread;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!thread.isAlive()) {
			thread = new CalGraphThread(holder, this.getContext());
		}
		thread.setRunning(true);
		if (D) Log.d(TAG, "Running set true");
		if (thread.getState() == Thread.State.NEW) {
			thread.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		thread.setRunning(false);
		if (D) Log.d(TAG, "Running set false");
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
				if (D) Log.e(TAG, "Surface Destroyed Error", e);
			}
		}
	}
	
	
}
