package org.example.qq_farm.server;

import org.example.qq_farm.common.Farm;
import org.example.qq_farm.common.Plot;
import org.example.qq_farm.common.Crop;
import org.example.qq_farm.common.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.example.qq_farm.common.PlotState.*;
import static org.example.qq_farm.common.Protocol.*;

public class Server {
    // 全局存放所有农场数据：Map<用户名, 农场对象>
    public static Map<String, Farm> allFarms = new ConcurrentHashMap<>();
    // 全局存放所有在线客户端处理线程：Map<用户名, Handler>
    public static Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("Server started on port 1234...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection from " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastStateUpdate(String farmOwnerName, String message) {
        for (ClientHandler client : onlineClients.values()) {
            if (farmOwnerName.equals(client.getCurrentUser())) {
                client.sendMessage(message);
            }
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String userName;
    private String currentUser;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String getCurrentUser() {
        return currentUser;
    }
    public void sendMessage(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
        try {
            String line = in.readLine();
            if (line != null && line.startsWith(LOGIN)) {
                String[] parts = line.split(" ");
                this.userName = parts[1];

                // 如果是新用户，创建农场；如果是老用户，加载现有农场
                Server.allFarms.putIfAbsent(userName, new Farm());
                Server.onlineClients.put(userName, this);

                this.currentUser = userName;
                System.out.println("User logged in: " + userName);
                sendFullFarmState(userName);
                Farm myFarm = Server.allFarms.get(userName);
                out.println(MONEY + " " + myFarm.getMoney());
            }

            while ((line = in.readLine()) != null) {
                processClientMessage(line);
            }
        } catch (IOException e) {
            System.out.println("Connection error with " + userName);
        } finally {
            if (userName != null) {
                Server.onlineClients.remove(userName);
                System.out.println(userName + " disconnected.");
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processClientMessage(String s) {
        String[] parts = s.split(" ");
        String cmd = parts[0];

        switch (cmd) {
            case PLANT:
                handlePlant(parts);
                break;
            case HARVEST:
                handleHarvest(parts);
                break;
            case STEAL:
                handleSteal(parts);
                break;
            case VISIT:
                handleVisit(parts);
                break;
            case GET_USER_LIST:
                handleGetUserList();
                break;
        }
    }
    private void handlePlant(String[] parts) {
        // PLANT x y cropName
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        String cropName = parts[3];

        if (!userName.equals(currentUser)) {
            out.println(FAIL + " You can only plant on your own farm!");
            return;
        }

        Farm myFarm = Server.allFarms.get(userName);
        Plot plot = myFarm.getPlot(x, y);
        synchronized (plot) {
            if (plot.getState() != EMPTY) {
                out.println(FAIL + " Plot is not empty!");
                return;
            }

            Crop crop = Crop.valueOf(cropName);
            int expense = plantAmount * crop.getPrice();
            int money = myFarm.getMoney();
            if (expense <= money) {
                plot.setState(GROWING);
                plot.setCrop(crop);
                myFarm.setMoney(money - expense);
                String msg1 = STATE_UPDATE + " " + x + " " + y + " GROWING " + cropName;
                String msg2 = MONEY + " " + myFarm.getMoney();
                Server.broadcastStateUpdate(userName, msg1);
                out.println(msg2);

                new Thread(() -> {
                    try {
                        Thread.sleep(10000);
                        synchronized (plot) {
                            if (plot.getState() == GROWING) {
                                plot.setState(RIPE);
                                String ripeMsg = STATE_UPDATE + " " + x + " " + y + " RIPE " + cropName;
                                Server.broadcastStateUpdate(userName, ripeMsg);
                                out.println(MSG + " Your crop at (" + x + "," + y + ") is ripe!");
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                out.println(FAIL + " Plant failed. No enough money!");
            }
        }
    }
    private void handleHarvest(String[] parts) {
        // HARVEST x y
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);

        if (!userName.equals(currentUser)) {
            out.println(FAIL + " You can only harvest your own farm!");
            return;
        }

        Farm myFarm = Server.allFarms.get(userName);
        Plot plot = myFarm.getPlot(x, y);
        synchronized (plot) {
            if (plot.getState() == RIPE) {
                Crop c = plot.getCrop();
                int moneyEarned = c.getPrice() * plot.getAmount();
                plot.setState(EMPTY);
                plot.setCrop(null);
                int money = myFarm.getMoney();
                myFarm.setMoney(money + moneyEarned);
                Server.broadcastStateUpdate(userName, STATE_UPDATE + " " + x + " " + y + " EMPTY null");
                out.println(MONEY + " " + myFarm.getMoney());
                out.println(MSG + " Harvested " + c.name() + ", earned $" + moneyEarned);
            } else {
                out.println(FAIL + " Crop is not ripe yet.");
            }
        }
    }
    private void handleSteal(String[] parts) {
        // STEAL targetUser x y
        String victimName = parts[1];
        int x = Integer.parseInt(parts[2]);
        int y = Integer.parseInt(parts[3]);

        if (userName.equals(victimName)) {
            out.println(FAIL + " Cannot steal from yourself.");
            return;
        }
        if (!Server.allFarms.containsKey(victimName)) {
            out.println(FAIL + " User not found.");
            return;
        }
        ClientHandler victimHandler = Server.onlineClients.get(victimName);
        if (victimHandler != null && victimName.equals(victimHandler.getCurrentUser())) {
            out.println(FAIL + " Steal failed! The owner is guarding the farm.");
            return;
        }

        Farm victimFarm = Server.allFarms.get(victimName);
        Plot plot = victimFarm.getPlot(x, y);
        synchronized (plot) {
            if (plot.getState() != RIPE) {
                out.println(FAIL + " Crop is not ripe.");
                return;
            }
            if (plot.getAmount() <= Protocol.baseAmount) {
                out.println(FAIL + " Not enough crops left to steal.");
                return;
            }

            int stealAmount = (int) (plot.getAmount() * 0.25);
            if (plot.getAmount() - stealAmount < Protocol.baseAmount) {
                stealAmount = plot.getAmount() - Protocol.baseAmount;
            }
            plot.decreaseAmount(stealAmount);
            int moneyStolen = stealAmount * plot.getCrop().getPrice();
            Farm myFarm = Server.allFarms.get(userName);
            int money = myFarm.getMoney();
            myFarm.setMoney(money + moneyStolen);
            out.println(MONEY + " " + myFarm.getMoney());
            out.println(MSG + " Stole " + stealAmount + " " + plot.getCrop().name() + " from " + victimName + "!");
            if (victimHandler != null) {
                victimHandler.sendMessage(MSG + " Someone stole your crops!");
            }
        }
    }
    private void handleGetUserList() {
        // 获取所有在线用户的名字
        StringBuilder sb = new StringBuilder();
        for (String user : Server.onlineClients.keySet()) {
            if (!user.equals(this.userName)) {
                sb.append(user).append(",");
            }
        }
        String userListStr = !sb.isEmpty() ? sb.substring(0, sb.length() - 1) : "";
        out.println(ONLINE_LIST + " " + userListStr);
    }

    private void handleVisit(String[] parts) {
        String targetName = parts[1];
        if (!Server.allFarms.containsKey(targetName)) {
            out.println(MSG + " User " + targetName + " does not exist.");
            return;
        }
        // 更新当前查看的目标
        this.currentUser = targetName;
        // 发送目标农场的所有地块状态给客户端重新绘制
        sendFullFarmState(targetName);
    }

    private void sendFullFarmState(String ownerName) {
        Farm farm = Server.allFarms.get(ownerName);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Plot plot = farm.getPlot(i, j);
                String cropName = (plot.getCrop() == null) ? "null" : plot.getCrop().name();    //获得当前的作物
                out.println(STATE_UPDATE + " " + i + " " + j + " " + plot.getState() + " " + cropName);
            }
        }
    }
}