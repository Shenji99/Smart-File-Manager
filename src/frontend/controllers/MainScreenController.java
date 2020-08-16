package frontend.controllers;

import backend.DataFile;
import backend.FileManager;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainScreenController implements Initializable {

    @FXML private SplitPane content;
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


    private FileManager fileManager;

    private FileListController fileListController;
    private FilePropertyController filePropertyController;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.fileManager = FileManager.getInstance();
        this.fileListController = new FileListController(this);
        this.filePropertyController = new FilePropertyController(this);

        this.fileManager.addObserver(this.fileListController);
        this.fileManager.addObserver(this.filePropertyController);

        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
    }

    public ImageView getPlayIcon() {
        return this.playIcon;
    }

    @FXML
    public void filePropertiesPaneClicked(MouseEvent event) {
        if(event.getTarget() != this.filePropertyController.getNameLabel()){
            filePropertyController.hideNameEdit();
        }
//        this.fileListController.listViewItemClicked(event);
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
        }
        this.fileListController.listViewItemClicked(event);
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

    public void updateFileProperties(Event event, DataFile file) throws IOException, InterruptedException, URISyntaxException {
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

    public ListView getFileList() {
        return fileList;
    }

    public void setFileList(ListView fileList) {
        this.fileList = fileList;
    }

    public SplitPane getContent() {
        return content;
    }

    public void setContent(SplitPane content) {
        this.content = content;
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


}
