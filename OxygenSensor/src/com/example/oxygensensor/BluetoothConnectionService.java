// Oxygen Sensor
// BluetoothConnectionService
//
// This class manages all of the activity regarding a bluetooth connection
// that includes, finding devices, connecting to devices, managing the
// connection, and closing the connection

package com.example.oxygensensor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothConnectionService {
	
	// Debugging
	private static final String TAG = "BluetoothConnectionService";
	private static final boolean D = true;
	
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private int mState;
	
	
	public static final int STATE_NONE = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;

	// Constructor to initialize a new connection service
	public BluetoothConnectionService (Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}
	
	// Sends the current state of the service to the main activity
	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;
		
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}
	
	// Returns the current state of the connection service
	public synchronized int getState() {
		return mState;
	}
	
	// Attempts to connect to a given Bluetooth device
	public synchronized void connect(BluetoothDevice device) {
		if (D) Log.d(TAG, "connect to: " + device);
		
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		
		
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}
	
	// Begins the connected thread and sends current device to main activity
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		if (D) Log.d(TAG, "connected");
		
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		
		setState(STATE_CONNECTED);
	}
	
	// Ends the connect or connected threads
	public synchronized void stop() {
		if (D) Log.d(TAG, "stop");
		
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
	}
	
	// Sends given byte array to the connected device
	// Throws NullPointerException if connected thread is null
	public void write(byte[] out) {
		ConnectedThread r;
		synchronized (this) {
			if (mState != STATE_CONNECTED) {
				return;
			}
			r = mConnectedThread;
		}
		try {
			r.write(out);
		} catch (NullPointerException e) {
			Log.e(TAG, "Failed to Write", e);
			connectionLost();
		}
	}
	
	// Alerts main activity that the connection was failed
	private void connectionFailed() {
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	// Alerts main activity that the connection was lost
	private void connectionLost() {
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	// Connect Thread that handles connecting to a device
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		
		// Constructor for the thread, takes in device that is meant to be connected
		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "Socket create() failed");
			}
			mmSocket = tmp;
		}
		
		// When thread is running, attempts to connect to the socket
		public void run() {
			mAdapter.cancelDiscovery();
			
			try {
				mmSocket.connect();
			} catch (IOException e) {
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connnection failure", e2);
				}
				connectionFailed();
				return;
			}
			
			synchronized (BluetoothConnectionService.this) {
				mConnectThread = null;
			}
			
			connected(mmSocket, mmDevice);
		}
		
		// When cancelled, the socket is closed
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
	
	// Connected Thread handles the communication to and from the device
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		
		// Constructor takes in the current connected socket
		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}
			
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		
		// While Thread running, reads incoming data from the device
		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;
			
			while(true) {
				try{
					// Reads bytes from incoming stream and stores it in buffer
					bytes = mmInStream.read(buffer);
					byte[] data = Arrays.copyOfRange(buffer, 0, bytes);
					//if (D) Log.d(TAG, new String(data));
					mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, data).sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}
		
		// Writes the given byte array to the device
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
				mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}
		
		// When cancelled, closes socket
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
