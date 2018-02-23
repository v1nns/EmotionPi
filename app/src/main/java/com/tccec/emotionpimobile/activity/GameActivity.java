package com.tccec.emotionpimobile.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Vibrator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.tccec.emotionpimobile.R;
import com.tccec.emotionpimobile.camera.CameraControllerActivity;
import com.tccec.emotionpimobile.game.Action;
import com.tccec.emotionpimobile.game.GameManager;
import com.tccec.emotionpimobile.custom.circularbutton.CircularProgressButton;
import com.tccec.emotionpimobile.game.GameManager.CommunicationResultListener;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Vinicius Longaray on 26/10/16.
 */
public class GameActivity extends CameraControllerActivity{
    // Used for logging success or failure messages
    private final String TAG = getClass().getName();

    /**
     * TextViews {@link TextView}, buttons {@link CircularProgressButton} and
     * ImageView {@link ImageView} from layout
     */
    private TextView titleTextView, ptsLabelTextView, ptsValueTextView, countdownTextView;
    private CircularProgressButton circularButton1;
    private ImageView mSlidingImage;

    private ToneGenerator tone;
    private Vibrator vibration;

    private int currentEmotion;
    private int raffledEmotion;
    private int[] emotions;

    private ExecutorService mExecutor;
    private Handler mHandler;

    private Boolean emotionMatched = false;
    private Boolean playing = false;
    private Boolean gameOver = false;

    private static final int TIMER_UPDATE_RESULT_PERIOD = 7500;

