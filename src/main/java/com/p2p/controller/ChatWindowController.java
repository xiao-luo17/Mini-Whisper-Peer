package com.p2p.controller;

import com.p2p.View.ViewAlter;
import com.p2p.util.Request;
import com.p2p.util.Response;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.p2p.util.StaticResourcesConfig.*;

public class ChatWindowController {

    @FXML
    private ScrollPane chatMessageScrollPane;
    @FXML
    private Label chatOtherName;
    @FXML
    private TextArea noticeMessage;
    @FXML
    private TextArea messageInput;
    @FXML
    private FlowPane chatMessage;
    @FXML
    private Button exitChat;
    @FXML
    private Button sendChatApplication;
    @FXML
    private Button sendMessage;

    private ViewAlter viewAlter;
    private DatagramSocket socket;
    private String receiveData;
    private boolean isCommunicating = false;
    private ObjectInputStream pipedIn;
    private ObjectOutputStream pipedOut;
    private boolean firstGet = true;//判断是否为第一次建立管道
    private Request request;
    private Response response;

    private InetAddress inetAddress;
    private int UDPPort;

    public void setChatOtherName(String name) {
        this.chatOtherName.setText(name);
    }

    public void setViewAlter(ViewAlter viewAlter) {
        this.viewAlter = viewAlter;
    }

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public void setPort(int UDPPort) {
        this.UDPPort = UDPPort;
    }

    public void setReceiveData(String receiveData) {
        this.receiveData = receiveData;
        chatMessage.getChildren().add(createMessageBlock(receiveData, false));
        chatMessage.setVgap(10);
        chatMessage.heightProperty().addListener(observable -> chatMessageScrollPane.setVvalue(1D));
    }

