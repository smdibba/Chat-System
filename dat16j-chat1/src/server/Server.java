package server;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ADMIN on 10/6/2017.
 */
public class Server extends Application {
    private static final int ACTIVE_CHECK_DELAY = 20 * 1000;
    public static final int ACTIVE_CHECK_INTERVAL = 60 * 1000; // 60 * 1000

    private HashMap<String, ClientData> userMap = new HashMap<>();

    @Override
    public void start(Stage primaryStage) throws IOException {
        ServerSocket serverSocket = new ServerSocket(555);
        System.out.println("Server listening on port:" + 555);
        checkActiveUsers();
        while (true) {
            new ClientThread(serverSocket.accept()).start();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void sendUserList() {
        synchronized (userMap) {
            String message = "LIST ";
            for (Map.Entry<String, ClientData> entry : userMap.entrySet()) {
                message = message + entry.getKey() + " ";
            }

            for (Map.Entry<String, ClientData> entry : userMap.entrySet()) {
                entry.getValue().out.println(message);
            }
        }
    }

    private void checkActiveUsers() {
        new Timer().schedule(
            new TimerTask() {

                @Override
                public void run() {
                    synchronized (userMap) {
                        boolean hasChanged = false;
                        for (Map.Entry<String, ClientData> entry : userMap.entrySet()) {
                            long lastTimestamp = entry.getValue().lastActive;
                            if (System.currentTimeMillis() - lastTimestamp > (ACTIVE_CHECK_INTERVAL + ACTIVE_CHECK_DELAY)) {
                                System.out.println("Kick user: " + entry.getKey());
                                userMap.remove(entry.getKey());
                                hasChanged = true;
                            }
                        }

                        if (hasChanged) {
                            sendUserList();
                        }
                    }
                }
            }, 0, ACTIVE_CHECK_INTERVAL + ACTIVE_CHECK_DELAY);
    }

    private class ClientThread extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        private boolean shutdown = false;

        public ClientThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);


                while (!shutdown) {
                    String message = in.readLine();
                    if (message != null && !message.isEmpty()) {

                        System.out.println("Message received: " + message);

                        if (message.startsWith("JOIN")) {
                            String username = this.getMessageBody(message, "JOIN".length());

                            if (username.length() > 12) {
                                // invalid username, exceeds max length (code: 1)
                                out.println("J_ER 1: Invalid username, exceeds max length");
                                System.out.println("J_ER 1: Invalid username, exceeds max length");
                            } else if (!username.matches("^[a-zA-Z0-9_-]*$")) {
                                // invalid username, reason: contains invalid char (code: 2)
                                out.println("J_ER 2: Invalid username, reason: contains invalid char");
                                System.out.println("J_ER 2: Invalid username, reason: contains invalid char");
                            } else {
                                // valid username syntax
                                synchronized (userMap) {
                                    if (userMap.containsKey(username)) {
                                        // invalid username, reason: already taken (code: 3)
                                        out.println("J_ER 3: Invalid username, reason: already taken");
                                        System.out.println("J_ER 3: Invalid username, reason: already taken");
                                    } else {
                                        // valid username
                                        userMap.put(username, new ClientData(out));
                                        this.username = username;
                                        out.println("J_OK");
                                        System.out.println("J_OK");

                                        sendUserList();
                                    }
                                }
                            }

                        } else if (message.startsWith("DATA")) {
                            System.out.println("DATA msg received" + message);
                            synchronized (userMap) {
                                for (Map.Entry<String, ClientData> entry : userMap.entrySet()) {
                                    entry.getValue().out.println(message);
                                }
                            }
                        } else if (message.startsWith("IMAV")) {
                            if (this.username != null) {
                                synchronized (userMap) {
                                    ClientData currentClientData = userMap.get(username);
                                    currentClientData.markAsActive();
                                }
                            }
                        } else if (message.startsWith("QUIT")) {
                            socket.close();
                            this.shutdown = true;
                        } else {
                            System.out.println("Invalid request from the client");
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                System.out.println("Client shutdown");
                synchronized (userMap) {
                    userMap.remove(this.username);
                    sendUserList();
                }
            }
        }

        private String getMessageBody(String msg, int prefixLength) {
            return msg.substring(prefixLength).trim();
        }
    }
}
