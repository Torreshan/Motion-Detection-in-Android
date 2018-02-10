package com.example.eddiesyn.myfirst_app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.example.eddiesyn.myfirst_app.DataProvider.OnDataAvailableListener;

import java.util.ArrayList;

public class PhoneMode extends AppCompatActivity implements AdapterView.OnItemSelectedListener, BluetoothConnectionService.BluetoothConnectionListener {
    /** This is the MAC address of the bluetooth sensor. */
    private String deviceAddress = "00:80:25:01:57:F6";
    /** Window size for data collection. */
    private static final int WINDOW_SIZE = 128;
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

    private TextView tv_step;
    private Button btn_start = null;
    private int step = 0;
    private double oriValue = 0;
    private double lstValue = 0;
    private double curValue = 0;
    private boolean motiveState = true;
    private boolean processState = false;

    /** Array for Data(X) in Window size */
    private ArrayList<Double> accX = new ArrayList<>(WINDOW_SIZE);
    private ArrayList<Double> accY = new ArrayList<>(WINDOW_SIZE);
    private ArrayList<Double> accZ = new ArrayList<>(WINDOW_SIZE);

    double range1 = 1;
    double range2 = 1;

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

    /** This flag is used to recall the provider-state in onPause/onResume */
    private boolean phoneWasProviding = false;
    /** This flag is used to recall the provider-state in onPause/onResume */
    private boolean bluetoothWasProviding = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_mode);

        // set up spinner
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.connections_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // set up engines
        dataProvider = new DataProvider(getApplicationContext());
        bluetoothService = new BluetoothConnectionService();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        dataProvider.registerListener(onDataAvailableListener);
        bluetoothService.registerListener(this);
        spinner.setOnItemSelectedListener(this);
        tv_step = (TextView) findViewById(R.id.tv_step);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(onClickListener2);

        // set up button functionality
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        btnVibrate = (ToggleButton) findViewById(R.id.btnVibrate);
        btnSend = (ToggleButton) findViewById(R.id.btnSend);
        btnCmd = (Button) findViewById(R.id.btnCmd);
        btnCmd.setOnClickListener(onClickListener1);
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        // App is visible again.

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

        if (bluetoothWasProviding) initBluetoothCon();

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
    private View.OnClickListener onClickListener1 = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.equals(btnCmd)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(PhoneMode.this);

                alert.setTitle("Send BT Cmd");
                alert.setMessage("Type the commands you want to send to the BT sensor");

                // Set an EditText view to get user input
                final EditText input = new EditText(PhoneMode.this);
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
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
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
//            for (int i = 0; i < data.getNrOfSamples(); i++) {
//
//            }
//            if(processState == true){
                for(int i=0; i<data.getNrOfSamples(); i++){
                    accX.addAll(data.getX());
                    accY.addAll(data.getY());
                    accZ.addAll(data.getZ());
                }
            while (accX.size() > WINDOW_SIZE)
                accX.remove(0);
            while (accY.size() > WINDOW_SIZE)
                accY.remove(0);
            while (accZ.size() > WINDOW_SIZE)
                accZ.remove(0);

                curValue = magnitude(mean(accX), mean(accY), mean(accZ));
                if(curValue > 0){
                    step++;
                }
                tv_step.setText(step + "");
//            }
//            accX.addAll(data.getX());
//            accY.addAll(data.getY());
//            accZ.addAll(data.getZ());
//            curValue = magnitude(mean(accX), mean(accY), mean(accZ));
            //向上加速的状态
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
//            //向下加速的状态
//            if (motiveState == false) {
//                if (curValue <= lstValue) lstValue = curValue;
//                else {
//                    if (Math.abs(curValue - lstValue) > range2) {
//                        //检测到一次峰值
//                        oriValue = curValue;
//                        if (processState == true) {
//                            step++;  //步数 + 1
//                            if (processState == true) {
//                                tv_step.setText(step + "");    //读数更新
//                            }
//                        }
//                        motiveState = true;
//                    }
//                }
//            }
        }
    };

    private View.OnClickListener onClickListener2 = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
//            step = 0;
            tv_step.setText(step + "");
            if (processState == true) {
                btn_start.setText("Start");
                processState = false;
            } else {
                btn_start.setText("Stop");
                processState = true;
            }
        }

    };

    public double magnitude(double x, double y, double z){
        double magnitude = 0.0;
        magnitude = Math.sqrt(x*x + y*y + z*z);
        return magnitude;
    }

    public double mean(ArrayList<Double> X){
        double outcome = 0.0;
        for(int i=0; i<X.size(); i++){
            outcome += X.get(i);
        }
        outcome = outcome / X.size();
        return outcome;
    }

    /**
     * gets called when the input selection dropdown was used
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
        switch (pos) {
            case Data.DATA_SOURCE_NONE:
                bluetoothService.stop();
                dataProvider.disableBluetoothSensorProviding();
                dataProvider.disablePhoneSensorProviding();
                btnVibrate.setVisibility(View.GONE);
                btnCmd.setVisibility(View.GONE);
                btnSend.setVisibility(View.GONE);
                break;
            case Data.DATA_SOURCE_PHONE:
                bluetoothService.stop();
                dataProvider.disableBluetoothSensorProviding();
                clearData();
                dataProvider.enablePhoneSensorProviding(SENSOR_DELAY);
                btnVibrate.setVisibility(View.VISIBLE);
                btnCmd.setVisibility(View.GONE);
                btnSend.setVisibility(View.GONE);
                break;
            case Data.DATA_SOURCE_BLUETOOTH:
                dataProvider.disablePhoneSensorProviding();
                clearData();
                initBluetoothCon();
                btnVibrate.setVisibility(View.GONE);
                btnCmd.setVisibility(View.VISIBLE);
                btnSend.setVisibility(View.VISIBLE);
                break;
        }
    }

    private synchronized void clearData() {
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
