package com.p2p.util;

import java.io.Serializable;

public class Request implements Serializable {
    private int requestType;
    private String registerName;
    private String password;
    private int UDPPort;
    private String chatRegisterName;
    private String relayMessage;
    private int threadType;

    public Request(int requestType, String registerName) {
        this.requestType = requestType;
        this.registerName = registerName;
    }

    public Request(int requestType, String registerName, String password, int UDPPort) {
        this(requestType, registerName);
        this.UDPPort = UDPPort;
        this.password = password;
    }

    public Request(int requestType, String registerName, String chatRegisterName) {
        this(requestType, registerName);
        this.chatRegisterName = chatRegisterName;
    }

    public Request(int requestType, String registerName, String chatRegisterName, String relayMessage) {
        this(requestType, registerName);
        this.chatRegisterName = chatRegisterName;
        this.relayMessage = relayMessage;
    }

    public Request(int requestType, String registerName, int threadType) {
        this(requestType, registerName);
        this.threadType = threadType;
    }

    public int getRequestType() {
        return requestType;
    }

    public String getRegisterName() {
        return registerName;
    }

    public int getUDPPort() {
        return UDPPort;
    }

    public String getChatRegisterName() {
        return chatRegisterName;
    }

    public String getPassword() {
        return password;
    }

    public String getRelayMessage() {
        return relayMessage;
    }

    public int getThreadType() {
        return threadType;
    }
}
