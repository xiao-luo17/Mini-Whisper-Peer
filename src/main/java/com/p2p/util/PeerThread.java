package com.p2p.util;

import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.p2p.util.StaticResourcesConfig.*;

public class PeerThread extends Thread {

    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private ObjectOutputStream pipedOut;
    private Request request;
    private Response response;

    public InetAddress address;//服务器地址
    public boolean keepCommunicating = true;

    /**
     * connect方法，用服务器IP和请求报文进行连接
     */
    public void connect(String serverIP, Request request, ObjectOutputStream pipedOut) throws IOException {
        isConnect = true;
        this.request = request;
        this.pipedOut = pipedOut;
        address = InetAddress.getByName(serverIP);//获得IP地址
        //根据IP地址和端口创建套接字，端口号为服务器的端口号，以此连接到服务器
        InetSocketAddress serverSocketA = new InetSocketAddress(address, PORT);
        //Socket, TCP连接
        socket = new Socket();
        socket.connect(serverSocketA);
        //获得服务器的输入，输出流，与服务器建立通信
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void setPipedOut(ObjectOutputStream pipedOut) {
        this.pipedOut = pipedOut;
    }

    public synchronized void close() {
        try {
            if (ois != null && oos != null) {
                ois.close();
                oos.close();
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (keepCommunicating) {
            synchronized (this) {
                try {
                    oos.writeObject(request);//被唤醒后向信息服务器发生请求
                    response = (Response) ois.readObject();//从信息服务器接收响应
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    close();
                    System.err.println("[系统异常] 无法连接或与服务器通信出错");
                    return;
                }
                try {
                    pipedOut.writeObject(response);//利用管道将响应发生给主程序
                    if (response.getResponseType() == STRING_TYPE) {
                        String message = response.getMessage();
                        if (message != null && message.equals(request.getRegisterName() + ",你已经从服务器退出！")){
                            System.out.println("[系统消息] 当前用户已退出登录状态 --- 保持PeerThread线程");
                        }
                    }
                    request = null;
                    response = null;
                    wait();//使子线程进入同步等待状态，等待其他程序将其唤醒
                } catch (InterruptedException e) {
                    System.out.println("[系统消息] 线程同步出现错误...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void notifyPeerThread() {
        notify();
    }
}
