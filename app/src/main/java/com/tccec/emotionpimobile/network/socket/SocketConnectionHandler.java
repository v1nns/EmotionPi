package com.tccec.emotionpimobile.network.socket;

/**
 * This interface defines methods that handle connection notifications and the data received from the socket connection.
 * 
 * Created by Vinicius Longaray on 29/10/16.
 */
public interface SocketConnectionHandler {

    /**
     * TODO
     */
    public void connectionEstablished();

    /**
     * TODO
     */
    public void connectionFinished(Exception error);

    /**
     * TODO
     */
    public void processDataReceived(String data);

}
