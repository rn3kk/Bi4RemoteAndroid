package com.example.be4remoteandroid.Core;


import static android.media.MediaFormat.MIMETYPE_AUDIO_OPUS;
import static android.media.MediaFormat.MIMETYPE_AUDIO_RAW;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;

import com.example.be4remoteandroid.RtpPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Semaphore;


public class UdpClient extends Thread{
    private boolean m_terminate = false;
    private Socket mSocket = null;
    private String username;
    private String password;
    private String mIp = "";
    private int mPort = 0;
    private boolean handshake = false;
    byte[] d = null;
    Semaphore m_semaphore = new Semaphore(1);

    MediaFormat mOutputFormat;

    public UdpClient(String ip, int port_tcp, String username, String password)
    {
        System.out.println("UdpClient()");
        this.mIp = ip;
        this.mPort = port_tcp;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run() {
        OpusAudioDecoder decoder = new OpusAudioDecoder();
        decoder.start();
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(100);
            SocketAddress sockaddr = new InetSocketAddress(mIp, mPort);
            byte[] buf = new byte[1024];
            while(!m_terminate)
            {
                try {
                    if(!handshake){
                        Thread.sleep(100);
                        byte[] p = RtpPacket.createHandshakePacket().toBytes();
                        DatagramPacket sendingPacket = new DatagramPacket(p, 0, p.length, sockaddr);
                        clientSocket.send(sendingPacket);
                    }
                    DatagramPacket rcvP = new DatagramPacket(buf, buf.length);
                    clientSocket.receive(rcvP);

                    RtpPacket rtp = RtpPacket.getRtpPacketFromBytes(rcvP);
                    if(rtp.get_payload_type() == RtpPacket.TYPE_HANDSHAKE_PAYLOAD){
                        System.out.println(new String(rcvP.getData()));
                        handshake = true;
                        System.out.println("Receive HANDSHAKE*******");
                    }
                    else if(rtp.get_payload_type() == RtpPacket.TYPE_OPUS_PAYLOAD){
                        decoder.put(rtp.get_payload());
                    }
                }
                catch (IOException | InterruptedException e){
//                    e.printStackTrace();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("UDP Listener is end()");
    }

    public void setTerminate()
    {
        m_terminate = true;
    }

}
