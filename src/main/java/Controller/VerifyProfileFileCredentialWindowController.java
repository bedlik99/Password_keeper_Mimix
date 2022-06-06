package Controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class VerifyProfileFileCredentialWindowController {

    private boolean actionWasConfirmed = false;
    @FXML
    public BorderPane windowRootBorderPane;
    @FXML
    public GridPane topGridPane;
    @FXML
    public Label titleLabel;
    @FXML
    public JFXTextField credentialTextField;
    @FXML
    public JFXButton confirmButton;
    @FXML
    public Label credentialLabel;

    public void initialize() {
        topGridPane.setOnMousePressed(mouseEvent -> topGridPane
                .setOnMouseDragged(dragEvent -> enableCustomWindowMoving(mouseEvent, dragEvent)));
        windowRootBorderPane.setOnKeyPressed(this::enableEnterKeyForConfirmingInput);
        credentialTextField.setOnKeyTyped(keyEvent -> confirmButton.setDisable(credentialTextField.getText().isBlank()));
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
        if (credentialTextField.getText().isBlank()) return;
        actionWasConfirmed = true;
        ((Stage) windowRootBorderPane.getScene().getWindow()).close();
    }

    private boolean wasActionConfirmed() {
        boolean isConfirmed = actionWasConfirmed;
        actionWasConfirmed = false;
        return isConfirmed;
    }

    public String getCredentialInput() {
        if(!wasActionConfirmed()) return null;
        return ( credentialTextField == null || credentialTextField.getText().isBlank()) ?
                "" : credentialTextField.getText();
    }

    public void setWindowMode(char credentialType, boolean signedInProfileCredentialsVerification) {
        if(credentialType == 'U') {
            credentialLabel.setText(" Profile username: ");
            titleLabel.setText(signedInProfileCredentialsVerification ?
                    "Verification of signed in profile username" : "Verification of importing profile username.");
        } else if(credentialType == 'P'){
            credentialLabel.setText(" Profile password: ");
            titleLabel.setText(signedInProfileCredentialsVerification ?
                    "Verification of signed in profile password" : "Verification of importing profile password.");
        }
    }

}
