package com.example.eddiesyn.myfirst_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.media.MediaPlayer;

import com.example.eddiesyn.myfirst_app.BluetoothConnectionService.BluetoothConnectionListener;
import com.example.eddiesyn.myfirst_app.DataProvider.OnDataAvailableListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is the Activity class
 *
 * @author laurenz
 *
 */
public class BluetoothMode extends Activity implements OnItemSelectedListener, BluetoothConnectionListener {
    /** This is the MAC address of the bluetooth sensor. */
    private String deviceAddress = "00:80:25:01:57:F6";

    /** Maximum number of points in the upper chart. Affects performance. */
    private static final int PLOT_HISTORY_SIZE = 300;

    /** The speed of phone sensor. Affects battery and performance. */
    private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;

    /** Intent request code for DeviceListActivity */
    private static final int INTENT_REQUEST_CHOOSE_DEVICE = 1;

    /** The input selection dropdown */
    private Spinner spinner = null;
    /** vibrator button */
    private ToggleButton btnVibrate = null;
    /** bt send button */
    private ToggleButton btnSend = null;
    /** bt other Command Button */
    private Button btnCmd = null;
//    private double dist1 = 0.0;
//    private double dist2 = 0.0;
//    private double dist3 = 0.0;
    private double[] dist = new double[4];
    private int idx = 5;

    private double[] Reference_jump = {2034.1, 558.9}; //std max
//    private double[] Reference_jump = {1903.2, 507.8};
//    private double[] Reference_jump = {1895.3, 526.1};

    private double[] Reference_run = {1515.2, 347.71};
//    private double[] Reference_run = {1476.4, 310.6};
//    private double[] Reference_run = {1507.6, 325.9};

    private double[] Reference_walk = {511.7, 67.3531};
//    private double[] Reference_walk = {506.3, 62.174};
//    private double[] Reference_walk = {523.2, 66.1};

    private double[] Reference_idle = {362.6, 2.288};
   // private double[] Reference_idle = {362.6, 15};
//    private double[] Reference_idle = {312.4, 9.4};

    private int step = 0;
//    private int times = 0;
    private TextView tv_step;
    private TextView tv_speed;
    private TextView tv_sumstep;
    private String sumstep = "Total Steps during walk:";
    private String speed_show;
    private String tv_show;
//    private double oriValue = 0;
//    private double lstValue = 0;
//    private double curValue = 0;
//    private boolean motiveState = true;
//    private boolean processState = true;
//    private double range1 = 3;
//    private double range2 = 0;

    /** Data values (again) for passing to JNI. X dimension. */
    private ArrayList<Double> accX = new ArrayList<Double>(PLOT_HISTORY_SIZE);
    /** Data values (again) for passing to JNI. Y dimension. */
    private ArrayList<Double> accY = new ArrayList<Double>(PLOT_HISTORY_SIZE);
    /** Data values (again) for passing to JNI. Z dimension. */
    private ArrayList<Double> accZ = new ArrayList<Double>(PLOT_HISTORY_SIZE);
    private ArrayList<Double> accA = new ArrayList<>(PLOT_HISTORY_SIZE);
    private ArrayList<Double> feature_old = new ArrayList<>(3);
    private ArrayList<Double> feature_new = new ArrayList<>(3);
    private ArrayList<Integer> Statefinder = new ArrayList<Integer>(10);

    /** The DataProvider object provides data values from various sources */
    private DataProvider dataProvider = null;
    /** The BluetoothConnectionService handles the connection */
    private BluetoothConnectionService bluetoothService;

    /** This is the local bluetooth module */
    private BluetoothAdapter bluetoothAdapter = null;
    /** This is the remote bluetooth device (the sensor) */
    private BluetoothDevice device = null;

    /** Phone vibrator */
    private Vibrator vibrator = null;

    /** This flag is used to prevent fft calculation overflow */
    private boolean fftIsBusy = false;

    /** This flag is used to recall the provider-state in onPause/onResume */
    private boolean phoneWasProviding = false;
    /** This flag is used to recall the provider-state in onPause/onResume */
    private boolean bluetoothWasProviding = false;

