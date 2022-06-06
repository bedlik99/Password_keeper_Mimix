package Controller;

import DataModel.PlatformCredential;
import DataModel.UserProfile;
import Main.PasswordKeeperMain;
import Repository.SecureDataRepo;
import Service.UserService;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ManageCredentialWindowController {

    private UserService userService = null;
    private SecureDataRepo secureDataRepo = null;
    private UserProfile signedInUserProfile = null;
    private boolean actionWasConfirmed = false;
    private char windowCredentialMode = '-';
    private PlatformCredential selectedCredential;
    @FXML
    public BorderPane windowRootBorderPane;
    @FXML
    public GridPane topGridPane;
    @FXML
    public Label titleLabel;
    @FXML
    public JFXTextField platformNameTextField;
    @FXML
    public JFXTextField usernameTextField;
    @FXML
    public JFXTextField passwordTextField;
    @FXML
    public JFXButton confirmButton;

    public void initialize() {
        userService = UserService.getInstance();
        secureDataRepo = UserService.getInstance().getSecureDataRepo();
        signedInUserProfile = UserService.getInstance().getSignedInUserProfile();
        topGridPane.setOnMousePressed(mouseEvent -> topGridPane
                .setOnMouseDragged(dragEvent -> enableCustomWindowMoving(mouseEvent, dragEvent)));
        windowRootBorderPane.setOnKeyPressed(this::enableEnterKeyForConfirmingInput);
        setAddButtonAvailability();
    }

    public void setupWindowMode(char windowCredentialMode, PlatformCredential selectedCredential) {
        this.windowCredentialMode = windowCredentialMode;
        this.selectedCredential = selectedCredential;
        if (windowCredentialMode == 'C') {
            titleLabel.setText("Add new platform credentials.");
            confirmButton.setText("Add");
            confirmButton.setDisable(true);
        } else {
            titleLabel.setText("Update platform credentials.");
            confirmButton.setText("Update");
            platformNameTextField.setText(selectedCredential.getPlatformName());
            usernameTextField.setText(selectedCredential.getPlatformUsername());
            passwordTextField.setText(selectedCredential.getPlatformPassword());
        }
    }

    private void setAddButtonAvailability() {
        platformNameTextField.setOnKeyTyped(keyEvent -> {
            confirmButton.setDisable((isAnyTextFieldEmpty() || doesPlatformNameExist()));
            if (doesPlatformNameExist()) {
                platformNameTextField.setFocusColor(Color.RED);
            } else {
                if (platformNameTextField.getFocusColor() == Color.RED) {
                    platformNameTextField.setFocusColor(Color.AQUA);
                }
            }
        });
        usernameTextField.setOnKeyTyped(keyEvent ->
                confirmButton.setDisable((isAnyTextFieldEmpty() || doesPlatformNameExist())));
        passwordTextField.setOnKeyTyped(keyEvent ->
                confirmButton.setDisable((isAnyTextFieldEmpty() || doesPlatformNameExist())));
    }

    private boolean isAnyTextFieldEmpty() {
        return (platformNameTextField.getText().isBlank() || usernameTextField.getText().isBlank()
                || passwordTextField.getText().isBlank());
    }

    private boolean doesPlatformNameExist() {
        if (userService.findPlatformCredentialByPlatformName(platformNameTextField.getText()) == null) return false;
        if (windowCredentialMode == 'C') return true;
        return windowCredentialMode == 'U' &&
                !platformNameTextField.getText().equals(selectedCredential.getPlatformName());
    }

    @FXML
    public void handleCancelButton() {
        ((Stage) windowRootBorderPane.getScene().getWindow()).close();
    }

    private void enableCustomWindowMoving(MouseEvent pressEvent, MouseEvent dragEvent) {
        windowRootBorderPane.getScene().getWindow().setX(dragEvent.getScreenX() - pressEvent.getSceneX());
        windowRootBorderPane.getScene().getWindow().setY(dragEvent.getScreenY() - pressEvent.getSceneY());
    }

    private void enableEnterKeyForConfirmingInput(KeyEvent keyEvent) {
        if (!confirmButton.isDisable() && keyEvent.getCode().equals(KeyCode.ENTER)) {
            handleConfirmationButton();
        }
    }

    @FXML
    public void handleConfirmationButton() {
        if (isAnyTextFieldEmpty() || doesPlatformNameExist()) return;
        if (secureDataRepo.getApplicationState() != 0) return;

        secureDataRepo.encryptInputCredential(usernameTextField.getText(), signedInUserProfile.getProfileUsername());
        PasswordKeeperMain.getWindowCreationManager()
                .showLoadingDialog("Encrypting and adding username", "LoadingWindow.fxml");
        String encryptedUsername = secureDataRepo.getStreamManager().getCommandResult();

        secureDataRepo.encryptInputCredential(passwordTextField.getText(), signedInUserProfile.getProfileUsername());
        PasswordKeeperMain.getWindowCreationManager()
                .showLoadingDialog("Encrypting and adding password", "LoadingWindow.fxml");
        String encryptedPassword = secureDataRepo.getStreamManager().getCommandResult();

        PlatformCredential newPlatformCredential =
                new PlatformCredential(platformNameTextField.getText(), encryptedUsername, encryptedPassword);
        if (windowCredentialMode == 'C') {
            signedInUserProfile.getPlatformCredentials().add(newPlatformCredential);
        } else {
            PlatformCredential currentPlatformCredential = userService
                    .findPlatformCredentialByPlatformName(selectedCredential.getPlatformName());
            int indexOfRecordToUpdate = signedInUserProfile.getPlatformCredentials().indexOf(currentPlatformCredential);
            signedInUserProfile.getPlatformCredentials().set(indexOfRecordToUpdate, newPlatformCredential);
        }
        actionWasConfirmed = true;
        ((Stage) windowRootBorderPane.getScene().getWindow()).close();
    }

    public boolean wasActionConfirmed() {
        boolean isConfirmed = actionWasConfirmed;
        actionWasConfirmed = false;
        return isConfirmed;
    }

}
