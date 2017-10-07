package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static final String WINDOW_PREFIX = "Chat APP";
    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        this.primaryStage.setTitle(WINDOW_PREFIX);
        this.primaryStage.setScene(new Scene(root, 300, 275));
        this.primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void setUsername(String username) {
        primaryStage.setTitle(WINDOW_PREFIX + " [" + username + "]");
    }
}
