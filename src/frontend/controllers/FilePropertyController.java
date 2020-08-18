package frontend.controllers;

import backend.FileManager;
import backend.data.DataFile;
import backend.exceptions.InvalidFileNameException;
import backend.exceptions.UnexpectedErrorException;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class FilePropertyController implements Initializable {

    private MainScreenController mainScreenController;

    private ThreadPoolExecutor executor;

    private MediaPlayer mediaPlayer;
    @FXML public ImageView thumbnail;
    @FXML public MediaView mediaView;
    @FXML public HBox controlsWrapper;
    @FXML public Button pauseVideoButton;
    @FXML public Label currentTime;
    @FXML public Label videoDuration;
    @FXML public Slider videoSlider;
    @FXML public Slider volumeSlider;
    @FXML public ImageView playIcon;

    private StackPane nameStackPane;
    @FXML public Label nameLabelValue;
    @FXML public Label sizeLabelValue;
    @FXML public Label pathLabelValue;
    @FXML public Label widthHeightLabel;
    @FXML public Label widthHeightLabelValue;
    @FXML public Label typeLabelValue;
    @FXML public Label changeDateLabel;

    @FXML public HBox fileTagsBox;
    @FXML public VBox propertiesWrapper;


    private InvalidationListener videoSliderListener;
    private InvalidationListener volumeSliderListener;
    private InvalidationListener playButtonListener;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
//        FileManager.getInstance().addObserver(this);

        this.widthHeightLabel.setVisible(false);
        this.widthHeightLabelValue.setVisible(false);

        this.controlsWrapper.setPrefWidth(this.mediaView.getFitWidth());
        this.controlsWrapper.setMaxWidth(this.mediaView.getFitWidth());

        this.pauseVideoButton.setText("||");
        this.videoSlider.setValue(0);
        this.volumeSlider.setValue(30);

        clearPanel();
        hideMediaPlayerVideo();

        this.playIcon.setVisible(false);

        String path = FileManager.getResourcePath(getClass(), "images", "playIcon.png");
        Image playImg = new Image("file:/"+path);
        this.playIcon.setImage(playImg);

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    }

    private void initializeVideoControl() {
        MediaPlayer player = this.mediaView.getMediaPlayer();

        if(this.videoSliderListener != null){
            this.videoSlider.valueProperty().removeListener(videoSliderListener);
        }

        if(this.playButtonListener != null){
            player.currentTimeProperty().removeListener(playButtonListener);
        }

        if(this.volumeSliderListener != null){
            this.volumeSlider.valueProperty().removeListener(volumeSliderListener);
        }

        player.setVolume(this.volumeSlider.getValue()/100);

        this.pauseVideoButton.setOnAction(event -> {
            MediaPlayer.Status status = player.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                if (player.getCurrentTime().greaterThanOrEqualTo(player.getTotalDuration())) {
                    player.seek(player.getStartTime());
                    player.play();
                    this.pauseVideoButton.setText("||");
                    playIcon.setVisible(false);
                }
                player.pause();
                playIcon.setVisible(true);
                pauseVideoButton.setText(">");
            } else {
                player.pause();
                playIcon.setVisible(true);
                pauseVideoButton.setText(">");
            }

            if (status == MediaPlayer.Status.HALTED || status == MediaPlayer.Status.STOPPED || status == MediaPlayer.Status.PAUSED) {
                player.play();
                playIcon.setVisible(false);
                pauseVideoButton.setText("||");
            }
        });


        this.playButtonListener = observable -> {
            if (player.getCurrentTime().greaterThanOrEqualTo(player.getTotalDuration())) {
                player.pause();
            }
            FilePropertyController.this.updateSliderValues(player);
        };
        player.currentTimeProperty().addListener(this.playButtonListener);

        this.videoSliderListener = observable -> {
            if (videoSlider.isPressed()) {
                player.seek(player.getMedia().getDuration().multiply(videoSlider.getValue() / 100));
            }
        };
        videoSlider.valueProperty().addListener(this.videoSliderListener);

        this.volumeSliderListener = observable -> {
            if(volumeSlider.isPressed()){
                player.setVolume(volumeSlider.getValue()/100);
            }
        };
        this.volumeSlider.valueProperty().addListener(this.volumeSliderListener);

    }


