package frontend.controllers;

import backend.FileManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainScreenController implements Initializable {

    @FXML public SplitPane contentPane;
    private FileManager fileManager;

    private FileListController fileListController;
    private FilePropertyController filePropertyController;

    private Stage stage;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.fileManager = FileManager.getInstance();
    }

    public void closeApp(ActionEvent actionEvent) {
        Platform.exit();
        FileManager.getInstance().stopAllBackgroundThreads();
        FileManager.getInstance().clearThumbnails(FileManager.getResourcePath(getClass(), "thumbnails"));
        System.exit(0);
    }

    public void loadFiles(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Dateien auswählen");
        List<File> files = fileChooser.showOpenMultipleDialog(this.stage);

        this.fileListController.setListViewLoadingSpinner(true);

        if(files != null) {
            loadFilesInThread(files);
        }
    }


    public void loadDirectory(ActionEvent actionEvent){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Ordner auswählen");
        File dir = directoryChooser.showDialog(this.stage);
        if(dir != null) {
            loadFilesInThread(dir);
        }
    }

    public void loadFilesInThread(List<File> files) {
        this.fileListController.setListViewLoadingSpinner(true);
        Thread t = new Thread(() -> {
            try {
                fileManager.addChildren(files);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                this.fileListController.setListViewLoadingSpinner(false);
                this.fileManager.loadThumbnailsInThread();
                this.fileManager.loadResolutionsInThread();
                this.fileManager.loadTagsInThread();
                this.fileListController.updateView(fileManager.getAllFiles());
                this.fileListController.setListViewLoadingSpinner(false);
            });
        });
        t.start();
    }

    public void loadFilesInThread(File f) {
        this.fileListController.setListViewLoadingSpinner(true);
        Thread t = new Thread(() -> {
            try {
                fileManager.addChild(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                this.fileListController.setListViewLoadingSpinner(false);
                this.fileManager.loadThumbnailsInThread();
                this.fileManager.loadResolutionsInThread();
                this.fileManager.loadTagsInThread();
                this.fileListController.updateView(fileManager.getAllFiles());
                this.fileListController.setListViewLoadingSpinner(false);
            });
        });
        t.start();
    }

    public ImageView createLodingSpinner(int width, int height) {
        ImageView spinnerIv = new ImageView();
        spinnerIv.setFitWidth(width);
        spinnerIv.setFitHeight(height);
        String pth = FileManager.getResourcePath(getClass(), "images", "spinner2.gif");
        Image spinner = new Image("file:/"+pth);
        spinnerIv.setImage(spinner);
        return spinnerIv;
    }


    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void showGeneralOptions(ActionEvent actionEvent) {

    }

    public FilePropertyController getFilePropertyController() {
        return this.filePropertyController;
    }

    public void setFileListController(FileListController fileListController) {
        this.fileListController = fileListController;
    }

    public void setFilePropertyController(FilePropertyController filePropertyController) {
        this.filePropertyController = filePropertyController;
    }

    public SplitPane getContentPane(){
        return this.contentPane;
    }
}
