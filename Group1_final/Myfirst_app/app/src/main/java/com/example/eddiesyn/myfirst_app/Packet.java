package com.example.eddiesyn.myfirst_app;


import java.util.ArrayList;

import android.util.Log;

/**
 * This class is designed to hold the information of a packet of data as sent from a InPriMo Activity Bluetooth sensor.
 *
 * @author laurenz
 *
 */
public class Packet
{
    /** Debug Log Tag */
    private static final String PACKET_TAG = "Packet";

    /** Frame flag byte */
    public static final Byte PACKET_FLAGBYTE = new Byte((byte) 0xA8);
    /** Control byte used for stuffing */
    public static final Byte PACKET_CONTROLBYTE = new Byte((byte) 0xA9);
    /** Stuff byte used for stuffing */
    public static final byte PACKET_STUFFBYTE = (byte) 0xDF;

    /** This is the ID of the accelerometer in the InPriMo Activity sensor */
    public static final byte SENSOR_ID_ACC = (byte) 0x10;

    /** The size of the Packet header structure */
    // packerTIMESTAMP_LENGTH = 6; packerHEAD_SIZE = (3 + packerTIMESTAMP_LENGTH + 2);
    // packerTAIL_SIZE = 1 ; packerOVERHEAD = (packerHEAD_SIZE + packerTAIL_SIZE)
    final static byte PACKET_OVERHEAD = 12;

    /** Checksum error */
    final static byte PACKET_ERROR_CHECKSUM = 1;
    /** More bytes received than expected */
    final static byte PACKET_ERROR_OVERRUN = 2;
    /** Less bytes received than expected */
    final static byte PACKET_ERROR_UNDERRUN = 4;
    /** No {@link #PACKET_FLAGBYTE} was found at the end of the frame */
    final static byte PACKET_ERROR_NOFRAMEND = 8;

    byte mError = 0;
    private boolean isValid = false;

    Integer mFrameCounter = null;
    Integer mSensorID = null;
    Integer mSensorPacketCounter = null;
    Integer mFrameLength = null;
    Integer mDataLength = null;
    private Long mTimestamp = null; // ms since last reset

    /** The Packet payload */
    private ArrayList<Byte> mData = null;
    /** this byte is used to calculate a frame's checksum */
    byte mChecksumCalc = 0;
    /** This should always be zero as it is the expected frame checksum */
    public final static byte mChecksumFrame = 0;

    private BluetoothSensorData sensorData = null;

    /**
     * Constructor for a {@link Packet} object.
     *
     * @param newFrame
     *            the raw (still stuffed) data bytes that will be used to build the Packet
     */
    public Packet(ArrayList<Byte> newFrame)
    {
        if (newFrame == null || newFrame.size() == 0)
        {
            Log.e(PACKET_TAG, "tried to create empty packet");
            return;
        }
        buildPacket(newFrame);
    }