    private CommunicationResultListener mListener = new CommunicationResultListener() {
        @Override
        public boolean onResult(Action id, String data) {
            Log.i(TAG, "called onResult from CommunicationResultListener on GameActivity");

            if(id == Action.PROCESSIMAGE) {
                if(currentEmotion == Integer.parseInt(data)) {
                    onStopCapturingCameraFrame();
                    emotionMatched = true;
                    /* TODO - set event to match emotion */
                    return true;
                }
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_game);

        emotions = new int[]{R.drawable.emoji_angry, R.drawable.emoji_happy, R.drawable.emoji_neutral, R.drawable.emoji_background};

        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        vibration = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        titleTextView = (TextView) findViewById(R.id.title);
        ptsLabelTextView = (TextView) findViewById(R.id.ptsLabel);
        ptsValueTextView = (TextView) findViewById(R.id.ptsValue);
        countdownTextView= (TextView) findViewById(R.id.countdown);

        // setting the typeface
        Typeface avenir_book = Typeface.createFromAsset(getAssets(), "fonts/avenir_book.ttf");
        titleTextView.setTypeface(avenir_book);
        ptsLabelTextView.setTypeface(avenir_book);
        ptsValueTextView.setTypeface(avenir_book);
        countdownTextView.setTypeface(avenir_book);

        mSlidingImage = (ImageView) findViewById(R.id.emojiImg);
        mSlidingImage.setImageDrawable(getResources().getDrawable(R.drawable.emoji_background));

        /* TODO - get from sharedpreferences */
        GameManager.getInstance().send(Action.INIT, new String("ViniciusMobile"));

        /*AlertDialog alertDialog = new AlertDialog.Builder(GameActivity.this).create();
        alertDialog.setTitle("ERROR");
        alertDialog.setMessage("Server not found!");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();*/

        countdownTextView.setVisibility(View.INVISIBLE);

        GameManager.getInstance().setCommunicationResultListener(mListener);
        mHandler = new Handler();

        // temp
        //ptsValueTextView.setText("10");

        circularButton1 = (CircularProgressButton) findViewById(R.id.circularButton1);
        circularButton1.setTypeface(avenir_book);
        circularButton1.setIndeterminateProgressMode(true);
        circularButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(playing) {
                    playing = false;

                    circularButton1.setProgress(0);
                    onStopCapturingCameraFrame();
                    GameManager.getInstance().send(Action.CANCELEMOTION, "");
                } else {
                    if(gameOver) {
                        finishGame();
                    } else {
                        playing = true;

                        executeNewRound();
                    }
                }
            }
        });
    }

    public void executeNewRound() {
        GameManager.getInstance().send(Action.ASKFOREMOTION, "Hey");
        onStartCapturingCameraFrame();

        if(mExecutor != null) {
            mExecutor.shutdown();
            try {
                mExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mExecutor = Executors.newSingleThreadExecutor();
        mExecutor.submit(new GameTask(mGameTaskListener));
        circularButton1.setProgress(50);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        if(mExecutor != null) {
            mExecutor.shutdown();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        onStopCapturingCameraFrame();

        if(mExecutor != null) {
            mExecutor.shutdown();
        }

        Intent BackpressedIntent = new Intent();
        BackpressedIntent.setClass(getApplicationContext(), HomeScreenActivity.class);
        BackpressedIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(BackpressedIntent);
        finish();
    }

    /**
     * {@link GameTaskListener} handles the UI by the call in GameTask
     */
    public interface GameTaskListener {
        /**
         * This method is invoked when must update the UI
         */
        public void onTaskUpdate(final int time);

        /**
         * This method is invoked when must change the UI the last time
         */
        public void onTaskFinished(final Boolean result, final int time);
    };

    private GameTaskListener mGameTaskListener = new GameTaskListener() {
        @Override
        public void onTaskUpdate(final int time) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateCountdown(time);
                }
            });
        }

        @Override
        public void onTaskFinished(final Boolean result, final int time) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updatePoints(result, time);
                }
            });
        }
    };

    class GameTask implements Callable<Boolean> {
        GameTaskListener myListener;

        public GameTask(GameTaskListener listener) {
            this.myListener = listener;
        }

        @Override
        public Boolean call() throws Exception {
            Log.d(TAG, "Called GameTask");

            long startTime = System.currentTimeMillis();
            Boolean result = false;
            int count = 0;
            int remainingSeconds = 0;

            /* Raffle a new emotion */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    raffleEmotion();
                }
            });

            while(!Thread.interrupted()) {
                long executionTime = System.currentTimeMillis() - startTime;

                if(playing) {
                    if(emotionMatched) {
                        /* Update points - Success */
                        if(myListener != null)
                            myListener.onTaskFinished(true, remainingSeconds);

                        emotionMatched = false;
                        result = true;

                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                executeNewRound();
                            }
                        }, 5000);

                        break;
                    } else {

                        if (executionTime > (TIMER_UPDATE_RESULT_PERIOD * 2)) {

                            onStopCapturingCameraFrame();

                            /* Update points - Failed */
                            if(myListener != null)
                                myListener.onTaskFinished(false, 0);

                            result = false;
                            break;
                        } else {
                            /* TODO countdown timer on UI thread */
                            int countingSeconds = (int) (executionTime / 1000);
                            //Log.i(TAG, "countingSeconds = " + Integer.toString(countingSeconds));

                            if (countingSeconds > count) {
                                count += 1;

                                remainingSeconds = ((TIMER_UPDATE_RESULT_PERIOD * 2) / 1000) - count;
                                if(myListener != null)
                                    myListener.onTaskUpdate(remainingSeconds);

                                /* temp */
                                /*if (count == 3) {
                                    Log.i(TAG, "emotionMatched true!");
                                    emotionMatched = true;
                                    onStopCapturingCameraFrame();
                                }*/
                            }
                        }
                    }

                } else {
                    result = false;
                    break;
                }
            }

            return result;
        }
    }

    private void raffleEmotion() {
        circularButton1.setProgress(50);

        Random rn = new Random();
        raffledEmotion = rn.nextInt() % (emotions.length - 1);
        raffledEmotion = raffledEmotion < 0 ? raffledEmotion * -1 : raffledEmotion ;

        if (currentEmotion == raffledEmotion) {
            raffledEmotion++;
            if(raffledEmotion == (emotions.length - 1)) {
                raffledEmotion = 0;
            }
        }

        switch(raffledEmotion) {
            case 0:
                titleTextView.setText(getResources().getString(R.string.angry));
                titleTextView.setTextColor(getResources().getColor(R.color.red));
                break;
            case 1:
                titleTextView.setText(getResources().getString(R.string.happy));
                titleTextView.setTextColor(getResources().getColor(R.color.yellow));
                break;
            case 2:
                titleTextView.setText(getResources().getString(R.string.neutral));
                titleTextView.setTextColor(getResources().getColor(R.color.black_bg));
                break;
        }

        countdownTextView.setTextColor(getResources().getColor(R.color.black_bg));
        countdownTextView.setText(Integer.toString((TIMER_UPDATE_RESULT_PERIOD * 2)/1000));
        countdownTextView.setVisibility(View.VISIBLE);

        mSlidingImage.setImageResource(emotions[raffledEmotion]);
        currentEmotion = raffledEmotion;
    }

    private void updateCountdown(final int time) {
        Log.d(TAG, "Update Countdown really called!");
        if(time > 0) {
            countdownTextView.setTextColor(getResources().getColor(R.color.black_bg));
            countdownTextView.setText(Integer.toString(time));
        }
    }

    private void updatePoints(Boolean match, final int time) {
        Log.d(TAG, "Update Points really called!");
        GameManager.getInstance().send(Action.CANCELEMOTION, "");

        Integer pts = new Integer(ptsValueTextView.getText().toString());
        if (match) {
            pts += (int)(15 * time * 0.1);
            tone.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

            countdownTextView.setTextColor(getResources().getColor(R.color.cpb_green));
            countdownTextView.setText(getResources().getString(R.string.countdownSuccess));

            circularButton1.setProgress(100);
        } else {
            if (playing) {
                if(pts != 0) {
                    //pts -= 10;
                    vibration.vibrate(500);

                    circularButton1.setProgress(-1);

                    countdownTextView.setTextColor(getResources().getColor(R.color.red));
                    countdownTextView.setText(getResources().getString(R.string.countdownFailed));

                    playing = false;
                    gameOver = true;
                } else {

                    countdownTextView.setText(getResources().getString(R.string.countdownSuccess));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            executeNewRound();
                        }
                    }, 5000);

                }
            }
        }
        ptsValueTextView.setText(pts.toString());
    }

    private void finishGame() {
        GameManager.getInstance().send(Action.CANCELEMOTION, "");

        // set a simple game counter in shared pref
        SharedPreferences sharedPreferences = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        Integer highscore = sharedPreferences.getInt("HIGHSCORE", 0);
        boolean newHighscore = false;

        Integer points = new Integer(ptsValueTextView.getText().toString());

        // Update highscore
        if(points > highscore) {
            newHighscore = true;
            highscore = points;
            editor.putInt("HIGHSCORE", points);
            editor.apply();
        }

        Intent intent = new Intent(this, GameOverActivity.class);
        intent.putExtra("points", points);
        intent.putExtra("best", highscore);
        intent.putExtra("newScore", newHighscore);

        startActivity(intent);
        finish();
    }
}
