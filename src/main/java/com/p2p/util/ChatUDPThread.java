package com.p2p.util;

import com.p2p.View.ViewAlter;
import com.p2p.controller.ChatWindowController;
import javafx.application.Platform;

import java.io.IOException;
import java.net.*;

import static com.p2p.util.StaticResourcesConfig.*;

public class ChatUDPThread implements Runnable {

    //每个用户进程登录成功时都开启了唯一的一个UDP端口，用于接收所有来自其他人的请求
    //ChatThread类只负责接收来自其他对等方的请求，存放到静态聊天列表，如果有一个新数据报来自一个不在列表中的IP地址和端口，就打开新窗口建立聊天

    //这里获取的是自己的socket
    private DatagramSocket socket;
    private Thread chatChild;
    private DatagramPacket packet;
    private boolean acceptChat = true;
    private ViewAlter viewAlter;

    public ChatUDPThread(DatagramSocket socket, ViewAlter viewAlter) {
        this.socket = socket;
        this.viewAlter = viewAlter;
    }

    public synchronized void start() {
        if (chatChild == null) {
            chatChild = new Thread(this);
            chatChild.start();
        }
    }

    public synchronized void stop() {
        chatChild.interrupt();
        chatChild = null;
    }

    //接收信息子线程的线程体
    public synchronized void run() {
        byte[] buffer = new byte[1024];
        packet = null;
        while (!socket.isClosed()) {
            try {
                for (int i = 0; i < buffer.length; i++)
                    buffer[i] = (byte) 0;
                packet = new DatagramPacket(buffer, buffer.length);//构件数据报
                //接收数据报
                socket.receive(packet);
                //获得发送信息的IP地址和端口号
                //这是对方的IP地址和对方的UDP端口
                InetAddress inetAddress = packet.getAddress();
                int port = packet.getPort();
                //构建发送方的套接字地址
                InetSocketAddress inetsocketAddress = new InetSocketAddress(inetAddress, port);
                if (chatP2PAddress.containsKey(inetsocketAddress)) {
                    //进一步解析数据报中的data信息，连接信息通过chatP2PAddress来检验，这里只用判断是聊天还是退出
                    byte[] dataBuffer = packet.getData();
                    // 当这个聊天用户发来的消息是退出消息时，接收方将这个聊天方移除出正在聊天列表，同时保留window界面并显示消息对方已经退出。
                    // 如果已经包含这个地址，那么直接根据数据报中的IP和端口寻找到对应该用户的聊天窗口。
                    //找到IP映射的name，再找到name映射的stage和controller
                    String name = chatP2PAddress.get(inetsocketAddress);
                    ChatWindowController controller = (ChatWindowController) viewAlter.getControllerByName(name);
                    if (getReceiveData(dataBuffer).equals("exit")) {
                        chatP2PAddress.remove(inetsocketAddress);
                        runLater(controller, name, "[系统消息]: 对方已经结束聊天... 可以关闭窗口或继续请求聊天...");
                        runLater(controller,false);
                        continue;
                    }
                    // 当这个聊天用户发来的消息不是退出消息时，发送给controller，正常显示在聊天窗口。
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            controller.setReceiveData(getReceiveData(dataBuffer));
                            notifyChatUDPThread();
                        }
                    });
                    wait();
                } else {
                    //进一步解析数据报中的data信息，检验是否包含拒绝请求
                    byte[] dataBuffer = packet.getData();
                    ChatWindowController controller = (ChatWindowController) viewAlter.getControllerByName(getReceiveName(dataBuffer));
                    if (getReceiveData(dataBuffer).equals("acceptChat")) {
                        chatP2PAddress.put(inetsocketAddress, getReceiveName(dataBuffer));
                        runLater(controller, getReceiveName(dataBuffer), "[系统消息]: 对方接受了你的聊天请求...");
                        runLater(controller,true);
                        continue;
                    }
                    if (getReceiveData(dataBuffer).equals("refuseChat")) {
                        runLater(controller, getReceiveName(dataBuffer), "[系统消息]: 对方拒绝了你的聊天请求...");
                        runLater(controller,false);
                        continue;
                    }
                    //这里要弹一个弹窗判断要不要接受聊天邀请
                    if (getReceiveData(dataBuffer).equals("chat")) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                viewAlter.createAlertWindow("|" + getReceiveName(dataBuffer) + "| 请求与你聊天");
                            }
                        });
                        //这里线程进入等待，此时不能接受其他用户发来的请求，处理完当前请求后才会去管其他的
                        wait();
                        if (acceptChat) {
                            //加入到聊天列表
                            //接受聊天解析
                            String name = getReceiveName(dataBuffer);
                            chatP2PAddress.put(inetsocketAddress, name);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    viewAlter.createChatWindow(socket, inetsocketAddress, getReceiveName(dataBuffer));
                                    ChatWindowController controller = (ChatWindowController) viewAlter.getControllerByName(getReceiveName(dataBuffer));
                                    controller.setCommunicating(true);
                                    notifyChatUDPThread();
                                }
                            });
                            wait();
                            //发送接受返回包
                            String acceptChat = registerName + "|acceptChat";
                            packet = null;
                            packet = new DatagramPacket(acceptChat.getBytes(), acceptChat.getBytes().length, inetsocketAddress.getAddress(), inetsocketAddress.getPort());//构件数据报
                            socket.send(packet);
                        } else {
                            //发送拒绝返回包
                            String acceptChat = registerName + "|refuseChat";
                            packet = null;
                            packet = new DatagramPacket(acceptChat.getBytes(), acceptChat.getBytes().length, inetsocketAddress.getAddress(), inetsocketAddress.getPort());//构件数据报
                            socket.send(packet);
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("[系统消息] 通过套接字异常退出线程 --- 退出ChatUDPThread线程");
                this.stop();
            }

        }
    }

    public void setAcceptChat(boolean acceptChat) {
        this.acceptChat = acceptChat;
    }

    public String getReceiveName(byte[] dataBuffer) {
        String dataMessage = new String(dataBuffer).trim();
        return dataMessage.subSequence(0, dataMessage.indexOf("|")).toString();
    }

    public String getReceiveData(byte[] dataBuffer) {
        String dataMessage = new String(dataBuffer).trim();
        int index = dataMessage.indexOf("|");
        return dataMessage.subSequence(index + 1, dataMessage.length()).toString();
    }

    public synchronized void notifyChatUDPThread() {
        notify();
    }

    public void runLater(ChatWindowController controller, String name, String message) {
        try {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    controller.setChatOtherName(name);
                    controller.setNoticeMessage(message);
                    notifyChatUDPThread();
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
                    notifyChatUDPThread();
                }
            });
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
