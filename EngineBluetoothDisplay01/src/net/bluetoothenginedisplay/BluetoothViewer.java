/*
 *
 Copyright (C) 2014 Jonas Cleveland
 
 This Project is built to run with the OB2 ELM 327. It updates RPM, Engine Load, and 
 Speed Values in real-time. The black backscreen and green color text are optimal for 
 augmented reality wearable displays.
 credits: BluetoothViewer by Janos Gyerik 
 *
 */

package net.bluetoothenginedisplay;


import java.util.Timer;
import java.util.TimerTask;

import net.bluetoothviewer.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current session.
 */
public class BluetoothViewer extends Activity {
    // Debugging
    private static final String TAG = "BluetoothViewer";
    private static final boolean D = true;

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    
  
    
    private RadioButton mToolbarConnectButton;
   
    

    // Name of the connected device
    private String mConnectedDeviceName = null;
   
   
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the Bluetooth services
    private BluetoothEngineDisplay mBluetoothService = null;

    // State variables
    private boolean paused = false;
    private boolean connected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int delay1 = 1000; // delay for 1 sec. 
        int delay2 = 1200; // delay for 1 sec. 
        int delay3 = 1400; // delay for 1 sec. 
        int period = 600; // repeat every .6 sec. 
        Timer timer1 = new Timer(); 
        Timer timer2 = new Timer(); 
        Timer timer3 = new Timer(); 

        TimerTask task1 = new TimerTask() { 
                public void run() { 
                    speedAsk();  // display the data
                } 
            }; 
            TimerTask task2 = new TimerTask() { 
                public void run() { 
                    rpmAsk();
                } 
            }; 
            TimerTask task3 = new TimerTask() { 
                public void run() { 
                    engAsk();
                } 
            }; 
        
        timer1.scheduleAtFixedRate(task1, delay1, period);
        timer2.scheduleAtFixedRate(task2, delay2, period);
        timer3.scheduleAtFixedRate(task3, delay3, period);
	
        
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.activity_display_screen);
        
        mToolbarConnectButton = (RadioButton) findViewById(R.id.btn_connect);
        mToolbarConnectButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		startDeviceListActivity();
        	}
        });

        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
    
    private void startDeviceListActivity() {
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupUserInterface() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the Bluetooth session
        } else {
            if (mBluetoothService == null) setupUserInterface();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
    }

    private void setupUserInterface() {
        Log.d(TAG, "setupUserInterface()");

        // Initialize the BluetoothService to perform Bluetooth connections
        mBluetoothService = new BluetoothEngineDisplay(mHandler);

      
        
        onBluetoothStateChanged();
    }
    
    public void startOBDCommands(){
    	
    	String protocal = "41545350300D";
    	sendMessage(hexStringToByteArray(protocal)); 
    	
    	String verify = "415444500D";
    	sendMessage(hexStringToByteArray(verify)); 
    	
    }
    
    public void speedAsk(){
    	String speed = "303130440D";
        Log.d(TAG, "speed Ask");
    	sendMessage(hexStringToByteArray(speed)); 
    }
    
    public void rpmAsk(){
    	String rpm = "303130430D";
        Log.d(TAG, "rpm Ask");
        sendMessage(hexStringToByteArray(rpm)); 	
    }
    
    public void engAsk(){
    	String engload = "303130340D";
        Log.d(TAG, "engine Ask");
        sendMessage(hexStringToByteArray(engload)); 
    }

    public void startAskLoop(){
    	
        	//speed             
        	String speed = "303130440D";
	        Log.d(TAG, "speed Ask");
        	sendMessage(hexStringToByteArray(speed)); 
            
            //rpm
            String rpm = "303130430D";
	        Log.d(TAG, "rpm Ask");
            sendMessage(hexStringToByteArray(rpm));    
            
            //engine load
            String engload = "303130340D";
	        Log.d(TAG, "engine Ask");
            sendMessage(hexStringToByteArray(engload));      
            
    }
    
    

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth services
        if (mBluetoothService != null) mBluetoothService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothEngineDisplay.STATE_CONNECTED) {
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
            // Get the message bytes and tell the BluetoothService to write
            mBluetoothService.write(message);

        }
    }
    
