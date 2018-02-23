package com.tccec.emotionpimobile.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.tccec.emotionpimobile.R;
import com.tccec.emotionpimobile.custom.ripplebackground.RippleBackground;
import com.tccec.emotionpimobile.network.wifi.NetworkManager;

import java.util.ArrayList;

public class FindWifiActivity extends Activity {

    private ImageView foundDevice;
    private TextView titleTextView;

    private NetworkManager.NetworkWifiListener mListener = new NetworkManager.NetworkWifiListener() {
        @Override
        public void onWLANFound() {
            foundDevice();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_wifi);

        titleTextView = (TextView) findViewById(R.id.searchingWLANTitle);

        // setting the typeface
        Typeface avenir_book = Typeface.createFromAsset(getAssets(), "fonts/avenir_book.ttf");
        titleTextView.setTypeface(avenir_book);

        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);

        foundDevice=(ImageView)findViewById(R.id.foundDevice);
        ImageView button=(ImageView)findViewById(R.id.centerImage);

        rippleBackground.startRippleAnimation();

        NetworkManager.getInstance().setListener(mListener);
        NetworkManager.getInstance().connectToRPiWifi();

        /*final Handler handler=new Handler();

            button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.startRippleAnimation();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        foundDevice();
                    }
                },3000);
            }
        });*/
    }

    private void foundDevice(){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(400);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        ArrayList<Animator> animatorList=new ArrayList<Animator>();
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleX", 0f, 1.2f, 1f);
        animatorList.add(scaleXAnimator);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleY", 0f, 1.2f, 1f);
        animatorList.add(scaleYAnimator);
        animatorSet.playTogether(animatorList);
        foundDevice.setVisibility(View.VISIBLE);
        animatorSet.start();


        titleTextView.setText(getResources().getString(R.string.connectingWLAN));
        Handler handler = new Handler();

        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                // Ok, it's connected on the RPi's wlan
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(getApplicationContext(), GameActivity.class));
                    }
                });
            }
        }, 3000);
    }

}
