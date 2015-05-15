package kr.dh8.mbugs.control;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import kr.dh8.mbugs.lib.crtp.CommanderPacket;
import kr.dh8.mbugs.lib.Link;
import kr.dh8.mbugs.lib.MbugsWifiLink;
import kr.dh8.mbugs.lib.ConnectionAdapter;

import kr.dh8.mbugs.R;

import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import 	android.os.StrictMode;

import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;
 


public class MainActivity extends Activity {

    private static final String TAG = "MbugsControl";

    private static final int MAX_THRUST = 65535;

    
    private DualJoystickView mJoysticks;
    private FlightDataView mFlightDataView;
    private MjpegView mMjpegview;
    
    // joystic related   
    public int resolution = 1000;

    private float right_analog_x;
	private int mRightAnalogXAxis = MotionEvent.axisFromString("AXIS_Z");

	private float right_analog_y;
	private int mRightAnalogYAxis = MotionEvent.axisFromString("AXIS_RZ");

	private float left_analog_x;
	private int mLeftAnalogXAxis = MotionEvent.axisFromString("AXIS_X");
	
	private float left_analog_y;
	private int mLeftAnalogYAxis = MotionEvent.axisFromString("AXIS_Y");

	private float split_axis_yaw_right;
	private int mSplitAxisYawRightAxis;

	private float split_axis_yaw_left;
	private int mSplitAxisYawLeftAxis;

	private float maxYawAngle = 150;
	
    float deadzone = (float) 0.1;

    private Thread mSendJoystickDataThread;
    
 
    float mRollTrim = (float) 0.0;
    float mPitchTrim = (float) 0.0; // if backward drift -> set positive pitch trim        

    boolean mMode = false;

	// link related
    private Link mbugsLink;

    
	// control parameters
    
    int maxThrust = 80; // if you want need more power. free this limit!
	int minThrust = 10;
    
	// mjpeg view related 
	
    private MjpegView mv;
    ImageView drawingImageView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
//	    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//	    	.detectDiskReads()
//	    	.detectDiskWrites()
//	    	.detectNetwork()   // or .detectAll() for all detectable problems
//	    	.penaltyLog()
//	    	.build());
//	    
//	    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//	    	.detectLeakedSqlLiteObjects()
//	    	.detectLeakedClosableObjects()
//	    	.penaltyLog()
//	    	.penaltyDeath()
//	    	.build());
//
//		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
	    
		
		
