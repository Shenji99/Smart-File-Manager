package frontend.controllers;

import backend.data.DataFile;
import backend.FileManager;
import backend.FileObserver;
import backend.exceptions.InvalidFileNameException;
import backend.exceptions.UnexpectedErrorException;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class FilePropertyController implements FileObserver {

    private final MainScreenController mainScreenController;

    private final VBox propertiesWrapper;
    private StackPane nameStackPane;

    private Label nameLabel;
    private Label pathLabel;
    private Label sizeLabel;
    private Label typeLabel;
    private Label changeDateLabel;
    private Label widthHeightLabelValue;
    private Label widthHeightLabel;

    private final ImageView thumbnail;
    private final ImageView playIcon;
    private final MediaView mediaView;


    private MediaPlayer mediaPlayer;
    private final HBox controlsWrapper;
    private final Slider videoSlider;
    private final Button playButton;
    private final Slider volumeSlider;
    private final Label currentTime;
    private final Label videoDuration;
    private final HBox fileTagsBox;

    private InvalidationListener videoSliderListener;
    private InvalidationListener volumeSliderListener;
    private InvalidationListener playButtonListener;


    public FilePropertyController(MainScreenController mainScreenController) {
        this.mainScreenController = mainScreenController;

        //these are all value labels
        this.propertiesWrapper = mainScreenController.getPropertiesWrapper();
        this.thumbnail = mainScreenController.getThumbnail();
        this.playIcon = mainScreenController.getPlayIcon();
        this.mediaView = mainScreenController.getMediaView();
        this.videoSlider = mainScreenController.getVideoSlider();
        this.playButton = mainScreenController.getVideoPauseButton();
        this.volumeSlider = mainScreenController.getVideoVolumeSlider();
        this.controlsWrapper = mainScreenController.getControlsWrapper();
        this.currentTime = mainScreenController.getCurrentTimeLabel();
        this.videoDuration = mainScreenController.getVideoDuratioNLabel();
        this.fileTagsBox = mainScreenController.getFileTagsBox();

        //value and label seperated because it is initially not visible only for images/videos
        this.widthHeightLabel = mainScreenController.getWidthHeightLabel();
        this.widthHeightLabelValue = mainScreenController.getWidthHeightLabelValue();
        this.widthHeightLabel.setVisible(false);
        this.widthHeightLabelValue.setVisible(false);

        this.nameLabel = mainScreenController.getNameLabel();
        this.pathLabel = mainScreenController.getPathLabel();
        this.sizeLabel = mainScreenController.getSizeLabel();
        this.typeLabel = mainScreenController.getTypeLabel();
        this.changeDateLabel = mainScreenController.getDateLabel();

        this.controlsWrapper.setPrefWidth(this.mediaView.getFitWidth());
        this.controlsWrapper.setMaxWidth(this.mediaView.getFitWidth());

        this.playButton.setText("||");
        this.videoSlider.setValue(0);
        this.volumeSlider.setValue(30);

        clearPanel();
        hideMediaPlayerVideo();

        this.playIcon.setVisible(false);

        String path = FileManager.getResourcePath(getClass(), "images", "playIcon.png");
        Image playImg = new Image("file:/"+path);
        this.playIcon.setImage(playImg);

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

        this.playButton.setOnAction(event -> {
            MediaPlayer.Status status = player.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                if (player.getCurrentTime().greaterThanOrEqualTo(player.getTotalDuration())) {
                    player.seek(player.getStartTime());
                    player.play();
                    this.playButton.setText("||");
                    playIcon.setVisible(false);
                }
                player.pause();
                playIcon.setVisible(true);
                playButton.setText(">");
            } else {
                player.pause();
                playIcon.setVisible(true);
                playButton.setText(">");
            }

            if (status == MediaPlayer.Status.HALTED || status == MediaPlayer.Status.STOPPED || status == MediaPlayer.Status.PAUSED) {
                player.play();
                playIcon.setVisible(false);
                playButton.setText("||");
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

    @Override
    public void onFileUpdate(DataFile dataFile) {
        if(this.pathLabel.getText().equals(dataFile.getPath())){
            Platform.runLater(() -> {
                try {
                    updateFileProperties(dataFile, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void updateThumbnail(DataFile f, boolean set) throws IOException, InterruptedException {
        if(set) {
            hideMediaPlayerVideo();
        }
        String newName = f.getPath().replace("\\", "+");
        newName = newName.replace("/", "+");
        newName = newName.replace(":", "+");

        String outpath = FileManager.getResourcePath(getClass(), "thumbnails", newName+".jpg");
        File image = new File(outpath);

        if (!image.exists()) {
            Image img = null;
            switch (f.getType()){
                case "mp4"  : img = FileManager.getInstance().createVideoThumbnail(f, outpath);
                break;
                case "webm" :
                case "ogg"  :
                case "wmv"  :
                case "avi"  :
                case "mov"  :
                case "jpg"  :
                case "jpeg" :
                case "png"  :
                case "webp" :
                case "tiff" :
                case "bmp"  : img = FileManager.getInstance().createImageThumbnail(f, outpath);
                break;
                case "gif"  : img = FileManager.getInstance().createImageGifThumbnail(f);
                break;
            }
            if(set){
                this.thumbnail.setImage(img);
            }
        } else {
            if(set){
                this.thumbnail.setImage(new Image(image.toURI().toString()));
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
        if(nameStackPane != null && nameLabel != null){
            Iterator iter = nameStackPane.getChildren().iterator();
            while(iter.hasNext()){
                Node n = (Node) iter.next();
                if(n instanceof TextField){
                    iter.remove();
                }
            }
            nameLabel.setVisible(true);
        }
    }

    public void updateFileProperties(Event event, DataFile f) throws IOException, InterruptedException {
        if(event instanceof MouseEvent){
            if(((MouseEvent)event).getClickCount() == 2){
                FileManager.getInstance().showFileInExplorer(f.getPath());
            }
        }
        updateFileProperties(f, true);
    }

    public void updateFileProperties(DataFile f, boolean updateThumbnail) throws InterruptedException, IOException {

        updateNameValueLabel(f);
        updateSizeValueLabel(f);
        updatePathValueLabel(f);
        updateTypeValueLabel(f);
        updateChangeDateValueLabel(f);

        boolean hasWidthHeight = false;

        //checking for mimetype image/... video/... would be better but it is more cost intensive
        switch (f.getType()) {
            case "jpg"  :
            case "jpeg" :
            case "gif"  :
            case "png"  :
            case "webp" :
            case "tiff" :
            case "bmp"  :
                hasWidthHeight = true;
                this.playIcon.setVisible(false);
                break;
            case "mp4"  :
                hasWidthHeight = true;
                this.playIcon.setVisible(true);
                break;
            default:
                this.playIcon.setVisible(false);
        }

        if(hasWidthHeight) {
            updateWidthHeightLabel(f);
        }else {
            this.widthHeightLabel.setVisible(false);
            this.widthHeightLabelValue.setVisible(false);
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
        typeLabel.setText(f.getType());
        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(f.getType());
            Clipboard.getSystemClipboard().setContent(content);
        });
        cm.getItems().add(copy);
        typeLabel.setOnContextMenuRequested(contextMenuEvent -> cm.show(typeLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
    }

    private void updatePathValueLabel(DataFile f) {
        Tooltip tooltip = new Tooltip();
        tooltip.setText(f.getPath());
        pathLabel.setTooltip(tooltip);
        pathLabel.setText(f.getPath());

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
        pathLabel.setOnContextMenuRequested(contextMenuEvent -> cm.show(pathLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));

        pathLabel.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 2) {
                FileManager.getInstance().showFileInExplorer(f.getPath());
            }
        });
    }

    private void updateSizeValueLabel(DataFile f) {
        sizeLabel.setText(f.getFormattedSize());
        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(Long.toString(f.getSize()));
            Clipboard.getSystemClipboard().setContent(content);
        });
        cm.getItems().add(copy);
        sizeLabel.setOnContextMenuRequested(contextMenuEvent -> cm.show(sizeLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
    }

    private void updateNameValueLabel(DataFile f) {
        nameLabel.setText(f.getName());
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
        nameLabel.setOnContextMenuRequested(contextMenuEvent -> cm.show(nameLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
        nameLabel.setOnMouseClicked(mouseEvent -> editFileName(mouseEvent, f));
    }

    private void updateWidthHeightLabel(DataFile f) throws IOException, InterruptedException {
        String res = FileManager.getResolution(f);
        if(!res.isEmpty()){
            this.widthHeightLabel.setVisible(true);
            this.widthHeightLabelValue.setVisible(true);
            String[] res2 = res.split("x");
            int width = Integer.parseInt(res2[0].trim());
            int height = Integer.parseInt(res2[1].trim());
            f.setWidth(width);
            f.setHeight(height);
            this.widthHeightLabelValue.setText(res);
        }
    }

    private void editFileName(Event event, DataFile f) {
        if((event instanceof MouseEvent && ((MouseEvent) event).getClickCount() == 2) || event instanceof ActionEvent) {
            nameLabel.setVisible(false);
            TextField textfield = new TextField(nameLabel.getText());
            textfield.setPrefHeight(nameLabel.getHeight() + 10);

            StackPane nameStackPane = (StackPane) nameLabel.getParent();
            this.mainScreenController.setNameStackPane(nameStackPane);
            nameStackPane.getChildren().add(textfield);

            textfield.setOnKeyPressed(event1 -> {
                if(event1.getCode().toString().equals("ENTER")) {
                    String newName = textfield.getText();
                    if(!newName.equals(f.getName())){
                        try{
                            f.rename(newName);
                            nameLabel.setText(newName);
                            this.mainScreenController.hideNameEdit();
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
                        this.mainScreenController.hideNameEdit();
                    }
                }
            });
        }
    }

    private void updateTags(DataFile f) {
        this.fileTagsBox.getChildren().clear();
        if(f.isTagsLoaded()){
            List<String> tags = f.getTags();
            for(String tag: tags){
                StackPane stack = new StackPane();
                Label l = new Label(tag);
                l.setAlignment(Pos.CENTER);
                l.getStyleClass().add("tag");

                stack.getChildren().add(l);
                this.fileTagsBox.getChildren().add(l);
            }
        }else {
            new Thread(() -> {
                try{
                    String cmd = FileManager.getResourcePath(getClass(), "exiftool", "exiftool.exe");
                        cmd += " -L -S -m -q -fast2 -fileName -directory -category -XMP:Subject ";
                        cmd += "\"" + f.getPath() +"\"";
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                    String res = new String(p.getInputStream().readAllBytes());
                    FileManager.getInstance().updateFiles(res);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }).start();

            try{
                this.fileTagsBox.getChildren().clear();
                this.fileTagsBox.getChildren().add(this.mainScreenController.createLodingSpinner(40,40));
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    private void updateTags(DataFile f, boolean set) {
//        try {
//            this.fileTagsBox.getChildren().clear();
//            String cmd = FileManager.getResourcePath(getClass(), "exiftool", "exiftool.exe");
//            cmd += " -S -m -q -fast2 -category ";
//            cmd += "\"" + f.getPath() + "\"";
//
//            Process p = Runtime.getRuntime().exec(cmd);
//            p.waitFor();
//            String res = new String(p.getInputStream().readAllBytes());
//            System.err.println(new String(p.getErrorStream().readAllBytes()));
////            System.out.println(res);
//            if(!res.isEmpty()) {
//                String[] tags = res.split(":")[1].split(",");
//                for(String tag: tags) {
//                    tag = tag.strip();
//                    f.addTag(tag);
//                    if(set) {
//                        StackPane stack = new StackPane();
//                        Label l = new Label(tag);
//                        l.setAlignment(Pos.CENTER);
//                        l.getStyleClass().add("tag");
//
//                        stack.getChildren().add(l);
//                        this.fileTagsBox.getChildren().add(l);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

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
            Media media = new Media(new File(pathLabel.getText()).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            this.mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.play();
            this.playButton.setText("||");
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
                this.playButton.setText("||");
                playIcon.setVisible(false);
            });

            mediaView.setOnMouseClicked(mouseEvent1 -> {
                if(mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    this.playButton.setText(">");
                    playIcon.setVisible(true);
                }else{
                    mediaPlayer.play();
                    this.playButton.setText("||");
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
        this.playButton.setVisible(b);
    }

    public Label getNameLabel() {
        return this.nameLabel;
    }

    public void setNameStackPane(StackPane nameStackPane) {
        this.nameStackPane = nameStackPane;
    }

    public void unhidePanel() {
        this.propertiesWrapper.setVisible(true);
    }

}
