package Controller;

import Main.PasswordKeeperMain;
import Service.UserService;
import com.jfoenix.controls.JFXProgressBar;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoadingWindowController {

    private UserService userService = null;
    @FXML
    public JFXProgressBar loadingProgressBar;
    @FXML
    public DialogPane loadingWindowDialogPane;

    private Timeline timeline;

    public void initialize() {
        userService = UserService.getInstance();
        timeline = new Timeline();
        KeyValue keyValue = new KeyValue(loadingProgressBar.progressProperty(), 1);
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.5), keyValue);
        timeline.getKeyFrames().add(keyFrame);
        loadingWindowDialogPane.setMinSize(PasswordKeeperMain.getMainSceneWidth() / 2, PasswordKeeperMain.getMainSceneHeight() / 3);
        loadingProgressBar.progressProperty().addListener((observableVal, previousVal, nextVal) -> {
            if (observableVal.getValue().doubleValue() == 1.0) {
                if(userService.getSecureDataRepo().getApplicationState() == 1){
                    progressLoadingInBackground();
                } else {
                    ((Stage) loadingWindowDialogPane.getScene().getWindow()).close();
                }
            }
        });
        progressLoadingInBackground();
    }

    private void progressLoadingInBackground() {
        Platform.runLater(() -> {
            loadingProgressBar.setProgress(0);
            timeline.playFromStart();
        });
    }

}
