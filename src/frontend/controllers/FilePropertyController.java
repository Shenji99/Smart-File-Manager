package frontend.controllers;

import backend.Constants;
import backend.DataFile;
import backend.FileObserver;
import backend.exceptions.InvalidFileNameException;
import backend.exceptions.UnexpectedErrorException;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
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
import java.net.URISyntaxException;
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

        this.nameLabel = mainScreenController.getNameLabel();
        this.pathLabel = mainScreenController.getPathLabel();
        this.sizeLabel = mainScreenController.getSizeLabel();
        this.typeLabel = mainScreenController.getTypeLabel();
        this.changeDateLabel = mainScreenController.getDateLabel();

        this.controlsWrapper.setMaxWidth(this.mediaView.getFitWidth());
        this.controlsWrapper.setMaxHeight(this.mediaView.getFitHeight());

        this.playButton.setText("||");
        this.videoSlider.setValue(0);
        this.volumeSlider.setValue(30);
        hideMediaPlayerVideo();

        this.playIcon.setVisible(false);

        String path = Constants.getResourcePath(getClass(), "images", "playIcon.png");
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
        Platform.runLater(() -> {
            try {
                if(this.pathLabel.getText().equals(dataFile.getPath())){
                    updateFileProperties(dataFile, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    void updateThumbnail(DataFile f, boolean set) throws IOException, InterruptedException {
        if(set) {
            hideMediaPlayerVideo();
        }
        String newName = f.getPath().replace("\\", "+");
        newName = newName.replace("/", "+");
        newName = newName.replace(":", "+");

        String outpath = Constants.getResourcePath(getClass(), "thumbnails", newName+".jpg");
        File image = new File(outpath);

        if (!image.exists()) {
            Image img = null;
            switch (f.getType()){
                case "webm":
                case "mp4": img = createVideoThumbnail(f, outpath); break;
                case "jpg":
                case "png":
                case "webp": img = createImageThumbnail(f, outpath); break;
                case "gif": img = createImageGifThumbnail(f); break;
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

    private Image createImageGifThumbnail(DataFile f) {
        return new Image(new File(f.getPath()).toURI().toString());
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

    private Image createImageThumbnail(DataFile f, String outpath) throws IOException, InterruptedException {
        String screenshotCmd = "ffmpeg -i \"" + f.getPath() + "\" -vf scale=320:-1 \"" + outpath + "\"";
        Process p2 = Runtime.getRuntime().exec(screenshotCmd);
        p2.waitFor();
        return new Image(new File(outpath).toURI().toString());
    }

    public Image createVideoThumbnail(DataFile f, String outpath) throws InterruptedException, IOException {
        //get duration of video
        String ffprobeCmd = "ffprobe -i \"" + f.getPath() + "\" -show_entries format=duration -v quiet -of csv=\"p=0\"";
        Process p = Runtime.getRuntime().exec(ffprobeCmd);
        p.waitFor();

        //make screenshot and save it in folder
        String s = new String(p.getInputStream().readAllBytes());
        int output = 0;
        try{
            output = Integer.parseInt(s.split("\\.")[0]);
            output = output / 2;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("FEHLER AUFGETRETEN: "+s.split("\\.")[0]);
        }

        String screenshotCmd = "ffmpeg -ss " + output + " -i \"" + f.getPath() + "\" -frames:v 1 -vf scale=320:-1 \"" + outpath+"\"";

        Process p2 = Runtime.getRuntime().exec(screenshotCmd);
        p2.waitFor();
        //System.out.println(new String(p2.getInputStream().readAllBytes()));
        //System.out.println(new String(p2.getErrorStream().readAllBytes()));
        String s2 = new File(outpath).toURI().toString();
        System.out.println(s2);
        return new Image(s2);
    }

    public void updateFileProperties(Event event, DataFile f) throws IOException, InterruptedException, URISyntaxException {
        if(event instanceof MouseEvent){
            if(((MouseEvent)event).getClickCount() == 2){
                this.mainScreenController.showFileInExplorer(f.getPath());
            }
        }
        updateFileProperties(f, true);
    }

    public void updateFileProperties(DataFile f, boolean updateThumbnail) throws InterruptedException, IOException, URISyntaxException {
        //NAME
        nameLabel.setText(f.getName());
        nameLabel.setOnContextMenuRequested(contextMenuEvent -> {
            ContextMenu cm = new ContextMenu();
            MenuItem copy = new MenuItem("copy");
            copy.setOnAction(actionEvent -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(f.getName());
                Clipboard.getSystemClipboard().setContent(content);
            });
            cm.getItems().add(copy);
            cm.show(nameLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        });

        nameLabel.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 2) {
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
        });

        //SIZE
        sizeLabel.setText(f.getFormattedSize());
        sizeLabel.setOnContextMenuRequested(contextMenuEvent -> {
            ContextMenu cm = new ContextMenu();
            MenuItem copy = new MenuItem("copy");
            copy.setOnAction(actionEvent -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(Long.toString(f.getSize()));
                Clipboard.getSystemClipboard().setContent(content);
            });
            cm.getItems().add(copy);
            cm.show(sizeLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        });

        //path
        Tooltip tooltip = new Tooltip();
        tooltip.setText(f.getPath());
        pathLabel.setTooltip(tooltip);
        pathLabel.setText(f.getPath());
        pathLabel.setOnContextMenuRequested(contextMenuEvent -> {
            ContextMenu cm = new ContextMenu();
            MenuItem copy = new MenuItem("copy");
            copy.setOnAction(actionEvent -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(f.getPath());
                Clipboard.getSystemClipboard().setContent(content);
            });

            MenuItem open = new MenuItem("open in explorer");
            open.setOnAction(actionEvent -> this.mainScreenController.showFileInExplorer(f.getPath()));
            cm.getItems().addAll(open, copy);
            cm.show(pathLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        });

        pathLabel.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 2) {
                this.mainScreenController.showFileInExplorer(f.getPath());
            }
        });


        //type
        typeLabel.setText(f.getType());
        typeLabel.setOnContextMenuRequested(contextMenuEvent -> {
            ContextMenu cm = new ContextMenu();
            MenuItem copy = new MenuItem("copy");
            copy.setOnAction(actionEvent -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(f.getType());
                Clipboard.getSystemClipboard().setContent(content);
            });
            cm.getItems().add(copy);
            cm.show(typeLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        });

        changeDateLabel.setText(f.formatDate());
        changeDateLabel.setOnContextMenuRequested(contextMenuEvent -> {
            ContextMenu cm = new ContextMenu();
            MenuItem copy = new MenuItem("copy");
            copy.setOnAction(actionEvent -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(f.getChangeDate().toString());
                Clipboard.getSystemClipboard().setContent(content);
            });
            cm.getItems().add(copy);
            cm.show(changeDateLabel, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        });

        switch (f.getType()){
            case "mp4":
            case "avi": this.playIcon.setVisible(true); break;
            default: this.playIcon.setVisible(false);
        }

        this.updateTags(f);
//        this.updateTags(f, true);
        if(updateThumbnail){
            this.updateThumbnail(f, true);
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
            try{
                setLoadingSpinner();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    private void setLoadingSpinner() {
        ImageView spinnerIv = new ImageView();
        spinnerIv.setFitHeight(40);
        spinnerIv.setFitWidth(40);
        String pth = Constants.getResourcePath(getClass(), "images", "spinner2.gif");
        Image spinner = new Image("file:/"+pth);
        spinnerIv.setImage(spinner);

        this.fileTagsBox.getChildren().add(spinnerIv);
    }

    private void updateTags(DataFile f, boolean set) {
        try {
            this.fileTagsBox.getChildren().clear();
            String cmd = Constants.getResourcePath(getClass(), "exiftool", "exiftool.exe");
            cmd += " -S -m -q -fast2 -category ";
            cmd += "\"" + f.getPath() + "\"";

            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            String res = new String(p.getInputStream().readAllBytes());
            System.err.println(new String(p.getErrorStream().readAllBytes()));
//            System.out.println(res);
            if(!res.isEmpty()) {
                String[] tags = res.split(":")[1].split(",");
                for(String tag: tags) {
                    tag = tag.strip();
                    f.addTag(tag);
                    if(set) {
                        StackPane stack = new StackPane();
                        Label l = new Label(tag);
                        l.setAlignment(Pos.CENTER);
                        l.getStyleClass().add("tag");

                        stack.getChildren().add(l);
                        this.fileTagsBox.getChildren().add(l);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            File f = new File(pathLabel.getText());
            Media media = new Media(f.toURI().toString());
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
        this.mediaView.setVisible(b);
        this.videoSlider.setVisible(b);
        this.volumeSlider.setVisible(b);
        this.controlsWrapper.setVisible(b);
        this.playButton.setVisible(b);
    }

    public Label getNameLabel() {
        return this.nameLabel;
    }

    public void setNameLabel(Label nameLabel) {
        this.nameLabel = nameLabel;
    }

    public StackPane getNameStackPane() {
        return nameStackPane;
    }

    public void setNameStackPane(StackPane nameStackPane) {
        this.nameStackPane = nameStackPane;
    }

    public void unhidePanel() {
        this.propertiesWrapper.setVisible(true);
    }

}
