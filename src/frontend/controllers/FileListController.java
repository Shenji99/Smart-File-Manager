package frontend.controllers;

import backend.DataFile;
import backend.FileManager;
import backend.FileObserver;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileListController implements FileObserver {

    private final ListView fileList;
    private final FileManager fileManager;

    private final MainScreenController mainScreenController;

    public FileListController(MainScreenController mainScreenController) {
        this.mainScreenController = mainScreenController;

        this.fileList = this.mainScreenController.getFileList();
        this.fileManager = this.mainScreenController.getFileManager();

        fileList.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        });


        fileList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                for(File f: files) {
                    try {
                        fileManager.addChild(f);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                this.mainScreenController.loadThumbnailsInThread();
                updateView(fileManager.getAllFiles());
                success = true;
            }
            /* let the source know whether the string was successfully
             * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @Override
    public void notify(DataFile dataFile) {

    }

    public void updateView(List<DataFile> files){
        fileList.getItems().clear();
        for(int i = 0; i < files.size(); i++) {
            DataFile df = files.get(i);
            if(!df.getType().isEmpty()){
                fileList.getItems().add(createListItem(df, i));
            }
        }
    }

    public void updateView(String path) {
        fileList.getItems().clear();
        ArrayList<DataFile> files = fileManager.getFiles(path);
        updateView(files);
    }

    public HBox createListItem(DataFile df, int index) {
        HBox nameWrapper = new HBox();
        Label name = new Label(df.getName());
        nameWrapper.setMinWidth(100);
        nameWrapper.getChildren().add(name);

        HBox typeWrapper = new HBox();
        Label type = new Label(df.getType());
        typeWrapper.setMinWidth(100);
        typeWrapper.getChildren().add(type);

        HBox sizeWrapper = new HBox();
        Label size = new Label(df.getFormattedSize());
        sizeWrapper.setMinWidth(100);
        sizeWrapper.getChildren().add(size);

        HBox dateWrapper = new HBox();
        Label date = new Label(df.getChangeDate().toString());
        dateWrapper.setMinWidth(100);
        dateWrapper.getChildren().add(date);

        HBox pathWrapper = new HBox();
        Label lpath = new Label(df.getPath());
        pathWrapper.setMinWidth(100);
        pathWrapper.getChildren().add(lpath);

        HBox a = new HBox();
        a.setSpacing(10);
        a.getChildren().addAll(nameWrapper, typeWrapper, sizeWrapper, dateWrapper, pathWrapper);
        return a;
    }

    private void addStyleClass(String s, Node...n) {
        for(Node node: n){
            node.getStyleClass().add(s);
        }
    }

    public void listViewItemClicked(Event event) {
        try {
            HBox a = (HBox) fileList.getSelectionModel().getSelectedItem();
            Label selectedFilePath = (Label) ((HBox)(a.getChildren().get(4))).getChildren().get(0);
            DataFile f = FileManager.getInstance().findFileByPath(selectedFilePath.getText());
            this.mainScreenController.updateFileProperties(event, f);

            if(event instanceof KeyEvent) {
                KeyEvent e = (KeyEvent) event;
                if(e.getCode() == KeyCode.DELETE){
                    fileManager.deleteFile(f.getPath());
                    updateView(fileManager.getAllFiles());
                }
            }
        }catch (Exception e){
            //can be ignored
        }
    }


}
