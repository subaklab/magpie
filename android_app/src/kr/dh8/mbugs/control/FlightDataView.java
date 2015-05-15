/**
 *    ||          ____  _ __                           
 * +------+      / __ )(_) /_______________ _____  ___ 
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2013 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package kr.dh8.mbugs.control;

import java.math.BigDecimal;

import kr.dh8.mbugs.R;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Compound component that groups together flight data UI elements
 *
 */
public class FlightDataView extends LinearLayout {

    private TextView textView_pitch;
    private TextView textView_roll;
    private TextView textView_thrust;
    private TextView textView_yaw;
    private TextView textView_mode;
    private TextView textView_linkQuality;
    private MainActivity context;
	private WifiManager wifi;

    public FlightDataView(Context context, AttributeSet attrs) {
      super(context, attrs);

      this.context = (MainActivity) context;
      
      setOrientation(LinearLayout.HORIZONTAL);

      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      inflater.inflate(R.layout.view_flight_data, this, true);
      
      textView_pitch = (TextView) findViewById(R.id.pitch);
      textView_roll = (TextView) findViewById(R.id.roll);
      textView_thrust = (TextView) findViewById(R.id.thrust);
      textView_yaw = (TextView) findViewById(R.id.yaw);
      textView_mode = (TextView) findViewById(R.id.mode);
      textView_linkQuality = (TextView) findViewById(R.id.linkQuality);
      //initialize
      updateFlightData();
	
		
      setLinkQualityText("n/a");
    }

    public FlightDataView(Context context) {
      this(context, null);
    }

    public void updateFlightData() {
        textView_pitch.setText(format(R.string.pitch, round(context.getPitch() * -1))); // inverse
        textView_roll.setText(format(R.string.roll, round(context.getRoll())));
        textView_thrust.setText(format(R.string.thrust, round(context.getThrust())));
        textView_yaw.setText(format(R.string.yaw, round(context.getYaw())));
        //textView_mode.setText(format(R.string.mode, context.getMode()));
        textView_mode.setText(context.getMode());
        updateLinkQualityText();
    }

    private String format(int identifier, Object o){
        return String.format(getResources().getString(identifier), o);
    }

    public static double round(double unrounded) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return rounded.doubleValue();
    }
    
    public void setWifiManager(WifiManager wifi)
    {
    	this.wifi = wifi;    	
    }
    
    public void updateLinkQualityText(){

    	WifiInfo info = null;
  		String quality = "n/a";

  		if (this.wifi != null) {
  			info = this.wifi.getConnectionInfo();
  		}

		if (info != null) {
  			quality = Integer.toString(info.getRssi());
  		}
  		
  		setLinkQualityText(quality);
    }

    
    public void setLinkQualityText(String quality){		
        textView_linkQuality.setText(format(R.string.linkQuality, quality));
    }

}
