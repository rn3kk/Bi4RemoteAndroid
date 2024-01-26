package com.example.be4remoteandroid.Core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class UdpOpusReciver extends Thread{

    public UdpOpusReciver()
    {

    }

    @Override
    public void run() {
        OpusAudioDecoder decoder = new OpusAudioDecoder();
        decoder.start();
        DatagramSocket s = null;
        try {
            s = new DatagramSocket(6992);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        byte[] buf = new byte[256];
        DatagramPacket rcvP = new DatagramPacket(buf, buf.length);
        while(true){
            try {
                s.receive(rcvP);
                byte[] data = Arrays.copyOfRange(rcvP.getData(), 0, rcvP.getLength());
                decoder.put(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
