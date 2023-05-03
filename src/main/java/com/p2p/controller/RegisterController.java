package com.p2p.controller;

import com.p2p.View.ViewAlter;
import com.p2p.util.Request;
import com.p2p.util.Response;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import java.io.*;
import java.net.DatagramSocket;
import java.net.URL;
import java.util.ResourceBundle;

import static com.p2p.util.StaticResourcesConfig.*;

public class RegisterController implements Initializable {


    private Request request;
    private Response response;
    private ObjectInputStream pipedIn;
    private ObjectOutputStream pipedOut;
    private boolean firstSetpipde = true;//判断是否为第一次建立管道

    private ViewAlter viewAlter;

    @FXML
    private ImageView topImg;

    @FXML
    private Button ExitButton;

    @FXML
    private Text actiontarget;

    @FXML
    private Text welcometext;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField IPAddress;

    @FXML
    private TextField username;

    @FXML
    void SignUpButtonAction(ActionEvent event) {

        //与服务端进行通信，验证和消息处理，决定返回的页面情况
        actiontarget.setFill(Color.RED);
        String usernameText = username.getText();
        String ipAddressText = IPAddress.getText();
        String passwordFieldText = passwordField.getText();
        serverIP = ipAddressText;
        if (usernameText.length() == 0 || ipAddressText.length() == 0 || passwordFieldText.length() == 0) {
            actiontarget.setText("必须输入注册名、密码和本地IP");
            return;
        }
        try {
            setFirstSetpipde();
            request=new Request(REGISTER_EXIT, usernameText, passwordFieldText, 0);//封装请求，注册UDPPort默认为0
            if(peerThread!=null){
                if(peerThread.isAlive()){//线程已经启动，已与信息服务器连接
                    peerThread.close();//断开与信息服务器的连接
                    //连接信息服务器，pipedOut传递给peerThread，peerThread再将响应写到缓冲器
                    peerThread.connect(ipAddressText,request,pipedOut);
                    peerThread.notifyPeerThread();//将线程唤醒
                }else{
                    peerThread.connect(ipAddressText,request,pipedOut);//连接信息服务器
                    peerThread.start();//启动线程，与信息服务器通信
                }
            }
            //pipedIn读取缓存区的响应
            response=(Response)pipedIn.readObject();
        } catch (Exception ex) {
            actiontarget.setText("[系统异常] 无法连接或与服务器通信出错");
            System.err.println("[系统异常] 无法连接或与服务器通信出错");
            return;
        }
        String message=response.getMessage();
        if(message!=null&&message.equals(request.getRegisterName()+",你已经注册成功")){
            actiontarget.setText(message+",可以密码登录");
            return;
        }
        if(message!=null&&message.equals("|" +request.getRegisterName() + "|" + "已被其他人使用，请使用其他名字注册")){
            actiontarget.setText(message+"或输入密码登录");
            return;
        }
        actiontarget.setText("没有成功，重新注册");
    }

    @FXML
    void SignInButtonAction(ActionEvent actionEvent) {
        //与服务端进行通信，验证和消息处理，决定返回的页面情况
        actiontarget.setFill(Color.RED);
        if (isRegister) {
            actiontarget.setText("不能重复登录!");
            return;
        }
        String usernameText = username.getText();
        String ipAddressText = IPAddress.getText();
        String passwordFieldText = passwordField.getText();
        serverIP = ipAddressText;
        if (usernameText.length() == 0 || ipAddressText.length() == 0 || passwordFieldText.length() == 0) {
            actiontarget.setText("必须输入注册名、密码和本地IP");
            return;
        }

        try {
            setFirstSetpipde();
            request=new Request(SIGN_IN, usernameText, passwordFieldText, UDPPort);//封装请求
            if(peerThread!=null){
                if(peerThread.isAlive()){//线程已经启动，已与信息服务器连接
                    peerThread.close();//断开与信息服务器的连接
                    //连接信息服务器，pipedOut传递给peerThread，peerThread再将响应写到缓冲器
                    peerThread.connect(ipAddressText,request,pipedOut);
                    peerThread.notifyPeerThread();//将线程唤醒
                }else{
                    peerThread.connect(ipAddressText,request,pipedOut);//连接信息服务器
                    peerThread.start();//启动线程，与信息服务器通信
                }
            }
            //pipedIn读取缓存区的响应
            response=(Response)pipedIn.readObject();
        } catch (Exception ex) {
            actiontarget.setText("[系统异常] 无法连接或与服务器通信出错");
            System.err.println("[系统异常] 无法连接或与服务器通信出错");
            return;
        }
        String message=response.getMessage();
        if(message!=null&&message.equals(request.getRegisterName()+",你已经登录成功")){
            registerName = usernameText;
            isRegister=true;
            viewAlter.gotoChatList(socket);
        }
        if(message!=null&&message.equals(request.getRegisterName()+",你的密码错误")){
            actiontarget.setText("密码错误，重新输入");
            return;
        }
        if(message!=null&&message.equals(request.getRegisterName()+",该账号已经在线")){
            actiontarget.setText("该账号已经在线，检查是否重复登录");
            return;
        }
        if(message!=null&&message.equals(request.getRegisterName()+"你还未注册")){
            actiontarget.setText("不存在的用户，请先注册后再登录");
        }
    }

    public void setViewAlter(ViewAlter viewAlter) {
        this.viewAlter = viewAlter;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    void ExitButtonAction(ActionEvent actionEvent) {
        viewAlter.WindowCloseEvent(isPeerConnect);
    }

    public void setTopImg() {
        topImg.setImage(new Image(TOPIMG_URL));
    }

    private void setFirstSetpipde() {
        if (firstSetpipde) {
            try {
                PipedInputStream pipedI = new PipedInputStream();
                PipedOutputStream pipedO = null;
                pipedO = new PipedOutputStream(pipedI);
                //序列化和反序列化
                pipedOut = new ObjectOutputStream(pipedO);
                pipedIn = new ObjectInputStream(pipedI);
                firstSetpipde = false;
                socket = new DatagramSocket();
                UDPPort = socket.getLocalPort();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
