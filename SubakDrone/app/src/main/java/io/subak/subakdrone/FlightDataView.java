package io.subak.subakdrone;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;

/**
 * Created by jeyong on 5/26/15.
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
