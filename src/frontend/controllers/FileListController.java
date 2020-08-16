package frontend.controllers;

import backend.data.DataFile;
import backend.FileManager;
import backend.FileObserver;
import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileListController implements FileObserver {

    private final ListView fileList;
    private final FileManager fileManager;

    private final MainScreenController mainScreenController;
    private final Label filesAmountLabel;
    private final Label filesTotalSizeLabel;

    public FileListController(MainScreenController mainScreenController) {
        this.mainScreenController = mainScreenController;

        this.fileList = this.mainScreenController.getFileList();
        this.fileManager = this.mainScreenController.getFileManager();

        this.filesAmountLabel = mainScreenController.getFilesAmountLabel();
        this.filesTotalSizeLabel = mainScreenController.getFilesTotalSizeLabel();

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
                this.fileManager.setTags();
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
    public void onFileUpdate(DataFile dataFile) {

    }

    public void updateView(List<DataFile> files) {
        int fileAmt = 0;
        long totalSize = 0;
        fileList.getItems().clear();
        for (DataFile df : files) {
            if (!df.getType().isEmpty()) {
                fileList.getItems().add(createListItem(df));
                fileAmt++;
                totalSize += df.getSize();
            }
        }
        this.filesAmountLabel.setText(Integer.toString(fileAmt));
        this.filesTotalSizeLabel.setText("("+DataFile.getFormattedSize(totalSize)+")");
    }

    public void updateView(String path) {
        fileList.getItems().clear();
        ArrayList<DataFile> files = fileManager.getFiles(path);
        updateView(files);
    }

    public HBox createListItem(DataFile df) {
        HBox nameWrapper = new HBox();
        nameWrapper.setMaxWidth(200);
        nameWrapper.setPrefWidth(200);
        Label name = new Label(df.getName());
        nameWrapper.getChildren().add(name);

        HBox typeWrapper = new HBox();
        typeWrapper.setMaxWidth(30);
        typeWrapper.setPrefWidth(30);
        Label type = new Label(df.getType());
        typeWrapper.getChildren().add(type);

        HBox sizeWrapper = new HBox();
        sizeWrapper.setMaxWidth(40);
        sizeWrapper.setPrefWidth(40);
        Label size = new Label(df.getFormattedSize());
        sizeWrapper.getChildren().add(size);

        HBox dateWrapper = new HBox();
        dateWrapper.setMaxWidth(70);
        dateWrapper.setPrefWidth(70);
        Label date = new Label(df.formatDate());
        dateWrapper.getChildren().add(date);

        HBox pathWrapper = new HBox();
        Label lpath = new Label(df.getPath());
        pathWrapper.getChildren().add(lpath);

        HBox a = new HBox();
        a.setSpacing(10);

        a.setOnContextMenuRequested(contextMenuEvent -> {
            ContextMenu cm = new ContextMenu();
            MenuItem open = new MenuItem("Datei öffnen");
            open.setOnAction(actionEvent -> {
                FileManager.getInstance().openFile(df.getPath());
            });

            MenuItem openInExplorer = new MenuItem("In Explorer zeigen");
            openInExplorer.setOnAction(actionEvent -> {
                FileManager.getInstance().showFileInExplorer(df.getPath());
            });


            cm.getItems().addAll(open, openInExplorer);
            cm.show(a, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        });

        a.getChildren().addAll(nameWrapper, typeWrapper, sizeWrapper, dateWrapper, pathWrapper);
        return a;
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
