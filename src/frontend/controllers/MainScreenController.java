package frontend.controllers;

import backend.FileManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class MainScreenController implements Initializable {


    @FXML public SplitPane contentPane;
    @FXML public Label appStatusLabel;
    private FileManager fileManager;

    private FileListController fileListController;
    private FilePropertyController filePropertyController;

    private Stage stage;

    private int thumbnailLoadedProgress;
    private int resolutionLoadedProgress;
    private int tagsLoadedProgress;

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
            loadImportedFiles(files);
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
            //methods below need a list... can be improved in the future
            LinkedList<File> files = new LinkedList<>();
            files.add(f);
            loadImportedFiles(files);
        });
        t.start();
    }

    private void loadImportedFiles(List<File> files) {
        Platform.runLater(() -> {

            synchronized (this){
                thumbnailLoadedProgress = 0;
                resolutionLoadedProgress = 0;
                tagsLoadedProgress = 0;
                showStatusLoading(3);
            }

            this.fileListController.setListViewLoadingSpinner(false);
            this.fileManager.loadThumbnailsInThread(files, (args) -> {
                synchronized (this){
                    updateStatus(args);
                }
            });
            this.fileManager.loadResolutionsInThread(files, (args) ->{
                synchronized (this){
                    updateStatus(args);
                }
            });
            this.fileManager.loadTagsInThread(files, (args) ->{
                synchronized (this){
                    updateStatus(args);
                }
            });

            //updates the view with the current settings (includes the newly imported files)
            this.fileListController.searchFiles();

            this.fileListController.setListViewLoadingSpinner(false);
        });
    }

    private void showStatusLoading(int taskAmount) {
        this.appStatusLabel.setText("Loading "+taskAmount+" Tasks...");
        HBox parent = (HBox) this.appStatusLabel.getParent();
        parent.setVisible(true);
        ImageView spinner = null;
        for(Node n: parent.getChildren()){
            if(n instanceof ImageView && n.getId() != null && n.getId().equals("spinner")){
                spinner = (ImageView) n;
                break;
            }
        }
        if(spinner == null){
            spinner = createLodingSpinner(20,20);
            spinner.setId("spinner");
            parent.getChildren().add(spinner);
        }
        spinner.setVisible(true);

    }


    private void updateStatus(Object... args) {
        try {
            String origin = (String) args[0];
            int size = (int) args[1];
            boolean reached = false;
            switch (origin){
                case "thumbnail":
                    thumbnailLoadedProgress++;
                    System.out.println("thumbnail: "+thumbnailLoadedProgress+"/"+size);
                    reached = thumbnailLoadedProgress == size;
                    break;
                case "tags":
                    tagsLoadedProgress++;
                    System.out.println("tags: "+tagsLoadedProgress+"/"+size);
                    reached = tagsLoadedProgress == size;
                    break;
                case "resolution":
                    resolutionLoadedProgress++;
                    System.out.println("resolution: "+resolutionLoadedProgress+"/"+size);
                    reached = resolutionLoadedProgress == size;
                    break;
            }
            if(reached){
                Platform.runLater(() -> {
                    String[] statustext = appStatusLabel.getText().split(" ");
                    if(statustext.length == 3){
                        int remainingTasks = Integer.parseInt(statustext[1])-1;
                        statustext[1] = Integer.toString(remainingTasks);
                        if(remainingTasks == 0){
                            appStatusLabel.setText("");
                            appStatusLabel.getParent().setVisible(false);
                        }else {
                            appStatusLabel.setText(String.join(" ", statustext));
                        }
                    }
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }
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

    public void showError(String message) {
        try{
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error Dialog");
                alert.setHeaderText("Ein Fehler ist aufgetreten!");
                alert.setContentText(message);
                alert.showAndWait();
            });
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SplitPane getContentPane(){
        return this.contentPane;
    }

}