    private void buildPacket(ArrayList<Byte> newFrame)
    {
        // first, get the a actual data of the frame
        newFrame = unstuff(newFrame);
        if (newFrame == null)
            return;

        // now, set member variables
        mFrameCounter = unsign(newFrame.get(1));
        mSensorID = unsign(newFrame.get(2));
        mSensorPacketCounter = unsign(newFrame.get(3));
        long days = unsign(newFrame.get(4)) + (unsign(newFrame.get(5)) * 256);
        mTimestamp = days * 24 * 60 * 60 * 1000;
        mTimestamp += unsign(newFrame.get(6)) + (unsign(newFrame.get(7)) * 256) + (unsign(newFrame.get(8)) * 256 * 256)
                + (unsign(newFrame.get(9)) * 256 * 256 * 256);
        mFrameLength = unsign(newFrame.get(10)) + (unsign(newFrame.get(11)) * 256);

        mDataLength = mFrameLength - 2 - PACKET_OVERHEAD; // -2: frameflags

        mData = new ArrayList<Byte>(newFrame.subList(PACKET_OVERHEAD, newFrame.size() - 3));

        // mChecksumFrame = 0; // is always zero //newFrame.get(newFrame.size() - 2);

        if (!this.hasValidChecksum())
        {
            // Log.d(MainActivity.TAG_PROTOCOL_DECODE,
            // String.format("Checksum mismatch (calc: 0x%02X, data: 0x%02X)", mChecksumCalc, mChecksumFrame));
            isValid = false;
        } else
        {
            isValid = true;
        }

        int expectedFrameSize = mFrameLength + 2;
        // check for more bytes than expected
        if (newFrame.size() > expectedFrameSize)
        {
            Log.d(PACKET_TAG,
                    "overrun (expected: " + (PACKET_OVERHEAD + 1 + 2 + 1 + mDataLength) + "B ,received: " + newFrame.size()
                            + "B)");
            mError |= PACKET_ERROR_OVERRUN;
            isValid = false;
        }
        // check for less bytes than expected
        else if (newFrame.size() < expectedFrameSize)
        {
            Log.d(PACKET_TAG,
                    "underrun (expected: " + (PACKET_OVERHEAD + 1 + 2 + 1 + mDataLength) + "B ,received: " + newFrame.size()
                            + "B)");
            // Log.v(PACKET_TAG, arraylistOutput(newFrame));
            mError |= PACKET_ERROR_UNDERRUN;
            isValid = false;
        }
        // check if the very last byte is a frame flag (it has to be)
        if (!newFrame.get(newFrame.size() - 1).equals(PACKET_FLAGBYTE))
        {
            Log.d(PACKET_TAG, String.format("last byte is not a frame flag (expected: 0x%02X ,received: 0x%02XB)",
                    PACKET_FLAGBYTE, newFrame.get(newFrame.size() - 1)));
            // Log.v(PACKET_TAG, arraylistOutput(newFrame));
            mError |= PACKET_ERROR_NOFRAMEND;
            isValid = false;
        }

        if (isValid && mSensorID == SENSOR_ID_ACC)
        {
            sensorData = new BluetoothSensorData(this);
        }
    }

    public BluetoothSensorData getSensorData()
    {
        return sensorData;
    }

    @Override
    public String toString()
    {
        return "FrameCounter: " + mFrameCounter + ", SensorID: " + String.format("0x%02X", mSensorID)
                + ", SensorPacketCounter: " + mSensorPacketCounter + ", Time: " + mTimestamp + ", FrameLength: " + mFrameLength
                + ", SampleRate: " + sensorData.getSamplerate() + "Hz";
    }

    /**
     * This method unstuffs bytes to get the actual data bytes that can be used to extract information from the Packet.
     *
     * @param in
     *            The raw bytes
     * @return unstuffed bytes
     */
    // gets the actual frame data bytes (used in constructor)
    private ArrayList<Byte> unstuff(ArrayList<Byte> in)
    {
        if (!(in.get(0).equals(PACKET_FLAGBYTE) && in.get(in.size() - 1).equals(PACKET_FLAGBYTE)))
        {
            Log.e(PACKET_TAG, "unstuffing: frame is not framed by flag bytes!");

            return null;
        }

        for (int i = 0; i < in.size(); i++)
        {
            // unstuff where needed
            if (in.get(i).equals(PACKET_CONTROLBYTE))
            {
                byte b = in.get(i + 1).byteValue();
                b |= ~PACKET_STUFFBYTE;
                in.set(i, new Byte((byte) b)); // control byte was at i
                in.remove(i + 1); // stuffed data was at i+1
            }

            // calculate checksum
            mChecksumCalc ^= in.get(i).intValue();
        }

        return in;
    }

    /**
     * Checks if the Checksum of this Packet is valid (that is, is {@value #mChecksumFrame})
     *
     * @return <code>true</code> if valid, <code>false</code> otherwise
     */
    public boolean hasValidChecksum()
    {
        if (mChecksumCalc != mChecksumFrame)
        {
            mError |= PACKET_ERROR_CHECKSUM;
            return false;
        }
        return true;
    }

    // There are no primitive unsigned bytes in Java. The usual thing is to cast it to bigger type
    private Integer unsign(Byte in)
    {
        return Integer.valueOf(in.byteValue() & 0xFF);
    }

    /**
     * Check this to see if there are any errors in a Packet
     *
     * @return <code>true</code> if the Packet is OK, <code>false</code> if there are any errors.
     */
    public boolean isValid()
    {
        return isValid;
    }

