package com.tccec.emotionpimobile.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;;
import android.support.annotation.CallSuper;
import android.util.Log;
import android.view.WindowManager;

import com.tccec.emotionpimobile.R;
import com.tccec.emotionpimobile.util.StringBase64;
import com.tccec.emotionpimobile.game.Action;
import com.tccec.emotionpimobile.game.GameManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Vinicius Longaray on 26/10/16.
 */
public class CameraControllerActivity extends Activity {
    // Used for logging success or failure messages
    private final String TAG = getClass().getName();

    /**
     * OpenCV loaded successfully
     */
    private boolean mOpenCvLoaded;

    /**
     * Fragment {@link Camera2BasicFragment} containing camera image
      */
    private Camera2BasicFragment mFragment;

    /**
     * Size of image that will be sent through socket
     */
    private final Integer WIDTH  = 320;
    private final Integer HEIGHT = 240;

    private final AtomicBoolean mRunning = new AtomicBoolean(true);
    private final AtomicBoolean mCaptureCameraFrame = new AtomicBoolean(false);

    private final Semaphore mSemaphore = new Semaphore(1, true);

    // Callback to inform if OpenCV is loaded
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvLoaded = true;
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private Camera2BasicFragment.CameraViewListener mListener = new Camera2BasicFragment.CameraViewListener() {
        @Override
        public boolean onCameraFrame(final Image image) {
            Log.i(TAG, "called onCameraFrame from CameraViewListener");
            onProcessCameraFrame(image);
            return true;
        }
    };

    /**
     * Constructor {@link CameraControllerActivity}
     */
    public CameraControllerActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_game);

        if (null == savedInstanceState) {

            mFragment = Camera2BasicFragment.newInstance();
            mFragment.setCameraViewListener(mListener);

            getFragmentManager().beginTransaction()
                    .replace(R.id.container, mFragment)
                    .commit();
        }

        mRunning.set(true);
        StoreImageFrameTask.start();

        /*int maxThreads = 5;
        ExecutorService executorService =
                new ThreadPoolExecutor(
                        maxThreads, // core thread pool size
                        maxThreads, // maximum thread pool size
                        1, // time to wait before resizing pool
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<Runnable>(maxThreads, true),
                        new ThreadPoolExecutor.CallerRunsPolicy());

        executorService.execute(new SendImageFrameTask());
        executorService.execute(new SendImageFrameTask());*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        } catch (Exception e) {
            // suppress exception
        }
        mRunning.set(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRunning.set(false);
    }

    @Override
    protected void onDestroy() {
        mCaptureCameraFrame.set(false);
        mRunning.set(false);
        super.onDestroy();
    }

    @CallSuper
    protected void onStartCapturingCameraFrame() {
        Log.d(TAG, "onStartCapturingCameraFrame");
        mCaptureCameraFrame.set(true);
    }

    @CallSuper
    protected void onStopCapturingCameraFrame() {
        Log.d(TAG, "onStopCapturingCameraFrame");
        mCaptureCameraFrame.set(false);
    }

    @CallSuper
    protected void onProcessCameraFrame(final Image image) {
        Log.d(TAG, "Calling onProcessCameraFrame");
        long startTime = System.currentTimeMillis();

        // Image to byte[]
        ByteBuffer bb = image.getPlanes()[0].getBuffer();
        byte[] buf = new byte[bb.remaining()];
        bb.get(buf);

        image.close();

        // byte[] to Bitmap
        Bitmap bmp = BitmapFactory.decodeByteArray(buf, 0, buf.length);
        Bitmap myBitmap32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Mat orig = new Mat(bmp.getHeight(),bmp.getWidth(),CvType.CV_8UC4);

        // Bitmap to openCV::Mat
        Utils.bitmapToMat(myBitmap32, orig);
        //Imgproc.cvtColor(orig, orig, Imgproc.COLOR_BGR2RGB);
        Imgproc.cvtColor(orig, orig, Imgproc.COLOR_BGR2GRAY);

        // rotate openCV::Mat
        Core.transpose(orig, orig);
        Core.flip(orig, orig, 0);

        // openCV::Mat to gray color
        //Mat tmp = new Mat();
        //Imgproc.cvtColor(orig, tmp, Imgproc.COLOR_RGB2GRAY);

        // resize gray color openCV::Mat
        Mat grayImageResized = new Mat();
        Size sz = new Size(HEIGHT, WIDTH);
        Imgproc.resize(orig, grayImageResized, sz);

        // compress to JPEG
        MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 90);
        MatOfByte buff = new MatOfByte();
        Imgcodecs.imencode(".jpg", grayImageResized, buff, params);

        byte[] bytes = buff.toArray();
        String test = StringBase64.encode(bytes);

        try {
            mSemaphore.acquire();

            GameManager.getInstance().sendRawBytes(Action.PROCESSIMAGE, "");
            GameManager.getInstance().sendRawBytes(Action.CONTINUEPROCESSIMAGE, test);
            GameManager.getInstance().sendRawBytes(Action.STOPPROCESSIMAGE, "");

            mSemaphore.release();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Log.d("MATSAMPLE MatOfByte", test);
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Added to FrameQueue - Time: " + Long.toString(executionTime) + " ms");
    }

    /**
     * Asks Camera2BasicFragment {@link Camera2BasicFragment} to store a new frame
     */
    Thread StoreImageFrameTask = new Thread(new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Called StoreImageFrameTask!");
            while(!Thread.interrupted()) {
                if(mRunning.get()) {
                    if (mCaptureCameraFrame.get()) {
                        if (mFragment.getState() == 0) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Log.d(TAG, "CALLING TAKEPICTURE!");
                                    mFragment.takePicture();
                                }
                            });
                        }
                    }
                }
            }
        }
    });
}