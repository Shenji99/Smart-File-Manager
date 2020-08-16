package frontend;

import backend.FileManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {



    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/scenes/mainscreen.fxml"));
        primaryStage.setTitle("Smart File Manager");
        primaryStage.setScene(new Scene(root, 960, 720));
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            FileManager.getInstance().clearThumbnails(FileManager.getResourcePath(getClass(), "thumbnails"));
            System.exit(0);
        });
    }


    public static void main(String[] args) {
        try{
            launch(args);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
