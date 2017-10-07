package client;

import client.core.Client;
import client.ui.LimitedTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class ChatController {
    @FXML
    private ListView<String> userListView;
    @FXML
    private ListView<String> messageListView;

    @FXML
    private LimitedTextField messageTextField;
    @FXML
    private Button sendButton;
    @FXML
    private Button quitButton;

    public ChatController() {

    }

    @FXML
    public void initialize() {
        messageTextField.setMaxLength(250);

        new Client("localhost", 555, messageListView, messageTextField, sendButton, quitButton, userListView);
    }

}
