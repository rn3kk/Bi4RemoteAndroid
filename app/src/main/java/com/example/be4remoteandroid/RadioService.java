package com.example.be4remoteandroid;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.example.be4remoteandroid.Core.TCPListener;

public class RadioService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private TCPListener m_tcpListener = null;
    private static RadioService m_instance = null;
    private SharedPreferences.OnSharedPreferenceChangeListener mListener;
    private final IBinder mBinder = new RadioServiceBinder();
    Messenger mToActivityMessanger;

    public static boolean isInstanceCreated() {
        return m_instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null)
                return START_NOT_STICKY;
        if(m_instance == null) {
            mToActivityMessanger = intent.getParcelableExtra(String.valueOf(R.string.messenger));
            SharedPreferences sp = getSharedPreferences(String.valueOf(R.string.settings), Context.MODE_PRIVATE);
            sp.registerOnSharedPreferenceChangeListener(this);
            String ip = sp.getString(String.valueOf(R.string.ip), "127.0.0.1");
            int tcp_port = sp.getInt(String.valueOf(R.string.tcp_port), 6991);
            String username = sp.getString(String.valueOf(R.string.user_name), "username");
            String password = sp.getString(String.valueOf(R.string.password), "password");
            m_tcpListener = new TCPListener(ip, tcp_port, username, password, mToActivityMessanger);
            m_tcpListener.start();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        m_instance = null;
        try {
            m_tcpListener.terminate();
            m_tcpListener.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if(m_tcpListener == null)
            return;
        m_tcpListener.terminate();
        try {
            m_tcpListener.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SharedPreferences sp = getSharedPreferences(String.valueOf(R.string.settings), Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(this);
        String ip = sp.getString(String.valueOf(R.string.ip), "127.0.0.1");
        int tcp_port = sp.getInt(String.valueOf(R.string.tcp_port), 6991);
        String username = sp.getString(String.valueOf(R.string.user_name), "username");
        String password = sp.getString(String.valueOf(R.string.password), "password");
        m_tcpListener = new TCPListener(ip, tcp_port, username, password, mToActivityMessanger);
        m_tcpListener.start();
    }

    public class RadioServiceBinder extends Binder {
        RadioService getService() {
            return RadioService.this;
        }

        void power_on_state(int state){
            CMD cmd = new CMD(CommandsDirection.TO_PINS, CommandsType.CMD_CHANGE_PWR_PIN, (state==1 ? "1" : "0"));
            m_tcpListener.send_to_server(cmd.toJsonString().getBytes());
        }

    }
}
