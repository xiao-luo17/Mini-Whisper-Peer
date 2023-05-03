package com.p2p.util;

import com.p2p.View.ViewAlter;
import com.p2p.controller.ChatWindowController;
import javafx.application.Platform;
import sun.rmi.transport.tcp.TCPChannel;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.p2p.util.StaticResourcesConfig.*;

public class ChatTCPThread extends Thread {

    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private ObjectOutputStream pipedOut;
    private Request request;
    private Response response;
    private ViewAlter viewAlter;

    public InetAddress address;//服务器地址
    public boolean TCPListening = true;
    private boolean acceptChat = true;
    private boolean firstSend = true;

    public void connect(String serverIP, ViewAlter viewAlter) throws IOException {
        address = InetAddress.getByName(serverIP);//获得IP地址
        //根据IP地址和端口创建套接字，端口号为服务器的端口号，以此连接到服务器
        InetSocketAddress serverSocketA = new InetSocketAddress(address, PORT);
        //Socket, TCP连接
        socket = new Socket();
        socket.connect(serverSocketA);
        //获得服务器的输入，输出流，与服务器建立通信
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
        //完成初次的线程建立
        if (!isTCPConnect) {
            try {
                oos.writeObject(new Request(0, registerName, 2));//向信息服务器发生初始化线程请求
                Response response = (Response) ois.readObject();
                System.out.println("[系统消息] " + response.getMessage());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        isTCPConnect = true;
        this.viewAlter = viewAlter;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void setPipedOut(ObjectOutputStream pipedOut) {
        this.pipedOut = pipedOut;
    }

    public synchronized void close() {
        try {
            isTCPConnect = false;
            if (ois != null && oos != null) {
                ois.close();
                oos.close();
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void run() {
        while (TCPListening) {
            try {
                if (firstSend) {
                    //第一次点击建立进程与类之间通讯
                    PipedInputStream pipedI = new PipedInputStream();
                    PipedOutputStream pipedO = new PipedOutputStream(pipedI);
                    pipedOut = new ObjectOutputStream(pipedO);
                    firstSend = false;
                }
                response = (Response) ois.readObject();//从信息服务器接收响应
                // 构建发送方的套接字地址
                InetSocketAddress inetSocketAddress = response.getChatP2PEndAddress();
                if (chatP2PAddress.containsKey(inetSocketAddress)) {
                    String message = response.getMessage();
                    // 进一步解析数据报中的data信息，连接信息通过chatP2PAddress来检验，这里只用判断是聊天还是退出
                    // 当这个聊天用户发来的消息是退出消息时，接收方将这个聊天方移除出正在聊天列表，同时保留window界面并显示消息对方已经退出。
                    // 如果已经包含这个地址，那么直接根据数据报中的IP和端口寻找到对应该用户的聊天窗口。
                    // 找到IP映射的name，再找到name映射的stage和controller
                    String name = chatP2PAddress.get(inetSocketAddress);
                    ChatWindowController controller = (ChatWindowController) viewAlter.getControllerByName(name);
                    if (getReceiveData(message).equals("exit")) {
                        chatP2PAddress.remove(inetSocketAddress);
                        runLater(controller, name, "[系统消息]: 对方已经结束聊天... 可以关闭窗口或继续请求聊天...");
                        runLater(controller, false);
                        continue;
                    }
                    // 当这个聊天用户发来的消息不是退出消息时，发送给controller，正常显示在聊天窗口。
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            controller.setReceiveData(getReceiveData(message));
                            notifyChatTCPThread();
                        }
                    });
                    wait();
                } else {
                    //进一步解析数据报中的data信息，检验是否包含拒绝请求
                    String message = response.getMessage();
                    ChatWindowController controller = (ChatWindowController) viewAlter.getControllerByName(getReceiveName(message));
                    if (getReceiveData(message).equals("acceptChat")) {
                        chatP2PAddress.put(inetSocketAddress, getReceiveName(message));
                        runLater(controller, getReceiveName(message), "[系统消息]: 对方接受了你的聊天请求...");
                        runLater(controller, true);
                        continue;
                    }
                    if (getReceiveData(message).equals("refuseChat")) {
                        runLater(controller, getReceiveName(message), "[系统消息]: 对方拒绝了你的聊天请求...");
                        runLater(controller, false);
                        continue;
                    }
                    //这里要弹一个弹窗判断要不要接受聊天邀请
                    if (getReceiveData(message).equals("chat")) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                viewAlter.createAlertWindow("|" + getReceiveName(message) + "| 请求与你聊天");
                            }
                        });
                        //这里线程进入等待，此时不能接受其他用户发来的请求，处理完当前请求后才会去管其他的
                        wait();
                        if (acceptChat) {
                            //加入到聊天列表
                            //接受聊天解析
                            String name = getReceiveName(message);
                            chatP2PAddress.put(inetSocketAddress, name);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //这里重载一下create方法支持UDP和TCP
                                    viewAlter.createChatWindow(inetSocketAddress, getReceiveName(message));
                                    ChatWindowController controller = (ChatWindowController) viewAlter.getControllerByName(getReceiveName(message));
                                    controller.setCommunicating(true);
                                    notifyChatTCPThread();
                                }
                            });
                            wait();
                            //发送接受返回包
                            String acceptChat = registerName + "|acceptChat";
                            //因为该peerThread与Register类的peerThread是同一个，在Register类里peerThread
                            //已经启动了，所以这里不需要再次connect
                            request = new Request(CHAT_RELAY, registerName, getReceiveName(message), acceptChat);
                            peerThread.setRequest(request);
                            peerThread.setPipedOut(pipedOut);
                            peerThread.notifyPeerThread();
                        } else {
                            //发送拒绝返回包
                            String acceptChat = registerName + "|refuseChat";
                            //因为该peerThread与Register类的peerThread是同一个，在Register类里peerThread
                            //已经启动了，所以这里不需要再次connect
                            request = new Request(CHAT_RELAY, registerName, getReceiveName(message), acceptChat);
                            peerThread.setRequest(request);
                            peerThread.setPipedOut(pipedOut);
                            peerThread.notifyPeerThread();
                        }
                    }
                }
                pipedOut.writeObject(response);//利用管道将响应发生给主程序
                response = null;
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.err.println("[系统消息] 通过套接字异常退出线程 --- 退出ChatTCPThread线程");
                this.stop();
            }
        }
    }

    public void setAcceptChat(boolean acceptChat) {
        this.acceptChat = acceptChat;
    }

    public String getReceiveName(String message) {
        return message.subSequence(0, message.indexOf("|")).toString();
    }

    public String getReceiveData(String message) {
        int index = message.indexOf("|");
        return message.subSequence(index + 1, message.length()).toString();
    }

    public void runLater(ChatWindowController controller, String name, String message) {
        try {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    controller.setChatOtherName(name);
                    controller.setNoticeMessage(message);
                    notifyChatTCPThread();
                }
            });
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void runLater(ChatWindowController controller, boolean isCommunicating) {
        try {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    controller.setCommunicating(isCommunicating);
                    notifyChatTCPThread();
                }
            });
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void notifyChatTCPThread() {
        notify();
    }
}
