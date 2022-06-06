package Controller;

import Main.PasswordKeeperMain;
import Repository.SecureDataRepo;
import Service.UserService;
import com.jfoenix.controls.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;

public class SignInController {

    private UserService userService = null;
    private static boolean closeApp = false;
    @FXML
    public BorderPane windowRootBorderPane;
    @FXML
    public JFXTextField signInProfileNameTextField;
    @FXML
    public JFXPasswordField signInProfilePasswordTextField;
    @FXML
    public JFXCheckBox newProfileCheckbox;
    @FXML
    public JFXTextField newProfileNameTextField;
    @FXML
    public JFXPasswordField newProfilePasswordTextField;
    @FXML
    public JFXButton createProfileButton;
    @FXML
    public JFXButton signInButton;
    @FXML
    public JFXTextArea programResponseTextField;
    @FXML
    public JFXButton closeButton;
    @FXML
    public Text signInPasswordVisibleText;
    @FXML
    public Text newPasswordVisibleText;
    @FXML
    public JFXButton showSignInPassButton;
    @FXML
    public JFXButton showNewPassButton;
    @FXML
    public BorderPane logBorderPane;
    @FXML
    public GridPane topGridPane;
    @FXML
    public JFXButton minimizeButton;
    @FXML
    public JFXComboBox<String> settingsComboBox;

    public void initialize() {
        userService = UserService.getInstance();
        ObservableList<String> settingsObservableList = FXCollections.observableArrayList();
        settingsObservableList.add("Import profile details file");
        settingsComboBox.setItems(settingsObservableList);
        JFXTextField[] jfxInputNameTextFields = new JFXTextField[]{signInProfileNameTextField, newProfileNameTextField};
        JFXPasswordField[] jfxInputPasswordTextFields = new JFXPasswordField[]{signInProfilePasswordTextField, newProfilePasswordTextField};

        programResponseTextField.setMaxSize(PasswordKeeperMain.getMainSceneWidth() / 2, PasswordKeeperMain.getMainSceneHeight() / 4);
        windowRootBorderPane.setMinSize(PasswordKeeperMain.getMainSceneWidth(), PasswordKeeperMain.getMainSceneHeight());
        newProfileCheckbox.selectedProperty().addListener((observableValue, oldValue, newValue) -> addCheckBoxListenerFunctionalities(observableValue));
        windowRootBorderPane.setOnKeyPressed(this::enableEnterKeyForConfirmingInput);
        addValidationToInputTextFields(jfxInputNameTextFields, jfxInputPasswordTextFields);
        showSignInPassButton.setOnAction(event -> setShowPasswordButtons(showSignInPassButton, signInPasswordVisibleText));
        showNewPassButton.setOnAction(event -> setShowPasswordButtons(showNewPassButton, newPasswordVisibleText));
        topGridPane.setOnMousePressed(mouseEvent -> topGridPane.setOnMouseDragged(dragEvent -> enableCustomWindowMoving(mouseEvent, dragEvent)));
        settingsComboBox.setCellFactory(stringListView -> setProfileSettingsComboboxCellFactory());
        setTraversableElements();
    }

    private void validateInitModeChoice(boolean disableSignInProfileNameTextField, boolean disableSignInProfilePasswordTextField, boolean disableSignInButton,
                                        boolean disableNewProfileUsernameTextField, boolean disableNewProfilePasswordTextField, boolean disableCreateProfileButton,
                                        boolean disableShowSignInPassButton, boolean disableShowNewPassButton) {
        signInProfileNameTextField.setDisable(disableSignInProfileNameTextField);
        signInProfilePasswordTextField.setDisable(disableSignInProfilePasswordTextField);
        signInButton.setDisable(disableSignInButton);
        newProfileNameTextField.setDisable(disableNewProfileUsernameTextField);
        newProfilePasswordTextField.setDisable(disableNewProfilePasswordTextField);
        createProfileButton.setDisable(disableCreateProfileButton);
        showSignInPassButton.setDisable(disableShowSignInPassButton);
        showNewPassButton.setDisable(disableShowNewPassButton);
    }

