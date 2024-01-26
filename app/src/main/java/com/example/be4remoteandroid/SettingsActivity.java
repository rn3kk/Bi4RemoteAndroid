package com.example.be4remoteandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {


    private TextView m_ip_viev = null;
    private TextView m_tcp_port_view = null;
    private TextView m_udp_port_view = null;
    private TextView m_user_name_view = null;
    private TextView m_password_view = null;
    private SharedPreferences m_sp = null;
    private Button m_save_settings_btn = null;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        m_sp = getSharedPreferences(String.valueOf(R.string.settings), Context.MODE_PRIVATE);
        String ip = m_sp.getString(String.valueOf(R.string.ip), "192.168.0.1");
        int tcp_port = m_sp.getInt(String.valueOf(R.string.tcp_port), 6991);
        int udp_port = m_sp.getInt(String.valueOf(R.string.udp_port), 6992);
        String user_name = m_sp.getString(String.valueOf(R.string.user_name), "user");
        String password = m_sp.getString(String.valueOf(R.string.password), "password");

        m_ip_viev = (TextView) findViewById(R.id.ip_edit_text);
        m_tcp_port_view = (TextView) findViewById(R.id.tcp_port_edit);
        m_udp_port_view = (TextView) findViewById(R.id.udp_port_edit);
        m_user_name_view = (TextView) findViewById(R.id.user_name_text_edit);
        m_password_view = (TextView) findViewById(R.id.password_text_edit);

        m_ip_viev.setText(ip);
        m_tcp_port_view.setText(String.valueOf(tcp_port));
        m_udp_port_view.setText(String.valueOf(udp_port));
        m_user_name_view.setText(user_name);
        m_password_view.setText(password);

        m_save_settings_btn = (Button) findViewById(R.id.save_settings_button);
        m_save_settings_btn.setOnClickListener(saveSettClick);
    }

    View.OnClickListener saveSettClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String ip = m_ip_viev.getText().toString();
            int tcp_port = Integer.parseInt(m_tcp_port_view.getText().toString());
            int udp_port = Integer.parseInt(m_udp_port_view.getText().toString());
            String user = m_user_name_view.getText().toString();
            String password = m_password_view.getText().toString();
            SharedPreferences.Editor editor = m_sp.edit();
            editor.putString(String.valueOf(R.string.ip), ip);
            editor.putInt(String.valueOf(R.string.tcp_port), tcp_port);
            editor.putInt(String.valueOf(R.string.udp_port), udp_port);
            editor.putString(String.valueOf(R.string.user_name), user);
            editor.putString(String.valueOf(R.string.password), password);
            editor.apply();
            finish();
        }
    };
}