    /** Used to display file chooser on file input */
    private ArrayList<String> files = null;

//    private FeatureExtraction Myfeature = null;

//com.example.eddiesyn.myfirst_app.

//    private native void initializeModel();
//
//    private native void stepModel();
//
//    private native void setModelInput(double val, int i);
//
//    private native double getModelOutput(int i);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_mode);

        // set up the spinner
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.connections_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        tv_step = (TextView) findViewById(R.id.tv_step);
        tv_speed =(TextView) findViewById(R.id.tv_speed);
        tv_sumstep = (TextView) findViewById(R.id.tv_sumstep);

        // set up the plots
//        accPlot = (XYPlot) findViewById(R.id.accPlot);
//        freqPlot = (XYPlot) findViewById(R.id.freqPlot);
        initPlot();

        // set up engines
        dataProvider = new DataProvider(getApplicationContext());
        bluetoothService = new BluetoothConnectionService();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        dataProvider.registerListener(onDataAvailableListener);
        bluetoothService.registerListener(this);
        spinner.setOnItemSelectedListener(this);

        // set up button functionality
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        btnVibrate = (ToggleButton) findViewById(R.id.btnVibrate);
        btnSend = (ToggleButton) findViewById(R.id.btnSend);
        btnCmd = (Button) findViewById(R.id.btnCmd);
        btnCmd.setOnClickListener(onClickListener);
        btnVibrate.setOnCheckedChangeListener(onCheckedChangeListener);
        btnSend.setOnCheckedChangeListener(onCheckedChangeListener);

    }

    @Override
    protected void onPause() {

        // App becomes (partly) invisible. Stop receiving data and save states.
        phoneWasProviding = dataProvider.isPhoneProviding();
        bluetoothWasProviding = dataProvider.isBluetoothProviding();
        dataProvider.disableBluetoothSensorProviding();
        dataProvider.disablePhoneSensorProviding();

        super.onPause();
        music.stop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // App is visible again.
        music.play(this, R.raw.game);
        if (bluetoothWasProviding) dataProvider.enableBluetoothSensorProviding(bluetoothService);
        if (phoneWasProviding) dataProvider.enablePhoneSensorProviding(SENSOR_DELAY);

    }





    /** builds a dialog box that asks whether to enable bluetooth */
    private void askToEnableBT() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Bluetooth turned off")
                .setMessage("Do you want to enable Bluetooth and try to connect?").setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        bluetoothAdapter.enable();
                        initBluetoothCon();
                        dialog.dismiss();
                    }
                }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    @Override
    protected void onStop() {
        // kill bluetooth connection when App is completely hidden

        bluetoothService.stop();

        super.onStop();
    }

    @Override
    protected void onStart() {
        // If we killed the connection on onStop() we should reconnect now.
        if (bluetoothWasProviding) {
            initBluetoothCon();
        }
        super.onStart();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_REQUEST_CHOOSE_DEVICE) {
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                deviceAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                    device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                    bluetoothService.connect(device);
                    dataProvider.enableBluetoothSensorProviding(bluetoothService);
                } else {
                    makeToast("BT address \"" + deviceAddress + "\" is invalid");
                }
            }
        }
    }

    /**
     * gets called when a button is clicked
     */
    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.equals(btnCmd)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(BluetoothMode.this);

                alert.setTitle("Send BT Cmd");
                alert.setMessage("Type the commands you want to send to the BT sensor");

                // Set an EditText view to get user input
                final EditText input = new EditText(BluetoothMode.this);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        bluetoothService.write(value.getBytes());
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            }
        }
    };

    /**
     * gets called when one of the toggle buttons is clicked
     */
    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView.equals(btnVibrate)) {
                if (isChecked) {
                    // start vibrating forever
                    vibrator.vibrate(new long[] { 0, 100 }, 0);
                } else {
                    vibrator.cancel();
                }
            } else if (buttonView.equals(btnSend)) {
                if (isChecked) {
                    new ControlPacket(ControlPacket.INSTRUCTION_START_STOP_MEASURE_SEND_BT).build().send(bluetoothService);
                } else {
                    new ControlPacket(ControlPacket.INSTRUCTION_SLEEP).build().send(bluetoothService);
                }
            }
        }
    };

    /**
     * The method in this listener gets called when the DataProvider has new
     * Data available.
     */
    private final OnDataAvailableListener onDataAvailableListener = new OnDataAvailableListener() {
        @Override
        public synchronized void onDataAvailable(Data data) {
            // add the samples in data to the plots and our value arrays
//            for(int i=0; i<data.getNrOfSamples(); i++){
//                accX.add
//            }
            accX.addAll(data.getX());
            accY.addAll(data.getY());
            accZ.addAll(data.getZ());

            while (accX.size() > PLOT_HISTORY_SIZE)
                accX.remove(0);
            while (accY.size() > PLOT_HISTORY_SIZE)
                accY.remove(0);
            while (accZ.size() > PLOT_HISTORY_SIZE)
                accZ.remove(0);

//            accA = FeatureExtraction.Smooth(FeatureExtraction.magnitude(accX, accY, accZ));


            feature_new = FeatureExtraction.Feature(accX);
            dist[0] = Dist.Mydist(feature_new, Reference_jump);
            dist[1] = Dist.Mydist(feature_new, Reference_run);
            dist[2] = Dist.Mydist(feature_new, Reference_walk);
            dist[3] = Dist.Mydist(feature_new, Reference_idle);
//            int idx = 5;
            double tmp = 100000;
            for(int i=0; i<4; i++){
                if(dist[i] < tmp){
                    idx = i;
                    tmp = dist[i];
                }
            }
//            if(feature.get(1) < 35){
//                idx = 3;
//            }
//            accA = FeatureExtraction.magnitude(accX, accY, accZ);
//            curValue = FeatureExtraction.Mean(accA);
//            if (motiveState == true) {
//                if (curValue >= lstValue) lstValue = curValue;
//                else {
//                    //检测到一次峰值
//                    if (Math.abs(curValue - lstValue) > range1) {
//                        oriValue = curValue;
//                        motiveState = false;
//                    }
//                }
//            }
//            if (motiveState == false) {
//                if (curValue <= lstValue) lstValue = curValue;
//                else {
//                    if (Math.abs(curValue - lstValue) > range2) {
//                        //检测到一次峰值
//                        oriValue = curValue;
//                        if (processState == true) {
//                            step++;  //步数 + 1
////                            if (processState == true) {
////                                tv_step.setText(step + "");    //读数更新
////                            }
//                        }
//                        motiveState = true;
//                    }
//                }
//            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    tv_sumstep.setText(sumstep + step + "");
                                //向下加速的状态
                    if(Dist.Eucd(feature_new,feature_old)){

                        speed_show = "Speed UP!";
                    }
                    else{

                        speed_show = "Slow down..";
                    }
                    tv_speed.setText(speed_show);
                    switch (idx){
                        case 0:
                            tv_show = "Jump";
                            if(feature_new.get(2) < 1){
                                tv_show = "Run";
                                step=0;
                                Statefinder.add(1);
                            }
                            step=0;
                            tv_step.setText(tv_show);
                            //tv_sumstep.setText(sumstep+step);
                            Statefinder.add(0);
                            break;
                        case 1:
                            tv_show = "Run";
                            tv_step.setText(tv_show);
                            step=0;
                            //tv_sumstep.setText(sumstep+step);
                            Statefinder.add(1);
                            break;
                        case 2:
                            tv_show = "Walk";
                            //if(feature_new.get(2) > 1){
                            //    tv_show = "Run";
                            //}
                            tv_step.setText(tv_show);
                            step++;

                            //tv_sumstep.setText(sumstep+step);
                            Statefinder.add(2);
                            break;
                        case 3:
                            tv_show = "Stand";
                            tv_step.setText(tv_show);
                            step=0;
//                            tv_sumstep.setText(sumstep+step);
                            speed_show = "Static state";
                            tv_speed.setText(speed_show);
                            Statefinder.add(3);
                            break;
                    }
                    if(step%3==0)
                        tv_sumstep.setText(sumstep+step/3);

                    while(Statefinder.size()>5)
                        Statefinder.remove(0);
                    Integer[] states = Statefinder.toArray(new Integer[0]);
                    if((states[0]==states[1])&&(states[1]==states[2])&&(states[2]==states[3])&&(states[3]==states[4])) {
                        speed_show = "constant speed";
                        tv_speed.setText(speed_show);
                    }

                }
            });
            feature_old = feature_new;
        }
    };

    /**
     * sets up plot style and content
     */
    private void initPlot() {

        for (int i = 0; i < PLOT_HISTORY_SIZE; i++) {
            accX.add(Double.valueOf(0));
            accY.add(Double.valueOf(0));
            accZ.add(Double.valueOf(0));
            accA.add(Double.valueOf(0));
        }

        for (int i=0; i < 3; i++){
            feature_new.add(Double.valueOf(0));
            feature_old.add(Double.valueOf(0));
        }
        for(int i = 0; i<10; i++)
            Statefinder.add(Integer.valueOf(0));
    }

    /**
     * gets called when the input selection dropdown was used
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        switch (pos) {
            case Data.DATA_SOURCE_NONE:
                bluetoothService.stop();
                dataProvider.disableBluetoothSensorProviding();
                dataProvider.disablePhoneSensorProviding();
                dataProvider.disableFileProviding();
                btnVibrate.setVisibility(View.GONE);
                btnCmd.setVisibility(View.GONE);
                btnSend.setVisibility(View.GONE);
                break;
            case Data.DATA_SOURCE_PHONE:
                bluetoothService.stop();
                dataProvider.disableBluetoothSensorProviding();
                dataProvider.disableFileProviding();
//                clearData();
                dataProvider.enablePhoneSensorProviding(SENSOR_DELAY);
                btnVibrate.setVisibility(View.VISIBLE);
                btnCmd.setVisibility(View.GONE);
                btnSend.setVisibility(View.GONE);
                break;
            case Data.DATA_SOURCE_BLUETOOTH:
                dataProvider.disablePhoneSensorProviding();
                dataProvider.disableFileProviding();
//                clearData();
                initBluetoothCon();
                btnVibrate.setVisibility(View.GONE);
                btnCmd.setVisibility(View.VISIBLE);
                btnSend.setVisibility(View.VISIBLE);
                break;
            case Data.DATA_SOURCE_FILE:
                dataProvider.disableBluetoothSensorProviding();
                dataProvider.disablePhoneSensorProviding();
//                clearData();
                initFileReading();
                btnVibrate.setVisibility(View.GONE);
                btnCmd.setVisibility(View.GONE);
                btnSend.setVisibility(View.GONE);
                break;
        }
    }

    private synchronized void clearData() {
        while(accX.size()>0) accX.remove(accX.size()-1);
        while(accY.size()>0) accY.remove(accY.size()-1);
        while(accZ.size()>0) accZ.remove(accZ.size()-1);
        while(accA.size()>0) accA.remove(accA.size()-1);
    }

    private void initFileReading() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String[] downloadDirFiles = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).list();
            if (downloadDirFiles == null) {
                makeToast("Download directory not found");
            } else {
                files = new ArrayList<String>(downloadDirFiles.length);
                for (int i = 0; i < downloadDirFiles.length; i++) {
                    if (downloadDirFiles[i].endsWith(".csv")) files.add(downloadDirFiles[i]);
                }
                if (files.size() == 0) {
                    makeToast("No .csv file found in " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item_fix, files);
                    builder.setSingleChoiceItems(arrayAdapter, 0, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                dataProvider.enableFileProviding(new FileReader(new File(Environment
                                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + files.get(which))));
                            } catch (FileNotFoundException e) {
                                makeToast("Could not access file");
                            }
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        } else if (Environment.getExternalStorageState().equals(Environment.MEDIA_UNMOUNTED)) {
            makeToast("SD card not accessible");
        } else {
            makeToast("Unknown SD card state");
        }
    }

    /**
     * Start new bluetooth connection.
     * <p>
     * If bluetooth is not enabled, the user will be asked to do so. Shows a
     * list of available bluetooth devices.
     */
    private void initBluetoothCon() {
        if (!bluetoothAdapter.isEnabled() && bluetoothAdapter.getState() != BluetoothAdapter.STATE_TURNING_ON)
            askToEnableBT();
        else {

            // set up new connection now - start by choosing the remote device
            // (next: onActivityResult())
            Intent chooseDeviceIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
            startActivityForResult(chooseDeviceIntent, INTENT_REQUEST_CHOOSE_DEVICE);

        }
    }

    @Override
    public void onBluetoothConnectionStateChanged(int state) {
        String text = "";
        if (state == BluetoothConnectionService.STATE_CONNECTED)
            text = "Connected.";
        else if (state == BluetoothConnectionService.STATE_CONNECTING)
            text = "Connecting...";
        else if (state == BluetoothConnectionService.STATE_NONE) text = "No connection.";
        makeToast(text);
    }

    @Override
    public void onBluetoothConnectionConnected(String name, String address) {
        makeToast("Connected to: " + name + " (" + address + ")");
    }

    @Override
    public void onBluetoothConnectionFailure(int whatFailed) {
        String text = "";
        if (whatFailed == BluetoothConnectionService.FAILURE_CONNECTION_LOST)
            text = "Bluetooth connection lost";
        else if (whatFailed == BluetoothConnectionService.FAILURE_WRITE_FAILED)
            text = "Bluetooth write failed";
        else if (whatFailed == BluetoothConnectionService.FAILURE_CONNECTING_FAILED) text = "Bluetooth connecting failed";
        makeToast(text);
    }

    /**
     * starts a thread that calculates the FFT spectrum
     */