    public void sendMessageButtonAction(ActionEvent actionEvent) {
        if (messageInput.getText().equals("")) {
            setNoticeMessage("不能发送空白消息!");
            return;
        }
        if (!isCommunicating) {
            setNoticeMessage("聊天通道已关闭，检查是否还未请求或已经关闭聊天!");
            return;
        }
        try {
            String message = registerName + "|" + messageInput.getText();
            chatMessage.getChildren().add(createMessageBlock(messageInput.getText(), true));
            chatMessage.setVgap(10);
            chatMessage.heightProperty().addListener(observable -> chatMessageScrollPane.setVvalue(1D));
            messageInput.clear();
            if (firstGet) {
                //第一次点击建立进程与类之间通讯
                PipedInputStream pipedI = new PipedInputStream();
                PipedOutputStream pipedO = new PipedOutputStream(pipedI);
                pipedOut = new ObjectOutputStream(pipedO);
                pipedIn = new ObjectInputStream(pipedI);
            }
            if (CHAT_TYPE == RELAY_CHAT_TYPE) {
                request = new Request(CHAT_RELAY, registerName, chatOtherName.getText(), message);
                peerThread.setRequest(request);
                peerThread.setPipedOut(pipedOut);
                peerThread.notifyPeerThread();
                response = (Response) pipedIn.readObject();
                setNoticeMessage(response.getMessage());
            }
            if (CHAT_TYPE == UDP_CHAT_TYPE) {
                byte[] buf = message.getBytes();
                DatagramPacket packet = null;
                packet = new DatagramPacket(buf, buf.length, inetAddress, UDPPort);
                //UDP是无连接通信，获得IP地址和端口号即可通信
                socket.send(packet);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendChatApplicationButtonAction(ActionEvent actionEvent) {
        if (isCommunicating) {
            setNoticeMessage("正在聊天，请不要重复发送聊天请求!");
            return;
        }
        try {
            if (firstGet) {
                //第一次点击建立进程与类之间通讯
                PipedInputStream pipedI = new PipedInputStream();
                PipedOutputStream pipedO = new PipedOutputStream(pipedI);
                pipedOut = new ObjectOutputStream(pipedO);
                pipedIn = new ObjectInputStream(pipedI);
            }
            String message = registerName + "|chat";
            if (CHAT_TYPE == RELAY_CHAT_TYPE) {
                request = new Request(CHAT_RELAY, registerName, chatOtherName.getText(), message);
                peerThread.setRequest(request);
                peerThread.setPipedOut(pipedOut);
                peerThread.notifyPeerThread();
                response = (Response) pipedIn.readObject();
            }
            if (CHAT_TYPE == UDP_CHAT_TYPE) {
                byte[] buf = message.getBytes();
                DatagramPacket packet = null;
                packet = new DatagramPacket(buf, buf.length, inetAddress, UDPPort);
                //UDP是无连接通信，获得IP地址和端口号即可通信
                socket.send(packet);
                setNoticeMessage("[系统消息] 正在尝试UDP穿透，如果等待时间过长请选择Relay中继模式");
            }
        } catch (IOException ee) {
            ee.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void exitChatButtonAction(ActionEvent actionEvent) {
        String message = registerName + "|exit";
        if (CHAT_TYPE == RELAY_CHAT_TYPE) {
            try {
                request = new Request(CHAT_RELAY, registerName, chatOtherName.getText(), message);
                peerThread.setRequest(request);
                peerThread.setPipedOut(pipedOut);
                peerThread.notifyPeerThread();
                response = (Response) pipedIn.readObject();
                setNoticeMessage(response.getMessage());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (CHAT_TYPE == UDP_CHAT_TYPE) {
            byte[] buf = message.getBytes();
            DatagramPacket packet = null;
            try {
                packet = new DatagramPacket(buf, buf.length, inetAddress, UDPPort);
                socket.send(packet);
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
        //移除这个聊天用户
        setCommunicating(false);
        chatP2PAddress.remove(new InetSocketAddress(inetAddress, UDPPort));
        setNoticeMessage("[系统消息]: 您已经结束聊天... 可以关闭窗口或继续请求聊天...");
    }

    public void setCommunicating(boolean communicating) {
        this.isCommunicating = communicating;
    }

    public void setNoticeMessage(String message) {
        noticeMessage.clear();
        noticeMessage.setText(message);
    }

    public HBox createMessageBlock(String message, boolean isSelf){
        Label label = new Label(message);
        label.setStyle("-fx-background-color: #222c3c;\n" +
                "    -fx-background-radius: 15px;\n" +
                "    -fx-padding: 8px;\n" +
                "    -fx-text-fill: #4cc9f0;\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-font-size: 15px");
        label.setWrapText(true);
        label.setMaxWidth(500);
        HBox hBox = new HBox();
        if(isSelf){
            Button button = new Button(registerName);
            button.setStyle("-fx-background-radius: 100;\n" +
                    "    -fx-padding: 8px;\n" +
                    "    -fx-text-fill: #4cc9f0;" +
                    "    -fx-font-weight: bold;\n" +
                    "    -fx-background-color: #245783;\n" +
                    "    -fx-font-size: 17px");

            HBox hBoxHead = new HBox();
            hBoxHead.getChildren().add(button);
            hBoxHead.setAlignment(Pos.CENTER);
            hBoxHead.setSpacing(4);
            hBox.setPrefWidth(960);
            hBox.getChildren().add(label);
            hBox.getChildren().add(hBoxHead);
            hBox.setAlignment(Pos.BASELINE_RIGHT);
        }else {
            Button button = new Button(chatOtherName.getText());
            button.setStyle("-fx-background-radius: 100;\n" +
                    "    -fx-padding: 8px;\n" +
                    "    -fx-text-fill: white;" +
                    "    -fx-font-weight: bold;\n" +
                    "    -fx-background-color: rgba(53,164,46,0.62);\n" +
                    "    -fx-font-size: 17px");
            HBox hBoxHead = new HBox();
            hBoxHead.getChildren().add(button);
            hBoxHead.setAlignment(Pos.CENTER);
            hBoxHead.setSpacing(4);
            hBox.setPrefWidth(960);
            hBox.getChildren().add(hBoxHead);
            hBox.getChildren().add(label);
            hBox.setAlignment(Pos.BASELINE_LEFT);
        }
        return hBox;
    }
}