//    @Override
//    public void onFileUpdate(DataFile dataFile) {
//        if(this.pathLabelValue.getText().equals(dataFile.getPath())) {
//            Platform.runLater(() -> {
//                try {
//                    updateFileProperties(dataFile, false);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
//        }
//    }

    private void updateSliderValues(MediaPlayer player) {
        Platform.runLater(() -> {
            if(!volumeSlider.isPressed()){
                this.videoSlider.setValue(player.getCurrentTime().toMillis() / player.getTotalDuration().toMillis() * 100);
            }
            int scnds = (int) player.getCurrentTime().toSeconds()%60;
            int minutes = (int) player.getCurrentTime().toMinutes();

            String minStr, secStr;

            if(minutes < 10){
                minStr = "0"+minutes;
            }else {
                minStr = Integer.toString(minutes);
            }
            if(scnds < 10){
                secStr = "0"+scnds;
            }else {
                secStr = Integer.toString(scnds);
            }
            this.currentTime.setText(minStr+":"+secStr);
        });
    }

    public void updateThumbnail(DataFile f, boolean set) {
        if(set) {
            hideMediaPlayerVideo();
        }
        this.playIcon.setVisible(false);
        this.thumbnail.setImage(null);
        String outpath = FileManager.createThumbnailPath(f);
        File image = new File(outpath);

        try {
            //show the spinner while loading
            if(set) {
                Platform.runLater(this::showThumbnailLoadingSpinner);
            }
            if (!image.exists()) {
                //if thumbnail doesnt exist yet it gets created
                //after that the thumbnail spinner is removed
                executor.submit(() -> {
                    try {
                        Image img = FileManager.createThumbnail(f, outpath);
                        if(set) {
                            Platform.runLater(() -> {
                                hideThumbnailLoadingSpinner();
                                this.thumbnail.setImage(img);
                                String mimetype = FileManager.getDataFileMimeType(f);
                                if (mimetype != null && mimetype.startsWith("video")) {
                                    this.playIcon.setVisible(true);
                                }
                            });
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
            }else if(set){
                Platform.runLater(() -> {
                    hideThumbnailLoadingSpinner();
                    this.thumbnail.setImage(new Image(image.toURI().toString()));
                    if(FileManager.getDataFileMimeType(f).equals("video/mp4")) {
                        this.playIcon.setVisible(true);
                    }
                });
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showThumbnailLoadingSpinner() {
        //get the spinner, if it doesnt exist, a spinner is created
        ImageView spinner = null;
        StackPane wrapper = (StackPane) this.thumbnail.getParent();
        for(Node n: wrapper.getChildren()){
            if(n instanceof ImageView && n.getId() != null){
                if(n.getId().equals("spinner")){
                    spinner = (ImageView) n;
                }
            }
        }
        if(spinner == null) {
            spinner = mainScreenController.createLodingSpinner(50, 50);
            spinner.setId("spinner");
            wrapper.getChildren().add(spinner);
        }
        spinner.setVisible(true);
    }

    public void hideThumbnailLoadingSpinner() {
        StackPane wrapper = (StackPane) this.thumbnail.getParent();
        for(Node n: wrapper.getChildren()){
            if(n instanceof ImageView && n.getId() != null && n.getId().equals("spinner")) {
                n.setVisible(false);
                return;
            }
        }
    }

    private void hideMediaPlayerVideo() {
        setMediaControlVisibility(false);

        if(mediaView.getMediaPlayer() != null) {
            this.mediaView.getMediaPlayer().pause();
        }
        playIcon.setOnMouseClicked(this::playVideo);
    }

    protected void hideNameEdit() {
        if(nameStackPane != null && nameLabelValue != null){
            Iterator iter = nameStackPane.getChildren().iterator();
            while(iter.hasNext()){
                Node n = (Node) iter.next();
                if(n instanceof TextField){
                    iter.remove();
                }
            }
            nameLabelValue.setVisible(true);
        }
    }

    public void updateFileProperties(Event event, DataFile f) {
        if(event instanceof MouseEvent){
            if(((MouseEvent)event).getClickCount() == 2){
                FileManager.getInstance().showFileInExplorer(f.getPath());
            }
        }
        updateFileProperties(f, true);
    }

    public void updateFileProperties(DataFile f, boolean updateThumbnail) {

        updateNameValueLabel(f);
        updateSizeValueLabel(f);
        updatePathValueLabel(f);
        updateTypeValueLabel(f);
        updateChangeDateValueLabel(f);

        String mimeType = FileManager.getDataFileMimeType(f);
        if(mimeType != null){
            this.playIcon.setVisible(mimeType.equals("video/mp4")); //only enable playicon at mp4 vids

            if(mimeType.startsWith("video") || mimeType.startsWith("image")) {
                updateWidthHeightLabel(f);
            }else {
                this.widthHeightLabel.setVisible(false);
                this.widthHeightLabelValue.setVisible(false);
            }
        }

        this.updateTags(f);
        if(updateThumbnail) {
            this.updateThumbnail(f, true);
        }
    }

    private void updateChangeDateValueLabel(DataFile f) {
        changeDateLabel.setText(f.formatDate());
        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(f.getChangeDate().toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        cm.getItems().add(copy);
        changeDateLabel.setOnContextMenuRequested(contextMenuEvent -> cm.show(changeDateLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
    }

    private void updateTypeValueLabel(DataFile f) {
        typeLabelValue.setText(f.getType());
        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(f.getType());
            Clipboard.getSystemClipboard().setContent(content);
        });
        cm.getItems().add(copy);
        typeLabelValue.setOnContextMenuRequested(contextMenuEvent -> cm.show(typeLabelValue, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
    }

    private void updatePathValueLabel(DataFile f) {
        Tooltip tooltip = new Tooltip();
        tooltip.setText(f.getPath());
        pathLabelValue.setTooltip(tooltip);
        pathLabelValue.setText(f.getPath());

        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(f.getPath());
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem open = new MenuItem("open in explorer");
        open.setOnAction(actionEvent -> FileManager.getInstance().showFileInExplorer(f.getPath()));
        cm.getItems().addAll(open, copy);
        pathLabelValue.setOnContextMenuRequested(contextMenuEvent -> cm.show(pathLabelValue, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));

        pathLabelValue.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 2) {
                FileManager.getInstance().showFileInExplorer(f.getPath());
            }
        });
    }

    private void updateSizeValueLabel(DataFile f) {
        sizeLabelValue.setText(f.getFormattedSize());
        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(Long.toString(f.getSize()));
            Clipboard.getSystemClipboard().setContent(content);
        });
        cm.getItems().add(copy);
        sizeLabelValue.setOnContextMenuRequested(contextMenuEvent -> cm.show(sizeLabelValue, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
    }

    private void updateNameValueLabel(DataFile f) {
        nameLabelValue.setText(f.getName());
        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(f.getName());
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(actionEvent -> editFileName(actionEvent, f));

        cm.getItems().addAll(copy, edit);
        nameLabelValue.setOnContextMenuRequested(contextMenuEvent -> cm.show(nameLabelValue, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
        nameLabelValue.setOnMouseClicked(mouseEvent -> editFileName(mouseEvent, f));
    }

    private void updateWidthHeightLabel(DataFile f) {
        this.widthHeightLabelValue.setVisible(false);
        StackPane wrapper = (StackPane) this.widthHeightLabelValue.getParent();
        wrapper.getChildren().add(mainScreenController.createLodingSpinner(20, 20));
        executor.submit(() -> {
            try {
                String res = FileManager.getResolution(f);
                Platform.runLater(() -> {
                    if(!res.isEmpty()) {
                        this.widthHeightLabel.setVisible(true);
                        this.widthHeightLabelValue.setVisible(true);
                        String[] res2 = res.split("x");
                        int width = Integer.parseInt(res2[0].trim());
                        int height = Integer.parseInt(res2[1].trim());
                        f.setWidth(width);
                        f.setHeight(height);
                        this.widthHeightLabelValue.setText(res);
                    }
                    wrapper.getChildren().removeIf(n -> n instanceof ImageView);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void editFileName(Event event, DataFile f) {
        if((event instanceof MouseEvent && ((MouseEvent) event).getClickCount() == 2) || event instanceof ActionEvent) {
            nameLabelValue.setVisible(false);
            TextField textfield = new TextField(nameLabelValue.getText());
            textfield.setPrefHeight(nameLabelValue.getHeight() + 10);

            this.nameStackPane = (StackPane) nameLabelValue.getParent();
            nameStackPane.getChildren().add(textfield);

            textfield.setOnKeyPressed(event1 -> {
                if(event1.getCode() == KeyCode.ENTER) {
                    String newName = textfield.getText();
                    if(!newName.equals(f.getName())){
                        try{
                            f.rename(newName);
                            nameLabelValue.setText(newName);
                            hideNameEdit();
                        }catch (InvalidFileNameException e){
                            //Show error
                            textfield.getStyleClass().add("error-border");
                            Tooltip tooltip = new Tooltip();
                            tooltip.setText(e.getMessage());
                            textfield.setTooltip(tooltip);
                        } catch (UnexpectedErrorException e) {
                            showError(e.getMessage());
                        }
                    }else {
                        hideNameEdit();
                    }
                }
            });
        }
    }

    public void filePropertiesPaneClicked(MouseEvent event) {
        if(event.getTarget() != this.nameLabelValue){
            hideNameEdit();
        }
    }

    private void updateTags(DataFile f) {
        this.fileTagsBox.getChildren().clear();
        if(f.isTagsLoaded()){
            createTags(f);
        }else {

            showTagsLoadingSpinner();


            executor.submit(() -> {
                try{
                    String cmd = FileManager.getResourcePath(getClass(), "exiftool", "exiftool.exe");
                        cmd += " -L -S -m -q -fast2 -fileName -directory -category -XMP:Subject ";
                        cmd += "\"" + f.getPath() +"\"";
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                    String res = new String(p.getInputStream().readAllBytes());
                    FileManager.getInstance().updateFiles(res);
                    Platform.runLater(() -> {
                        removeTagsLoadingSpinner();
                        createTags(f);
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            });

        }
    }

    private void removeTagsLoadingSpinner() {
        for(Node n: this.fileTagsBox.getChildren()){
            if(n instanceof ImageView && n.getId() != null && n.getId().equals("spinner")){
                n.setVisible(false);
                this.fileTagsBox.getChildren().remove(n);
                return;
            }
        }
    }

    private void showTagsLoadingSpinner() {
        try{
            ImageView spinner = null;
            for(Node n: this.fileTagsBox.getChildren()){
                if(n instanceof ImageView && n.getId() != null && n.getId().equals("spinner")){
                    spinner = (ImageView) n;
                    break;
                }
            }

            if(spinner == null){
                spinner = this.mainScreenController.createLodingSpinner(40,40);
                spinner.setId("spinner");
            }
            spinner.setVisible(true);

            this.fileTagsBox.getChildren().clear();
            this.fileTagsBox.getChildren().add(spinner);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTags(DataFile f) {
        List<String> tags = f.getTags();
        for(String tag: tags){
            StackPane stack = new StackPane();
            Label l = new Label(tag);
            l.setAlignment(Pos.CENTER);
            l.getStyleClass().add("tag");

            stack.getChildren().add(l);
            this.fileTagsBox.getChildren().add(l);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Dialog");
        alert.setHeaderText("Error occured");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void playVideo(MouseEvent mouseEvent) {
        try {
            if(this.mediaPlayer != null) {
                this.mediaPlayer.dispose();
            }
            if(this.videoSlider != null){
                this.videoSlider.setValue(0);
            }
            Media media = new Media(new File(pathLabelValue.getText()).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            this.mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.play();
            this.pauseVideoButton.setText("||");
            this.playIcon.setVisible(false);

            setMediaControlVisibility(true);

            initializeVideoControl();

            mediaPlayer.setOnReady(() -> {
                int minDuration = (int) this.mediaPlayer.getTotalDuration().toMinutes();
                int secDuration = (int) (this.mediaPlayer.getTotalDuration().toSeconds()%60);
                String minDurationStr, secDurationStr;

                if(minDuration < 10){
                    minDurationStr = "0"+minDuration;
                }else {
                    minDurationStr = Integer.toString(minDuration);
                }
                if(secDuration < 10){
                    secDurationStr = "0"+secDuration;
                }else {
                    secDurationStr = Integer.toString(secDuration);
                }
                this.videoDuration.setText(minDurationStr+":"+secDurationStr);
            });

            playIcon.setOnMouseClicked(mouseEvent1 -> {
                mediaPlayer.play();
                this.pauseVideoButton.setText("||");
                playIcon.setVisible(false);
            });

            mediaView.setOnMouseClicked(mouseEvent1 -> {
                if(mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    this.pauseVideoButton.setText(">");
                    playIcon.setVisible(true);
                }else{
                    mediaPlayer.play();
                    this.pauseVideoButton.setText("||");
                    playIcon.setVisible(false);
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void clearPanel() {
        this.propertiesWrapper.setVisible(false);
    }

    private void setMediaControlVisibility(boolean b) {
        //resetting the size is important so that the container resizes
        // when the new video/image is bigger or smaller
        if(b) {
            this.mediaView.setFitWidth(400);
            this.mediaView.setFitHeight(300);
        }else {
            //cant be 0 otherwise it is buggy
            this.mediaView.setFitWidth(10);
            this.mediaView.setFitHeight(10);
        }
        this.mediaView.setVisible(b);
        this.videoSlider.setVisible(b);
        this.volumeSlider.setVisible(b);
        this.controlsWrapper.setVisible(b);
        this.pauseVideoButton.setVisible(b);
    }

    public Label getNameLabel() {
        return this.nameLabelValue;
    }

    public void unhidePanel() {
        this.propertiesWrapper.setVisible(true);
    }

    public void setMainScreenController(MainScreenController controller) {
        this.mainScreenController = controller;
    }


}
