package Util;

import Controller.ManageCredentialWindowController;
import Controller.UpdateProfileCredentialsWindowController;
import Controller.VerifyProfileFileCredentialWindowController;
import DataModel.PlatformCredential;
import Main.PasswordKeeperMain;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Optional;

public class WindowCreationManager {

    public void showSignInWindow(Window windowOwner) {
        Stage stage = new Stage();
        Parent root;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/window_structure/SignInWindow.fxml"));
        root = loadStandardWindow(fxmlLoader);
        stage.setScene(new Scene(root, PasswordKeeperMain.getMainSceneWidth(), PasswordKeeperMain.getMainSceneHeight()));
        stage.initOwner(windowOwner);
        stage.setTitle("Secure Password Keeper - profile management");
        stage.getScene().getStylesheets().add(this.getClass().getResource("/css/signInWindowStyle.css").toExternalForm());
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.getScene().setFill(Color.TRANSPARENT);
        stage.showAndWait();
    }

    public boolean showManagePlatformCredentialsWindow(Window windowOwner, char windowMode,
                                                       PlatformCredential selectedPlatformName) {
        Stage stage = new Stage();
        Parent root;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/window_structure/ManageCredentialWindow.fxml"));
        root = loadStandardWindow(fxmlLoader);
        stage.setScene(new Scene(root, PasswordKeeperMain.getMainSceneWidth() / 2, PasswordKeeperMain.getMainSceneHeight() / 2));
        stage.initOwner(windowOwner);
        stage.getScene().getStylesheets().add(this.getClass().getResource("/css/manageCredentialWindowStyle.css").toExternalForm());
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.getScene().setFill(Color.TRANSPARENT);

        ManageCredentialWindowController controller = fxmlLoader.getController();
        controller.setupWindowMode(windowMode, selectedPlatformName);
        stage.showAndWait();

        return controller.wasActionConfirmed();
    }

    public String showVerifyProfileFileCredentialWindow(Window windowOwner, char credentialType, boolean isSignedInProfileCredentialsVerification) {
        Stage stage = new Stage();
        Parent root;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/window_structure/VerifyProfileFileCredentialWindow.fxml"));
        root = loadStandardWindow(fxmlLoader);
        stage.setScene(new Scene(root, PasswordKeeperMain.getMainSceneWidth() / 2, PasswordKeeperMain.getMainSceneHeight() / 2));
        stage.initOwner(windowOwner);
        stage.getScene().getStylesheets().add(this.getClass().getResource("/css/verifyProfileFileCredentialWindowStyle.css").toExternalForm());
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.getScene().setFill(Color.TRANSPARENT);
        VerifyProfileFileCredentialWindowController controller = fxmlLoader.getController();
        controller.setWindowMode(credentialType, isSignedInProfileCredentialsVerification);
        stage.showAndWait();
        return controller.getCredentialInput();
    }

    public String[] showUpdateProfileCredentialsWindow(Window windowOwner, String[] initialSignedInUserPlainCredentials) {
        Stage stage = new Stage();
        Parent root;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/window_structure/UpdateProfileCredentialsWindow.fxml"));
        root = loadStandardWindow(fxmlLoader);
        stage.setScene(new Scene(root, PasswordKeeperMain.getMainSceneWidth() / 2, PasswordKeeperMain.getMainSceneHeight() / 2));
        stage.initOwner(windowOwner);
        stage.getScene().getStylesheets().add(this.getClass().getResource("/css/updateProfileCredentialsWindowStyle.css").toExternalForm());
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.getScene().setFill(Color.TRANSPARENT);
        UpdateProfileCredentialsWindowController controller = fxmlLoader.getController();
        controller.initializeInputTextFields(initialSignedInUserPlainCredentials);
        stage.showAndWait();
        return controller.getCredentialInput();
    }

    public boolean showConfirmationDialog(String title, String headerText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText("Are you sure?");
        alert.setWidth(PasswordKeeperMain.getMainSceneWidth() / 2);
        alert.setHeight(PasswordKeeperMain.getMainSceneHeight() / 3);
        Optional<ButtonType> result = alert.showAndWait();
        return (result.isPresent() && result.get() == ButtonType.OK);
    }

    public void showLoadingDialog(String title, String fxmlWindowName) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().add(this.getClass().getResource("/css/loadingWindowStyle.css").toExternalForm());
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/window_structure/" + fxmlWindowName));
        try {
            dialog.getDialogPane().setContent(fxmlLoader.load());
        } catch (Exception e) {
            return;
        }
        dialog.showAndWait();
    }

    public void showInformationDialog(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    private Parent loadStandardWindow(FXMLLoader fxmlLoader) {
        Parent root= null;
        try {
            root = fxmlLoader.load();
        } catch (Exception ignored) {
        }
        return root;
    }

}
