package client.core;

import client.Main;
import client.ui.LimitedTextField;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ADMIN on 10/6/2017.
 */
public class Client extends Thread {

    private String host;
    private int port;
    private String username;

    private ListView<String> userListView;
    private ListView<String> messageListView;
    private LimitedTextField messageTextField;
    private Button sendButton;
    private Button quitButton;

    private boolean shutdown = false;

    private ObservableList<String> messageList = FXCollections.observableArrayList();
    private ObservableList<String> userList = FXCollections.observableArrayList();

    private interface StringCallback {
        void call(String string);
    }

    public Client(String host, int port,
                  ListView<String> messageListView, LimitedTextField messageTextField,
                  Button sendButton, Button quitButton, ListView<String> userListView) {
        this.host = host;
        this.port = port;

        this.messageListView = messageListView;
        this.messageTextField = messageTextField;
        this.sendButton = sendButton;
        this.quitButton = quitButton;
        this.userListView = userListView;


        this.messageListView.setItems(messageList);
        this.userListView.setItems(userList);

        this.start();
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(this.host, this.port);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);


            // 1. Setup listeners
            this.sendButton.setOnAction((actionEvent) -> {
                String message = this.messageTextField.getText().trim();
                if (message != null && !message.isEmpty()) {
                    out.println("DATA " + this.username + ": " + message);
                    this.messageTextField.setText(null);
                }
            });

            messageTextField.setOnKeyPressed((keyEvent) -> {
                if (keyEvent.getCode() == KeyCode.ENTER)  {
                   this.sendButton.fire();
                }
            });

            this.quitButton.setOnAction((actionEvent) -> {
                out.println("QUIT");
                shutdown = true;
                Platform.exit();
            });

            // 2. Show username dialog
            this.getUsername("", (username) -> {
                this.username = username;
                out.println("JOIN " + this.username);
            });

            while (!shutdown) {
                String message = in.readLine();
                System.out.println("Message received: " + message);
                if (message != null && !message.isEmpty()) {
                    if (message.startsWith("J_OK")) {
                        Platform.runLater(() -> {
                            Main.setUsername(this.username);
                        });
                        // Start heartbeat loop
                        checkActiveUsers(out);
                    } else if (message.startsWith("J_ER")) {
                        String data = this.getMessageBody(message, "J_ER".length());
                        String[] errorParts = data.split(":");
                        if (errorParts.length < 2 || errorParts[0].isEmpty() || errorParts[1].isEmpty()) {
                            System.out.println("Invalid response from the server");

                            // Show username dialog
                            this.getUsername("Invalid username!", (username) -> {
                                this.username = username;
                                out.println("JOIN " + this.username);
                            });
                        } else {
                            String errorCode = errorParts[0];
                            String errorMessage = this.getMessageBody(data, errorCode.length() + 1);

                            // Show username dialog
                            this.getUsername(errorMessage, (username) -> {
                                this.username = username;
                                out.println("JOIN " + this.username);
                            });
                        }
                    } else if (message.startsWith("DATA")) {
                        Platform.runLater(() -> {
                            this.messageList.add(this.getMessageBody(message, "DATA".length()));
                        });
                    } else if (message.startsWith("LIST")) {
                        String data = this.getMessageBody(message, "LIST".length());
                        String[] userList = data.split(" ");
                        Platform.runLater(() -> {
                            this.userList.clear();
                            this.userList.addAll(userList);
                        });
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private void checkActiveUsers(PrintWriter out) {
        new Timer().schedule(
            new TimerTask() {

                @Override
                public void run() {
                    if (out != null) {
                        out.println("IMAV");
                    }
                }
            }, 0, Server.ACTIVE_CHECK_INTERVAL);
    }

    private void getUsername(String message, StringCallback callback) {
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Please choose a username");
            dialog.setHeaderText(message);
            dialog.setContentText("Username:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> callback.call(name));
        });
    }

    private String getMessageBody(String msg, int prefixLength) {
        return msg.substring(prefixLength).trim();
    }
}
