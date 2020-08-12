package frontend.controllers;

import backend.DataFile;
import backend.FileManager;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainScreenController implements Initializable {

    @FXML private SplitPane content;
    @FXML private ListView fileList;
    @FXML private ImageView thumbnail;
    @FXML private ImageView playIcon;
    @FXML private MediaView mediaView;

    private FileManager fileManager;

    private FileListController fileListController;
    private FilePropertyController filePropertyController;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        this.fileManager = FileManager.getInstance();
        this.fileListController = new FileListController(this);
        this.filePropertyController = new FilePropertyController(this);

        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);

    }

    private void addStyleClass(String s, Node...n) {
        for(Node node: n){
            node.getStyleClass().add(s);
        }
    }

    protected void showFileInExplorer(String path) {
        try {
            Runtime.getRuntime().exec("explorer.exe /select," + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void loadThumbnailsInThread() {
        Thread t = new Thread(() -> {
            if(fileManager.getRootFiles() != null){
                List<DataFile> files = fileManager.getAllFiles();
                for(DataFile df: files){
                    try {
                        filePropertyController.updateThumbnail(df, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    protected void removeOldThumbnailsInThread() {
        //TODO
    }


    public ImageView getPlayIcon() {
        return this.playIcon;
    }

    @FXML
    public void filePropertiesPaneClicked(MouseEvent event) {
        if(event.getTarget() != this.filePropertyController.getNameLabel()){
            filePropertyController.hideNameEdit();
        }
        this.fileListController.listViewItemClicked(event);
    }


    @FXML
    public void listViewItemClicked(Event event) {
        if(event.getTarget() != this.filePropertyController.getNameLabel()) {
            filePropertyController.hideNameEdit();
        }
        this.fileListController.listViewItemClicked(event);
    }


    public void orderBySizeClicked(ActionEvent actionEvent) {
        if(fileManager.getAllFiles() != null){
            if(((CheckBox) actionEvent.getSource()).isSelected()){
                List files = fileManager.getAllFiles();
                fileManager.sort(files);
                fileListController.updateView(files);
            }else {
                fileListController.updateView(fileManager.getAllFiles());
            }
        }
    }

    public void updateFileProperties(Event event, DataFile file) throws IOException, InterruptedException, URISyntaxException {
        this.filePropertyController.updateFileProperties(event, file);
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
}
