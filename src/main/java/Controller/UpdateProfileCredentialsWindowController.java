package Controller;

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

public class UpdateProfileCredentialsWindowController {

    private UserService userService;
    private boolean actionWasConfirmed = false;
    @FXML
    public BorderPane windowRootBorderPane;
    @FXML
    public GridPane topGridPane;
    @FXML
    public Label titleLabel;
    @FXML
    public JFXButton confirmButton;
    @FXML
    public Label usernameLabel;
    @FXML
    public Label passwordLabel;
    @FXML
    public JFXTextField usernameTextField;
    @FXML
    public JFXTextField passwordTextField;

    private final String[] inputCredentials = new String[]{"", ""};

    public void initialize() {
        userService = UserService.getInstance();
        topGridPane.setOnMousePressed(mouseEvent -> topGridPane
                .setOnMouseDragged(dragEvent -> enableCustomWindowMoving(mouseEvent, dragEvent)));
        windowRootBorderPane.setOnKeyPressed(this::enableEnterKeyForConfirmingInput);
        usernameTextField.setOnKeyTyped(keyEvent -> {
            boolean isDisabled = userService.validateInput(usernameTextField.getText(), "dummy123") != 0;
            usernameTextField.setFocusColor(isDisabled ? Color.RED : Color.AQUA);
            confirmButton.setDisable(isDisabled);

        });
        passwordTextField.setOnKeyTyped(keyEvent -> {
            boolean isDisabled = userService.validateInput("dummy", passwordTextField.getText()) != 0;
            passwordTextField.setFocusColor(isDisabled ? Color.RED : Color.AQUA);
            confirmButton.setDisable(isDisabled);
        });
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
        if (userService.validateInput(usernameTextField.getText(), passwordTextField.getText()) != 0) return;
        actionWasConfirmed = true;
        inputCredentials[0] = usernameTextField.getText();
        inputCredentials[1] = passwordTextField.getText();
        ((Stage) windowRootBorderPane.getScene().getWindow()).close();
    }

    private boolean wasActionConfirmed() {
        boolean isConfirmed = actionWasConfirmed;
        actionWasConfirmed = false;
        return isConfirmed;
    }

    public String[] getCredentialInput() {
        if (!wasActionConfirmed() || inputCredentials[0].isBlank() || inputCredentials[1].isBlank()) return null;
        return inputCredentials;
    }

    public void initializeInputTextFields(String[] initialSignedInUserPlainCredentials) {
        usernameTextField.setText(initialSignedInUserPlainCredentials[0]);
        passwordTextField.setText(initialSignedInUserPlainCredentials[1]);
    }

}
