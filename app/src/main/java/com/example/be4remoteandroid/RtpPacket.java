package com.example.be4remoteandroid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RtpPacket {
    public static int TYPE_OPUS_PAYLOAD = 1;
    public static int TYPE_HANDSHAKE_PAYLOAD = 2;

    public static byte[] HANDSHAKE_BODY = "handshake".getBytes();
    public static byte[] HANDSHAKE_OK_BODY = "handshake_ok".getBytes();

    //fields for byte1
    static String version = "10";  // binary not hex
    static String padding = "0";   // binary not hex
    static String extension = "0"; // binary not hex
    static String csi_count = "0000"; // binary not hex

    //fields for byte2
    static String marker = "0";
    static String payload_opus = String.format("%7s", Integer.toBinaryString(TYPE_OPUS_PAYLOAD)).replaceAll(" ", "0");
    static String payload_hanshake = String.format("%7s", Integer.toBinaryString(TYPE_HANDSHAKE_PAYLOAD)).replaceAll(" ", "0");


    public static  byte[] byte1 = String.format("%02x", Integer.parseInt( version + padding + extension + csi_count, 2)).getBytes();
    public static byte[] byte2_opus = String.format("%02x", Integer.parseInt( marker + payload_opus, 2)).getBytes();
    public static byte[] byte2_handshake = String.format("%02x", Integer.parseInt( marker + payload_hanshake, 2)).getBytes();
    public static byte[] ssrc = String.format("%08x", 123456).getBytes();

    private final int m_seq_num;
    private final int m_timestamp;

    public int get_payload_type() {
        return m_payload_type;
    }

    public byte[] get_payload() {
        return m_payload;
    }

    private final int m_payload_type;
    private final byte[] m_payload;



    public RtpPacket(int seq_num, int time, int payload_type, byte[] payload)
    {
        m_seq_num = seq_num;
        m_timestamp = time;
        m_payload_type = payload_type;
        m_payload = payload;

    }

    public byte[] toBytes()  {
        try {
            byte[] timestamp = String.format("%08x", m_timestamp).getBytes();
            byte[] sequence_number = String.format("%04x", m_seq_num).getBytes();
            ByteArrayOutputStream baos = null;
            if (m_payload_type == RtpPacket.TYPE_OPUS_PAYLOAD) {
                int len = RtpPacket.byte1.length + RtpPacket.byte2_opus.length + timestamp.length + sequence_number.length + RtpPacket.ssrc.length + m_payload.length;
                baos = new ByteArrayOutputStream(len);
                baos.write(RtpPacket.byte1);
                baos.write(RtpPacket.byte2_opus);
                baos.write(sequence_number);
                baos.write(timestamp);
                baos.write(RtpPacket.ssrc);
                baos.write(m_payload);
            } else if (m_payload_type == RtpPacket.TYPE_HANDSHAKE_PAYLOAD) {
                int len = RtpPacket.byte1.length + RtpPacket.byte2_handshake.length + timestamp.length + sequence_number.length + RtpPacket.ssrc.length + m_payload.length;
                baos = new ByteArrayOutputStream(len);
                baos.write(RtpPacket.byte1);
                baos.write(RtpPacket.byte2_handshake);
                baos.write(sequence_number);
                baos.write(timestamp);
                baos.write(RtpPacket.ssrc);
                baos.write(m_payload);
            }

            if (baos != null) {
                return baos.toByteArray();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public static RtpPacket createHandshakePacket(){
        return new RtpPacket(0,0, RtpPacket.TYPE_HANDSHAKE_PAYLOAD, RtpPacket.HANDSHAKE_BODY);
    }

    public static String getBinaryStringFromBytes(byte[] bytes){
        int b_int = (int) Long.parseLong(new String(bytes), 16);
        return Integer.toBinaryString(b_int);
    }

    public static RtpPacket getRtpPacketFromBytes(DatagramPacket packet) throws IOException {
        byte[] data = packet.getData();
        byte[] b1 =Arrays.copyOfRange(data, 0, 2);
        byte[] b2 =Arrays.copyOfRange(data, 2, 4);
        String byte1 = RtpPacket.getBinaryStringFromBytes(b1);
        String byte2 = RtpPacket.getBinaryStringFromBytes(b2);
        byte2 = String.format("%08d", Integer.parseInt(byte2));

        String version = byte1.substring(0,2);
        String padding = byte1.substring(2,3);
        String extension = byte1.substring(3,4);
        String csi_count =  byte1.substring(4,8);

        int market = Integer.parseInt(byte2.substring(0,1));
//        RtpPacket.getBinaryStringFromBytes()
//        int payload_type = Integer.parseInt(byte2.substring(1,8));
        int payload_type = (int) Long.parseLong(byte2.substring(1,8), 2);

        byte[] seq_num_bytes = Arrays.copyOfRange(data, 4, 8);
        int seq_num = (int) Long.parseLong(new String(seq_num_bytes), 16);

        byte[] time_stamp_bytes = Arrays.copyOfRange(data, 8, 16);
        int time_stamp = (int) Long.parseLong(new String(time_stamp_bytes), 16);

        byte[] ssrc_bytes = Arrays.copyOfRange(data, 16, 24);
        int ssrc = (int) Long.parseLong(new String(ssrc_bytes), 16);
        byte[] payload = Arrays.copyOfRange(data, 24, packet.getLength());

        return new RtpPacket(seq_num, time_stamp, payload_type, payload);
    }

}
