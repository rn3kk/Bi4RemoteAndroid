package com.example.be4remoteandroid.Core;

import static com.example.be4remoteandroid.TCP_Protocol.CONNECTED_TO_RADIO;
import static com.example.be4remoteandroid.TCP_Protocol.DISCONNECTED_FROM_RADIO;

import android.annotation.SuppressLint;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.example.be4remoteandroid.CMD;
import com.example.be4remoteandroid.CommandsDirection;
import com.example.be4remoteandroid.CommandsType;

import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ExtensionValue;
import org.msgpack.value.FloatValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.TimestampValue;
import org.msgpack.value.Value;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

public class TCPListener extends Thread {
    private boolean mTerminate = false;
    private Socket mSocket = null;
    private String mUsername;
    private String mPassword;
    private String mIp = "";
    private int mPort = 0;
    private UdpClient mUdp = null;
    private Messenger mMesanger;


    private PriorityQueue<byte[]> mOutQueue = new PriorityQueue<byte[]>();
    private Semaphore m_mutex = new Semaphore(1);

    private int m_pwr_state = 0;
    private int m_ptt_state = 0;
    private int m_freq = 0;
    private String m_mode = "";


    public TCPListener(String ip, int port_tcp, String username, String password, Messenger m){
        System.out.println("TCPListener()");
        this.mIp = ip;
        this.mPort = port_tcp;
        this.mUsername = username;
        this.mPassword = password;
        this.mMesanger = m;
    }

    public void send_to_server(byte[] data){
        try {
            m_mutex.acquire();
            mOutQueue.add(data.clone());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        m_mutex.release();
    }


    @SuppressLint("DefaultLocale")
    public void run() {
        while (!mTerminate) {
            try {
                mSocket = new Socket(mIp, mPort);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                continue;
            }
//            connected_to_radio();
            try {

//                BufferedInputStream is = new BufferedInputStream(mSocket.getInputStream());
//                ByteBuffer is = new ByteBuffer(mSocket.getInputStream());
                OutputStream os = mSocket.getOutputStream();


                try {
                    byte[] commandAuth = (new CMD(CommandsDirection.TO_SERVER, CommandsType.CMD_AUTORISATION_TOKEN, mUsername + " " + mPassword)).toMsgPackBytes();
                    if (commandAuth != null) {
                        os.write(commandAuth);
                        os.flush();
                        System.out.println("Sended autorisation token");
                    }
                }
                catch (IOException e){
                    e.printStackTrace();
                }

                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(mSocket.getInputStream());
                while (!mTerminate)
                {
                    while(unpacker.hasNext()){
                        // [Advanced] You can check the detailed data format with getNextFormat()
                        // Here is a list of message pack data format: https://github.com/msgpack/msgpack/blob/master/spec.md#overview
                        MessageFormat format = unpacker.getNextFormat();

                        // You can also use unpackValue to extract a value of any type
                        Value v = unpacker.unpackValue();
                        switch (v.getValueType()) {
                            case NIL:
                                v.isNilValue(); // true
                                System.out.println("read nil");
                                break;
                            case BOOLEAN:
                                boolean b = v.asBooleanValue().getBoolean();
                                System.out.println("read boolean: " + b);
                                break;
                            case INTEGER:
                                IntegerValue iv = v.asIntegerValue();
                                if (iv.isInIntRange()) {
                                    int i = iv.toInt();
                                    System.out.println("read int: " + i);
                                }
                                else if (iv.isInLongRange()) {
                                    long l = iv.toLong();
                                    System.out.println("read long: " + l);
                                }
                                else {
                                    BigInteger i = iv.toBigInteger();
                                    System.out.println("read long: " + i);
                                }
                                break;
                            case FLOAT:
                                FloatValue fv = v.asFloatValue();
                                float f = fv.toFloat();   // use as float
                                double d = fv.toDouble(); // use as double
                                System.out.println("read float: " + d);
                                break;
                            case STRING:
                                String s = v.asStringValue().asString();
                                System.out.println("read string: " + s);
                                break;
                            case BINARY:
                                byte[] mb = v.asBinaryValue().asByteArray();
                                System.out.println("read binary: size=" + mb.length);
                                break;
                            case ARRAY:
                                System.out.println("VVVVVVVV");
                                ArrayValue a = v.asArrayValue();
                                int cmd = -1;
                                String value = "";
                                for (Value e : a) {
                                    if(e.isIntegerValue()) {
                                        cmd = e.asIntegerValue().asInt();
                                    }
                                    else if(e.isStringValue()){
                                        value = e.asStringValue().asString();
                                    }

                                }
                                System.out.println(String.format("read array element. CMD:%d, VALUE:%s ", cmd, value));
                                break;
                            case EXTENSION:
                                ExtensionValue ev = v.asExtensionValue();
                                if (ev.isTimestampValue()) {
                                    // Reading the value as a timestamp
                                    TimestampValue ts = ev.asTimestampValue();
                                    Instant tsValue = ts.toInstant();
                                }
                                else {
                                    byte extType = ev.getType();
                                    byte[] extValue = ev.getData();
                                }
                                break;
                        }
                    }
                }
                System.out.println("close socket");
                mSocket.close();
                if (mUdp != null) {
                    mUdp.setTerminate();
                    mUdp.join();
                }
//                if (mUdp != null){
//                    mUdp.setTerminate();
//                    mUdp.join();
//                }
//                mUdp = new UdpClient(mIp, 6992, "username", "password");
//                mUdp.start();

//                while (!mTerminate) {
//                    try {
//                        if(mOutQueue.size() > 0) {
//                            byte[] data = null;
//                            try {
//                                m_mutex.acquire();
//                                data = mOutQueue.peek();
//
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            m_mutex.release();
//                            if(data != null)
//                                os.write(data);
//                        }
////                        if ((read = is.read(buffer)) == -1) break;
////                        if (read > 0) {
////                            String text = new String(buffer, 0, read);
////                            System.out.println(text);
////
////                        }
//                    } catch (IOException e) {
////                        e.printStackTrace();
//                    }
//
//                }


            }
            catch (IOException | InterruptedException e){
                e.printStackTrace();
            }
            disconnected_from_radio();
        }
        System.out.println("TcpListener is end()");
    }

    public void terminate() {
        mTerminate = true;
    }

    private void sendMessageToActivity(Message m){
        if(m == null)
            return;
        try {
            mMesanger.send(m);
        }
        catch (RemoteException r)
        {
            r.printStackTrace();
        }
    }

    private void executeCmd(CMD cmd){
        Message m = Message.obtain();
        m.obj = cmd;
        sendMessageToActivity(m);
    }

    private void connected_to_radio(){
        Message m = Message.obtain();
        m.arg1 = CONNECTED_TO_RADIO;
        sendMessageToActivity(m);
    }

    private void disconnected_from_radio(){
        Message m = Message.obtain();
        m.arg1 = DISCONNECTED_FROM_RADIO;
        sendMessageToActivity(m);
    }


}
