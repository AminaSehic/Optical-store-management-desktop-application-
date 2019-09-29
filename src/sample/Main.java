package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/sample.fxml"));
        Controller ctrl = new Controller();
        loader.setController(ctrl);
        Parent root = loader.load();
        primaryStage.setTitle("Optika");
        primaryStage.setScene(new Scene(root, 400, 200));
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