    private void addValidationToInputTextFields(JFXTextField[] inputNameTextFields, JFXPasswordField[] inputPasswordTextFields) {
        for (JFXTextField jfxInputTextField : inputNameTextFields) {
            jfxInputTextField.setOnKeyTyped(keyEvent -> {
                if (jfxInputTextField.getText().length() > 0 && jfxInputTextField.getText().matches(".*\\s.*")) {
                    jfxInputTextField.setText(jfxInputTextField.getText().replaceAll("\\s+", ""));
                    jfxInputTextField.positionCaret(jfxInputTextField.getText().length());
                }
            });
        }
        for (JFXPasswordField jfxInputPasswordTextField : inputPasswordTextFields) {
            jfxInputPasswordTextField.setOnKeyTyped(keyEvent -> {
                if (jfxInputPasswordTextField.getText().length() > 0 && jfxInputPasswordTextField.getText().matches(".*\\s.*")) {
                    jfxInputPasswordTextField.setText(jfxInputPasswordTextField.getText().replaceAll("\\s+", ""));
                    jfxInputPasswordTextField.positionCaret(jfxInputPasswordTextField.getText().length());
                }
                if (!newProfileCheckbox.isSelected()) {
                    signInPasswordVisibleText.setText(jfxInputPasswordTextField.getText());
                } else {
                    newPasswordVisibleText.setText(jfxInputPasswordTextField.getText());
                }
            });
            jfxInputPasswordTextField.textProperty().addListener((observableValue, oldValue, newValue) -> {
                if (newValue.length() > 40) {
                    jfxInputPasswordTextField.setText(oldValue);
                }
            });
        }
    }

