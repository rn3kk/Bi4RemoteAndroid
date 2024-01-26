package com.example.be4remoteandroid.Core;


import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static android.media.MediaFormat.MIMETYPE_AUDIO_OPUS;
import static android.media.MediaFormat.MIMETYPE_AUDIO_RAW;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class OpusAudioEncoder extends Thread{

    private List<byte[]> m_List = new ArrayList<>();
    private List<byte[]> m_encoded = new ArrayList<>();
    Semaphore m_Mutex = new Semaphore(1);
    Semaphore m_mutex_encoded = new Semaphore(1);
    MediaCodec md = null;
    OpusAudioDecoder m_decoder = null;
    DatagramSocket m_s = null;

    @Override
    public void run() {
//        m_decoder = new OpusAudioDecoder();
//        m_decoder.start();

        try {
            m_s = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);
            MediaCodecInfo[] ci_list = mcl.getCodecInfos();


            md = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_OPUS);
            md.setCallback(new MediaCodec.Callback() {

                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    int len = 0;
                    ByteBuffer inputBuffer = codec.getInputBuffer(index);

                    byte[] d = get();
                    if( d != null && d.length > 0) {
//                        System.out.println(String.format("Encoder input {%s} buffer processing ", index) + d.length);
                        inputBuffer.clear();
                        inputBuffer.put(d);
                        len = d.length;
                        codec.queueInputBuffer(index, 0, len, 0, 0);
                        return;
                    }
                    codec.queueInputBuffer(index, 0, 0, 0, 0);
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                    MediaFormat bufferFormat = codec.getOutputFormat(index); // option A
                    byte[] b = new byte[info.size];
                    outputBuffer.get(b,0, info.size);
                    outputBuffer.clear();
                    System.out.println(String.format("Encoder Out {%s} buffer ", index) + info.presentationTimeUs + " "  + info.size + " " + b.toString());

                    try {
                        m_mutex_encoded.acquire();
                        m_encoded.add(b);
                    } catch ( InterruptedException e) {
                        e.printStackTrace();
                    }
                    m_mutex_encoded.release();
                    codec.releaseOutputBuffer(index, info.presentationTimeUs);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    System.out.println("Codec error" + e);
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });
            MediaFormat mf = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_RAW, 48000, 1);
//            mf.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//            mf.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
//            mf.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            mf.setInteger(MediaFormat.KEY_BIT_RATE, 38400);
            mf.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
            md.configure(mf, null, null, CONFIGURE_FLAG_ENCODE);
            md.start();
            MediaFormat inpfor = md.getOutputFormat();
            ByteBuffer b0 = inpfor.getByteBuffer("csd-0");
            String b1 = inpfor.getString("csd-1");
            String b2 = inpfor.getString("csd-2");
            System.out.println("TEST");

        }
        catch (IOException e)
        {
            System.out.println("Error media codec");
        }

        byte[] data = null;
        while(!Thread.interrupted())
        {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(m_encoded.size() > 0){
                try {
                    m_mutex_encoded.acquire();
                    data = m_encoded.get(0).clone();
                    m_encoded.remove(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                m_mutex_encoded.release();
                if(data!=null) {
                    SocketAddress sockaddr = new InetSocketAddress("192.168.0.30", 6992);
                    DatagramPacket p = new DatagramPacket(data, data.length, sockaddr);
                    data = null;
                    try {
                        m_s.send(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }



    public void put(byte[] data){
        if(m_List.size() > 20)
            return;

        try {
            m_Mutex.acquire();
            m_List.add(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        m_Mutex.release();
    }

    public byte[] get(){
        if(m_List.size() ==0)
            return null;

        byte[] data = null;
        try {
            m_Mutex.acquire();
            data = m_List.get(0).clone();
            m_List.remove(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        m_Mutex.release();
        System.out.println("Get data for decoder " + data.toString());
        return data;
    }

}

