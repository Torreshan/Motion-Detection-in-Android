package com.example.eddiesyn.myfirst_app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This class is designed to handle the connection with the Bluetooth sensor. It should be instantiated only once.
 * <p>
 * Use it to initiate or terminate connections, receive and send raw data and for recognizing Bluetooth connection related
 * errors. Get your data by using the callbacks provided by the {@link BluetoothConnectionListener} interface and registering as
 * a listener.
 * <p>
 * <p>
 * The code is derived from the BluetoothChatService of the Android Bluetooth Chat sample applicaion.
 *
 * @author laurenz
 *
 */
public class BluetoothConnectionService
{
    private static final String TAG = "BluetoothConnectionService";

    /** We're doing nothing. */
    public static final int STATE_NONE = 0;
    /** Now initiating a connection */
    public static final int STATE_CONNECTING = 1;
    /** Now connected to the remote device */
    public static final int STATE_CONNECTED = 2;
    /** Connecting to the remote device has failed */
    public static final int FAILURE_CONNECTING_FAILED = 0;
    /** The connection was lost */
    public static final int FAILURE_CONNECTION_LOST = 1;
    /** Sending data to the remote device failed */
    public static final int FAILURE_WRITE_FAILED = 2;

    // Member fields
    private final BluetoothAdapter mAdapter;
    private int mState;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    /** this is the "magic" UUID used to connect to the device's virtual COM port via RFCOMM. */
    private static final UUID UUID_SP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /** Constructor for the {@link BluetoothConnectionService} class. Only instantiate one object. */
    public BluetoothConnectionService()
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    private ArrayList<BluetoothConnectionListener> listOfRegisteredListeners = new ArrayList<BluetoothConnectionListener>();

    /**
     * Register as a listener to receive callbacks via the {@link BluetoothConnectionListener} interface. The listener you want
     * to register is most likely "<code>this</code>".
     *
     * @param l
     *            the listener to register
     */
    public void registerListener(BluetoothConnectionListener l)
    {
        listOfRegisteredListeners.add(l);
    }

    /**
     * Stop calling the functions of the {@link BluetoothConnectionListener} interface for the given listener.
     *
     */
    public void unregisterListener(BluetoothConnectionListener l)
    {
        listOfRegisteredListeners.remove(l);
    }

    /**
     * Classes that implement this interface and register as a listener will have the callback methods called. See the method
     * comment for a description of their functionality.
     *
     * @see BluetoothConnectionService#registerListener(BluetoothConnectionListener)
     * @see #onBluetoothConnectionConnected(String, String)
     * @see #onBluetoothConnectionFailure(int)
     * @see #onBluetoothConnectionReceive(byte[], int)
     * @see #onBluetoothConnectionStateChanged(int)
     * @see #onBluetoothConnectionWrite(byte[])
     */
    public static interface BluetoothConnectionListener
    {
        /**
         * This method gets called after the state of the bluetooth connection changed.
         *
         * @param state
         *            The new connection state
         * @see BluetoothConnectionService#STATE_NONE
         * @see BluetoothConnectionService#STATE_CONNECTING
         * @see BluetoothConnectionService#STATE_CONNECTED
         */
        void onBluetoothConnectionStateChanged(int state);

        /**
         * This method gets called when a bluetooth connection is successfully established.
         *
         * @param name
         *            The friendly name of the remote device
         * @param address
         *            Its address
         */
        void onBluetoothConnectionConnected(String name, String address);

        /**
         * When there is (raw) data received over RFCOMM, it will be available through this function.
         *
         * @param buffer
         *            The buffer that holds the received (raw) data
         * @param numberOfBytesInBuffer
         *            The number of valid bytes in that buffer
         */
        void onBluetoothConnectionReceive(byte[] buffer, int numberOfBytesInBuffer);

        /**
         * When you send something to the device (for example, by using the {@link BluetoothConnectionService#write(byte[])}
         * method), this method gets called with the data you sent.
         *
         * @param buffer
         *            The data sent
         */
        void onBluetoothConnectionWrite(byte[] buffer);

