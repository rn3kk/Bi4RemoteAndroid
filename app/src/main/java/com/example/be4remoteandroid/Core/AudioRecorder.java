package com.example.be4remoteandroid.Core;

import static android.media.MediaFormat.MIMETYPE_AUDIO_OPUS;
import static android.media.MediaFormat.MIMETYPE_AUDIO_RAW;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class AudioRecorder extends Thread {
    private AudioRecord m_recorder = null;
    private boolean m_terminate = false;

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override
    public void run() {

        OpusAudioEncoder opus_encoder = new OpusAudioEncoder();
        opus_encoder.start();

        int minBuffSize = 960;

        m_recorder = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(48000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(2 * minBuffSize)
                .build();
        m_recorder.startRecording();

        while(!m_terminate)
        {
            byte[] b = new byte[2 * minBuffSize];
            int l = m_recorder.read(b, 0, 2 * minBuffSize);
            if(l>0) {
                opus_encoder.put(b);
            }
        }
        m_recorder.stop();
    }

    public void setTerminate(){
        m_terminate = true;
    }
}