//    private void getFrequencies() {
//        if (!fftIsBusy) {
//            fftIsBusy = true;
//            new FrequencyGetter().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null, null, null);
//        }
//    }

    /**
     * This gets called in a separate thread and uses the JNI to call simulink
     * functions that Fast-Fourier-Transform the acceleration input.
     */
//    private class FrequencyGetter extends AsyncTask<Void, Void, Void> {
//        @Override
//        protected Void doInBackground(Void... params) {
//
//            for (int i = 0; i < PLOT_HISTORY_SIZE; i++)
//                setModelInput(accX.get(i), i);
//            stepModel();
//            for (int i = 0; i < freqSeriesX.size(); i++)
//                freqSeriesX.setY(Double.valueOf(getModelOutput(i)), i);
//
//            for (int i = 0; i < PLOT_HISTORY_SIZE; i++)
//                setModelInput(accY.get(i), i);
//            stepModel();
//            for (int i = 0; i < freqSeriesY.size(); i++)
//                freqSeriesY.setY(Double.valueOf(getModelOutput(i)), i);
//
//            for (int i = 0; i < PLOT_HISTORY_SIZE; i++)
//                setModelInput(accZ.get(i), i);
//            stepModel();
//            for (int i = 0; i < freqSeriesZ.size(); i++)
//                freqSeriesZ.setY(Double.valueOf(getModelOutput(i)), i);
//
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    freqPlot.redraw();
//                }
//            });
//            fftIsBusy = false;
//
//            return null;
//        }
//    }

    /** display toast message. */
    private void makeToast(String text) {
        runOnUiThread(new ToastMaker(text));
    }

    /**
     * used by {@link BluetoothMode#makeToast(String)} to display a toast. Must
     * be run on the UI thread
     */
    private class ToastMaker implements Runnable {
        private String text;

        public ToastMaker(String text) {
            this.text = text;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public void onBluetoothConnectionReceive(byte[] buffer, int numberOfBytesInBuffer) {
    }

    @Override
    public void onBluetoothConnectionWrite(byte[] buffer) {
    }
}
