package com.tccec.emotionpimobile.network.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * Created by Vinicius on 11/11/2016.
 */
public class NetworkManager {
    // Used for logging success or failure messages
    private String TAG = getClass().getName();

    private static NetworkManager mInstance = null;

    private Context mContext;
    private WifiManager mWifiMngr;

    private Handler mHandler;

    private final String networkSSID = "EmotionPi";
    private final String networkKey = "emotionpitesting";

    /**
     * The {@link NetworkWifiListener} to inform activity when found the wlan.
     */
    private NetworkWifiListener mListener;

    public interface NetworkWifiListener {
        /**
         * This method is invoked when delivery of the frame needs to be done.
         * The returned value is a boolean to determine if processed the frame correctly
         */
        public void onWLANFound();
    };


    public static NetworkManager getInstance() {
        if(mInstance == null) {
            mInstance = new NetworkManager();
        }
        return mInstance;
    }

    public void Init(Context context) {
        this.mContext = context;
        mWifiMngr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mHandler = new Handler();
    }

    public boolean checkWifiOnAndConnected() {
        boolean result = false;

        if (mWifiMngr.isWifiEnabled()) { // Wi-Fi adapter is ON
            WifiInfo wifiInfo = mWifiMngr.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 ){
                //Not connected to an access point
                result = false;
            } else if (wifiInfo != null && !TextUtils.isEmpty(wifiInfo.getSSID())) {
                String ssid = wifiInfo.getSSID();
                Log.d(TAG, "ssid wlan: " + ssid);

                if(ssid.equals("\"EmotionPi\"")) {
                    result = true;
                } else {
                    result = false;
                }
            }
        }
        else {
            mWifiMngr.setWifiEnabled(true);
            result = false; // Wi-Fi adapter is OFF
        }
        return result;
    }

    public void connectToRPiWifi() {
        WifiConfiguration wifiConfig = new WifiConfiguration();

        wifiConfig.SSID = String.format("\"%s\"", networkSSID);
        wifiConfig.preSharedKey = String.format("\"%s\"", networkKey);

        boolean found = false;

        // Iterate through network connections already stored
        List<WifiConfiguration> list = mWifiMngr.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                mWifiMngr.disconnect();
                mWifiMngr.enableNetwork(i.networkId, true);
                mWifiMngr.reconnect();
                found = true;

                break;
            }
        }

        // Not found wifi configured
        if(!found) {
            int netId = mWifiMngr.addNetwork(wifiConfig);
            mWifiMngr.disconnect();
            mWifiMngr.enableNetwork(netId, true);
            mWifiMngr.reconnect();
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!checkWifiOnAndConnected()) {
                    mHandler.postDelayed(this, 1000);
                } else {
                    if(mListener != null) {
                        Log.d(TAG, "WLAN \"EmotionPi\" Connected!");
                        mListener.onWLANFound();
                    }
                }
            }
        }, 500);
    }

    public void setListener(NetworkWifiListener listener) {
        this.mListener = listener;
    }

}
