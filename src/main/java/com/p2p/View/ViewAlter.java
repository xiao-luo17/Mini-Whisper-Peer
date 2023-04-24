package com.p2p.View;

import com.p2p.controller.AlertWindowController;
import com.p2p.controller.ChatListController;
import com.p2p.controller.ChatWindowController;
import com.p2p.controller.RegisterController;
import com.p2p.util.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import static com.p2p.util.StaticResourcesConfig.*;

public class ViewAlter extends Application {

    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        //开启用户通信进程
        peerThread = new PeerThread();
        //开启用户界面进程
        stage = primaryStage;
        stage.setOnCloseRequest(event -> System.exit(0));
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(new Image("/static/icon.jpg"));
        stage.setTitle("Chat System");
        gotoRegister();
        stage.show();
    }

    /**
     * 跳转到登录界面
     */
    public void gotoRegister() {
        try {
            RegisterController register = (RegisterController) replaceSceneContent(REGISTER_PATH,822,500);
            register.setTopImg();
            register.setViewAlter(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 跳转到主界面
     */
    public void gotoChatList(DatagramSocket socket) {
        try {
            ChatListController chatList = (ChatListController) replaceSceneContent(CHATLIST_PATH,1200,700);
            chatList.setViewAlter(this);
            chatList.setSocket(socket);
            chatList.setTopImg();
            //从列表程序获取到inetSocketAddress，转为inetAddress和port。开启用户聊天进程打开数据包和UPD端口
            chatThread = new ChatThread(socket, this);
            chatThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 弹出聊天窗口
     */
    public void createChatWindow(DatagramSocket socket, InetSocketAddress inetSocketAddress, String stageName) {
        //开启用户聊天窗口，这个得在继承了application接口的类里进行，同时新将经过RegisterController中新建的ChatThread UDP监听线程加入
        try {
            Stage secondStage = new Stage();

            FXMLLoader loader = new FXMLLoader();
            InputStream inputStream = ViewAlter.class.getResourceAsStream(CHATWINDOW_PATH);
            loader.setBuilderFactory(new JavaFXBuilderFactory());
            loader.setLocation(ViewAlter.class.getResource(CHATWINDOW_PATH));

            AnchorPane secondPane = loader.load(inputStream);
            Scene secondScene = new Scene(secondPane, 822, 500);
            secondStage.setScene(secondScene);
            secondStage.show();
            inputStream.close();
            //加入队列
            STAGE.put(stageName, secondStage);

            ChatWindowController chatWindow = loader.getController();
            chatWindow.setChatOtherName(stageName);
            chatWindow.setSocket(socket);
            chatWindow.setInetAddress(inetSocketAddress.getAddress());
            chatWindow.setPort(inetSocketAddress.getPort());
            chatWindow.setViewAlter(this);
            //加入队列
            CONTROLLER.put(stageName, chatWindow);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 弹出询问窗口
     */
    public void createAlertWindow(String alertMessage) {
        //开启询问窗口，这个得在继承了application接口的类里进行
        try {
            Stage secondStage = new Stage();
            secondStage.initStyle(StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader();
            InputStream inputStream = ViewAlter.class.getResourceAsStream(ALERTWINDOW_PATH);
            loader.setBuilderFactory(new JavaFXBuilderFactory());
            loader.setLocation(ViewAlter.class.getResource(ALERTWINDOW_PATH));

            AnchorPane pane = loader.load(inputStream);
            Scene secondScene = new Scene(pane, 300, 150, Color.TRANSPARENT);
            secondStage.setScene(secondScene);
            secondStage.show();

            inputStream.close();

            AlertWindowController alertWindow = loader.getController();
            alertWindow.setAlertMessage(alertMessage);
            alertWindow.setViewAlter(this);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 替换场景
     */
    private Initializable replaceSceneContent(String fxmlURL,int width,int height) throws Exception {

        FXMLLoader loader = new FXMLLoader();
        InputStream inputStream = ViewAlter.class.getResourceAsStream(fxmlURL);
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        loader.setLocation(ViewAlter.class.getResource(fxmlURL));
        try {
            AnchorPane page = loader.load(inputStream);
            page.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            page.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
            Scene scene = new Scene(page, width, height, Color.TRANSPARENT);
            stage.setScene(scene);
            stage.sizeToScene();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            inputStream.close();
        }
        return loader.getController();
    }

    public void WindowCloseEvent(boolean isConnect) {
        if(isConnect){
            peerThread.keepCommunicating = false;
            peerThread.notifyPeerThread();
            peerThread.interrupt();
            peerThread.close();
            peerThread = null;
        }
        stage.close();
    }

    public Object getControllerByName(String name) {
        return CONTROLLER.get(name);
    }
}
