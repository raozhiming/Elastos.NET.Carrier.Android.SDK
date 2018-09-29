package org.elastos.carrier.common;

import org.elastos.carrier.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketUtils {
    private static final String TAG = "Test.SocketUtils";
    private Socket mSocket = null;
    private OutputStream mOutputStream = null;
    private BufferedReader mBufferedReader = null;

    public SocketUtils(){}

    public boolean connectRobot(String host, String port)
    {
        if (host == null || port == null) {
            return false;
        }

        int ntries = 0;

        Log.d(TAG, "Connecting to test robot(" + host + ":" + port + ").");

        try {
            while(ntries < 3) {
                try {
                    mSocket = new Socket(host, Integer.parseInt(port));
                }
                catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                if (mSocket != null) {
                    Log.d(TAG, "isConnected: ["+mSocket.isConnected()+"]");
                }

                if (mSocket != null && mSocket.isConnected()) {
                    break;
                }

                Log.d(TAG, "Connecting to test rebot failed, try again");
                ntries++;
                Thread.sleep(1 * 1000);
            }

            if(mSocket == null) {
                Log.d(TAG, "Connecting to test robot failed.");
                return false;
            }

            Log.d(TAG, "Connected to robot.");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public void disconnectRobot()
    {
        if (mSocket != null) {
            try {
                mSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Disconnected robot.");
        }

        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

        if (mBufferedReader != null) {
            try {
                mBufferedReader.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public int sendData2Robot(String cmd2Robot)
    {
        OutputStream outputStream = null;
        try {
            if (mSocket.isConnected()) {
                if (mOutputStream == null) {
                    mOutputStream = mSocket.getOutputStream();
                }

                outputStream = mOutputStream;
                outputStream.write((cmd2Robot + "\n").getBytes("utf-8"));
                outputStream.flush();
            }
            else {
                Log.d(TAG, "the robot' connection is broken.");
                return -1;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return 1;
    }

    public int recvDataFromRobot(TestRecvDataArgs args)
    {
        BufferedReader br = null;
        int length = 0;
        try {
            if (mBufferedReader == null) {
                InputStream is = mSocket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                mBufferedReader = new BufferedReader(isr);
            }

            br = mBufferedReader;
            String revData = br.readLine();
            Log.d(TAG, "revData==========="+revData);
            args.mArgs = revData.split("\\s+");
            length = args.mArgs.length;
        }
        catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return length;
    }

    public static class TestRecvDataArgs {
        public TestRecvDataArgs() {}
        public String[] mArgs = null;
    };
}