    private ListCell<String> setProfileSettingsComboboxCellFactory() {
        ListCell<String> cell = new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        };
        cell.setOnMousePressed(e -> {
            if (cell.getItem().equals("Import profile details file")) {
                importFileContent();
            }
            resetSettingSelection();
        });
        return cell;
    }

    private void importFileContent() {
        FileChooser fileChooser = new FileChooser();
        SecureDataRepo secureDataRepo = userService.getSecureDataRepo();
        File selectedFile = fileChooser.showOpenDialog(windowRootBorderPane.getScene().getWindow());
        if (selectedFile == null || !selectedFile.exists() || !selectedFile.isFile() ||
                !selectedFile.getName().endsWith(".json")) return;
        secureDataRepo.decryptImportedFileAndCreateProfile(selectedFile, windowRootBorderPane.getScene().getWindow());
    }

    private void resetSettingSelection() {
        settingsComboBox.getSelectionModel().clearSelection();
    }

    private void clearAllInputTextFields() {
        if (!signInProfileNameTextField.getText().isBlank()) signInProfileNameTextField.clear();
        if (!signInProfilePasswordTextField.getText().isBlank()) signInProfilePasswordTextField.clear();
        if (!newProfileNameTextField.getText().isBlank()) newProfileNameTextField.clear();
        if (!newProfilePasswordTextField.getText().isBlank()) newProfilePasswordTextField.clear();
        if (!signInPasswordVisibleText.getText().isBlank()) signInPasswordVisibleText.setText("");
        if (!newPasswordVisibleText.getText().isBlank()) newPasswordVisibleText.setText("");
    }

    private void setShowPasswordButtons(JFXButton jfxShowButton, Text visiblePasswordText) {
        if (!visiblePasswordText.isVisible()) {
            visiblePasswordText.setVisible(true);
            jfxShowButton.setText("Hide");
        } else {
            visiblePasswordText.setVisible(false);
            jfxShowButton.setText("Show");
        }
    }

    private void enableEnterKeyForConfirmingInput(KeyEvent keyEvent) {
        if (newProfileCheckbox.isSelected() && keyEvent.getCode().equals(KeyCode.ENTER)) {
            handleCreateNewProfileButton();
        }
        if (!newProfileCheckbox.isSelected() && keyEvent.getCode().equals(KeyCode.ENTER)) {
            handleSignInButton();
        }
    }

    private void enableCustomWindowMoving(MouseEvent pressEvent, MouseEvent dragEvent) {
        windowRootBorderPane.getScene().getWindow().setX(dragEvent.getScreenX() - pressEvent.getSceneX());
        windowRootBorderPane.getScene().getWindow().setY(dragEvent.getScreenY() - pressEvent.getSceneY());
    }

    private void addCheckBoxListenerFunctionalities(ObservableValue<? extends Boolean> observableValue) {
        clearAllInputTextFields();
        if (observableValue.getValue().booleanValue()) {
            validateInitModeChoice(true, true, true,
                    false, false, false, true, false);
            if (signInPasswordVisibleText.isVisible()) {
                setShowPasswordButtons(showSignInPassButton, signInPasswordVisibleText);
            }
        } else {
            validateInitModeChoice(false, false, false,
                    true, true, true, false, true);
            if (newPasswordVisibleText.isVisible()) {
                setShowPasswordButtons(showNewPassButton, newPasswordVisibleText);
            }
        }
    }

    private void setTraversableElements() {
        closeButton.setFocusTraversable(false);
        minimizeButton.setFocusTraversable(false);
        showSignInPassButton.setFocusTraversable(false);
        showNewPassButton.setFocusTraversable(false);
        programResponseTextField.setFocusTraversable(false);
    }

    @FXML
    public void handleSignInButton() {
        if (signInProfileNameTextField == null || signInProfilePasswordTextField == null ||
                userService.validateInput(
                        signInProfileNameTextField.getText(), signInProfilePasswordTextField.getText()) != 0) {
            programResponseTextField.setText(userService.getResponses()[0]);
            return;
        }
        int responseState = userService.verifyProfileCredentials(signInProfileNameTextField.getText(),
                signInProfilePasswordTextField.getText());
        switch (responseState) {
            case 0:
                if (userService.getSecureDataRepo().isPasswordCorrect()) {
                    userService.getSecureDataRepo().resetCredentialsCorrectionState();
                    programResponseTextField.setText(userService.getResponses()[4]);
                    ((Stage) windowRootBorderPane.getScene().getWindow()).close();
                } else {
                    programResponseTextField.setText(userService.getResponses()[1]);
                }
                break;
            case 1:
                showApplicationProgress('S', userService.getResponses()[4], userService.getResponses()[1],
                        "Signing in to profile");
                break;
            case -1:
                programResponseTextField.setText(userService.getResponses()[1]);
                break;
        }
    }

    @FXML
    public void handleCreateNewProfileButton() {
        if (newProfileNameTextField == null || newProfilePasswordTextField == null || userService.validateInput(
                newProfileNameTextField.getText(), newProfilePasswordTextField.getText()) != 0) {
            programResponseTextField.setText(userService.getResponses()[2]);
            return;
        }
        createProfileButton.setDisable(true);
        int responseState = userService.orderProfileCreation(newProfileNameTextField.getText(),
                newProfilePasswordTextField.getText());
        switch (responseState) {
            case 0:
                createProfileButton.setDisable(false);
                programResponseTextField.setText(userService.getResponses()[3]);
                break;
            case 1:
                createProfileButton.setDisable(false);
                showApplicationProgress('C', userService.getResponses()[3], userService.getResponses()[7],
                        "Creating profile");
                break;
            case -1:
                createProfileButton.setDisable(false);
                programResponseTextField.setText(userService.getResponses()[6]);
                break;
            case -2:
                createProfileButton.setDisable(false);
                programResponseTextField.setText(userService.getResponses()[5]);
                break;
        }
    }

    @FXML
    public void handleCloseAppButton() {
        closeApp = true;
        ((Stage) windowRootBorderPane.getScene().getWindow()).close();
    }

    @FXML
    public void handleMinimizeAppButton() {
        ((Stage) windowRootBorderPane.getScene().getWindow()).setIconified(true);
    }

    private void showApplicationProgress(char mode, String successfulMessage,
                                         String failureMessage, String windowTitle) {
        PasswordKeeperMain.getWindowCreationManager().showLoadingDialog(windowTitle, "LoadingWindow.fxml");
        if (userService.getSecureDataRepo().getApplicationState() == 0) {
            switch (mode) {
                case 'C':
                    programResponseTextField.setText(successfulMessage);
                    break;
                case 'S':
                    if (userService.getSecureDataRepo().isPasswordCorrect()) {
                        userService.getSecureDataRepo().setUserSignedIn(true);
                        userService.getSecureDataRepo().resetCredentialsCorrectionState();
                        ((Stage) windowRootBorderPane.getScene().getWindow()).close();
                    } else {
                        programResponseTextField.setText(failureMessage);
                    }
                    break;
            }
        } else {
            programResponseTextField.setText(failureMessage);
        }
    }

    public static boolean isCloseApp() {
        return closeApp;
    }

}