	   final Button descPitchTrimBotton = (Button) findViewById(R.id.desc_pitch_trim);
	    descPitchTrimBotton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	    	mPitchTrim--;
	        mFlightDataView.updateFlightData();
	
	    }
	  });
	
	
	   final Button incPitchTrimBotton = (Button) findViewById(R.id.inc_pitch_trim);
	   incPitchTrimBotton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	mPitchTrim++;
	            mFlightDataView.updateFlightData();
	
	        }
	    });
	
	    
	   final Button descRollTrimBotton = (Button) findViewById(R.id.desc_roll_trim);
	   descRollTrimBotton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            // Perform action on click
	        	mRollTrim--;
	            mFlightDataView.updateFlightData();
	
	        }
	    });
	    
	   final Button incRollTrimBotton = (Button) findViewById(R.id.inc_roll_trim);
	   incRollTrimBotton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            // Perform action on click
	    	mRollTrim++;
	        mFlightDataView.updateFlightData();
	
	    }
	   });
	   

	   final Button toggleModeBotton = (Button) findViewById(R.id.toggle_mode);
	   toggleModeBotton.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            // Perform action on click
	        mMode = !mMode;
	        mFlightDataView.updateFlightData();
	
	    }
	   });
	   
		
		mJoysticks = (DualJoystickView) findViewById(R.id.joysticks);
		mJoysticks.setMovementRange(resolution, resolution);

	    mFlightDataView = (FlightDataView) findViewById(R.id.flightdataview);
    	WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
	    mFlightDataView.setWifiManager(wifi);

	    mv = (MjpegView) findViewById(R.id.mjpegview);
	    
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        resetInputMethod(); // connect joystic event!    
        
        // try connect mbugs!
        try {
            linkConnect();
        } catch (IllegalStateException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    
        //mv.setAlpha(1.0f); // TODO: FIX default show!
        mv.setAlpha(0.0f); // TODO: FIX hide

        // mjpeg stream 
        String URL = "http://192.168.1.1:8080/?action=stream";
        new DoRead().execute(URL);
	}
	
	
    @Override
    protected void onPause() {
        super.onPause();
        if (mbugsLink != null) {
            linkDisconnect();
        }
        mv.stopPlayback();
    }
    
  
    private void resetInputMethod() {
        Toast.makeText(this, "Using on-screen controller", Toast.LENGTH_SHORT).show();       
        mJoysticks.setOnJostickMovedListener(_listenerLeft, _listenerRight);
    }

	

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a joystick since a generic motion event
        // could be almost anything.
        if ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0
                && event.getAction() == MotionEvent.ACTION_MOVE) {

            // default axis are set to work with PS3 controller
            right_analog_x = (float) (event.getAxisValue(mRightAnalogXAxis));
            right_analog_y = (float) (event.getAxisValue(mRightAnalogYAxis));
            left_analog_x = (float) (event.getAxisValue(mLeftAnalogXAxis));
            left_analog_y = (float) (event.getAxisValue(mLeftAnalogYAxis));

            split_axis_yaw_right = (float) (event.getAxisValue(mSplitAxisYawRightAxis));
            split_axis_yaw_left = (float) (event.getAxisValue(mSplitAxisYawLeftAxis));

            mFlightDataView.updateFlightData();
            return true;
        } else {
            return super.dispatchGenericMotionEvent(event);
        }
    }
    
    private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            right_analog_y = (float) tilt / resolution;
            right_analog_x = (float) pan / resolution;

            mFlightDataView.updateFlightData();
        }

        @Override
        public void OnReleased() {
            // Log.i("Joystick-Right", "Release");
            right_analog_y = 0;
            right_analog_x = 0;
        }

        public void OnReturnedToCenter() {
            // Log.i("Joystick-Right", "Center");
            right_analog_y = 0;
            right_analog_x = 0;
        }
    };

    private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            left_analog_y = (float) tilt / resolution;
            left_analog_x = (float) pan / resolution;

            mFlightDataView.updateFlightData();
        }

        @Override
        public void OnReleased() {
            left_analog_y = 0;
            left_analog_x = 0;
        }

        public void OnReturnedToCenter() {
            left_analog_y = 0;
            left_analog_x = 0;
        }
    };


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true; 
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mv.setAlpha(1.0f); // TODO: FIX, It's show           
//            	mv.startPlayback();
                break;
            case R.id.menu_disconnect:
                mv.setAlpha(0.0f); // TODO: FIX hide
                mv.stopPlayback();

                break;
        }
        return true;
    }

			
	///////////////// joystic related
    public float getRightAnalog_X() {
        return right_analog_x;
    }
    
    public float getRightAnalog_Y() {
        return right_analog_y;
    }
    
    public float getLeftAnalog_X() {
        return left_analog_x;
    }

    public float getLeftAnalog_Y() {
        return left_analog_y;
    }
    
    public float getThrustFactor() {
        int addThrust = 0;

		if ((maxThrust - minThrust) < 0) {
            addThrust = 0; // do not allow negative values
        } else {
            addThrust = (maxThrust - minThrust);
        }
        return addThrust;
    }

    public float getYawFactor() {
        return maxYawAngle;
    }


	public double getThrust() {
        float thrust = getRightAnalog_Y();
        thrust = thrust * -1; // invert
        //Log.d(TAG, Float.toString(minThrust + (thrust * getThrustFactor())));

		if (thrust > deadzone) {
            return minThrust + (thrust * getThrustFactor());
        }
        return 0;
	}
	

    public float getRollPitchFactor() {
        // float maxRollPitchAngle = (float) 20.0; // default roll pitch angle is 20
        float maxRollPitchAngle = (float) 20.0;
        
		return maxRollPitchAngle;
    }
    
    private float getRollTrim() {
		return mRollTrim;
    }
    
    public float getRoll() {
        float roll = getRightAnalog_X();      
       // return (roll + getRollTrim()) * getRollPitchFactor() * getDeadzone(roll)- (float)9.0;
        //return (roll + getRollTrim()) * getRollPitchFactor() * getDeadzone(roll);
        //return (roll * getRollPitchFactor() * getDeadzone(roll) + getRollTrim());
        return roll * getRollPitchFactor() * getDeadzone(roll) + getRollTrim();
    }
    
    private float getPitchTrim() {
        //float mPitchTrim = (float) 0.0; // if backward drift -> set positive pitch trim
		return mPitchTrim * -1;
    }

    public float getPitch() {
    	float pitch = getLeftAnalog_Y(); 
        //return (pitch + getPitchTrim()) * getRollPitchFactor() * getDeadzone(pitch) - (float)16.0;
        return (pitch * getRollPitchFactor() * getDeadzone(pitch)) + getPitchTrim();
    }

    public float getYaw() {
        float yaw = 0;
        yaw = getLeftAnalog_X();

        return yaw * getYawFactor() * getDeadzone(yaw);
    }
    
    public String getMode() {
    	if (isXmode() == true) {
    		return "X";
    	} else {
    		return "+";
    	}
    }

    private float getDeadzone(float axis) {
        if (axis < deadzone && axis > deadzone * -1) {
            return 0;
        }
        return 1;
    }
    
    ///////////////////// link related
    
    public boolean isXmode() {
       return mMode;
    }

    private void linkConnect() {
        // ensure previous link is disconnected
        linkDisconnect();
        
        try {
            // create link
        	mbugsLink = new MbugsWifiLink("192.168.1.1", 2002);

            // add listener for connection status
        	mbugsLink.addConnectionListener(new ConnectionAdapter() {
                @Override
                public void connectionSetupFinished(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void connectionLost(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
                        }
                    });
                    linkDisconnect();
                }

                @Override
                public void connectionFailed(Link l) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                    linkDisconnect();
                }

                @Override
                public void linkQualityUpdate(Link l, final int quality) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFlightDataView.setLinkQualityText(quality + "%");
                        }
                    });
                }
            });
        	
        	mbugsLink.connect();
            
            mSendJoystickDataThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mbugsLink != null) {
                     	mbugsLink.send(new CommanderPacket(getRoll(), getPitch(), getYaw(),
                                (char) (getThrust()/100 * MAX_THRUST), isXmode()));  

                        try {
                            Thread.sleep(50, 0);                         

                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
            mSendJoystickDataThread.start();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
            Toast.makeText(this, "MBUGS Wifi not attached", Toast.LENGTH_SHORT).show();
        }
    }

        
    private void linkDisconnect() {
        if (mbugsLink != null) {
        	mbugsLink.disconnect();
            mbugsLink = null;
        }
        if (mSendJoystickDataThread != null) {
            mSendJoystickDataThread.interrupt();
            mSendJoystickDataThread = null;
        }
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // link quality is not available when there is no active connection
                mFlightDataView.setLinkQualityText("n/a");
            }
        });
    }

    
    

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();     
            Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if(res.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());  
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-IOException ", e);
                //Error connecting to camera
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            //mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.setDisplayMode(MjpegView.SIZE_STANDARD);
            mv.showFps(true);
        }
    }
    
}
