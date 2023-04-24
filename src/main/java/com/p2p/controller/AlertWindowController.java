package com.p2p.controller;

import com.p2p.View.ViewAlter;
import com.p2p.util.ChatThread;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

import static com.p2p.util.StaticResourcesConfig.*;

public class AlertWindowController implements Initializable {

    @FXML
    private Button acceptChat;
    @FXML
    private Button refuseChat;
    @FXML
    private Label alertMessageLabel;


    private ViewAlter viewAlter;
    private String alertMessage;

    public void acceptChatButtonAction(ActionEvent actionEvent) {
        chatThread.setAcceptChat(true);
        chatThread.notifyChatThread();
        Stage stage = (Stage) refuseChat.getScene().getWindow();
        stage.close();
    }

    public void refuseChatButtonAction(ActionEvent actionEvent) {
        chatThread.setAcceptChat(false);
        chatThread.notifyChatThread();
        Stage stage = (Stage) refuseChat.getScene().getWindow();
        stage.close();
    }

    public void setViewAlter(ViewAlter viewAlter) {
        this.viewAlter = viewAlter;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
        alertMessageLabel.setText(alertMessage);
    }
}
