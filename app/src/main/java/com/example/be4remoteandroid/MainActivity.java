package com.example.be4remoteandroid;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements Handler.Callback, ServiceConnection{

    Handler mMessageHandler = new Handler(this);
    final private RadioService mRadioService = null;
    private boolean mBound = false;
    private TextView mSmeter = null;
    private Button mPwrBtn;
    private Menu mMenu;
    private RadioService.RadioServiceBinder mBinder = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPwrBtn = findViewById(R.id.pwrBtn);
        mPwrBtn.setOnClickListener(onPwrClic);

        mSmeter = findViewById(R.id.s_meter);
        mSmeter.setText("");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    10);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    10);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.settings_menu_item:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.connect_menu_item:
                if(RadioState.mConnected == false) {
                    intent = new Intent(this, RadioService.class);
                    if (!RadioService.isInstanceCreated()) {
                        if (intent != null) {
                            intent.putExtra(String.valueOf(R.string.messenger), new Messenger(mMessageHandler));
                            startService(intent);
                            bindService(intent, this, Context.BIND_AUTO_CREATE);
                        }
                    }
                }
                else{
                    intent = new Intent(this, RadioService.class);
                    unbindService(this);
                    stopService(intent);
                }
                return true;
            case R.id.help_menu_item:
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        if( message.obj != null) {
            CMD cmdFromRadioServer = (CMD) message.obj;
            switch (cmdFromRadioServer.cmd){
                case CommandsType.FROM_RADIO_PINS_PWR_STATE:
                    int value = Integer.parseInt(cmdFromRadioServer.value);
                    if(value == 1) {
                        RadioState.mPwrState = 1;
                        mPwrBtn.setText(R.string.turn_off);
                    }
                    else {
                        RadioState.mPwrState = 0;
                        mPwrBtn.setText(R.string.turn_on);
                    }
                    break;
                case CommandsType.FROM_RADIO_S_METER:
                    String str  = cmdFromRadioServer.value;
                    mSmeter.setText(str);
                    break;
                case CommandsType.FROM_RADIO_FREQ:
                    value = Integer.parseInt(cmdFromRadioServer.value);
                    RadioState.mFreq = value;
            }

        }
        else if(message.arg1 == CommandsType.CONNECTED_TO_RADIO){
            System.out.println("change menu");
            MenuItem m = mMenu.findItem(R.id.connect_menu_item);
            m.setTitle("DISC");
            RadioState.mConnected = true;
        }
        else if(message.arg1 == CommandsType.DISCONNECTED_FROM_RADIO){
            System.out.println("change menu2");
            MenuItem m = mMenu.findItem(R.id.connect_menu_item);
            m.setTitle("CONN");
            RadioState.mConnected = false;
        }

        return true;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        System.out.println("Connected");
        mBound = true;
        mBinder = (RadioService.RadioServiceBinder) iBinder;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        System.out.println("Disconnected");
        mBound = false;
        mBinder = null;
    }

    private View.OnClickListener onPwrClic = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(mBinder != null){
                mBinder.power_on_state( RadioState.mPwrState == 1? 0:1);
            }
        }
    };

}