public void startOBDCommandsButton(){
    	
    	String protocal = "41545350300D";
    	speakMessage(hexStringToByteArray(protocal)); 
    	
    	String verify = "415444500D";
    	speakMessage(hexStringToByteArray(verify)); 
    	
    }
    
    private void speakMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothEngineDisplay.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
            // Get the message bytes and tell the BluetoothService to write
            mBluetoothService.simpleWrite(message);
        }
    }
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }        
        return data;
    }
    
    public static String byteArrayToHex(byte[] a) {
 	   StringBuilder sb = new StringBuilder();
 	   for(byte b: a)
 	      sb.append(String.format("%02x", b&0xff));
 	   return sb.toString();
 	}
   

    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothEngineDisplay.STATE_CONNECTED:
                	Log.d(TAG, "STATE Connected");
                	connected = true;
                	startOBDCommands();
                    break;
                case BluetoothEngineDisplay.STATE_CONNECTING:
                	Log.d(TAG, "STATE Connecting");
                    break;
                case BluetoothEngineDisplay.STATE_NONE:
                	connected = false;
                	Log.d(TAG, "STATE None");
                    break;
                }
                onBluetoothStateChanged();
                break;
            case MESSAGE_WRITE:
            	Log.d(TAG, "STATE writing");
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                if (D) Log.d(TAG, "written = '" + writeMessage + "'");
                break;
            case MESSAGE_READ:
            	Log.d(TAG, "STATE reading");
            	if (paused) {
            		Log.d(TAG, "STATE Paused in reading");
            		break;
            	}
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                if (D) Log.d(TAG, readMessage);
          
                displayOnHit(readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
            	Log.d(TAG, "STATE Message Device Name");
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
            	
            	Log.d(TAG, "STATE Toast");

            	
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
        	
        	Log.d(TAG, "STATE Request Connect");

        	
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mBluetoothService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
        	
        	Log.d(TAG, "STATE enable BT");

        	
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a Bluetooth session
                setupUserInterface();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    public void displayOnHit(String readMessage){
    	
    	String speedval;
    	String engval;
    	String rpmval;
    	int sLength = readMessage.length(); 
    	int decode;
    	
    	if(sLength>4)
    	{
    	
    		if(readMessage.toUpperCase().startsWith("41 0D")){ // equals("41 0C")
    			TextView editText = (TextView) findViewById(R.id.speedView);
    			//41 0C 0F A0
    			speedval = readMessage.substring(6,8);
    			speedval = speedval.replaceAll("\\s+","");
  
    			decode = (int) Math.round(Integer.decode("0x" + speedval.toUpperCase())/1.609344);
    			editText.setText(Double.toString(decode) +" mph", TextView.BufferType.EDITABLE);
    		}
    	
    		if(readMessage.toUpperCase().startsWith("41 0C")){ //equals("41 0D")
    			TextView editText = (TextView) findViewById(R.id.rpmView);
    			rpmval = readMessage.substring(6,11);
    			rpmval = rpmval.replaceAll("\\s+","");

    			decode = (int)Math.round(Integer.decode("0x" + rpmval.toUpperCase())/4);
    			editText.setText( Double.toString(decode) +" rpm", TextView.BufferType.EDITABLE);
    		}
    	
    		if(readMessage.toUpperCase().startsWith("41 04")){
    			TextView editText = (TextView) findViewById(R.id.engineView);
    			engval = readMessage.substring(6,8);
    			decode = (int)Math.round(((double)Integer.decode("0x" + engval.toUpperCase())/255)*100);
    			editText.setText(" " + Double.toString(decode) + " %", TextView.BufferType.EDITABLE);
    		}
    		    	
    	}
    	
     
    
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_quit:
        	this.finish();
        }
        return false;
    }

    private void disconnectDevices() {
    	if (mBluetoothService != null) mBluetoothService.stop();
    	
    	onBluetoothStateChanged();
    }

    private void onBluetoothStateChanged() {
    	if (connected) {
			mToolbarConnectButton.setVisibility(View.GONE);

    	}
    	else {
			mToolbarConnectButton.setVisibility(View.VISIBLE);

    	}
		paused = false;
    }

    
}