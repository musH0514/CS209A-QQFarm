package org.example.qq_farm.common;

public class Protocol {
    // 客户端->服务器指令
    public static final String LOGIN = "LOGIN";
    public static final String PLANT = "PLANT";
    public static final String HARVEST = "HARVEST";
    public static final String VISIT = "VISIT";
    public static final String STEAL = "STEAL";
    public static final String GET_USER_LIST = "GET_USER_LIST";
    // 服务器->客户端指令
    public static final String STATE_UPDATE = "STATE_UPDATE";
    public static final String MONEY = "MONEY";
    public static final String FAIL = "FAIL";
    public static final String MSG = "MSG";
    public static final String ONLINE_LIST = "ONLINE_LIST";

    public static final int plantAmount = 100;
    public static final int emptyAmount = 0;
    public static final int baseAmount = 25;
}