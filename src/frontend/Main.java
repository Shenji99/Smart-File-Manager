package frontend;

import backend.Constants;
import backend.FileManager;
import frontend.controllers.MainScreenController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {



    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/scenes/mainscreen.fxml"));
        Parent root = loader.load();
        MainScreenController controller = loader.getController();
        controller.setStage(primaryStage);
        primaryStage.setTitle("Smart File Manager");
        primaryStage.setScene(new Scene(root, Constants.DEFAULT_WINDOW_WIDHT, Constants.DEFAULT_WINDOW_HEIGHT));
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
