package com.p2p.util;

import javafx.stage.Stage;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class StaticResourcesConfig {

    public static String serverIP;
    public static final int PORT = 8000;
    public static boolean isRegister = false;//判断是否已经登录

    //聊天消息处理模式 |relay服务端转发|  |UDP局域网直连|  |TCP局域网直连|  |UDP内网穿透|
    public static int CHAT_TYPE = 1;
    public static int RELAY_CHAT_TYPE = 1;//支持公网转发
    public static int UDP_CHAT_TYPE = 2;//局域网直连
    public static int TCP_CHAT_TYPE = 3;//局域网直连
    public static int UDP_P2P_TYPE = 4;//锥形NAT下公网直连

    //PeerThread线程管理 -- 请求发送线程
    public static PeerThread peerThread;
    public static boolean isPeerConnect = false;

    //ChatTCPThread线程管理 -- relay模式接收线程
    public static ChatTCPThread chatTCPThread;
    public static boolean isTCPConnect = false;

    //ChatUDPThread线程管理 -- UDP模式线程
    public static ChatUDPThread chatUDPThread;
    public static DatagramSocket socket;
    //唯一UDP端口号
    public static int UDPPort;

    //主窗口无边框鼠标拖动
    public static double xOffset = 0;
    public static double yOffset = 0;

    //窗口管理
    public static Map<String, Stage> STAGE = new HashMap<>();
    public static Map<String, Object> CONTROLLER = new HashMap<>();

    //fxml文件路径
    public static String REGISTER_PATH = "/register.fxml";
    public static String CHATLIST_PATH = "/chatList.fxml";
    public static String CHATWINDOW_PATH = "/chatWindow.fxml";
    public static String ALERTWINDOW_PATH = "/alertWindow.fxml";
    public static String TOPIMG_URL = "/static/chat.jpg";

    public static String registerName;

    public static Map<InetSocketAddress, String> chatP2PAddress = new HashMap<>();//存放各个聊天对象的地址

    //请求代码，标识请求方需要的数据类型
    /**
     * 注册请求
     */
    public static final int REGISTER_EXIT = 1;
    /**
     * 获取用户列表请求
     */
    public static final int GET_REGISTER_MAP = 2;
    /**
     * 获取聊天对象IP地址请求
     */
    public static final int GET_OTHER_ADDRESS = 3;
    /**
     * 退出请求
     */
    public static final int EXIT = 4;
    /**
     * 登录请求
     */
    public static final int SIGN_IN = 5;
    /**
     * 服务端转发请求
     */
    public static final int CHAT_RELAY = 6;

    //响应代码，标识接收方需要准备的响应类型
    /**
     * String响应
     */
    public static final int STRING_TYPE = 1;
    /**
     * Vector响应
     */
    public static final int VECTOR_TYPE = 2;
    /**
     * IP地址类响应
     */
    public static final int IP_ADDRESS_TYPE = 3;
    /**
     * 消息转发响应
     */
    public static final int RELAY_MESSAGE_TYPE = 4;
}
