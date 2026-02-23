package org.example.qq_farm.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class Client extends Application {
    private ClientController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("FarmUI.fxml"));
        Parent root = fxmlLoader.load();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText("Welcome to QQ Farm");
        dialog.setContentText("Enter your username:");
        Optional<String> result = dialog.showAndWait();
        String userName = result.orElse("Player" + (int)(Math.random() * 1000));
        if (userName.trim().isEmpty()) {
            userName = "Player" + (int)(Math.random() * 1000);
        }

        controller = fxmlLoader.getController();
        controller.setUserName(userName);
        try {
            Socket socket = new Socket("localhost", 1234);
            controller.setSocket(socket);
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("QQ Farm Client - " + userName);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            controller.closeConnection();
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}