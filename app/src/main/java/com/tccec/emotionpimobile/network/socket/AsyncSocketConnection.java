package com.tccec.emotionpimobile.network.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

/**
 * The AsyncSocketConnection class is an AsyncTask that can be used to open a mSocket connection with a server and to write/read data asynchronously.
 *
 * Created by Vinicius Longaray on 29/10/16.
 */
public class AsyncSocketConnection extends AsyncTask<Void, String, Exception> {
    // Used for logging success or failure messages
    private String TAG = getClass().getName();

    private String mAddressIP;
    private int mPort;
    private SocketConnectionHandler mSocketConnectionHandler;

    private BufferedReader in;
    private BufferedWriter out;
    private DataOutputStream dout;
    private Socket mSocket;
    private boolean interrupted = false;

    public AsyncSocketConnection(String mAddressIP, int mPort, SocketConnectionHandler mSocketConnectionHandler) {
        this.mAddressIP = mAddressIP;
        this.mPort = mPort;
        this.mSocketConnectionHandler = mSocketConnectionHandler;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Exception result) {
        super.onPostExecute(result);
        Log.d(TAG, "Finished communication with the mSocket. Result = " + result);
        //TODO If needed move the didDisconnect(error); method call here to implement it on UI thread.
    }

    @Override
    protected Exception doInBackground(Void... params) {
        Exception error = null;

        try {
            Log.d(TAG, "Opening mSocket connection.");
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress(mAddressIP, mPort));

            in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            dout = new DataOutputStream(mSocket.getOutputStream());

            mSocketConnectionHandler.connectionEstablished();

            while(!interrupted) {
                String line = in.readLine();
                Log.d(TAG, "Received from socket:" + line);
                mSocketConnectionHandler.processDataReceived(line);
            }
        } catch (UnknownHostException ex) {
            Log.e(TAG, "doInBackground(): " + ex.toString());
            error = interrupted ? null : ex;
        } catch (IOException ex) {
            Log.d(TAG, "doInBackground(): " + ex.toString());
            error = interrupted ? null : ex;
        } catch (Exception ex) {
            Log.e(TAG, "doInBackground(): " + ex.toString());
            error = interrupted ? null : ex;
        } finally {
            try {
                mSocket.close();
                dout.close();
                out.close();
                in.close();
            } catch (Exception ex) {}
        }

        mSocketConnectionHandler.connectionFinished(error);
        return error;
    }

    public void write(final String data) {
        try {
            //Log.d(TAG, "writ(): data = " + data);
            out.write(data);
            out.newLine();
            out.flush();
        } catch (IOException ex) {
            Log.e(TAG, "write(): " + ex.toString());
        } catch (NullPointerException ex) {
            Log.e(TAG, "write(): " + ex.toString());
        }
    }

    public void disconnect() {
        try {
            Log.d(TAG, "Closing the mSocket connection.");

            interrupted = true;
            if(mSocket != null) {
                mSocket.close();
            }
            if(dout != null & out != null & in != null) {
                dout.close();
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            Log.e(TAG, "disconnect(): " + ex.toString());
        }
    }
}