        /**
         * This method gets called when a connection error occurs.
         *
         * @param whatFailed
         *            Tells you what error occured
         * @see BluetoothConnectionService#FAILURE_CONNECTING_FAILED
         * @see BluetoothConnectionService#FAILURE_CONNECTION_LOST
         * @see BluetoothConnectionService#FAILURE_WRITE_FAILED
         */
        void onBluetoothConnectionFailure(int whatFailed);
    }

    private synchronized void setState(int state)
    {
        if (mState != state)
        {
            mState = state;
            for (BluetoothConnectionListener l : listOfRegisteredListeners)
            {
                l.onBluetoothConnectionStateChanged(state);
            }
        }
    }

    /**
     * Return the current connection state.
     *
     * @see BluetoothConnectionService#STATE_NONE
     * @see BluetoothConnectionService#STATE_CONNECTING
     * @see BluetoothConnectionService#STATE_CONNECTED
     */
    public synchronized int getState()
    {
        return mState;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * <p>
     * If you don't have a {@link BluetoothDevice}, get one by calling {@link BluetoothAdapter#getRemoteDevice(String address)}
     *
     * @param device
     *            The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device)
    {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING)
        {
            if (mConnectThread != null)
            {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Stop all threads (and thus terminate a possibly established connection)
     */
    public synchronized void stop()
    {
        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a device. It runs straight through; the connection
     * either succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device)
        {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try
            {
                tmp = device.createInsecureRfcommSocketToServiceRecord(UUID_SP);
            } catch (IOException ex) // IOExcecption is thrown if connect fails.
            {
                Log.e(this.toString(), "InvocationIOException " + ex.getMessage());
            }
            mmSocket = tmp;
        }

        public void run()
        {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try
            {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            } catch (IOException ex)
            {
                Log.e(this.toString(), "InvocationIOException during mmSocket.connect() - " + ex.getMessage());
                // Close the socket
                try
                {
                    mmSocket.close();
                } catch (IOException e2)
                {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnectionService.this)
            {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            } catch (IOException e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection.
     * <p>
     * If you want to initiate a new connection, use {@link BluetoothConnectionService#connect(BluetoothDevice)}.
     *
     * @param socket
     *            The BluetoothSocket on which the connection was made
     * @param device
     *            The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
    {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        for (BluetoothConnectionListener l : listOfRegisteredListeners)
        {
            l.onBluetoothConnectionConnected(device.getName(), device.getAddress());
        }

        setState(STATE_CONNECTED);
    }

    /**
     * This thread runs during a connection with a remote device. It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e)
            {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true)
            {
                try
                {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    for (BluetoothConnectionListener l : listOfRegisteredListeners)
                    {
                        l.onBluetoothConnectionReceive(buffer, bytes);
                    }
                } catch (IOException e)
                {
                    Log.i(TAG, "disconnected"/* , e */);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer
         *            The bytes to write
         * @return <code>true</code> if write successful, <code>false</code> otherwise.
         */
        public boolean write(byte[] buffer)
        {
            try
            {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                for (BluetoothConnectionListener l : listOfRegisteredListeners)
                {
                    l.onBluetoothConnectionWrite(buffer);
                }
                return true;
            } catch (IOException e)
            {
                Log.e(TAG, "Exception during write", e);
                return false;
            }
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            } catch (IOException e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner. Use this to send something to you remote device.
     *
     * @param out
     *            The bytes to write
     * @return <code>true</code> if write successful, <code>false</code> otherwise.
     *
     * @see ConnectedThread#write(byte[])
     */
    public boolean write(byte[] out)
    {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this)
        {
            if (mState != STATE_CONNECTED)
            {
                writeFailed();
                return false;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        return r.write(out);
    }

    /**
     * Indicate that the write attempt failed and notify the UI Activity.
     */
    private void writeFailed()
    {
        for (BluetoothConnectionListener l : listOfRegisteredListeners)
        {
            l.onBluetoothConnectionFailure(FAILURE_WRITE_FAILED);
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed()
    {
        setState(STATE_NONE);
        for (BluetoothConnectionListener l : listOfRegisteredListeners)
        {
            l.onBluetoothConnectionFailure(FAILURE_CONNECTING_FAILED);
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost()
    {
        setState(STATE_NONE);
        for (BluetoothConnectionListener l : listOfRegisteredListeners)
        {
            l.onBluetoothConnectionFailure(FAILURE_CONNECTION_LOST);
        }
    }

}
