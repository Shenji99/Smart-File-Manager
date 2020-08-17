package frontend.controllers;

import backend.FileManager;
import backend.data.DataFile;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainScreenController implements Initializable {

    @FXML private ListView fileList;
    @FXML private ImageView thumbnail;
    @FXML private ImageView playIcon;
    @FXML private MediaView mediaView;

    @FXML private VBox propertiesWrapper;
    @FXML private HBox controlsWrapper;
    @FXML private Slider videoSlider;
    @FXML private Button pauseVideoButton;
    @FXML private Slider volumeSlider;

    @FXML private Label currentTime;
    @FXML private Label videoDuration;

    @FXML private Label filesAmountLabel;
    @FXML private Label filesTotalSizeLabel;

    @FXML private HBox fileTagsBox;

    @FXML private Label nameLabelValue;
    @FXML private Label sizeLabelValue;
    @FXML private Label pathLabelValue;
    @FXML private Label typeLabelValue;
    @FXML private Label changeDateLabel;
    @FXML private Label widthHeightLabel;
    @FXML private Label widthHeightLabelValue;


    private FileManager fileManager;

    private FileListController fileListController;
    private FilePropertyController filePropertyController;
    private Stage stage;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.fileManager = FileManager.getInstance();
        this.fileListController = new FileListController(this);
        this.filePropertyController = new FilePropertyController(this);

        this.fileManager.addObserver(this.fileListController);
        this.fileManager.addObserver(this.filePropertyController);

    }

    public ImageView getPlayIcon() {
        return this.playIcon;
    }

    @FXML
    public void filePropertiesPaneClicked(MouseEvent event) {
        if(event.getTarget() != this.filePropertyController.getNameLabel()){
            filePropertyController.hideNameEdit();
        }
    }


    public void clearListClicked(ActionEvent actionEvent) {
        this.fileManager.deleteAllFiles();
        this.filePropertyController.clearPanel();
        fileListController.updateView(fileManager.getAllFiles());
    }

    @FXML
    public void listViewItemClicked(Event event) {
        if(event.getTarget() != this.filePropertyController.getNameLabel()) {
            filePropertyController.hideNameEdit();
            this.fileListController.listViewItemClicked(event);
        }
    }


    public void orderBySizeClicked(ActionEvent actionEvent) {
        if(fileManager.getAllFiles() != null) {
            List files = fileManager.getAllFiles();
            fileManager.sort(files, "size");
            fileListController.updateView(files);
        }
    }

    public void orderByTypeClicked(ActionEvent actionEvent) {
        if(fileManager.getAllFiles() != null) {
            List files = fileManager.getAllFiles();
            fileManager.sort(files, "type");
            fileListController.updateView(files);
        }
    }

    public void orderByNameClicked(ActionEvent actionEvent) {
        if(fileManager.getAllFiles() != null) {
            List files = fileManager.getAllFiles();
            fileManager.sort(files, "name");
            fileListController.updateView(files);
        }
    }

    public void orderByDateClicked(ActionEvent actionEvent) {
        if(fileManager.getAllFiles() != null) {
            List files = fileManager.getAllFiles();
            fileManager.sort(files, "changeDate");
            fileListController.updateView(files);
        }
    }

    public void updateFileProperties(Event event, DataFile file) throws IOException, InterruptedException {
        this.filePropertyController.updateFileProperties(event, file);
        this.filePropertyController.unhidePanel();
    }

    public void searchFiles(KeyEvent keyEvent) {
        TextField src = (TextField) keyEvent.getSource();
        if(!src.getText().isEmpty()){
            ArrayList<DataFile> found = (ArrayList<DataFile>) fileManager.searchFiles(src.getText());
            fileListController.updateView(found);
        }else {
            fileListController.updateView(fileManager.getAllFiles());
        }
    }

    public void loadThumbnailsInThread() {
        this.fileManager.loadThumbnailsInThread(this.filePropertyController);
    }

    public void closeApp(ActionEvent actionEvent) {
        Platform.exit();
        FileManager.getInstance().clearThumbnails(FileManager.getResourcePath(getClass(), "thumbnails"));
        System.exit(0);
    }

    public void loadFiles(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Dateien auswählen");
        List<File> files = fileChooser.showOpenMultipleDialog(this.stage);
        if(files != null){
            try {
                fileManager.addChildren(files);
            } catch (IOException e) {
                e.printStackTrace();
            }
            loadThumbnailsInThread();
            loadResolutionsInThread();
            this.fileManager.setTags();
            this.fileListController.updateView(fileManager.getAllFiles());
        }
    }

    public void loadDirectory(ActionEvent actionEvent){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Ordner auswählen");
        File dir = directoryChooser.showDialog(this.stage);
        if(dir != null){
            try {
                fileManager.addChild(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            loadThumbnailsInThread();
            loadResolutionsInThread();
            this.fileManager.setTags();
            this.fileListController.updateView(fileManager.getAllFiles());
        }

    }



    public ListView getFileList() {
        return fileList;
    }

    public void setFileList(ListView fileList) {
        this.fileList = fileList;
    }

    public ImageView getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(ImageView thumbnail) {
        this.thumbnail = thumbnail;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public FileListController getFileListController() {
        return fileListController;
    }

    public void setFileListController(FileListController fileListController) {
        this.fileListController = fileListController;
    }

    public FilePropertyController getFilePropertyController() {
        return filePropertyController;
    }

    public void setFilePropertyController(FilePropertyController filePropertyController) {
        this.filePropertyController = filePropertyController;
    }

    public void setNameStackPane(StackPane pane) {
        this.filePropertyController.setNameStackPane(pane);
    }

    public void hideNameEdit() {
        this.filePropertyController.hideNameEdit();
    }

    public void playVideo(MouseEvent mouseEvent) {
        this.filePropertyController.playVideo(mouseEvent);
    }

    public MediaView getMediaView() {
        return mediaView;
    }

    public Slider getVideoSlider() {
        return videoSlider;
    }

    public Button getVideoPauseButton() {
        return pauseVideoButton;
    }

    public Slider getVideoVolumeSlider() {
        return volumeSlider;
    }

    public HBox getControlsWrapper() {
        return controlsWrapper;
    }

    public Label getCurrentTimeLabel() {
        return currentTime;
    }

    public Label getVideoDuratioNLabel() {
        return videoDuration;
    }

    public VBox getPropertiesWrapper() {
        return this.propertiesWrapper;
    }

    public Label getFilesAmountLabel() {
        return this.filesAmountLabel;
    }

    public Label getFilesTotalSizeLabel() {
        return this.filesTotalSizeLabel;
    }

    public HBox getFileTagsBox() {
        return this.fileTagsBox;
    }

    public Label getNameLabel() {
        return this.nameLabelValue;
    }

    public Label getPathLabel() {
        return this.pathLabelValue;
    }

    public Label getSizeLabel() {
        return this.sizeLabelValue;
    }

    public Label getTypeLabel() {
        return this.typeLabelValue;
    }

    public Label getDateLabel() {
        return this.changeDateLabel;
    }

    public Label getWidthHeightLabel() {
        return this.widthHeightLabel;
    }

    public Label getWidthHeightLabelValue() {
        return this.widthHeightLabelValue;
    }

    public void loadResolutionsInThread() {
        this.fileManager.loadResoulutionsInThread();
    }


    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
