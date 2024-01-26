package com.example.be4remoteandroid;

import org.json.JSONException;
import org.json.JSONObject;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CMD {


    final public int direction;
    final public  int cmd;
    final public String value;

    final static public String DIRECTION = "d";
    final static public String COMMAND = "c";
    final static public String VALUE = "v";

    public CMD(int direction, int cmd, String value) {
        this.direction = direction;
        this.cmd = cmd;
        this.value = value;
    }

    CMD(int cmd, String value) {
        this.direction = 0;
        this.cmd = cmd;
        this.value = value;
    }

    public String toJsonString(){
        JSONObject obj = new JSONObject();
        try {
            if(direction > 0) {
                obj.put(DIRECTION, direction);
            }
            obj.put(COMMAND, cmd);
            obj.put(VALUE, value);
            return obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] toMsgPackBytes() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packArrayHeader(3).packInt(direction).packInt(cmd).packString(value);
        packer.close();
        return packer.toByteArray();
    }





    public static CMD cmdFromJson(String jsonstr){
        try {
            JSONObject obj = new JSONObject(jsonstr);
            int cmd = obj.getInt(COMMAND);
            String value = obj.getString(VALUE);
            return new CMD(cmd, value);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
