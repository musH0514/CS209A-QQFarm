package org.example.qq_farm.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.example.qq_farm.common.Crop;
import org.example.qq_farm.common.PlotState;
import static org.example.qq_farm.common.Protocol.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientController {
    @FXML private TextArea displayArea;
    @FXML private GridPane farmGrid;
    @FXML private Label nameLabel;
    @FXML private Label moneyLabel;
    @FXML private Label locationLabel;
    @FXML private ComboBox<String> friendListSelector;

    private String userName;
    private String currentUser;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isRunning = true;
    private Button[][] plotButtons = new Button[4][4];

    public void setUserName(String userName) {
        this.userName = userName;
        this.currentUser = userName;
        nameLabel.setText(userName);
        locationLabel.setText("My Farm");
        displayArea.appendText("[System] Welcome to QQ Farm, " + userName + "!\n");
    }

    public void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println(LOGIN + " " + userName);
        startListening();
    }
    private void startListening() {
        Thread thd = new Thread(() -> {
            try {
                String line;
                while (isRunning && (line = in.readLine()) != null) {
                    processServerMessage(line);
                }
            } catch (IOException e) {
                if (isRunning) {
                    Platform.runLater(() -> displayArea.appendText("[Error] Lost connection.\n"));
                }
            }
        });
        thd.setDaemon(true);
        thd.start();
    }

    private void processServerMessage(String message) {
        Platform.runLater(() -> {
            String[] parts = message.split(" ");
            String cmd = parts[0];

            switch (cmd) {
                case STATE_UPDATE:
                    // STATE_UPDATE x y STATE [CropName]
                    if (parts.length >= 4) {
                        updatePlotUI(Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]),
                                parts[3],
                                parts.length > 4 ? parts[4] : "");
                    }
                    break;
                case MONEY:
                    // MONEY amount
                    if (parts.length > 1) {
                        int money = Integer.parseInt(parts[1]);
                        moneyLabel.setText(String.valueOf(money));
                    }
                    break;
                case ONLINE_LIST:
                    if (parts.length > 1) {
                        // 协议格式: ONLINE_LIST name1,name2,name3
                        String listStr = message.substring(message.indexOf(" ") + 1);
                        String[] names = listStr.split(",");

                        // 更新下拉框内容，保留用户当前的输入（如果有）
                        String currentInput = friendListSelector.getEditor().getText();
                        friendListSelector.setItems(FXCollections.observableArrayList(names));

                        // 如果用户之前正在输入，恢复输入内容
                        if (!currentInput.isEmpty()) {
                            friendListSelector.getEditor().setText(currentInput);
                        }
                    }
                    break;
                case FAIL:
                    // FAIL Reason...
                    String reason = message.substring(message.indexOf(" ") + 1);
                    showAlert("Action Failed", reason);
                    break;
                case MSG:
                    // MSG Content...
                    String msgContent = message.substring(message.indexOf(" ") + 1);
                    displayArea.appendText("[Info] " + msgContent + "\n");
                    break;
            }
        });
    }
    private void updatePlotUI(int x, int y, String stateStr, String cropName) {
        Button btn = plotButtons[x][y];
        PlotState state = PlotState.valueOf(stateStr);
        String style = "-fx-border-color: #5d4037; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5; ";

        // 存储 cropName 到按钮的 userData 中，方便后续逻辑判断是否显示菜单
        btn.getProperties().put("state", state);

        switch (state) {
            case EMPTY:
                btn.setText("EMPTY");
                btn.setStyle(style + "-fx-background-color: #d7ccc8;");
                break;
            case GROWING:
                btn.setText("GROWING\n" + cropName);
                Crop crop = Crop.valueOf(cropName);
                switch(crop) {
                    case RICE:
                        btn.setStyle(style + "-fx-background-color: #a5d6a7;");
                        break;
                    case CORN:
                        btn.setStyle(style + "-fx-background-color: #f1ef8b;");
                        break;
                    case WHEAT:
                        btn.setStyle(style + "-fx-background-color: #d1d0ab;");
                        break;
                    default:
                        btn.setStyle(style + "-fx-background-color: #d7ccc8;");
                        break;
                }
                break;
            case RIPE:
                btn.setText("RIPE\n" + cropName);
                btn.setStyle(style + "-fx-background-color: #f8bb7e; -fx-font-weight: bold;");
                break;
        }
    }
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    public void initialize() {
        friendListSelector.setOnShowing(e -> {
            if (out != null) {
                out.println(GET_USER_LIST);
            }
        });

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Button btn = new Button("EMPTY");
                btn.setPrefSize(90, 90);
                btn.setStyle("-fx-background-color: #d7ccc8; -fx-border-color: #8d6e63; -fx-background-radius: 5;");
                btn.getProperties().put("state", PlotState.EMPTY);

                int finalX = i;
                int finalY = j;
                btn.setOnMouseClicked(e -> handlePlotClick(btn, finalX, finalY, e));

                plotButtons[i][j] = btn;
                farmGrid.add(btn, j, i);  // j是列(x)，i是行(y)，或者反过来取决于你的逻辑，这里统一即可
            }
        }
    }
    private void handlePlotClick(Button btn, int x, int y, javafx.scene.input.MouseEvent event) {
        ContextMenu menu = new ContextMenu();
        boolean isAtHome = userName.equals(currentUser);
        String btnText = btn.getText();

        // 从按钮文本或属性判断状态
        boolean isEmpty = btnText.contains("EMPTY");
        boolean isRipe = btnText.contains("RIPE");
        boolean isGrowing = btnText.contains("GROWING");

        if (isAtHome) {
            if (isEmpty) {
                Menu plantMenu = new Menu("Plant...");
                for (Crop c : Crop.values()) {
                    MenuItem item = new MenuItem(c.getCropName() + " ($" + c.getPrice() + ")");
                    item.setOnAction(e -> out.println(PLANT + " " + x + " " + y + " " + c.name()));
                    plantMenu.getItems().add(item);
                }
                menu.getItems().add(plantMenu);
            } else if (isRipe) {
                MenuItem harvestItem = new MenuItem("Harvest");
                harvestItem.setOnAction(e -> out.println(HARVEST + " " + x + " " + y));
                menu.getItems().add(harvestItem);
            } else if (isGrowing) {
                MenuItem info = new MenuItem("Growing... (Wait)");
                info.setDisable(true);
                menu.getItems().add(info);
            }
        } else {
            if (isRipe) {
                MenuItem stealItem = new MenuItem("Steal Crop");
                stealItem.setStyle("-fx-text-fill: red;");
                // STEAL targetUser x y
                stealItem.setOnAction(e -> out.println(STEAL + " " + currentUser + " " + x + " " + y));
                menu.getItems().add(stealItem);
            } else {
                MenuItem info = new MenuItem("Cannot Steal");
                info.setDisable(true);
                menu.getItems().add(info);
            }
        }

        if (!menu.getItems().isEmpty()) {
            // 关键修复：必须显示菜单
            menu.show(btn, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    @FXML
    public void handleVisit() {
        String friend = friendListSelector.getValue();
        if (friend.isEmpty() || friend.equals(userName)) {
            // 如果输入自己名字，相当于回家
            handleReturnHome();
            return;
        }

        // 发送 VISIT 指令
        // 客户端不直接切界面，而是等待服务器发送 target 的地块数据
        // 但我们需要更新 Label 显示我们正在去哪里
        currentUser = friend;
        locationLabel.setText(friend + "'s Farm");
        out.println(VISIT + " " + friend);

        // 先清空当前的显示（可选，等待服务器刷新覆盖也可以）
        displayArea.appendText("[Action] Visiting " + friend + "...\n");
    }

    @FXML
    public void handleReturnHome() {
        if (userName.equals(currentUser)) {
            return;
        }
        currentUser = userName;
        locationLabel.setText("My Farm");
        out.println(VISIT + " " + userName);
        displayArea.appendText("[Action] Returned home.\n");
    }

    public void closeConnection() {
        isRunning = false;
        try { if (socket != null) socket.close(); } catch (IOException e) { }
    }
}