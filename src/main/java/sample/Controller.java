package sample;
//https://stackoverflow.com/questions/13032257/combo-box-javafx-with-fxml

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import sample.statistics.PrintResults;
import sample.statistics.StatAggregator;
import sample.statistics.StatRunner;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.BrokenBarrierException;

public class Controller implements Initializable {


    @FXML // fx:id="fruitCombo"
    private ComboBox<String> architectCombo;

    @FXML
    private TextField numberOfQueries;

    @FXML
    private TextField step;

    @FXML
    private TextField from;

    @FXML
    private TextField to;

    @FXML
    private TextField second;

    @FXML
    private TextField third;

    @FXML
    private ComboBox<String> paramCombo;

    @FXML
    private Label secondlbl;

    @FXML
    private Label thirdlbl;

    @FXML
    private Label result;

    public void runProgram(ActionEvent actionEvent) throws BrokenBarrierException, InterruptedException {

        result.setText("Testing...");
        int architectureType = architectCombo.getSelectionModel().getSelectedIndex();
        String archName = architectCombo.getSelectionModel().getSelectedItem();
        int X = Integer.parseInt(numberOfQueries.getText());
        int paramType = paramCombo.getSelectionModel().getSelectedIndex();
        String paramName = paramCombo.getSelectionModel().getSelectedItem();
        int fromVal = Integer.parseInt(from.getText());
        int toVal = Integer.parseInt(to.getText());
        int stepVal = Integer.parseInt(step.getText());
        int secParam = Integer.parseInt(second.getText());
        int thirdParam = Integer.parseInt(third.getText());

        StatRunner runner = null;
        String secondName ="";
        String thirdName = "";
        switch (paramType) {
            case 0:
                runner = new StatRunner(8081 + architectureType,
                        paramType + 1,
                        stepVal, fromVal, toVal, 0, secParam, thirdParam, X);
                secondName = "M";
                thirdName = "D";
                break;
            case 1:
                runner = new StatRunner(8081 + architectureType,
                        paramType + 1,
                        stepVal, fromVal, toVal, secParam, 0, thirdParam, X);
                secondName = "N";
                thirdName = "D";
                break;
            case 2:
                runner = new StatRunner(8081 + architectureType,
                        paramType + 1,
                        stepVal, fromVal, toVal, secParam, thirdParam, 0, X);
                secondName = "N";
                thirdName = "M";
                break;

        }

        System.out.println("RESULT" + StatAggregator.getClientTimes());
        System.out.println("Server" + StatAggregator.clientServerTimes);
        System.out.println("Sorting" + StatAggregator.clientSortingTimes);

        PrintResults p = new PrintResults(archName + ": " + " metrics: 1 (sorting time)" +
                " X: " + X + " " + secondName +": " + secParam + " " + thirdName + ": " + thirdParam,
                paramName, runner.scale(stepVal, fromVal, toVal), StatAggregator.clientSortingTimes, paramName, 1);

        p = new PrintResults(archName + ": " + " metrics: 2 (time on server)" +
                " X: " + X + " " + secondName +": " + secParam + " " + thirdName + ": " + thirdParam,
                paramName, runner.scale(stepVal, fromVal, toVal), StatAggregator.clientServerTimes, paramName, 2);

        p = new PrintResults(archName + ": " + " metrics: 3 (time on client)" +
                " X: " + X + " " + secondName +": " + secParam + " " + thirdName + ": " + thirdParam,
                paramName, runner.scale(stepVal, fromVal, toVal), StatAggregator.getClientTimes(), paramName, 3);

        StatAggregator.reset();

        result.setText("Ready! See ./results/");

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        architectCombo.getItems().setAll("One thread per client", "Thread pool", "Non-blocking server");
        architectCombo.getSelectionModel().selectFirst();

        paramCombo.getItems().setAll("N (number of elements)", "M (clients in time)", "Delta (query delta)");
        paramCombo.getSelectionModel().selectFirst();

        paramCombo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue != null) {
                    switch(newValue) {
                        case "N (number of elements)":
                            secondlbl.setText("M (clients in time):");
                            thirdlbl.setText("Delta (query delta):");
                            break;
                        case "M (clients in time)":
                            secondlbl.setText("N (number of elements):");
                            thirdlbl.setText("Delta (query delta):");
                            break;
                        case "Delta (query delta)":
                            secondlbl.setText("N (number of elements):");
                            thirdlbl.setText("M (clients in time):");
                            break;
                    }
                }
            }

        });

    }
}

