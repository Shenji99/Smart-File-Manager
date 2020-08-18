package frontend.controllers;

import backend.FileManager;
import backend.FileObserver;
import backend.data.DataFile;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class FileListController implements Initializable, FileObserver {

    private MainScreenController mainScreenController;

    @FXML public ListView fileList;
    @FXML public Label filesAmountLabel;
    @FXML public Label filesTotalSizeLabel;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        FileManager.getInstance().addObserver(this);

        this.fileList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        fileList.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        });


        fileList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                try {
                    this.mainScreenController.loadFilesInThread(db.getFiles());
                    success = true;
                }catch (Exception e){
                    e.printStackTrace();
                    success = false;
                }
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

    public void setListViewLoadingSpinner(boolean on) {
        StackPane stack = (StackPane) this.fileList.getParent();
        boolean spinnerExists = false;
        for(Node n: stack.getChildren()) {
            if(n instanceof ImageView) {
                if(on){
                    n.setVisible(true);
                }else {
                    n.setVisible(false);
                }
                spinnerExists = true;
            }
        }
        if(!spinnerExists){
            stack.getChildren().add(this.mainScreenController.createLodingSpinner(100, 100));
        }
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
        ArrayList<DataFile> files = FileManager.getInstance().getFiles(path);
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

        HBox wrapper = new HBox();
        wrapper.setSpacing(10);

        //CONTEXT MENU FOR LIST ITEM
        ContextMenu cm = new ContextMenu();
        MenuItem open = new MenuItem("Datei öffnen");
        MenuItem openInExplorer = new MenuItem("In Explorer zeigen");
        open.setOnAction(actionEvent -> FileManager.getInstance().openFile(df.getPath()));
        openInExplorer.setOnAction(actionEvent -> FileManager.getInstance().showFileInExplorer(df.getPath()));
        cm.getItems().addAll(open, openInExplorer);

        wrapper.setOnContextMenuRequested(contextMenuEvent -> cm.show(wrapper, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));

        wrapper.getChildren().addAll(nameWrapper, typeWrapper, sizeWrapper, dateWrapper, pathWrapper);
        return wrapper;
    }

    public void listViewItemClicked(Event event) {
        FilePropertyController filePropertyController = mainScreenController.getFilePropertyController();
        if(event.getTarget() != filePropertyController.getNameLabel()) {
            filePropertyController.hideNameEdit();
            try {
                HBox a = (HBox) fileList.getSelectionModel().getSelectedItem();
                Label selectedFilePath = (Label) ((HBox)(a.getChildren().get(4))).getChildren().get(0);
                DataFile f = FileManager.getInstance().findFileByPath(selectedFilePath.getText());

                this.mainScreenController.getFilePropertyController().updateFileProperties(event, f);
                this.mainScreenController.getFilePropertyController().unhidePanel();

                if(event instanceof KeyEvent) {
                    KeyEvent e = (KeyEvent) event;
                    if(e.getCode() == KeyCode.DELETE){
                        FileManager.getInstance().deleteFile(f.getPath());
                        updateView(FileManager.getInstance().getAllFiles());
                    }
                }
            }catch (Exception e){
                //can be ignored
            }
        }
    }


    public void orderBySizeClicked(ActionEvent actionEvent) {
        FileManager fileManager = FileManager.getInstance();
        if(fileManager.getAllFiles() != null) {
            List files = fileManager.getAllFiles();
            fileManager.sort(files, "size");
            updateView(files);
        }
    }

    public void orderByTypeClicked(ActionEvent actionEvent) {
        FileManager fileManager = FileManager.getInstance();
        if(fileManager.getAllFiles() != null) {
            List files = fileManager.getAllFiles();
            fileManager.sort(files, "type");
            updateView(files);
        }
    }

    public void orderByNameClicked(ActionEvent actionEvent) {
        FileManager fileManager = FileManager.getInstance();
        if(fileManager.getAllFiles() != null) {
            List files = fileManager.getAllFiles();
            fileManager.sort(files, "name");
            updateView(files);
        }
    }

    public void orderByDateClicked(ActionEvent actionEvent) {
        FileManager fileManager = FileManager.getInstance();
        if(fileManager.getAllFiles() != null) {
            List files = fileManager.getAllFiles();
            fileManager.sort(files, "changeDate");
            updateView(files);
        }
    }

    public void clearListClicked(ActionEvent actionEvent) {
        FileManager fileManager = FileManager.getInstance();
        fileManager.deleteAllFiles();
        fileManager.stopAllBackgroundThreads();
        mainScreenController.getFilePropertyController().clearPanel();
        updateView(fileManager.getAllFiles());
    }

    public void searchFiles(KeyEvent keyEvent) {
        TextField src = (TextField) keyEvent.getSource();
        if(!src.getText().isEmpty()){
            ArrayList<DataFile> found = (ArrayList<DataFile>) FileManager.getInstance().searchFiles(src.getText());
            updateView(found);
        }else {
            updateView(FileManager.getInstance().getAllFiles());
        }
    }

    public void setMainScreenController(MainScreenController controller) {
        this.mainScreenController = controller;
    }

}
