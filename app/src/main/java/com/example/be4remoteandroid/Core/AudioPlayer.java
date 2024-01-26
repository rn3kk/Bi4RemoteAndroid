package com.example.be4remoteandroid.Core;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class AudioPlayer extends Thread{
    private boolean m_terminate = false;
    List<byte[]> m_data = new ArrayList<byte[]>();
    Semaphore m_mutex = new Semaphore(1);

    @Override
    public void run() {
        int minBuffSize = 1920;
        AudioTrack m_audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(48000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(minBuffSize)
                .build();

        m_audioTrack.play();
        while(!m_terminate)
        {
            if(m_data.size() >0) {
                byte[] d = null;
                try {
                    m_mutex.acquire();
                    d = m_data.get(0);
                    m_data.remove(0);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
                m_mutex.release();
                m_audioTrack.write(d, 0, d.length);
            }
        }
        m_audioTrack.stop();
    }

    public void setTerminate(){
        m_terminate = true;
    }

    public void put(byte[] data){
        try{
            m_mutex.acquire();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
        m_data.add(data);
        m_mutex.release();
    }

}
