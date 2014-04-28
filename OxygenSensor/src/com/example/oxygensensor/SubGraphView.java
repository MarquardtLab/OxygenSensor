// Oxygen Sensor
// SubGraphView
//
// This class defines the SubGraphView which is a custom surface view
// that is placed on the top of the calibration tab
// The SubGraphView displays the real-time intensity data during calibration
// including the guidelines to show which data will be collected
// After a model is created, it shows the real-time O2 concentration

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

public class SubGraphView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "SubGraphView";
	private static final boolean D = true;
	
	class SubGraphThread extends Thread {
		private final int CAL_SAMPLES = MainActivity.CAL_SAMPLES;
		private final int MAX_SAMPLES = MainActivity.MAX_SAMPLES;
		private final long FPS = 24;
		private SurfaceHolder mSurfaceHolder;
		private boolean running = false;
		
		public SubGraphThread(SurfaceHolder surfaceHolder, Context context) {
			mSurfaceHolder = surfaceHolder;
		}
		
		// Receives the graphing data from the main activity
		@SuppressLint("HandlerLeak")
		private Handler mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MESSAGE_DATA:
					currentIndex = msg.getData().getInt(MainActivity.INDEX);
					dataReference = msg.getData().getFloatArray(MainActivity.REFERENCE);
					dataSample = msg.getData().getFloatArray(MainActivity.SAMPLE);
					maxY = msg.getData().getInt(MainActivity.MAX);
					drawModel = msg.getData().getBoolean(MainActivity.MODEL);
					if (drawModel) {
						oxygenConcentration = msg.getData().getFloatArray(MainActivity.O2CONCENTRATION);
						minC = msg.getData().getFloat(MainActivity.MINC);
						maxC = msg.getData().getFloat(MainActivity.MAXC);
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
		
		// This method runs when the surface is active
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
			
			// Draw the graph axis
			canvas.drawLine(getLeft() + getWidth() / 9, getTop(), getLeft() + getWidth() / 9, getBottom() - getHeight() / 4, paint);
			canvas.drawLine(getLeft() + getWidth() / 9, getBottom() - getHeight() / 4, getRight(), getBottom() - getHeight() / 4, paint);
			
			int index = currentIndex - 1;
			
			// When the model doesn't exist, display the intensity data
			if (!drawModel) {
				canvas.drawText("Intensity", getLeft() + getWidth() / 160, getHeight() / 2, paint);
				// execute code if there is more than one data point
				if (currentIndex > 1) {
					// Display the intensity values
					paint.setColor(Color.BLUE);
					canvas.drawText("Reference: " + dataReference[index], getLeft() + getWidth() / 9, 30, paint);
					paint.setColor(Color.rgb(200, 100, 0));
					canvas.drawText("Sample: " + dataSample[index], getLeft() + getWidth() / 9, 60, paint);
					
					// Draw the intensities for both the reference data and the sample data
					for (int i = 0; i < currentIndex - 1; i++) {
						paint.setColor(Color.BLUE);					
						canvas.drawLine((getLeft() + getWidth() / 9) + i * ((getWidth() - getWidth() / 9) / index), 
								        (getBottom() - getHeight() / 4) - map(dataReference[i], 0, maxY, 0, (getHeight() - getHeight() / 4)), 
								        (getLeft() + getWidth() / 9) + (i + 1) * ((getWidth() - getWidth() / 9) / index), 
								        (getBottom() - getHeight() / 4) - map(dataReference[i + 1], 0, maxY, 0, ((getHeight() - getHeight() / 4))), 
								        paint);
						paint.setColor(Color.rgb(200, 100, 0));
						canvas.drawLine((getLeft() + getWidth() / 9) + i * ((getWidth() - getWidth() / 9) / index), 
										(getBottom() - getHeight() / 4) - map(dataSample[i], 0, maxY, 0, ((getHeight() - getHeight() / 4))), 
										(getLeft() + getWidth() / 9) + (i + 1) * ((getWidth() - getWidth() / 9) / index), 
										(getBottom() - getHeight() / 4) - map(dataSample[i + 1], 0, maxY, 0, ((getHeight() - getHeight() / 4))), 
										paint);
					}
				}
			// When the model is created, display the real-time oxygen concentration
			} else {
				canvas.drawText("[C]", getLeft() + getWidth() / 160, getHeight() / 2, paint);
				// If the minimum value is less than 0%, draw a new 0% axis
				if (minC < 0) {
					paint.setColor(Color.BLACK);
					canvas.drawText("0%", 60, (getBottom() - getHeight() / 4) - map(0, minC, maxC, 0, (getHeight() - getHeight() / 4)), paint);
					canvas.drawLine(getWidth() / 9,
							(getBottom() - getHeight() / 4) - map(0, minC, maxC, 0, (getHeight() - getHeight() / 4)),
							getRight(),
							(getBottom() - getHeight() / 4) - map(0, minC, maxC, 0, (getHeight() - getHeight() / 4)),
							paint);
				}
				// If the maximum value is greater than 100%, draw a new 100% axis
				if (maxC > 100) {
					paint.setColor(Color.BLACK);
					canvas.drawText("100%", 40, (getBottom() - getHeight() / 4) - map(100, minC, maxC, 0, (getHeight() - getHeight() / 4)), paint);
					canvas.drawLine(getWidth() / 9,
							(getBottom() - getHeight() / 4) - map(100, minC, maxC, 0, (getHeight() - getHeight() / 4)),
							getRight(),
							(getBottom() - getHeight() / 4) - map(100, minC, maxC, 0, (getHeight() - getHeight() / 4)),
							paint);
				}
				// Draw the oxygen concentration data
				paint.setColor(Color.rgb(200, 100, 0));
				canvas.drawText("Oxygen Concentration: " + roundToThreeSigFigs(oxygenConcentration[index]) + "%", getLeft() + getWidth() / 9, 30, paint);
				for (int i = 0; i < currentIndex - 1; i++) {
					canvas.drawLine((getLeft() + getWidth() / 9) + i * ((getWidth() - getWidth() / 9) / (currentIndex - 1)), 
					        (getBottom() - getHeight() / 4) - map(oxygenConcentration[i], minC, maxC, 0, (getHeight() - getHeight() / 4)), 
					        (getLeft() + getWidth() / 9) + (i + 1) * ((getWidth() - getWidth() / 9) / (currentIndex - 1)), 
					        (getBottom() - getHeight() / 4) - map(oxygenConcentration[i + 1], minC, maxC, 0, ((getHeight() - getHeight() / 4))), 
					        paint);
				}
			}
			// If we have filled the graph, draw calibration target lines
			if (currentIndex == MAX_SAMPLES - 1) {
				paint.setColor(Color.BLACK);
				canvas.drawLine((getLeft() + getWidth() / 9) + ((MAX_SAMPLES / 2 - CAL_SAMPLES / 2 - 1) * ((getRight() - getWidth() / 9) / index)), 
								getTop(), 
								(getLeft() + getWidth() / 9) + ((MAX_SAMPLES / 2 - CAL_SAMPLES / 2 - 1) * ((getRight() - getWidth() / 9) / index)), 
								getBottom() - getHeight() / 4, paint);				
				canvas.drawLine((getLeft() + getWidth() / 9) + ((MAX_SAMPLES / 2 + CAL_SAMPLES / 2 - 1) * ((getRight() - getWidth() / 9) / index)), 
								getTop(), 
								(getLeft() + getWidth() / 9) + ((MAX_SAMPLES / 2 + CAL_SAMPLES / 2 - 1) * ((getRight() - getWidth() / 9) / index)), 
								getBottom() - getHeight() / 4, paint);
			}
			
		}
		
		// Maps a value from a given range to a new range
		private float map(float val, float inMin, float inMax, float outMin, float outMax) {
			return (val - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
		}
		
		// Rounds a float to three significant figures
		private float roundToThreeSigFigs(float in) {
			BigDecimal bd = new BigDecimal(in);
			bd = bd.round(new MathContext(3));
			return bd.floatValue();
		}
	}
	
	public static final int MESSAGE_DATA = 1;

	private SubGraphThread thread;
	
	private int currentIndex;
	private float[] dataReference;
	private float[] dataSample;
	private boolean drawModel;
	private float[] oxygenConcentration;
	private float minC;
	private float maxC;
	private float maxY;
	
	public SubGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
		
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		thread = new SubGraphThread(holder, context);
		if (D) Log.d(TAG, "GraphThread created");
	}
	
	public SubGraphThread getThread() {
		return thread;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!thread.isAlive()) {
			thread = new SubGraphThread(holder, this.getContext());
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
