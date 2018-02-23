package com.tccec.emotionpimobile.game;

import android.util.Log;

import com.tccec.emotionpimobile.network.socket.AsyncSocketConnection;
import com.tccec.emotionpimobile.network.socket.SocketConnectionHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * This class uses a Singleton Pattern in order to be used during the entire execution.
 * It contains the current connection to the server {@link AsyncSocketConnection}
 *
 * Created by Vinicius on 29/10/2016.
 */
public class GameManager implements SocketConnectionHandler {
    // Used for logging success or failure messages
    private String TAG = getClass().getName();

    private static String SERVER_IP = "192.168.42.1";
    //private static String SERVER_IP = "192.168.0.22";
    private static int SERVER_PORT = 5000;

    private static GameManager mInstance = null;

    private AsyncSocketConnection mSocket;
    private CommunicationResultListener mListener;

    private boolean mSocketConnected;

    private final Semaphore semaphore = new Semaphore(1, true);

    /**
     * Interface used to handle something specific in the current activity
     */
    public interface CommunicationResultListener {
        /**
         * This method is invoked when got a result from the communication with the server must be treated.
         */
        public boolean onResult(Action id, String data);
    };

    // Contains a safe queue to store images got from camera
    private BlockingQueue<CommunicationPackage> mCommPackage;

    private class CommunicationPackage {
        Action mId;
        String mData;
        String mRawData;

        public CommunicationPackage(Action id, String data, Boolean isRaw) {
            this.mId = id;

            if (isRaw) {
                this.mRawData = data;
            } else {
                this.mData = data;
            }
        }

        public Action getId() {
            return mId;
        }

        public void setId(Action id) {
            this.mId = id;
        }

        public String getData() {
            return mData;
        }

        public void setData(String data) {
            this.mData = data;
        }

        public String getRawData() {
            return mRawData;
        }

        public void setRawData(String rawData) {
            this.mRawData = rawData;
        }
    };

    private GameManager() {
        mCommPackage= new LinkedBlockingQueue<CommunicationPackage>();

        mSocket = new AsyncSocketConnection(SERVER_IP, SERVER_PORT, this);
        mSocket.execute();
    }

    public static GameManager getInstance() {
        if(mInstance == null) {
            mInstance = new GameManager();
        }
        return mInstance;
    }

    @Override
    public void connectionEstablished() {
        /* TODO */
        mSocketConnected = true;

        // Send everything
        while(!mCommPackage.isEmpty()) {
            try {
                CommunicationPackage tmp = mCommPackage.take();

                if(tmp.getData() != null) {
                    send(tmp.getId(), tmp.getData());
                } else if(tmp.getRawData() != null) {
                    sendRawBytes(tmp.getId(), tmp.getRawData());
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void connectionFinished(Exception error) {
        /* TODO */
        if(error != null) {
            error.printStackTrace();
        }

        mSocketConnected = false;
    }

    @Override
    public void processDataReceived(String data) {
        Log.d(TAG, "RECEIVED: " + data);
        Integer id = Integer.parseInt(data.substring(0,2));
        String rx = data.substring(3);

        switch(Action.fromInteger(id))
        {
            case PROCESSIMAGE:
                if(mListener != null) {
                    mListener.onResult(Action.PROCESSIMAGE, rx);
                }
                break;
            case INIT:
            case ASKFOREMOTION:
            case CANCELEMOTION:
            case CONFIRMHITORMISS:
            case LISTPLAYERS:
            default:
                break;
        }
    }

    public void send(Action id, String data) {
        if(!mSocketConnected) {
            Log.e(TAG, "Socket connection not established to send data");
            mCommPackage.add(new CommunicationPackage(id, data, false));
            return;
        }

        try {
            semaphore.acquire();

            String tx = new String();
            String action = new String();

            switch (id) {
                case INIT:
                    action = Action.INIT.getIntegerValue().toString();
                    tx += ("00" + action).substring(action.length());
                    break;
                case ASKFOREMOTION:
                    action = Action.ASKFOREMOTION.getIntegerValue().toString();
                    tx += ("00" + action).substring(action.length());

                    break;
                case CANCELEMOTION:
                    action = Action.CANCELEMOTION.getIntegerValue().toString();
                    tx += ("00" + action).substring(action.length());

                    break;
                case CONFIRMHITORMISS:
                    action = Action.CONFIRMHITORMISS.getIntegerValue().toString();
                    tx += ("00" + action).substring(action.length());

                    break;
                case LISTPLAYERS:
                    action = Action.LISTPLAYERS.getIntegerValue().toString();
                    tx += ("00" + action).substring(action.length());

                    break;
                default:
                    break;
            }

            tx += "-";
            tx += data;
            Log.d(TAG, "Sending message to server tx:[" + tx + "]");
            mSocket.write(tx);

            semaphore.release();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendRawBytes(Action id, String data) {
        if(!mSocketConnected) {
            Log.e(TAG, "Socket connection not established to send data");
            mCommPackage.add(new CommunicationPackage(id, data, true));
            return;
        }

        try {

            semaphore.acquire();

            String strCommand = new String();
            String strAction = new String();

            switch (id) {
                case PROCESSIMAGE:
                    Log.d(TAG, "Sending PROCESSIMAGE command");
                    strAction = Action.PROCESSIMAGE.getIntegerValue().toString();

                    strCommand = ("00" + strAction).substring(strAction.length());
                    strCommand += "-00000";

                    mSocket.write(strCommand);

                    break;
                case CONTINUEPROCESSIMAGE:
                    Log.d(TAG, "Sending CONTINUEPROCESSIMAGE command");
                    strAction = Action.CONTINUEPROCESSIMAGE.getIntegerValue().toString();

                    strCommand = ("00" + strAction).substring(strAction.length());
                    strCommand += "-";

                    StringBuilder str = new StringBuilder(data);
                    int idx = str.length() - 512;

                    while (idx > 0) {
                        str.insert(idx, "\n");
                        idx = idx - 512;
                    }

                    strCommand += ("00000" + Integer.toString(str.length())).substring(Integer.toString(str.length()).length());
                    mSocket.write(strCommand);

                    Log.d(TAG, "Sending raw image to server");
                    mSocket.write(str.toString());

                    break;
                case STOPPROCESSIMAGE:
                    Log.d(TAG, "Sending STOPPROCESSIMAGE command");
                    strAction = Action.STOPPROCESSIMAGE.getIntegerValue().toString();

                    strCommand = ("00" + strAction).substring(strAction.length());
                    strCommand += "-00000";

                    mSocket.write(strCommand);

                    break;
                default:
                    Log.e(TAG, "This action id must not call sendRawBytes");
                    break;
            }

            semaphore.release();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCommunicationResultListener(CommunicationResultListener listener) {
        if(listener != null) {
            mListener = listener;
        }
    }
}
