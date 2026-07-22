package com.unibazaar;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/unibazaar/splash.fxml"));
        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(getClass().getResource("/com/unibazaar/styles.css").toExternalForm());
        primaryStage.setTitle("UniBazaar");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