    /**
     * returns the (raw) data bytes of the payload of a Packet
     *
     * @return the raw data
     */
    public ArrayList<Byte> getData()
    {
        return mData;
    }

    /**
     * Returns the timestamp (ms since last sensor reset) that was transmitted in the Packet header.
     *
     * @return the timestamp
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }

    public Integer getmSensorID()
    {
        return mSensorID;
    }

    /**
     * This class is used to give nicer access to the payload of a packet.
     *
     */
    public class BluetoothSensorData
    {
        private int mSamplerate;
        private int mNrOfSamples;
        private ArrayList<Integer> mX = new ArrayList<Integer>();
        private ArrayList<Integer> mY = new ArrayList<Integer>();
        private ArrayList<Integer> mZ = new ArrayList<Integer>();
        private ArrayList<Double> mT = new ArrayList<Double>();
        private long mBaseTimestamp;

        /**
         * Constructor. Pass the Packet whose payload is to be saved in this object
         *
         * @param packet
         *            A Packet
         */
        public BluetoothSensorData(Packet packet)
        {
            reset();

            ArrayList<Byte> data = packet.getData();

            mBaseTimestamp = packet.getTimestamp(); // alternative option: System.currentTimeMillis();

            mSamplerate = unsign(data.get(0));
            if (mSamplerate == 0)
            {
                mSamplerate = 30;
                Log.d(PACKET_TAG, "Sample rate was 0, set to 30");
            }
            for (int i = 1; i < data.size() - 6;)
            {
                mX.add(getDoubleValFromBytes(data.get(i++), data.get(i++)));
                mY.add(getDoubleValFromBytes(data.get(i++), data.get(i++)));
                mZ.add(getDoubleValFromBytes(data.get(i++), data.get(i++)));
            }

            mNrOfSamples = mX.size();
            double samplePeriod = (double) 1000 / (double) mSamplerate; // ms between data points
            for (int i = 0; i < mNrOfSamples; i++)
            {
                mT.add(mBaseTimestamp + i * samplePeriod);
            }
//			Log.d(PACKET_TAG, "BaseTime = " + mBaseTimestamp + "ms, lastTime = " + mT.get(mT.size() - 1) + "ms , step = "
//					+ samplePeriod + "ms");

            // int x = (unsign(data.get(2)) << 8) + unsign(data.get(1));
            // String b = "0b";
            // for(int i=15; i>=0; i--)
            // {
            // b+=((x&(1<<i))==0)?"0":"1";
            // }
            // Log.d(TAG_BLUETOOTHSENSORDATA, b);
        }

        private void reset()
        {
            mX.clear();
            mY.clear();
            mZ.clear();
            mT.clear();

            mSamplerate = 0;
            mNrOfSamples = 0;
        }

        /** Returns the samplerate of the data as stated in the originating {@link Packet}s header. */
        public int getSamplerate()
        {
            return mSamplerate;
        }

        /** Returns the number of sample points available in this Packet. */
        public int getNrOfSamples()
        {
            return mNrOfSamples;
        }

        /** Returns the data of the X dimension as ArrayList&lt;Integer&gt;. */
        public ArrayList<Integer> getX()
        {
            return mX;
        }

        /** Returns the data of the Y dimension as ArrayList&lt;Integer&gt;. */

        public ArrayList<Integer> getY()
        {
            return mY;
        }

        /** Returns the data of the Z dimension as ArrayList&lt;Integer&gt;. */
        public ArrayList<Integer> getZ()
        {
            return mZ;
        }

        /**
         * Returns the respective timestamps of the data samples as ArrayList&lt;Double&gt;. These are calculated from the base
         * timestamp and the sample rate.
         */
        public ArrayList<Double> getT()
        {
            return mT;
        }

        // There are no primitive unsigned bytes in Java. The usual thing is to cast it to bigger type
        private int unsign(Byte in)
        {
            return (int) (in.byteValue() & 0xFF);
        }

        private int getDoubleValFromBytes(byte lo, byte hi)
        {
            int val = (((hi & 0xFF) << 8) | (lo & 0xFF)) << 16 >> 16;
            return val;
        }

    }
}
