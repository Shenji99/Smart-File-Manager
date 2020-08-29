package frontend.controllers;

import backend.FileManager;
import backend.data.DataFile;
import backend.exceptions.InvalidFileNameException;
import backend.exceptions.InvalidNameException;
import backend.exceptions.UnexpectedErrorException;
import backend.tasks.Callback;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
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
    @FXML public Label changeDateLabelValue;
    private StackPane artistStackPane;
    @FXML public Label artistsLabel;
    @FXML public Label artistsLabelValue;

    TextField editFileArtistTextField;
    TextField editFileNameTextField;

    @FXML public FlowPane fileTagsBox;
    @FXML public FlowPane presetTagsContainer;
    @FXML public VBox propertiesWrapper;

    @FXML public Button confirmDeleteFileTagsButton;
    @FXML public Button deleteAllTagsButton;
    @FXML public Button abortDeletingTagsButton;
    @FXML public Button addTagPresetButton;
    @FXML public Button addSingleTagButton;
    @FXML public TextField addSingleTagToFileTextField;
    @FXML public Button confirmDeleteFilePresetTagsButton;
    @FXML public TextField addTagTextField;
    @FXML public Button deleteFileTagsButton;
    @FXML public Button addTagsButton;
    @FXML public Button deletePresetTagsButton;
    @FXML public Button abortTagsAddingButton;
    @FXML public Button saveTagsPresetButton;
    @FXML public HBox nameEditButtonsWrapper;
    @FXML public HBox artistsEditButtonsWrapper;

    @FXML public Button deleteSelectedPresetTagsButton;
    @FXML public Button abortDeletePresetTagsButton;


    private HashSet<String> selectedTags;
    private HashSet<String> selectedToRemovePresetTags;
    private HashSet<String> selectedToRemoveFileTags;

    private InvalidationListener videoSliderListener;
    private InvalidationListener volumeSliderListener;
    private InvalidationListener playButtonListener;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        FileManager.getInstance().addTagObserver(tags -> this.updatePresetTagList(tags));
        FileManager.getInstance().addFileObserver(dataFile -> {
            DataFile df = FileManager.getInstance().findFileByPath(this.pathLabelValue.getText());
            if(df == null || df.getId().equals(dataFile.getId())) {
                this.nameLabelValue.setText(dataFile.getName());
                this.typeLabelValue.setText(dataFile.getType());
                this.pathLabelValue.setText(dataFile.getPath());
                this.pathLabelValue.getTooltip().setText(dataFile.getPath());
                this.sizeLabelValue.setText(dataFile.getFormattedSize());
                this.changeDateLabelValue.setText(dataFile.formatDate());
                this.artistsLabelValue.setText(dataFile.getArtistsAsString());
                this.widthHeightLabelValue.setText(dataFile.getWidth()+"x"+dataFile.getHeight());
            }
        });


        try {
            FileManager.getInstance().loadPresetTags(new File("\\C:\\Users\\LMaro\\Desktop\\tags.csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        this.widthHeightLabel.setVisible(false);
        this.widthHeightLabelValue.setVisible(false);
        this.artistsLabel.setVisible(false);
        this.artistsLabelValue.setVisible(false);

        this.abortTagsAddingButton.setVisible(false);
        this.saveTagsPresetButton.setVisible(false);
        this.deleteSelectedPresetTagsButton.setVisible(false);
        this.abortDeletePresetTagsButton.setVisible(false);
        this.abortDeletingTagsButton.setVisible(false);
        this.confirmDeleteFileTagsButton.setVisible(false);

        this.nameEditButtonsWrapper.setVisible(false);
        this.artistsEditButtonsWrapper.setVisible(false);

        this.controlsWrapper.setPrefWidth(this.mediaView.getFitWidth());
        this.controlsWrapper.setMaxWidth(this.mediaView.getFitWidth());

        this.pauseVideoButton.setText("||");
        this.videoSlider.setValue(0);
        this.volumeSlider.setValue(30);

        clearPanel();
        hideMediaPlayerVideo();

        this.playIcon.setVisible(false);

        String path = FileManager.getResourcePath("images", "playIcon.png");
        Image playImg = new Image("file:/" + path);
        this.playIcon.setImage(playImg);

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    }

    private void initializeVideoControl() {
        MediaPlayer player = this.mediaView.getMediaPlayer();

        if (this.videoSliderListener != null) {
            this.videoSlider.valueProperty().removeListener(videoSliderListener);
        }

        if (this.playButtonListener != null) {
            player.currentTimeProperty().removeListener(playButtonListener);
        }

        if (this.volumeSliderListener != null) {
            this.volumeSlider.valueProperty().removeListener(volumeSliderListener);
        }

        player.setVolume(this.volumeSlider.getValue() / 100);

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
            if (volumeSlider.isPressed()) {
                player.setVolume(volumeSlider.getValue() / 100);
            }
        };
        this.volumeSlider.valueProperty().addListener(this.volumeSliderListener);

    }

    private void updateSliderValues(MediaPlayer player) {
        Platform.runLater(() -> {
            if (!volumeSlider.isPressed()) {
                this.videoSlider.setValue(player.getCurrentTime().toMillis() / player.getTotalDuration().toMillis() * 100);
            }
            int scnds = (int) player.getCurrentTime().toSeconds() % 60;
            int minutes = (int) player.getCurrentTime().toMinutes();

            String minStr, secStr;

            if (minutes < 10) {
                minStr = "0" + minutes;
            } else {
                minStr = Integer.toString(minutes);
            }
            if (scnds < 10) {
                secStr = "0" + scnds;
            } else {
                secStr = Integer.toString(scnds);
            }
            this.currentTime.setText(minStr + ":" + secStr);
        });
    }

    public void updateThumbnail(DataFile f, boolean set) {
        if (set) {
            hideMediaPlayerVideo();
        }
        this.playIcon.setVisible(false);
        this.thumbnail.setImage(null);
        String outpath = FileManager.createThumbnailPath(f);
        File image = new File(outpath);

        try {
            //show the spinner while loading
            if (set) {
                Platform.runLater(this::showThumbnailLoadingSpinner);
            }
            if (!image.exists()) {
                //if thumbnail doesnt exist yet it gets created
                //after that the thumbnail spinner is removed
                executor.submit(() -> {
                    try {
                        Image img = FileManager.createThumbnail(f, outpath);
                        if (set) {
                            if(f.getPath().equals(this.pathLabelValue.getText())){
                                Platform.runLater(() -> {
                                    hideThumbnailLoadingSpinner();
                                    this.thumbnail.setImage(img);
                                    String mimetype = FileManager.getDataFileMimeType(f);
                                    if (mimetype != null && mimetype.equals("video/mp4")) {
                                        this.playIcon.setVisible(true);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else if (set) {
                Platform.runLater(() -> {
                    hideThumbnailLoadingSpinner();
                    this.thumbnail.setImage(new Image(image.toURI().toString()));
                    if (FileManager.getDataFileMimeType(f).equals("video/mp4")) {
                        this.playIcon.setVisible(true);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showThumbnailLoadingSpinner() {
        //get the spinner, if it doesnt exist, a spinner is created
        ImageView spinner = null;
        StackPane wrapper = (StackPane) this.thumbnail.getParent();
        for (Node n : wrapper.getChildren()) {
            if (n instanceof ImageView && n.getId() != null) {
                if (n.getId().equals("spinner")) {
                    spinner = (ImageView) n;
                }
            }
        }
        if (spinner == null) {
            spinner = mainScreenController.createLodingSpinner(50, 50);
            spinner.setId("spinner");
            wrapper.getChildren().add(spinner);
        }
        spinner.setVisible(true);
    }

    public void hideThumbnailLoadingSpinner() {
        StackPane wrapper = (StackPane) this.thumbnail.getParent();
        for (Node n : wrapper.getChildren()) {
            if (n instanceof ImageView && n.getId() != null && n.getId().equals("spinner")) {
                n.setVisible(false);
                return;
            }
        }
    }

    private void hideMediaPlayerVideo() {
        setMediaControlVisibility(false);

        if (this.mediaView != null && this.mediaView.getMediaPlayer() != null) {
            this.mediaView.getMediaPlayer().pause();
        }
        playIcon.setOnMouseClicked(this::playVideo);
    }

    protected void hideNameEdit() {
        if (nameStackPane != null && nameLabelValue != null) {
            Iterator iter = nameStackPane.getChildren().iterator();
            while (iter.hasNext()) {
                Node n = (Node) iter.next();
                if (n instanceof TextField) {
                    iter.remove();
                }
            }
            nameLabelValue.setVisible(true);
            nameEditButtonsWrapper.setVisible(false);
        }
    }

    public void updateFileProperties(Event event, DataFile f) {
        if (event instanceof MouseEvent) {
            if (((MouseEvent) event).getClickCount() == 2) {
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
        abortAddingTags();
        abortDeletingPresetTags();

        hideArtistNameEdit();

        String mimeType = FileManager.getDataFileMimeType(f);
        this.playIcon.setVisible(mimeType.equals("video/mp4")); //only enable playicon at mp4 vids

        if (mimeType.startsWith("video") || mimeType.startsWith("image")) {
            updateWidthHeightLabel(f);
            updateArtistsLabel(f);
        } else {
            this.widthHeightLabel.setVisible(false);
            this.widthHeightLabelValue.setVisible(false);
            this.artistsLabel.setVisible(false);
            this.artistsLabelValue.setVisible(false);
        }

        this.updateTags(f);
        if (updateThumbnail) {
            this.updateThumbnail(f, true);
        }
    }

    private void updateArtistsLabel(DataFile f) {
        this.artistsLabel.setVisible(true);

        if(f.isArtistsLoaded()){
            ((StackPane)this.artistsLabelValue.getParent()).getChildren().removeIf(e -> e instanceof ImageView);
            this.artistsLabelValue.setVisible(true);
            this.artistsLabelValue.setText(f.getArtistsAsString());
        }else {
            this.artistsLabelValue.setVisible(false);
            StackPane wrapper = (StackPane) this.artistsLabelValue.getParent();
            wrapper.getChildren().add(mainScreenController.createLodingSpinner(20, 20));
            executor.submit(() -> {
                try {
                    String res = FileManager.getArtists(f);
                    Platform.runLater(() -> {
                        if (!res.isEmpty() && f.getPath().equals(this.pathLabelValue.getText())) {
                            this.artistsLabel.setVisible(true);
                            this.artistsLabelValue.setVisible(true);
                            String[] artists = res.split(":")[1].split(",");
                            for(String artist: artists){
                                f.addArtist(artist.trim());
                            }
                            f.setArtistsLoaded(true);
                            this.artistsLabelValue.setText(f.getArtistsAsString());
                        }
                        wrapper.getChildren().removeIf(n -> n instanceof ImageView);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(f.getArtistsAsString());
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(actionEvent -> editFileArtists(actionEvent, f));

        cm.getItems().addAll(copy, edit);
        artistsLabelValue.setOnContextMenuRequested(contextMenuEvent -> cm.show(artistsLabelValue, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
        this.artistsLabelValue.setOnMouseClicked(mouseEvent -> editFileArtists(mouseEvent, f));
    }

    private void updateChangeDateValueLabel(DataFile f) {
        changeDateLabelValue.setText(f.formatDate());
        ContextMenu cm = new ContextMenu();
        MenuItem copy = new MenuItem("copy");
        copy.setOnAction(actionEvent -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(f.getChangeDate().toString());
            Clipboard.getSystemClipboard().setContent(content);
        });
        cm.getItems().add(copy);
        changeDateLabelValue.setOnContextMenuRequested(contextMenuEvent -> cm.show(changeDateLabelValue, contextMenuEvent.getScreenX(), contextMenuEvent.getScreenY()));
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
        tooltip.setShowDelay(new Duration(200));
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
            if (mouseEvent.getClickCount() == 2) {
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
                    if (!res.isEmpty() && f.getPath().equals(this.pathLabelValue.getText())) {
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
        if ((event instanceof MouseEvent && ((MouseEvent) event).getClickCount() == 2) || event instanceof ActionEvent) {
            nameLabelValue.setVisible(false);
            editFileNameTextField = new TextField(nameLabelValue.getText());
            editFileNameTextField.setPrefHeight(nameLabelValue.getHeight() + 10);
            this.nameEditButtonsWrapper.setVisible(true);

            this.nameStackPane = (StackPane) nameLabelValue.getParent();
            nameStackPane.getChildren().add(editFileNameTextField);

            editFileNameTextField.setOnKeyPressed(event1 -> {
                if (event1.getCode() == KeyCode.ENTER) {
                    prepareFileForUpdate();
                    submitFileNameChange(f);
                }
            });
        }
    }

    private void submitFileNameChange(DataFile f) {
        String newName = editFileNameTextField.getText();
        if (!newName.equals(f.getName())) {
            try {
                FileManager.getInstance().renameFile(f, newName);
                hideNameEdit();
            } catch (InvalidFileNameException e) {
                //Show error
                MainScreenController.showError(e.getMessage());
                editFileNameTextField.getStyleClass().add("error-border");
                Tooltip tooltip = new Tooltip();
                tooltip.setText(e.getMessage());
                editFileNameTextField.setTooltip(tooltip);
            }
        } else {
            hideNameEdit();
        }
    }

    private void editFileArtists(Event event, DataFile f) {
        if ((event instanceof MouseEvent && ((MouseEvent) event).getClickCount() == 2) || event instanceof ActionEvent) {
            artistsLabelValue.setVisible(false);
            editFileArtistTextField = new TextField(artistsLabelValue.getText());
            editFileArtistTextField.setPrefHeight(artistsLabelValue.getHeight() + 10);
            this.artistsEditButtonsWrapper.setVisible(true);

            this.artistStackPane = (StackPane) artistsLabelValue.getParent();
            artistStackPane.getChildren().add(editFileArtistTextField);

            editFileArtistTextField.setOnKeyPressed(event1 -> {
                if (event1.getCode() == KeyCode.ENTER) {
                    submitFileArtistsChange(f);
                }
            });
        }
    }

    private void submitFileArtistsChange(DataFile f) {
        String newName = editFileArtistTextField.getText().trim();
        hideArtistNameEdit();

        if (!newName.equals(f.getName())) {
            StackPane wrapper = (StackPane) this.artistsLabelValue.getParent();
            this.artistsLabelValue.setVisible(false);
            wrapper.getChildren().add(mainScreenController.createLodingSpinner(20, 20));

            boolean isPlayableVideo = FileManager.getInstance().isPlayableVideo(f);
            prepareFileForUpdate();
            FileManager.getInstance().updateFileArtists(f, newName, callback -> {
                if(f.getPath().equals(this.pathLabelValue.getText())){
                    Platform.runLater(() -> {
                        this.artistsLabelValue.setVisible(true);
                        this.artistsLabelValue.setText(f.getArtistsAsString());
                        wrapper.getChildren().removeIf(e -> e instanceof ImageView);
                        if(isPlayableVideo){
                            playIcon.setVisible(true);
                        }
                    });
                }
            }, error -> {
                if(f.getPath().equals(this.pathLabelValue.getText())){
                    Platform.runLater(() -> {
                        this.artistsLabelValue.setVisible(true);
                        wrapper.getChildren().removeIf(e -> e instanceof ImageView);
                        MainScreenController.showError((String) error[0]);
                        if(isPlayableVideo){
                            playIcon.setVisible(true);
                        }
                    });
                }
            });
        }
    }

    private void hideArtistNameEdit() {
        if (artistStackPane != null && artistsLabelValue != null) {
            Iterator iter = artistStackPane.getChildren().iterator();
            while (iter.hasNext()) {
                Node n = (Node) iter.next();
                if (n instanceof TextField) {
                    iter.remove();
                }
            }
            artistsLabelValue.setVisible(true);
            artistsEditButtonsWrapper.setVisible(false);
        }
    }

    public void filePropertiesPaneClicked(MouseEvent event) {
        if (event.getTarget() != this.nameLabelValue) {
            hideNameEdit();
        }

        if(event.getTarget() != this.artistsLabelValue) {
            hideArtistNameEdit();
        }

        if (event.getTarget() != this.addTagTextField) {
            this.addTagTextField.getStyleClass().remove("error-border");
        }
    }

    private void updateTags(DataFile f) {
        //maybe solve differently that the user can still add tags from preset
        this.abortAddingTags();

        if (f.isTagsLoaded()) {
            setTagsOfFile(f);
        } else {
            showTagsLoadingSpinner();
            updateSingleFileTags(f, args -> {
                String path = (String) args[0];
                if (path.equals(this.pathLabelValue.getText())) {
                    Platform.runLater(() -> {
                        removeTagsLoadingSpinner();
                        setTagsOfFile(f);
                    });
                }
            });
        }
    }

    public void updateSingleFileTags(DataFile f, Callback callback) {
        executor.submit(() -> {
            try {
                String cmd = FileManager.getResourcePath("exiftool", "exiftool.exe");
                cmd += " -L -S -m -q -fast2 -fileName -directory -category ";
                cmd += "\"" + f.getPath() + "\"";
                Process p = Runtime.getRuntime().exec(cmd);
                p.waitFor();
                String res = new String(p.getInputStream().readAllBytes());
                FileManager.getInstance().updateFiles(res);
                callback.run(f.getPath());
            } catch (UnexpectedErrorException e) {
                MainScreenController.showError(e.getMessage());
            } catch (InterruptedException | IOException | InvalidNameException e) {
                e.printStackTrace();
            }
        });
    }

    private void removeTagsLoadingSpinner() {
        for (Node n : this.fileTagsBox.getChildren()) {
            if (n instanceof ImageView && n.getId() != null && n.getId().equals("spinner")) {
                n.setVisible(false);
                this.fileTagsBox.getChildren().remove(n);
                return;
            }
        }
    }

    private void showTagsLoadingSpinner() {
        try {
            ImageView spinner = null;
            for (Node n : this.fileTagsBox.getChildren()) {
                if (n instanceof ImageView && n.getId() != null && n.getId().equals("spinner")) {
                    spinner = (ImageView) n;
                    break;
                }
            }

            if (spinner == null) {
                spinner = this.mainScreenController.createLodingSpinner(40, 40);
                spinner.setId("spinner");
            }
            spinner.setVisible(true);

            this.fileTagsBox.getChildren().clear();
            this.fileTagsBox.getChildren().add(spinner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setTagsOfFile(DataFile f) {
        this.fileTagsBox.getChildren().clear();
        List<String> tags = f.getTags();
        for (String tag : tags) {
            this.fileTagsBox.getChildren().add(createTagNode(tag));
        }
    }

    public Node createTagNode(String text) {
        StackPane stackpane = new StackPane();
        Label l = new Label(text);
        stackpane.getChildren().add(l);
        FlowPane.setMargin(stackpane, new Insets(3));
        l.setPadding(new Insets(3));
        l.setAlignment(Pos.CENTER);
        if(!stackpane.getStyleClass().contains("tag")){
            stackpane.getStyleClass().add("tag");
        }

        if(!stackpane.getStyleClass().contains("primary-purple-background")){
            stackpane.getStyleClass().add("primary-purple-background");
        }
        return stackpane;
    }

    public void playVideo(MouseEvent mouseEvent) {
        try {
            if (this.mediaPlayer != null) {
                this.mediaPlayer.dispose();
            }
            if (this.videoSlider != null) {
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
                int secDuration = (int) (this.mediaPlayer.getTotalDuration().toSeconds() % 60);
                String minDurationStr, secDurationStr;

                if (minDuration < 10) {
                    minDurationStr = "0" + minDuration;
                } else {
                    minDurationStr = Integer.toString(minDuration);
                }
                if (secDuration < 10) {
                    secDurationStr = "0" + secDuration;
                } else {
                    secDurationStr = Integer.toString(secDuration);
                }
                this.videoDuration.setText(minDurationStr + ":" + secDurationStr);
            });

            playIcon.setOnMouseClicked(mouseEvent1 -> {
                mediaPlayer.play();
                this.pauseVideoButton.setText("||");
                playIcon.setVisible(false);
            });

            mediaView.setOnMouseClicked(mouseEvent1 -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    this.pauseVideoButton.setText(">");
                    playIcon.setVisible(true);
                } else {
                    mediaPlayer.play();
                    this.pauseVideoButton.setText("||");
                    playIcon.setVisible(false);
                }
            });

            mediaView.setOnError(mediaErrorEvent -> {
                MainScreenController.showError(mediaErrorEvent.toString());
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearPanel() {
        this.propertiesWrapper.setVisible(false);
        this.hideMediaPlayerVideo();
        if (this.mediaView != null && this.mediaView.getMediaPlayer() != null) {
            this.mediaView.getMediaPlayer().dispose();
        }
    }

    private void setMediaControlVisibility(boolean b) {
        //resetting the size is important so that the container resizes
        // when the new video/image is bigger or smaller
        if (b) {
            this.mediaView.setFitWidth(400);
            this.mediaView.setFitHeight(300);
        } else {
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

    private void updatePresetTagList(Set<String> tags) {
        this.presetTagsContainer.getChildren().clear();
        for (String tag : tags) {
            this.presetTagsContainer.getChildren().add(createTagNode(tag));
        }
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

    public void addTagToPreset(ActionEvent actionEvent) {
        try {
            String tagText = this.addTagTextField.getText();
            if (!tagText.isEmpty()) {
                if (FileManager.getInstance().addTagToPreset(tagText)) {
                    this.addTagTextField.getStyleClass().remove("error-border");
                    this.addTagTextField.clear();
                } else {
                    MainScreenController.showError("Tag existiert bereits");
                    this.addTagTextField.getStyleClass().add("error-border");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addErrorBorder(Node n) {
        n.getStyleClass().add("error-border");
    }

    public void removeErrorBorder(Node n) {
        n.getStyleClass().remove("error-border");
    }

    public void addTagsToFile(ActionEvent actionEvent) {
        selectedTags = new HashSet<String>();

        this.addTagsButton.setVisible(false);
        this.deleteFileTagsButton.setVisible(false);
        this.deletePresetTagsButton.setDisable(true);
        this.addTagPresetButton.setDisable(true);

        this.abortTagsAddingButton.setVisible(true);
        this.saveTagsPresetButton.setVisible(true);

        DataFile current = FileManager.getInstance().findFileByPath(this.pathLabelValue.getText());
        if (current != null) {
            for (Node n : this.presetTagsContainer.getChildren()) {
                Label l = (Label) ((StackPane) n).getChildren().get(0);
                n.getStyleClass().remove("primary-purple-background");
                if(!n.getStyleClass().contains("green-background")){
                    n.getStyleClass().add("green-background");
                }
                if (!current.hasTag(l.getText())) {
                    n.setCursor(Cursor.HAND);
                    n.setOnMouseClicked(mouseEvent -> {
                        String text = l.getText();
                        this.selectedTags.add(text);
                        n.setDisable(true);
                        Node tagNode = createTagNode(text);
                        tagNode.setId("presetEditTag");
                        tagNode.getStyleClass().remove("primary-purple-background");
                        if(!tagNode.getStyleClass().contains("green-background")){
                            tagNode.getStyleClass().add("green-background");
                        }
                        tagNode.setCursor(Cursor.HAND);
                        addToFileTagsBox(tagNode, n);
                    });
                } else {
                    n.setDisable(true);
                }
            }
        }

    }

    private void addToFileTagsBox(Node n, Node reference) {
        this.fileTagsBox.getChildren().add(n);
        n.setOnMouseClicked(mouseEvent -> {
            this.fileTagsBox.getChildren().remove(n);
            this.selectedTags.remove(((Label) ((StackPane) n).getChildren().get(0)).getText());
            reference.setDisable(false);
        });
    }

    public void abortAddingTags() {
        if (selectedTags != null) {
            selectedTags.clear();
        }

        this.addTagsButton.setVisible(true);
        this.deleteFileTagsButton.setVisible(true);
        this.abortTagsAddingButton.setVisible(false);
        this.saveTagsPresetButton.setVisible(false);
        this.deletePresetTagsButton.setDisable(false);
        this.addTagPresetButton.setDisable(false);

        this.fileTagsBox.getChildren().removeIf(n -> n.getId() != null && n.getId().equals("presetEditTag"));


        this.presetTagsContainer.setDisable(false);
        this.presetTagsContainer.getChildren().forEach(n -> {
            n.setCursor(Cursor.DEFAULT);
            n.setOnMouseClicked(null);
            n.getStyleClass().remove("green-background");
            if(!n.getStyleClass().contains("primary-purple-background")){
                n.getStyleClass().add("primary-purple-background");
            }
            n.setDisable(false);
        });
    }

    public void abortTagsAddingButtonClicked(ActionEvent actionEvent) {
        abortAddingTags();
    }

    private void updateTags() {
        DataFile df = FileManager.getInstance().findFileByPath(this.pathLabelValue.getText());
        this.updateTags(df);
    }

    public void deletePresetTagsButtonClicked(ActionEvent actionEvent) {
        abortAddingTags();
        this.selectedToRemovePresetTags = new HashSet<>();

        this.addTagsButton.setDisable(true);
        this.addTagPresetButton.setDisable(true);
        this.deleteSelectedPresetTagsButton.setDisable(true);
        this.deleteFileTagsButton.setDisable(true);
        this.addSingleTagButton.setDisable(true);

        this.abortDeletePresetTagsButton.setVisible(true);
        this.deletePresetTagsButton.setVisible(false);
        this.confirmDeleteFilePresetTagsButton.setVisible(true);

        for (Node n : this.presetTagsContainer.getChildren()) {
            Label l = (Label) ((StackPane) n).getChildren().get(0);
            n.getStyleClass().remove("primary-purple-background");
            if(!n.getStyleClass().contains("green-background")){
                n.getStyleClass().add("green-background");
            }

            n.setCursor(Cursor.HAND);
            n.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    EventHandler<MouseEvent> that = this;
                    String text = l.getText();
                    selectedToRemovePresetTags.add(text);
                    FilePropertyController.this.selectedToRemovePresetTags.add(text);
                    n.getStyleClass().remove("green-background");
                    if(!n.getStyleClass().contains("red-background")){
                        n.getStyleClass().add("red-background");
                    }
                    n.setOnMouseClicked(mouseEvent1 -> {
                        n.getStyleClass().remove("red-background");
                        if(!n.getStyleClass().contains("green-background")){
                            n.getStyleClass().add("green-background");
                        }
                        selectedToRemovePresetTags.remove(text);
                        n.setOnMouseClicked(that);
                    });
                }
            });
        }
    }

    public void addSingleTagToFile(ActionEvent actionEvent) {
        String tagName = this.addSingleTagToFileTextField.getText();
        if (!tagName.isEmpty()) {


            this.addSingleTagButton.setDisable(true);
            prepareFileForUpdate();
            this.showTagsLoadingSpinner();

            boolean isPlayableVideo = FileManager.getInstance().isPlayableVideo(FileManager.getInstance().findFileByPath(this.pathLabelValue.getText()));
            DataFile df = FileManager.getInstance().findFileByPath(this.pathLabelValue.getText());
            try {
                FileManager.getInstance().addTagToFile(df, tagName, callback -> {
                    Platform.runLater(() -> {
                        addSingleTagButton.setDisable(false);
                        if(pathLabelValue.getText().equals(df.getPath())){
                            updateTags();
                            if (isPlayableVideo) {
                                playIcon.setVisible(true);
                            }
                        }
                    });
                }, error -> {
                    String msg = (String) error[0];
                    MainScreenController.showError(msg);
                    df.setTagsLoaded(false);
                    Platform.runLater(() -> {
                        addSingleTagButton.setDisable(false);
                        if (pathLabelValue.getText().equals(df.getPath())) {
                            updateTags();
                            if(isPlayableVideo){
                                playIcon.setVisible(true);
                            }
                        }
                    });
                });
            } catch (InvalidNameException e) {
                MainScreenController.showError(e.getMessage());
                addSingleTagButton.setDisable(false);
                if (FileManager.getInstance().isPlayableVideo(df) && pathLabelValue.getText().equals(df.getPath())) {
                    updateTags();
                    playIcon.setVisible(true);
                }
            }
        }
    }

    public void saveAddedPresetTagsClicked(ActionEvent actionEvent) {
        if(this.selectedTags.size() <= 0){
            return;
        }
        DataFile df = FileManager.getInstance().findFileByPath(this.pathLabelValue.getText());
        try {
            this.addSingleTagButton.setDisable(true);
            prepareFileForUpdate();
            this.showTagsLoadingSpinner();
            boolean isPlayableVideo = FileManager.getInstance().isPlayableVideo(df);

            FileManager.getInstance().addTagsToFile(df, this.selectedTags, callback -> {
                Platform.runLater(() -> {
                    if(pathLabelValue.getText().equals(df.getPath())) {
                        updateTags();
                        this.addSingleTagButton.setDisable(false);
                        abortAddingTags();
                        if (isPlayableVideo) {
                            playIcon.setVisible(true);
                        }
                    }
                });
            }, error -> {
                String msg = (String) error[0];
                MainScreenController.showError(msg);
                df.setTagsLoaded(false);
                Platform.runLater(() -> {
                    if (pathLabelValue.getText().equals(df.getPath())) {
                        abortAddingTags();
                        this.addSingleTagButton.setDisable(false);
                        updateTags();
                        if (isPlayableVideo) {
                            playIcon.setVisible(true);
                        }
                    }
                });
            });
        } catch (InvalidNameException e) {
            MainScreenController.showError(e.getMessage());
            updateTags();
            abortAddingTags();
            if (FileManager.getInstance().isPlayableVideo(df) && pathLabelValue.getText().equals(df.getPath())) {
                playIcon.setVisible(true);
            }
        }
    }

    public void deleteAllTagsPressed(ActionEvent actionEvent) {
        DataFile df = FileManager.getInstance().findFileByPath(this.pathLabelValue.getText());
        boolean isPlayableVideo = FileManager.getInstance().isPlayableVideo(df);

        if(df.getTags().size() > 0){
            mainScreenController.showConfirmationDialog("Möchtest du wirklich alle Tags der Datei löschen?", "", onConfirm -> {
                prepareFileForUpdate();
                this.showTagsLoadingSpinner();
                FileManager.getInstance().deleteAllTags(df, callback2 -> Platform.runLater(() -> {
                    if(df.getPath().equals(this.pathLabelValue.getText())){
                        updateTags();
                        this.addSingleTagButton.setDisable(false);
                        if(isPlayableVideo){
                            playIcon.setVisible(true);
                        }
                    }
                }));
            });
        }
    }

    public void addTagPresetTagTyped(KeyEvent keyEvent) {
        this.addTagPreset();
    }

    private void addTagPreset() {
        try {
            //check if typed word is already a preset tag
            String text = this.addTagTextField.getText().trim();
            if (!text.isEmpty()) {
                if (!FileManager.getInstance().getPresetTags().contains(text)) {
                    this.removeErrorBorder(this.addTagTextField);
                    this.addTagPresetButton.setDisable(false);
                } else {
                    this.addErrorBorder(this.addTagTextField);
                    this.addTagPresetButton.setDisable(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addSingleTagTextFieldTyped(KeyEvent keyEvent) {
        this.addSingleTag();
    }

    private void addSingleTag() {
        //check if typed word is already a tag
        String text = this.addSingleTagToFileTextField.getText().trim();
        if (!text.isEmpty()) {
            if (!FileManager.getInstance().findFileByPath(this.pathLabelValue.getText()).hasTag(text)) {
                this.removeErrorBorder(this.addSingleTagToFileTextField);
                this.addSingleTagButton.setDisable(false);
            } else {
                this.addErrorBorder(this.addSingleTagToFileTextField);
                this.addSingleTagButton.setDisable(true);
            }
        }
    }

    public void addTagPresetKeyPressed(KeyEvent keyEvent) {
        //neded because keytyped has no key code
        if(keyEvent.getCode() == KeyCode.ENTER){
            addTagToPreset(null);
        }
    }

    public void addSingleTagKeyPressed(KeyEvent keyEvent) {
        //neded because keytyped has no key code
        if(keyEvent.getCode() == KeyCode.ENTER){
            addSingleTagToFile(null);
        }
    }

    public void abortDeletePresetTagsButtonClicked(ActionEvent actionEvent) {
        abortDeletingPresetTags();
    }

    private void abortDeletingPresetTags() {
        try {
            if(this.selectedToRemovePresetTags != null){
                this.selectedToRemovePresetTags.clear();
            }
            this.addTagsButton.setDisable(false);
            this.addTagPresetButton.setDisable(false);
            this.addSingleTagButton.setDisable(false);
            this.deleteSelectedPresetTagsButton.setDisable(false);
            this.deleteFileTagsButton.setDisable(false);
            this.confirmDeleteFilePresetTagsButton.setVisible(false);

            this.abortDeletePresetTagsButton.setVisible(false);
            this.deletePresetTagsButton.setVisible(true);

            this.presetTagsContainer.getChildren().forEach(n -> {
                n.setCursor(Cursor.DEFAULT);
                n.setOnMouseClicked(null);
                n.getStyleClass().remove("green-background");
                n.getStyleClass().remove("red-background");
                if(!n.getStyleClass().contains("primary-purple-background")){
                    n.getStyleClass().add("primary-purple-background");
                }
                n.setDisable(false);
            });
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void confirmDeleteFilePresetTagsButtonClicked(ActionEvent actionEvent) {
        if(this.selectedToRemovePresetTags != null && this.selectedToRemovePresetTags.size() > 0){
            this.mainScreenController.showConfirmationDialog("Möchtest du wirklich alle ausgewählten Tags löschen?", "", onConfirm -> {
                FileManager.getInstance().removePresetTags(this.selectedToRemovePresetTags);
                updatePresetTagList(FileManager.getInstance().getPresetTags());
                abortDeletingPresetTags();
            });
        }else {
            this.mainScreenController.showInformation("Keine Tags ausgewählt!");
        }
    }

    public void deleteFileTagsButtonClicked(ActionEvent actionEvent) {
        selectedToRemoveFileTags = new HashSet<>();

        abortAddingTags();
        abortDeletingPresetTags();

        this.addSingleTagButton.setDisable(true);
        this.deleteAllTagsButton.setDisable(true);
        this.deletePresetTagsButton.setDisable(true);

        this.addTagsButton.setVisible(false);
        this.abortDeletingTagsButton.setVisible(true);
        this.confirmDeleteFileTagsButton.setVisible(true);

        this.fileTagsBox.getChildren().forEach(n -> {
            Label l = (Label) ((StackPane) n).getChildren().get(0);

            n.getStyleClass().remove("primary-purple-background");
            if(!n.getStyleClass().contains("green-background")){
                n.getStyleClass().add("green-background");
            }
            n.setCursor(Cursor.HAND);
            n.setOnMouseClicked(new EventHandler<MouseEvent>() {
                EventHandler<MouseEvent> that = this;

                @Override
                public void handle(MouseEvent mouseEvent) {
                    selectedToRemoveFileTags.add(l.getText());
                    n.getStyleClass().remove("green-background");
                    if (!n.getStyleClass().contains("red-background")) {
                        n.getStyleClass().add("red-background");
                    }

                    n.setOnMouseClicked(mouseEvent1 -> {
                        selectedToRemoveFileTags.remove(l.getText());
                        n.getStyleClass().remove("red-background");
                        if (!n.getStyleClass().contains("green-background")) {
                            n.getStyleClass().add("green-background");
                        }
                        n.setOnMouseClicked(that);
                    });
                }
            });
        });
    }

    public void abortDeletingTagsButtonClicked(ActionEvent actionEvent) {
        abortDeletingFileTags();
    }

    private void prepareFileForUpdate() {
        this.hideMediaPlayerVideo();
        this.addSingleTagToFileTextField.clear();

        if (this.mediaPlayer != null) {
            this.mediaPlayer.dispose();
        }
        boolean isPlayableVideo = FileManager.getInstance().isPlayableVideo(FileManager.getInstance().findFileByPath(this.pathLabelValue.getText()));
        if (isPlayableVideo) {
            this.playIcon.setVisible(false);
        }
    }

    public void confirmDeleteFileTagsButtonClicked(ActionEvent actionEvent) {
        if(this.selectedToRemoveFileTags != null && this.selectedToRemoveFileTags.size() > 0){
            this.mainScreenController.showConfirmationDialog("Möchtest du wirklich alle ausgewählten Tags löschen?", "", onConfirm -> {
                this.saveTagsPresetButton.setDisable(true);
                this.abortDeletingTagsButton.setDisable(true);
                prepareFileForUpdate();
                this.showTagsLoadingSpinner();
                boolean isPlayableVideo = FileManager.getInstance().isPlayableVideo(FileManager.getInstance().findFileByPath(this.pathLabelValue.getText()));
                DataFile df = FileManager.getInstance().findFileByPath(this.pathLabelValue.getText());

                FileManager.getInstance().removeTagsFromFile(df, this.selectedToRemoveFileTags, callback-> {
                    Platform.runLater(() -> {
                        if(pathLabelValue.getText().equals(df.getPath())) {
                            updateTags();
                            abortAddingTags();
                            if (isPlayableVideo) {
                                playIcon.setVisible(true);
                            }
                        }
                        abortDeletingFileTags();
                    });
                }, onError -> {
                    String msg = (String) onError[0];
                    MainScreenController.showError(msg);
                    df.setTagsLoaded(false);
                    Platform.runLater(() -> {
                        if (pathLabelValue.getText().equals(df.getPath())) {
                            abortAddingTags();
                            updateTags();
                            if (isPlayableVideo) {
                                playIcon.setVisible(true);
                            }
                        }
                        abortDeletingFileTags();
                    });
                });

            });
        }else {
            this.mainScreenController.showInformation("Keine Tags ausgewählt!");
        }
    }

    private void abortDeletingFileTags() {
        this.addSingleTagButton.setDisable(false);
        this.deleteAllTagsButton.setDisable(false);
        this.deletePresetTagsButton.setDisable(false);

        this.addTagsButton.setVisible(true);
        this.abortDeletingTagsButton.setVisible(false);
        this.confirmDeleteFileTagsButton.setVisible(false);

        this.fileTagsBox.getChildren().forEach(n -> {
            n.setCursor(Cursor.DEFAULT);
            n.setOnMouseClicked(null);
            n.getStyleClass().remove("red-background");
            n.getStyleClass().remove("green-background");
            if(!n.getStyleClass().contains("primary-purple-background")){
                n.getStyleClass().add("primary-purple-background");
            }
        });
    }

    public void submitFileArtistsChange(ActionEvent actionEvent) {
        submitFileArtistsChange(FileManager.getInstance().findFileByPath(this.pathLabelValue.getText()));
    }

    public void abortFileArtistsChange(ActionEvent actionEvent) {
        hideArtistNameEdit();
    }

    public void submitFileNameChange(ActionEvent actionEvent) {
        submitFileNameChange(FileManager.getInstance().findFileByPath(this.pathLabelValue.getText()));
    }

    public void abortFileNameChange(ActionEvent actionEvent) {
        hideNameEdit();
    }
}
