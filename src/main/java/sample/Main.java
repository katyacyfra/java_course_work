package sample;
//https://developers.google.com/protocol-buffers/docs/javatutorial

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import sample.statistics.PrintResults;
import sample.statistics.StatAggregator;
import sample.statistics.StatHolder;
import sample.statistics.StatRunner;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BrokenBarrierException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/sample.fxml"));
        primaryStage.setTitle("Server Tester");
        primaryStage.setScene(new Scene(root, 400, 370));
        primaryStage.show();

    }


    public static void main(String[] args) throws IOException, InterruptedException, BrokenBarrierException {
        launch(args);
    }
}
