package com.p2p.util;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Vector;

public class Response implements Serializable {

    private int responseType;
    private String message;
    private Vector<String> allRegisterOnline;
    private Vector<String> allRegisterDone;
    private InetSocketAddress chatP2PEndAddress;
    private String chatOtherRegisterName;
    public  Response(int responseType){
        this.responseType=responseType;
    }
    public Response(int responseType, String message) {
        this.responseType = responseType;
        this.message = message;
    }

    public Response(int responseType, Vector<String> allNameOfRegister, Vector<String> allRegisterDone) {
        this(responseType);
        this.allRegisterOnline = allNameOfRegister;
        this.allRegisterDone = allRegisterDone;
    }

    public Response(int responseType, InetSocketAddress chatP2PEndAddress) {
        this(responseType);
        this.chatP2PEndAddress = chatP2PEndAddress;
    }

    public Response(int responseType, InetSocketAddress chatP2PEndAddress, String chatOtherRegisterName, String message) {
        this(responseType);
        this.chatP2PEndAddress = chatP2PEndAddress;
        this.chatOtherRegisterName = chatOtherRegisterName;
        this.message = message;
    }

    public int getResponseType() {
        return responseType;
    }

    public String getMessage() {
        return message;
    }

    public Vector<String> getAllRegisterOnline() {
        return allRegisterOnline;
    }

    public InetSocketAddress getChatP2PEndAddress() {
        return chatP2PEndAddress;
    }

    public Vector<String> getAllRegisterDone() {
        return allRegisterDone;
    }

    public String getChatOtherRegisterName() {
        return chatOtherRegisterName;
    }
}
