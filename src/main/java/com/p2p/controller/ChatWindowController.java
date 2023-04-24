package com.p2p.controller;

import com.p2p.View.ViewAlter;
import com.p2p.util.ChatThread;
import com.p2p.util.PeerThread;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import sun.security.x509.IPAddressName;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.p2p.util.StaticResourcesConfig.*;

public class ChatWindowController {

    @FXML
    private Label chatOtherName;
    @FXML
    private TextArea noticeMessage;
    @FXML
    private TextArea messageInput;
    @FXML
    private VBox chatMessage;
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
        Label label = new Label(receiveData);
        label.setStyle("-fx-background-color: rgba(92, 122, 221, 0.77);\n" +
                "    -fx-background-radius: 15px;\n" +
                "    -fx-padding: 8px;\n" +
                "    -fx-text-fill: white;\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-font-size: 15px");
        label.setWrapText(true);
        label.setMaxWidth(500);
        Button buttonOther = new Button(chatOtherName.getText());
        buttonOther.setStyle("-fx-background-radius: 100;\n" +
                "    -fx-padding: 8px;\n" +
                "    -fx-text-fill: white;" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-background-color: rgb(158,207,224);\n" +
                "    -fx-font-size: 17px");
        HBox hBox = new HBox();
        HBox hBoxHead = new HBox();
        hBoxHead.getChildren().add(buttonOther);
        hBoxHead.setAlignment(Pos.CENTER);
        hBoxHead.setSpacing(4);

        hBox.getChildren().add(hBoxHead);
        hBox.getChildren().add(label);
        hBox.setAlignment(Pos.BASELINE_LEFT);
        chatMessage.getChildren().add(hBox);
        chatMessage.setSpacing(10);
    }

    public void sendMessageButtonAction(ActionEvent actionEvent) {
        if (messageInput.getText().equals("")) {
            setNoticeMessage("不能发送空白消息!");
            return;
        }
        if(!isCommunicating){
            setNoticeMessage("聊天通道已关闭，检查是否还未请求或已经关闭聊天!");
            return;
        }
        String message = registerName + "|" + messageInput.getText();
        byte[] buf = message.getBytes();
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(buf, buf.length, inetAddress, UDPPort);
            //UDP是无连接通信，获得IP地址和端口号即可通信
            socket.send(packet);
        } catch (IOException ee) {
            ee.printStackTrace();
        }
        Label label = new Label(messageInput.getText());
        label.setStyle("-fx-background-color: rgba(92, 122, 221, 0.77);\n" +
                "    -fx-background-radius: 15px;\n" +
                "    -fx-padding: 8px;\n" +
                "    -fx-text-fill: white;" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-font-size: 15px");
        label.setWrapText(true);
        label.setMaxWidth(500);
        Button buttonSelf = new Button(registerName);
        buttonSelf.setStyle("-fx-background-radius: 100;\n" +
                "    -fx-padding: 8px;\n" +
                "    -fx-text-fill: white;" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-background-color: #d7b0b0;\n" +
                "    -fx-font-size: 17px");
        HBox hBox = new HBox();
        HBox hBoxHead = new HBox();
        hBoxHead.getChildren().add(buttonSelf);
        hBoxHead.setAlignment(Pos.CENTER);
        hBoxHead.setSpacing(4);

        hBox.getChildren().add(label);
        hBox.getChildren().add(hBoxHead);
        hBox.setAlignment(Pos.BASELINE_RIGHT);
        chatMessage.getChildren().add(hBox);
        chatMessage.setSpacing(10);
        messageInput.clear();
    }

    public void sendChatApplicationButtonAction(ActionEvent actionEvent) {
        String message = registerName + "|chat";
        byte[] buf = message.getBytes();
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(buf, buf.length, inetAddress, UDPPort);
            //UDP是无连接通信，获得IP地址和端口号即可通信
            socket.send(packet);
        } catch (IOException ee) {
            ee.printStackTrace();
        }
    }

    public void exitChatButtonAction(ActionEvent actionEvent) {
        String message = registerName + "|exit";
        byte[] buf = message.getBytes();
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(buf, buf.length, inetAddress, UDPPort);
            socket.send(packet);
        } catch (IOException ee) {
            ee.printStackTrace();
        }
        //移除这个聊天用户
        setCommunicating(false);
        chatP2PAddress.remove(new InetSocketAddress(inetAddress, UDPPort));
        setNoticeMessage("[系统消息]: 您已经结束聊天... 可以关闭窗口或继续请求聊天...");
    }

    public void setCommunicating(boolean communicating){
        this.isCommunicating = communicating;
    }

    public void setNoticeMessage(String message) {
        noticeMessage.clear();
        noticeMessage.setText(message);
    }
}
