package com.example.eddiesyn.myfirst_app;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Created by eddiesyn on 18-2-1.
 */

public class DataFileReader {
    private CSVReader csvReader = null;
    private AsyncReader asyncReader = null;

    public DataFileReader(Reader reader) {
        csvReader = new CSVReader(reader);

    }

    public void start() {
        asyncReader = new AsyncReader();
        asyncReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
    }

    public void stop() {
        if (asyncReader != null) asyncReader.cancel(true);
    }

    private ArrayList<DataFileReaderListener> listOfRegisteredListeners = new ArrayList<DataFileReaderListener>();

    public void registerListener(DataFileReaderListener l) {
        listOfRegisteredListeners.add(l);
    }

    public void unregisterListener(DataFileReaderListener l) {
        listOfRegisteredListeners.remove(l);
    }

    private void notifyListeners(Double timeSec, Double x, Double y, Double z) {
        for (DataFileReaderListener l : listOfRegisteredListeners) {
            l.onDataFileNewDataPoint(timeSec, x, y, z);
        }
    }

    public static interface DataFileReaderListener {

        void onDataFileNewDataPoint(Double timeSec, Double x, Double y, Double z);

        void onDataFileError(Exception exception);
    }

    private class AsyncReader extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            long startTime = System.currentTimeMillis();
            String[] nextLine;
            try {
                while ((nextLine = csvReader.readNext()) != null) {
                    Double nextTime = Double.valueOf(nextLine[0]) * 1000; // sec-->ms
                    Double nextX = Double.valueOf(nextLine[1]);
                    Double nextY = Double.valueOf(nextLine[2]);
                    Double nextZ = Double.valueOf(nextLine[3]);
                    Double remainingTime = (startTime + nextTime) - System.currentTimeMillis();
                    if (remainingTime > 0) {
                        try {
                            Thread.sleep((long) Math.round(remainingTime));
                        } catch (InterruptedException e) {
                        }
                    }
                    notifyListeners(nextTime, nextX, nextY, nextZ);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                for (DataFileReaderListener l : listOfRegisteredListeners) {
                    l.onDataFileError(e);
                }
                cancel(true);
            }

            return null;
        }

    }

}
