package frontend.controllers;

import backend.DataFile;
import backend.FileObserver;
import backend.exceptions.InvalidFileNameException;
import backend.exceptions.UnexpectedErrorException;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.Iterator;

public class FilePropertyController implements FileObserver {

    private MainScreenController mainScreenController;

    private StackPane nameStackPane;
    private Label nameLabel;
    private Label pathLabel;

    private ImageView thumbnail;
    private ImageView playIcon;
    private MediaView mediaView;

    private HBox controlsWrapper;
    private Slider videoSlider;
    private Button playButton;
    private Slider volumeSlider;

    private InvalidationListener videoSliderListener;
    private InvalidationListener volumeSliderListener;
    private InvalidationListener playButtonListener;


    public FilePropertyController(MainScreenController mainScreenController) {

        this.mainScreenController = mainScreenController;
        this.thumbnail = mainScreenController.getThumbnail();
        this.playIcon = mainScreenController.getPlayIcon();
        this.mediaView = mainScreenController.getMediaView();
        this.videoSlider = mainScreenController.getVideoSlider();
        this.playButton = mainScreenController.getVideoPauseButton();
        this.volumeSlider = mainScreenController.getVideoVolumeSlider();
        this.controlsWrapper = mainScreenController.getControlsWrapper();

        this.videoSlider.setValue(0);
        this.volumeSlider.setValue(30);
        this.playButton.setText(">");
        hideMediaPlayerVideo();

        this.playIcon.setVisible(false);

        String path = getClass().getResource("/images").getPath() + "/playIcon.png";
        path = path
            .replace("/", "\\")
            .substring(1);
        Image playImg = new Image(new File(path).toURI().toString());
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

        if(player != null) {
            player.setVolume(this.volumeSlider.getValue()/100);

            this.playButton.setOnAction(event -> {
                MediaPlayer.Status status = player.getStatus();
                if (status == MediaPlayer.Status.PLAYING) {
                    if (player.getCurrentTime().greaterThanOrEqualTo(player.getTotalDuration())) {
                        player.seek(player.getStartTime());
                        player.play();
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

    }

    private void updateSliderValues(MediaPlayer player) {
        Platform.runLater(() -> {
            if(!volumeSlider.isPressed()){
                this.videoSlider.setValue(player.getCurrentTime().toMillis() / player.getTotalDuration().toMillis() * 100);
            }
        });
    }

    @Override
    public void notify(DataFile dataFile) {

    }

    void updateThumbnail(DataFile f, boolean set) throws IOException, InterruptedException, URISyntaxException {

        hideMediaPlayerVideo();


        String newName = f.getPath().replace("\\", "+");
        newName = newName.replace("/", "+");
        newName = newName.replace(":", "+");

        String outpath = getClass().getResource("/thumbnails")
                .getPath() + "/" + newName + ".jpg";
        outpath = outpath
                .replace("/", "\\")
                .substring(1);

        File image = new File(outpath);

        if (!image.exists()) {
            Image img = null;
            switch (f.getType()){
                case "webm":
                case "mp4": img = createVideoThumbnail(f, outpath); break;
                case "jpg":
                case "png":
                case "gif":
                case "webp": img = createImageThumbnail(f, outpath); break;
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
        ListView source = (ListView)event.getSource();
        Scene scene = source.getScene();

        if(event instanceof MouseEvent){
            if(((MouseEvent)event).getClickCount() == 2){
                this.mainScreenController.showFileInExplorer(f.getPath());
            }
        }

        //NAME
        nameLabel = (Label) scene.lookup("#nameLabelValue");
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
                        System.out.println(newName);
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
        Label sizeLabel =  (Label) scene.lookup("#sizeLabelValue");
        if(f.getSize() >= 1000000000){
            float res = f.getSize()/1000000000f;
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            sizeLabel.setText(df.format(res)+" GB");
        }else if(f.getSize() >= 1000000){
            sizeLabel.setText(f.getSize()/1000000+" MB");
        }else if(f.getSize() >= 1000){
            sizeLabel.setText(f.getSize()/1000+" KB");
        }
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
        pathLabel = (Label) scene.lookup("#pathLabelValue");
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
        Label typeLabel = (Label) scene.lookup("#typeLabelValue");
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

        Label changeDate = (Label) scene.lookup("#changeDateLabel");
        changeDate.setText(f.formatDate());
        changeDate.setOnContextMenuRequested(contextMenuEvent -> {
            ContextMenu cm = new ContextMenu();
            MenuItem copy = new MenuItem("copy");
            copy.setOnAction(actionEvent -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(f.getChangeDate().toString());
                Clipboard.getSystemClipboard().setContent(content);
            });
            cm.getItems().add(copy);
            cm.show(changeDate, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY());
        });

        switch (f.getType()){
            case "mp4":
            case "avi": this.playIcon.setVisible(true); break;
            default: this.playIcon.setVisible(false);
        }

        this.updateThumbnail(f, true);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Dialog");
        alert.setHeaderText("Error occured");
        alert.setContentText(message);
        alert.showAndWait();
    }

    MediaPlayer mediaPlayer;
    public void playVideo(MouseEvent mouseEvent) {
        File f = new File(pathLabel.getText());
        Media media = new Media(f.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        this.mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.play();
        this.playIcon.setVisible(false);
        setMediaControlVisibility(true);

        initializeVideoControl();

        playIcon.setOnMouseClicked(mouseEvent1 -> {
            mediaPlayer.play();
            playIcon.setVisible(false);
        });

        mediaView.setOnMouseClicked(mouseEvent1 -> {
            if(mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playIcon.setVisible(true);
            }else{
                mediaPlayer.play();
                playIcon.setVisible(false);
            }
        });
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


}
