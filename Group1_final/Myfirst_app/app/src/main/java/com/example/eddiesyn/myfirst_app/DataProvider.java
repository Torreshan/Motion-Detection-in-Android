package com.example.eddiesyn.myfirst_app;


import java.io.Reader;
import java.util.ArrayList;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.eddiesyn.myfirst_app.BluetoothConnectionService.BluetoothConnectionListener;
import com.example.eddiesyn.myfirst_app.DataFileReader.DataFileReaderListener;
import com.example.eddiesyn.myfirst_app.PacketDecoder.OnPacketDecodedListener;

/**
 * This class is used to take care of various data sources and sends them in a
 * unified way, using {@link Data} objects.
 * <p>
 * The most interesting interface method is {@link OnDataAvailableListener}.
 * Choose where data comes from by using the enableXXXProvider() methods. Don't
 * forget to register your class as a listener to receive data.
 *
 * @author laurenz
 *
 */
public class DataProvider implements BluetoothConnectionListener, OnPacketDecodedListener, SensorEventListener, DataFileReaderListener {
    /**
     * A reference to the {@link BluetoothConnectionService} passed in the
     * {@link #DataProvider(Context)} constructor. Used to receive bluetooth
     * data and pass to internal decoding.
     */
    private BluetoothConnectionService mBluetoothConnectionService = null;
    /**
     * The internal {@link PacketDecoder} used for {@link Packet} decoding.
     */
    private PacketDecoder mPacketDecoder = null;

    private final SensorManager sensorMgr;
    private final Sensor accSensor;

    /**
     * Number of registered listeners that have the bluetooth sensor data
     * providing functionality enabled
     */
    private int mBluetoothProviding = 0;

    /**
     * Number of registered listeners that have the phone sensor data providing
     * functionality enabled
     */
    private int mPhoneProviding = 0;

    /**
     * Number of registered listeners that have the file data providing
     * functionality enabled
     */
    private int mDataFileProviding = 0;

    private DataFileReader mDataFileReader = null;

