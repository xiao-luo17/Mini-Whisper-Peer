package com.p2p.controller;

import com.p2p.View.ViewAlter;
import com.p2p.util.PeerThread;
import com.p2p.util.Request;
import com.p2p.util.Response;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.awt.*;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import static com.p2p.util.StaticResourcesConfig.*;

public class ChatListController implements Initializable {

    @FXML
    private ImageView topImg;
    @FXML
    private TextArea NoticeMessage;
    @FXML
    private FlowPane onlineList;
    @FXML
    private FlowPane registerList;

    private ViewAlter viewAlter;

    private Request request;
    private Response response;
    private ObjectInputStream pipedIn;
    private ObjectOutputStream pipedOut;
    private boolean firstGet = true;//判断是否为第一次建立管道
    private DatagramSocket socket;

    public void setViewAlter(ViewAlter viewAlter) {
        this.viewAlter = viewAlter;
    }

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void setTopImg() {
        topImg.setImage(new Image(TOPIMG_URL));
    }

    public void getVectorButtonAction(ActionEvent actionEvent) {
        //获取用户vector
        try {
            if (firstGet) {
                //第一次点击建立进程与类之间通讯
                PipedInputStream pipedI = new PipedInputStream();
                PipedOutputStream pipedO = new PipedOutputStream(pipedI);
                pipedOut = new ObjectOutputStream(pipedO);
                pipedIn = new ObjectInputStream(pipedI);
            }
            //因为该peerThread与Register类的peerThread是同一个，在Register类里peerThread
            //已经启动了，所以这里不需要再次connect
            request = new Request(GET_REGISTER_MAP, registerName);
            peerThread.setRequest(request);
            peerThread.setPipedOut(pipedOut);
            peerThread.notifyPeerThread();
            ;
            response = (Response) pipedIn.readObject();
            //从响应中得到在线的P2P端注册名列表
            Vector<String> registerVectorOnline = response.getAllRegisterOnline();
            Vector<String> registerVectorDone = response.getAllRegisterDone();
            System.out.println("[系统消息] 请求到P2P服务端注册名列表");
            //进行界面onlineList按钮数据刷新
            onlineList.getChildren().clear();
            //进行registerList区块数据刷新
            registerList.getChildren().clear();
            //注册在线用户List的button点击事件
            Iterator<String> iteratorOnline = registerVectorOnline.iterator();
            while (iteratorOnline.hasNext()) {
                String name = iteratorOnline.next();
                Button button = new Button("聊天");
                //注册事件handler
                button.setOnAction(e -> {
                    //这里要进行对目标用户的ip地址返回
                    //进行连接询问
                    //如果选择了自己
                    chatButtonActionEvent(name);
                });
                //将注册好的按钮和事件添加到列表
                button.setStyle("-fx-background-radius: 20px;\n" +
                        "    -fx-border-radius: 20px;\n" +
                        "    -fx-text-fill: #2aa9e0");
                button.setPrefSize(70, 70);

                onlineList.getChildren().add(createUserBlock(name, isOnline(registerVectorOnline, name), button));
                onlineList.setHgap(20);
                onlineList.setVgap(20);
            }
            //所有注册用户List的button点击事件
            Iterator<String> iteratorDone = registerVectorDone.iterator();
            while (iteratorDone.hasNext()) {
                String name = iteratorDone.next();
                Button button = new Button("聊天");
                //注册事件handler
                button.setOnAction(e -> {
                    //目前没有需要做的按钮事件，先空着
                    if(isOnline(registerVectorOnline,name).equals("在线")){
                        chatButtonActionEvent(name);
                    }
                });
                button.setStyle("-fx-background-radius: 20px;\n" +
                        "    -fx-border-radius: 20px;\n" +
                        "    -fx-text-fill: #2aa9e0");
                button.setPrefSize(70, 70);
                registerList.getChildren().add(createUserBlock(name, isOnline(registerVectorOnline, name), button));
                registerList.setVgap(10);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void chatButtonActionEvent(String name) {
        if (name.equals(registerName)) {
            NoticeMessage.setText("不能选择与自己对话!");
        } else {
            request = new Request(GET_OTHER_ADDRESS, registerName, name);
            peerThread.setRequest(request);
            peerThread.setPipedOut(pipedOut);
            peerThread.notifyPeerThread();
            try {
                response = (Response) pipedIn.readObject();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            //这里请求到的是目标用户的UDP开放端口号
            NoticeMessage.setText("请求目标用户... |" + response.getChatP2PEndAddress() + "|");
            viewAlter.createChatWindow(socket, response.getChatP2PEndAddress(), name);
        }
    }

    public void exitButtonAction(ActionEvent actionEvent) {
        if (registerName != null && peerThread.isAlive()) {
            try {
                PipedInputStream pipedI = new PipedInputStream();
                PipedOutputStream pipedO = new PipedOutputStream(pipedI);
                pipedOut = new ObjectOutputStream(pipedO);
                pipedIn = new ObjectInputStream(pipedI);
                request = new Request(EXIT, registerName);
                peerThread.setRequest(request);
                peerThread.setPipedOut(pipedOut);
                peerThread.notifyPeerThread();
                response = (Response) pipedIn.readObject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            isRegister = false;
        }
        socket.close();
        viewAlter.gotoRegister();
    }

    public HBox createUserBlock(String name, String isOnline, Button button) {
        Label labelName = new Label(name);
        labelName.setStyle("-fx-text-fill: white;-fx-font-weight: bold;\n" +
                "    -fx-font-size: 17px;\n" +
                "    -fx-padding: 2px;\n" +
                "    -fx-fit-to-width: true");

        VBox vBox = new VBox();
        vBox.setPrefSize(140, 100);
        vBox.setStyle("-fx-padding: 5px;\n" +
                "    -fx-background-radius: 20px;\n" +
                "    -fx-background-color: #273142");

        Label labelOnline = new Label(isOnline);
        String labelOnlineStyle = "-fx-text-fill: white;\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-padding: 5px;\n" +
                "    -fx-background-radius: 100px;\n";
        if (isOnline.equals("在线")) {
            labelOnline.setStyle(labelOnlineStyle + "-fx-background-color:#35a42e;");
        } else {
            labelOnline.setStyle(labelOnlineStyle + "-fx-background-color:#c31d21;");
        }

        HBox hBox = new HBox();
        hBox.setPrefSize(200, 100);
        hBox.setStyle("-fx-padding: 10px;\n" +
                "    -fx-background-radius: 20px;\n" +
                "    -fx-background-color: #273142");

        vBox.getChildren().add(labelName);
        vBox.getChildren().add(button);
        hBox.getChildren().add(vBox);
        hBox.getChildren().add(labelOnline);
        return hBox;
    }

    public String isOnline(Vector<String> registerVectorOnline, String name) {
        if (registerVectorOnline.contains(name)) {
            return "在线";
        } else {
            return "离线";
        }
    }

}
