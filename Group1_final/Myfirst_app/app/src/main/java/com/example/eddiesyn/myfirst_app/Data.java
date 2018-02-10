package com.example.eddiesyn.myfirst_app;

import java.util.ArrayList;

/**
 * Data is a class designed to transfer a set of data points from various sources in a standardized way. The class is used by
 * the {@link DataProvider} class to transfer its data.
 * <p>
 * The getters return {@link ArrayList}s of actual data points.
 * <p>
 * Each data point has a Double for each dimension (X,Y,Z) and their respective time (T).
 *
 * @author laurenz
 *
 */
public class Data
{
    /** There is no source (could be used for fake data) */
    public static final int DATA_SOURCE_NONE = 0;
    /** This Data originates from the phone sensor */
    public static final int DATA_SOURCE_PHONE = 1;
    /** This Data originates from the Bluetooth sensor */
    public static final int DATA_SOURCE_BLUETOOTH = 2;
    /** This Data originates from a file with data points */
    public static final int DATA_SOURCE_FILE = 3;

    private int mNrOfSamples;
    private ArrayList<Double> mX = new ArrayList<Double>();
    private ArrayList<Double> mY = new ArrayList<Double>();
    private ArrayList<Double> mZ = new ArrayList<Double>();
    private ArrayList<Double> mT = new ArrayList<Double>();
    private int source;

    /**
     * Constructor for the {@link Data} object.
     * This is where the object gets its values. The timestamp should be in ms since epoch (sensor reset or unix epoch). Acceleration data should be in m/s^2.
     *
     * @param mX		ArrayList of Doubles representing the data values in the X dimension
     * @param mY		ArrayList of Doubles representing the data values in the Y dimension
     * @param mZ		ArrayList of Doubles representing the data values in the Z dimension
     * @param mT		ArrayList of Doubles representing the timestamps of the data values
     * @param source	The data source. Should be one of {@link #DATA_SOURCE_PHONE}, {@link #DATA_SOURCE_BLUETOOTH}, {@link #DATA_SOURCE_FILE}
     */
    public Data(ArrayList<Double> mX, ArrayList<Double> mY, ArrayList<Double> mZ, ArrayList<Double> mT, int source)
    {
        this.mX = mX;
        this.mY = mY;
        this.mZ = mZ;
        this.mT = mT;
        this.source = source;
        mNrOfSamples = Math.min(mX.size(), mY.size());
        mNrOfSamples = Math.min(mNrOfSamples, mZ.size());
        mNrOfSamples = Math.min(mNrOfSamples, mT.size());
    }

    /**
     * This gives the number of points that are available in all three dimensions
     * @return The number of samples in this Data object's ArrayList of data points
     */
    public int getNrOfSamples()
    {
        return mNrOfSamples;
    }

    /**
     * This returns the data points' X dimension
     * @return ArrayList of Doubles representing the data values in the X dimension
     */
    public ArrayList<Double> getX()
    {
        return mX;
    }

    /**
     * This returns the data points' Y dimension
     * @return ArrayList of Doubles representing the data values in the Y dimension
     */
    public ArrayList<Double> getY()
    {
        return mY;
    }

    /**
     * This returns the data points' Z dimension
     * @return ArrayList of Doubles representing the data values in the Z dimension
     */
    public ArrayList<Double> getZ()
    {
        return mZ;
    }

    /**
     * This returns the data points' time stamps
     * @return ArrayList of Doubles representing the data timestamps
     */
    public ArrayList<Double> getT()
    {
        return mT;
    }

    /**
     * This returns the source of the Data object.
     * Should be one of {@link #DATA_SOURCE_PHONE}, {@link #DATA_SOURCE_BLUETOOTH}, {@link #DATA_SOURCE_FILE}
     * @return the source
     */
    public int getSource()
    {
        return source;
    }

}

