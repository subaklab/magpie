package io.subak.connect;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import io.subak.connect.crtp.CrtpPacket;


/**
 * Created by jeyong on 5/26/15.
 */
public class SubakWifiLink extends AbstractLink {


    /**
     * Number of packets without acknowledgment before marking the connection as
     * broken and disconnecting.
     */
    public static final int RETRYCOUNT_BEFORE_DISCONNECT = 10;

    /**
     * This number of packets should be processed between reports of the link
     * quality.
     */
    public static final int PACKETS_BETWEEN_LINK_QUALITY_UPDATE = 5;


    private static int TRANSFER_TIMEOUT = 100;

    private static final String LOG_TAG = "Mbugs Wifi Link";

    private Thread mWifiLinkThread;

    private final BlockingDeque<CrtpPacket> mSendQueue;


    /// socket

    private OutputStream outputStream = null;
    private Socket mConnection;
    private String IPaddress;
    private int server_port;
    private InetAddress serverAddr;


    /**
     * Create a new link using the Crazyradio.
     */
    public SubakWifiLink(String _IPaddress, int _server_port) {
        this.mSendQueue = new LinkedBlockingDeque<CrtpPacket>();
        this.IPaddress = _IPaddress;
        this.server_port = _server_port;

    }



    public void start()
    {
        try  {
            serverAddr = InetAddress.getByName(this.IPaddress);
            Log.d(LOG_TAG, this.IPaddress);

            if (mConnection == null) {
                mConnection = new Socket(serverAddr, server_port);
                Log.d(LOG_TAG, "CONNECTED");
                Log.d(LOG_TAG, serverAddr.toString());

            }

        } catch (UnknownHostException e1) {
            Log.d(LOG_TAG, "Stack trace");
            e1.printStackTrace();
        } catch (IOException e1) {
            Log.d(LOG_TAG, "IO exception");
            e1.printStackTrace();
        }


    }


    public void stop()
    {
        try  {
            if (mConnection != null) {
                mConnection.close();
            }
        } catch (UnknownHostException e1) {
            Log.d(LOG_TAG, "Stack trace");
            e1.printStackTrace();
        } catch (IOException e1) {
            Log.d(LOG_TAG, "IO eception");
            e1.printStackTrace();
        }
    }

    @Override
    public void connect() throws IllegalStateException {
        Log.d(LOG_TAG, "MBUGS Wifi Link start()");
        notifyConnectionInitiated();

        if (mWifiLinkThread == null) {
            mWifiLinkThread = new Thread(wifiControlRunnable);
            mWifiLinkThread.start();
        }
    }


    @Override
    public void disconnect() {
        stop();
    }

    @Override
    public boolean isConnected() {
        return mWifiLinkThread != null;
    }

    @Override
    public void send(CrtpPacket p) {
        this.mSendQueue.addLast(p);
    }


    /**
     * Handles communication with the dongle to send and receive packets
     */
    private final Runnable wifiControlRunnable = new Runnable() {
        @Override
        public void run() {
            start();

            Log.d(LOG_TAG, "MBUGS Wifi Link start()");

            notifyConnectionSetupFinished();

            while (mConnection != null) {
                try {
                    CrtpPacket p = mSendQueue.pollFirst(5, TimeUnit.MILLISECONDS);
                    if (p == null) { // if no packet was available in the send
                        // queue
                        //p = CrtpPacket.NULL_PACKET;
                        continue;
                    }

                    byte[] receiveData = new byte[33];
                    final byte[] sendData = p.toByteArray();

                    final int receivedByteCount = sendTransfer(sendData, receiveData);

                } catch (InterruptedException e) {
                    break;
                }
            }

            stop();

        }
    };

    //#Utility functions

    public int sendControlTransfer(int requestType, int request, int value, int index, byte[] data){
        return 0;
    }

    public int sendTransfer(byte[] data, byte[] receiveData){
        //Log.d(LOG_TAG, "MBUGS Wifi sendTransfer()");

        try {
            //start();

            //Log.d(LOG_TAG, "data stream");
            //Log.d(LOG_TAG, data.toString());
            outputStream = mConnection.getOutputStream();
            //Log.d(LOG_TAG, "getOutputStream");
            outputStream.write(data);
            //Log.d(LOG_TAG, "write");
            outputStream.flush();
            //mConnection.close();
            //Log.d(LOG_TAG, "flush");


            //stop();


        } catch (UnknownHostException e1) {
            Log.d(LOG_TAG, "Stack trace");
            e1.printStackTrace();
        } catch (IOException e1) {
            Log.d(LOG_TAG, "IO eception in sendTransfer()");
            e1.printStackTrace();
        }

        return 0;
    }
}
