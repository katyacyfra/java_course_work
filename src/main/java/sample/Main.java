package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/sample.fxml"));
        primaryStage.setTitle("Server Tester");
        primaryStage.setScene(new Scene(root, 400, 370));
        primaryStage.show();
    }

    public static void main(String[] args)  {
        launch(args);
        /*
        Thread serverThread = new Thread(() -> ServerNonBlocking.main(args));

        serverThread.start();
        try {
            TimeUnit.MILLISECONDS.sleep(200);
            StatRunner runner = new StatRunner(8083,
                    1,
                    1000, 1000, 10000, 0, 3, 100, 3);
        ServerNonBlocking.quit();
        serverThread.join();

        System.out.println("Client" + StatAggregator.getClientTimes());
        System.out.println("Server" + StatAggregator.clientServerTimes);
        System.out.println("Sorting" + StatAggregator.clientSortingTimes);

        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */
    }

}