    /**
     * Constructor for the DataProvider object.
     * <p>
     * You could instantiate this object (use only one) in your onCreate()
     * method.
     *
     * @param context
     *            use the getApplicationContext() method
     */
    public DataProvider(Context context) {
        sensorMgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    /**
     * Tells you whether there is at least one registered listener that enabled
     * the Bluetooth data providing functionality of this class
     *
     * @return <code>true</code> if there is bluetooth data providing ongoing,
     *         <code>false</code> otherwise.
     */
    public boolean isBluetoothProviding() {
        return (mBluetoothProviding > 0);
    }

    /**
     * Tells you whether there is at least one registered listener that enabled
     * the phone sensor data providing functionality of this class
     *
     * @return <code>true</code> if there is phone sensor data providing
     *         ongoing, <code>false</code> otherwise.
     */
    public boolean isPhoneProviding() {
        return (mPhoneProviding > 0);
    }

    /**
     * Enables the object to send you data from the phone's linear acceleration
     * sensor.
     *
     * @param delay
     *            The sensor delay (which determines the sample rate)
     * @see SensorManager#SENSOR_DELAY_GAME
     */
    public void enablePhoneSensorProviding(int delay) {
        sensorMgr.registerListener(this, accSensor, delay);
        mPhoneProviding++;
    }

    /**
     * Stop sending phone sensor data. Also saves battery by not using the phone
     * sensor.
     */
    public void disablePhoneSensorProviding() {
        if (sensorMgr != null) sensorMgr.unregisterListener(this);
        if (mPhoneProviding > 0) mPhoneProviding--;
    }

    public void enableFileProviding(Reader fileReader) {
        mDataFileReader = new DataFileReader(fileReader);
        mDataFileReader.registerListener(this);
        mDataFileReader.start();
        mDataFileProviding++;
    }

    public void disableFileProviding() {
        if (mDataFileReader != null) {
            mDataFileReader.stop();
            mDataFileReader.unregisterListener(this);
        }
        if (mDataFileProviding > 0) mDataFileProviding--;
    }

    /**
     * This enables the DataProvider object to send you data from a Bluetooth
     * sensor.
     * <p>
     * The {@link Packet}s received over Bluetooth rfcomm are decoded internally
     * by a {@link PacketDecoder}, so no need to worry about that. You might
     * want to implement your own one though, as this default implementation
     * drops some packets due to CRC errors.
     * <p>
     * The methods needs a {@link BluetoothConnectionService} reference to
     * handle the Bluetooth connection stuff. So you should instantiate one of
     * those before.
     *
     * @param bluetoothService
     *            A reference to a BluetoothConnectionService object
     */
    public void enableBluetoothSensorProviding(BluetoothConnectionService bluetoothService) {
        mBluetoothConnectionService = bluetoothService;
        mBluetoothConnectionService.registerListener(this);
        mPacketDecoder = new PacketDecoder();
        mPacketDecoder.registerListener(this);
        mBluetoothProviding++;
    }

    /**
     * Stop sending {@link Data}s from the bluetooth sensor.
     * <p>
     * This does not kill the bluetooth connection, the sensor may still send
     * raw data and it will still be decoded. See
     * {@link BluetoothConnectionService#stop()} for that.
     */
    public void disableBluetoothSensorProviding() {
        if (mBluetoothConnectionService != null) mBluetoothConnectionService.unregisterListener(this);
        if (mPacketDecoder != null) mPacketDecoder.registerListener(this);
        if (mBluetoothProviding > 0) mBluetoothProviding--;
    }

    private ArrayList<OnDataAvailableListener> listOfRegisteredListeners = new ArrayList<OnDataAvailableListener>();

    /**
     * Register as a listener to receive data via the
     * {@link OnDataAvailableListener} interface. The listener you want to
     * register is most likely "<code>this</code>".
     *
     * @param l
     *            the listener to register
     */
    public void registerListener(OnDataAvailableListener l) {
        listOfRegisteredListeners.add(l);
    }

    /**
     * Stop calling the functions of the {@link OnDataAvailableListener}
     * interface for the given listener.
     *
     * @param the
     *            listener to unregister
     */
    public void unregisterListener(OnDataAvailableListener l) {
        listOfRegisteredListeners.remove(l);
    }

    private void notifyListeners(Data data) {
        for (OnDataAvailableListener l : listOfRegisteredListeners) {
            l.onDataAvailable(data);
        }
    }

    /**
     * Classes that implement this interface and register as a listener (see
     * {@link DataProvider#registerListener(OnDataAvailableListener)}) will be
     * able to receive {@link Data} objects with this method.
     */
    public static interface OnDataAvailableListener {
        void onDataAvailable(Data data);
    }

    /**
     * This gets freshly decoded Bluetooth {@link Packet}s, puts their
     * information into a {@link Data} object and notifies registered listeners
     */
    @Override
    public void onNewPacketDecoded(Packet newPacket) {
        // check if the Packet is actually acceleration data
        if (newPacket.getmSensorID() == Packet.SENSOR_ID_ACC) {
            ArrayList<Integer> mXInt = newPacket.getSensorData().getX();
            ArrayList<Integer> mYInt = newPacket.getSensorData().getY();
            ArrayList<Integer> mZInt = newPacket.getSensorData().getZ();
            ArrayList<Double> mX = new ArrayList<Double>(mXInt.size());
            ArrayList<Double> mY = new ArrayList<Double>(mYInt.size());
            ArrayList<Double> mZ = new ArrayList<Double>(mZInt.size());
            ArrayList<Double> mT = newPacket.getSensorData().getT();

            // convert from Integers to Doubles, and 12bit/6g to m*s^-2
//            double c = ((6 * 9.81) / 4096) / 341;
            double c = 1.0;
//            double  c = (( 6 * 9.81 ) / 2048 );
            for (Integer i : mXInt)
                mX.add(Double.valueOf(i.doubleValue()) * c);
            for (Integer i : mYInt)
                mY.add(Double.valueOf(i.doubleValue()) * c);
            for (Integer i : mZInt)
                mZ.add(Double.valueOf(i.doubleValue()) * c);

            Data data = new Data(mX, mY, mZ, mT, Data.DATA_SOURCE_BLUETOOTH);
            notifyListeners(data);

            // Log.d("DataProvider", newPacket.toString());
        }
    }

    @Override
    public void onNewPacketError(Packet invalidPacket) {
    }

    @Override
    public void onBluetoothConnectionStateChanged(int state) {
    }

    @Override
    public void onBluetoothConnectionConnected(String name, String address) {
    }

    /**
     * This method gets called from the {@link BluetoothConnectionService} when
     * new data is received. This method takes the data and passes it to the
     * internal {@link PacketDecoder} for decoding. The decoding will happen in
     * a parallel thread.
     */
    @Override
    public void onBluetoothConnectionReceive(byte[] buffer, int numberOfBytesInBuffer) {
        ArrayList<Byte> bytes = new ArrayList<Byte>(numberOfBytesInBuffer);
        for (int i = 0; i < numberOfBytesInBuffer; i++)
            bytes.add(buffer[i]);
        mPacketDecoder.addBytesToQueue(bytes);
        mPacketDecoder.triggerDecoding();
    }

    @Override
    public void onBluetoothConnectionWrite(byte[] buffer) {
    }

    @Override
    public void onBluetoothConnectionFailure(int whatFailed) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        ArrayList<Double> mX = new ArrayList<Double>(1);
        ArrayList<Double> mY = new ArrayList<Double>(1);
        ArrayList<Double> mZ = new ArrayList<Double>(1);
        ArrayList<Double> mT = new ArrayList<Double>(1);

        mX.add(Double.valueOf(event.values[0]));
        mY.add(Double.valueOf(event.values[1]));
        mZ.add(Double.valueOf(event.values[2]));
        mT.add(Double.valueOf(event.timestamp) / 1000);

        Data data = new Data(mX, mY, mZ, mT, Data.DATA_SOURCE_PHONE);
        notifyListeners(data);
    }

    @Override
    public void onDataFileNewDataPoint(Double timeSec, Double x, Double y, Double z) {
        ArrayList<Double> mX = new ArrayList<Double>(1);
        ArrayList<Double> mY = new ArrayList<Double>(1);
        ArrayList<Double> mZ = new ArrayList<Double>(1);
        ArrayList<Double> mT = new ArrayList<Double>(1);

        mX.add(Double.valueOf(x));
        mY.add(Double.valueOf(y));
        mZ.add(Double.valueOf(z));
        mT.add(Double.valueOf(timeSec));

        Data data = new Data(mX, mY, mZ, mT, Data.DATA_SOURCE_FILE);
        notifyListeners(data);
    }

    @Override
    public void onDataFileError(Exception exception) {
        Log.e("DataProvider", exception.toString());

    }

}

