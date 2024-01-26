package com.example.be4remoteandroid.Core;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaFormat.MIMETYPE_AUDIO_OPUS;
import static android.media.MediaFormat.MIMETYPE_AUDIO_RAW;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

public class OpusAudioDecoder extends Thread{

    private List<byte[]> m_List = new ArrayList<>();
    Semaphore m_Mutex = new Semaphore(1);
    MediaCodec md = null;
    boolean send_flag_config = false;

    @Override
    public void run() {
        AudioPlayer player = new AudioPlayer();
        player.start();

        try {


            md = MediaCodec.createDecoderByType(MIMETYPE_AUDIO_OPUS);
            md.setCallback(new MediaCodec.Callback() {

                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
//                    System.out.println("Input buffer callback " + index);
//                    int len = 0;
                    ByteBuffer inputBuffer;
                    try {
                        inputBuffer = codec.getInputBuffer(index);
                    }
                    catch (IllegalStateException e){
                        e.printStackTrace();
                        return;
                    }

                    byte[] d = get();
                    if( d != null && d.length > 0) {
                        System.out.println("Decoder Input buffer processing " + d.length);

                        inputBuffer.put(d);
//                        d[0] = 120;
                        int len = d.length;
//                        if(!send_flag_config){
//                            codec.queueInputBuffer(index, 0, len, 0, BUFFER_FLAG_CODEC_CONFIG);
//                            send_flag_config = true;
//                        }
//                        else
                        codec.queueInputBuffer(index, 0, len, 0, 0);
                        return;
                    }
                    codec.queueInputBuffer(index, 0, 0, 0, 0);
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
//                    System.out.println("Out biffer callback");
                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                    MediaFormat bufferFormat = codec.getOutputFormat(index); // option A
                    byte[] b = new byte[info.size];
                    outputBuffer.get(b,0,info.size);
                    player.put(b);
                    outputBuffer.clear();
                    codec.releaseOutputBuffer(index, info.presentationTimeUs);
//                    System.out.println("Output callback END");
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    System.out.println("Codec error" + e);
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });

            //https://android.googlesource.com/platform/frameworks/av/+/refs/heads/android12-release/media/libstagefright/foundation/tests/OpusHeader/OpusHeaderTest.cpp
            //TODO разобраться с формирование заголовка Opus стрима

            MediaFormat mf = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_OPUS, 48000, 1);
            mf.setInteger(MediaFormat.KEY_BIT_RATE, 60*50);
            mf.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256);

//            ByteBuffer csdbuf0 = ByteBuffer.wrap("java.nio.HeapByteBuffer[pos=0 lim=83 cap=83]".getBytes());
//            ByteBuffer csdbuf0 = ByteBuffer.wrap("OpusHead".getBytes());
//            ByteBuffer csdbuf1 = ByteBuffer.allocate(Long.BYTES);
//            ByteBuffer csdbuf2 = ByteBuffer.allocate(Long.BYTES);

//            csdbuf1.putLong(0);
//            csdbuf2.putLong(60);
//            mf.setByteBuffer("csd-0", csdbuf0);
//            mf.setByteBuffer("csd-1", csdbuf1);
//            mf.setByteBuffer("csd-2", csdbuf2);

            md.configure(mf, null, null, 0);
            md.start();


        }
        catch (IOException e)
        {
            System.out.println("Error media codec");
        }
    }



    public void put(byte[] data){
        if(m_List.size() > 20)
            return;

        try {
//            System.out.println("Put start " + data.toString());
            m_Mutex.acquire();
            m_List.add(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        m_Mutex.release();
//        System.out.println("Put end");

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
        return data;
    }

}
