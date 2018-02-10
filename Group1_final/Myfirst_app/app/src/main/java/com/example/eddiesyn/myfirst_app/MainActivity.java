package com.example.eddiesyn.myfirst_app;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        music.play(this, R.raw.game1);
    }

    /** Called when the user taps the Phone button */
//    public void ChoosePhone(View view){
//        Intent intent1 = new Intent(this, PhoneMode.class);
//        startActivity(intent1);
//    }

    public void ChooseBluetooth(View view){
        Intent intent2 = new Intent(this, BluetoothMode.class);
        startActivity(intent2);
    }
    @Override
    protected void onPause() {

        // App becomes (partly) invisible. Stop receiving data and save states.
        //phoneWasProviding = dataProvider.isPhoneProviding();
        //bluetoothWasProviding = dataProvider.isBluetoothProviding();
        //dataProvider.disableBluetoothSensorProviding();
        //dataProvider.disablePhoneSensorProviding();

        super.onPause();
        music.stop(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // App is visible again.
        music.play(this, R.raw.game1);
        //if (bluetoothWasProviding) dataProvider.enableBluetoothSensorProviding(bluetoothService);
        //if (phoneWasProviding) dataProvider.enablePhoneSensorProviding(SENSOR_DELAY);

    }

}
