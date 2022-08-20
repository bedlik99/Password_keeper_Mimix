package Controller;

import DataModel.PlatformCredential;
import DataModel.SecuredProfileDetails;
import DataModel.UserProfile;
import Main.PasswordKeeperMain;
import Repository.SecureDataRepo;
import Service.UserService;
import Util.FileEncrypterDecrypter;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindowController {

    private UserService userService = null;
    private ObservableList<String> observablePlatformNamesList = null;
    private UserProfile signedInUserProfile = null;
    private SortedList<String> sortedPlatformNamesList = null;
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
    private boolean profileFileWasDeleted = false;
    @FXML
    public JFXTextField usernameTextField;
    @FXML
    public JFXTextField passwordTextField;
    @FXML
    public GridPane topGridPane;
    @FXML
    public BorderPane windowRootBorderPane;
    @FXML
    public JFXButton maximizeButton;
    @FXML
    public JFXButton minimizeButton;
    @FXML
    public JFXButton closeButton;
    @FXML
    public ListView<String> platformNamesListView;
    @FXML
    public Label platformNameLabel;
    @FXML
    public JFXButton addNewCredentialsButton;
    @FXML
    public GridPane centerGridPane;
    @FXML
    public JFXButton removeCredentialButton;
    @FXML
    public JFXButton updateCredentialsButton;
    @FXML
    public JFXButton clearSelectionButton;
    @FXML
    public JFXComboBox<String> settingsComboBox;

    private final PlatformCredential selectedCredential = new PlatformCredential();
    private int indexOfSelectedPlatform = -1;
    private final String[] SETTINGS_OPTIONS = {
            "1. Generate profile details file\n(importable only on this device system profile)\n_____________________________________________________",
            "2. Generate unprotected, not encrypted,\nplain credentials file (unrecommended)\n_____________________________________________________",
            "3. Import credentials from plain credentials file (2)\n_____________________________________________________",
            "4. Update profile login credentials\n_____________________________________________________",
            "5. Delete profile\n_____________________________________________________"
    };

    public void initialize() {
        ObservableList<String> settingsObservableList = FXCollections.observableArrayList();
        settingsObservableList.add(SETTINGS_OPTIONS[0]);
        settingsObservableList.add(SETTINGS_OPTIONS[1]);
        settingsObservableList.add(SETTINGS_OPTIONS[2]);
        settingsObservableList.add(SETTINGS_OPTIONS[3]);
        settingsObservableList.add(SETTINGS_OPTIONS[4]);

        settingsComboBox.setItems(settingsObservableList);
        topGridPane.setOnMousePressed(mouseEvent -> topGridPane
                .setOnMouseDragged(dragEvent -> enableCustomWindowMoving(mouseEvent, dragEvent)));
        settingsComboBox.setCellFactory(stringListView -> setProfileSettingsComboboxCellFactory());
        initializeSortedListView();
        setTraversableElements();
    }

    public void initializeAfterSigningIn() {
        userService = UserService.getInstance();
        initializeSortedListView();
        signedInUserProfile = userService.getSignedInUserProfile();
        signedInUserProfile.getPlatformCredentials().forEach(platformCredential ->
                observablePlatformNamesList.add(platformCredential.getPlatformName()));
        platformNamesListView.getSelectionModel().clearSelection();
        if (profileFileWasDeleted) profileFileWasDeleted = false;
    }

    private void initializeSortedListView() {
        if (observablePlatformNamesList == null) {
            observablePlatformNamesList = FXCollections.observableArrayList();
            sortedPlatformNamesList = new SortedList<>(new FilteredList<>(
                    observablePlatformNamesList, platformName -> true),
                    (name1, name2) -> Integer.compare(name1.toLowerCase(Locale.ROOT)
                            .compareTo(name2.toLowerCase(Locale.ROOT)), 0));
        }
        setPlatformListViewActions();
    }

    public void resetSettingSelection() {
        settingsComboBox.getSelectionModel().clearSelection();
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
            if (cell.getItem().equals(SETTINGS_OPTIONS[4])) {
                deleteProfile();
            } else if (cell.getItem().equals(SETTINGS_OPTIONS[0])) {
                generateSecuredProfileFile();
            } else if (cell.getItem().equals(SETTINGS_OPTIONS[3])) {
                updateProfileCredentials();
            } else if (cell.getItem().equals(SETTINGS_OPTIONS[1])) {
                generatePlainPlatformCredentialsFile();
            } else if (cell.getItem().equals(SETTINGS_OPTIONS[2])) {
                importPlainPlatformCredentialsFile();
            }
            resetSettingSelection();
        });
        return cell;
    }

    private void updateProfileCredentials() {
        userService.getSecureDataRepo()
                .updateSignedInUserCredentials(windowRootBorderPane.getScene().getWindow(), signedInUserProfile);
    }

    private void generateSecuredProfileFile() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(windowRootBorderPane.getScene().getWindow());
        if (selectedDirectory == null || !selectedDirectory.exists() || !selectedDirectory.isDirectory()) return;
        boolean credentialsSuccessfullyGenerated = userService.generateSecuredProfileCredentials();
        SecuredProfileDetails securedProfileDetails = userService.getSecureDataRepo().getSecuredProfileDetails();
        if (!credentialsSuccessfullyGenerated || !securedProfileDetails.isProfileDetailsInitialized()) return;
        userService.getSecureDataRepo().encryptGeneratedFileContent(securedProfileDetails.returnJSONObject().toJSONString());
        PasswordKeeperMain.getWindowCreationManager()
                .showLoadingDialog("Encrypting profile details file", "LoadingWindow.fxml");
        String encryptedContent = userService.getSecureDataRepo().getStreamManager().getCommandResult();
        userService.getSecureDataRepo().getStreamManager().clearCommandResult();
        if (encryptedContent == null || encryptedContent.isBlank()) return;
        saveAllProfilePlatformCredentials(encryptedContent, selectedDirectory);
    }

    private void saveAllProfilePlatformCredentials(String content, File selectedDirectory) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(selectedDirectory.getAbsolutePath()
                + "/profile_details_" + FileEncrypterDecrypter.generateRandomText(6) + "_"
                + formatter.format(new Date(System.currentTimeMillis())) + ".json"))) {
            bw.write(content);
            bw.flush();
        } catch (IOException ignored) {
        }
    }

    private void deleteProfile() {
        String selectedItem = settingsComboBox.getSelectionModel().getSelectedItem();
        if (selectedItem == null || !selectedItem.equals(SETTINGS_OPTIONS[4])) return;
        boolean profileDeletionWasConfirmed = isActionConfirmed("Profile Deletion",
                "You are going to delete this profile.\nYou will be signed out automatically.");
        if (!profileDeletionWasConfirmed) return;
        SecureDataRepo secDataRepo = userService.getSecureDataRepo();
        File signedInUserProfileFile = new File(secDataRepo.getProfilesDirPath() + secDataRepo.getSignedInUserProfileFileName());
        if (signedInUserProfileFile.exists()) profileFileWasDeleted = signedInUserProfileFile.delete();
        if (!profileFileWasDeleted) return;
        secDataRepo.getOrderedProfileUsernamesList().remove(secDataRepo.getSignedInUserOrderedUsername());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(secDataRepo.getOrderedProfileNamesFilePath()))) {
            bw.write(secDataRepo.getOrderedProfileUsernamesList().toJSONString());
            bw.flush();
        } catch (IOException e) {
            return;
        }
        handleSignOutAppButton();
    }

    private void enableCustomWindowMoving(MouseEvent pressEvent, MouseEvent dragEvent) {
        if (((Stage) windowRootBorderPane.getScene().getWindow()).isMaximized()) return;
        windowRootBorderPane.getScene().getWindow().setX(dragEvent.getScreenX() - pressEvent.getSceneX());
        windowRootBorderPane.getScene().getWindow().setY(dragEvent.getScreenY() - pressEvent.getSceneY());
    }

    private void setTraversableElements() {
        closeButton.setFocusTraversable(false);
        minimizeButton.setFocusTraversable(false);
        maximizeButton.setFocusTraversable(false);
        addNewCredentialsButton.setFocusTraversable(false);
        removeCredentialButton.setFocusTraversable(false);
        updateCredentialsButton.setFocusTraversable(false);
        settingsComboBox.setFocusTraversable(false);
        clearSelectionButton.setFocusTraversable(false);
    }

    private void setPlatformListViewActions() {
        platformNamesListView.setItems(sortedPlatformNamesList);
        platformNamesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        platformNamesListView.setOnMouseClicked(mouseEvent -> {
            if (platformNamesListView.getSelectionModel().getSelectedItem() != null) {
                removeCredentialButton.setDisable(false);
                updateCredentialsButton.setDisable(false);
            }
            setTextFieldsOnSelectedListCellName();
        });
        platformNamesListView.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                if (platformNamesListView.getSelectionModel().getSelectedItem() != null) {
                    removeCredentialButton.setDisable(false);
                    updateCredentialsButton.setDisable(false);
                }
                setTextFieldsOnSelectedListCellName();
            }
        });
    }

    private void setTextFieldsOnSelectedListCellName() {
        String platformName = platformNamesListView.getSelectionModel().getSelectedItem();
        if (platformName == null) return;
        if (platformName.equals(selectedCredential.getPlatformName())) return;
        clearVisibleDataInFields();
        PlatformCredential platformCredential = userService.findPlatformCredentialByPlatformName(platformName);
        if (platformCredential == null) return;

        String[] decryptedCredentials = decryptPlatformUsernameAndPassword(platformCredential);
        String decryptedUsername = decryptedCredentials[0];
        String decryptedPassword = decryptedCredentials[1];

        platformNameLabel.setText(" Platform: " + platformName);
        usernameTextField.setText(decryptedUsername);
        passwordTextField.setText(decryptedPassword);
        selectedCredential.setAllPlatformAttributes(platformName, decryptedUsername, decryptedPassword);
        indexOfSelectedPlatform = signedInUserProfile.getPlatformCredentials().indexOf(platformCredential);
    }

    private void generatePlainPlatformCredentialsFile() {
        String decryptedCredentials = "[" +
                new ArrayList<>(userService.getSignedInUserProfile().getPlatformCredentials())
                        .stream()
                        .map(credential -> {
                            String[] decryptedCredential = decryptPlatformUsernameAndPassword(credential);
                            return new PlatformCredential(credential.getPlatformName(), decryptedCredential[0], decryptedCredential[1]);
                        })
                        .collect(Collectors.toList())
                        .stream()
                        .map(PlatformCredential::toString)
                        .collect(Collectors.joining(",")) + "]";

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(windowRootBorderPane.getScene().getWindow());
        if (selectedDirectory == null || !selectedDirectory.exists() || !selectedDirectory.isDirectory()) return;
        saveAllProfilePlatformCredentials(decryptedCredentials, selectedDirectory);
    }

    private void importPlainPlatformCredentialsFile() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(windowRootBorderPane.getScene().getWindow());
        if (selectedFile == null || !selectedFile.exists() || !selectedFile.isFile() ||
                !selectedFile.getName().endsWith(".json")) return;
        try {
            JSONArray jsonFileContent = (JSONArray) new JSONParser().parse(Files.readString(Path.of(selectedFile.getAbsolutePath())));
            List<PlatformCredential> importedPlatformCredentials = new ArrayList<>();
            List<String> importedPlatformNames = new ArrayList<>();
            jsonFileContent.forEach(platformCredential -> {
                JSONObject credential = (JSONObject) platformCredential;
                importedPlatformCredentials.add(new PlatformCredential(
                        credential.get("platformName").toString(),
                        credential.get("platformUsername").toString(),
                        credential.get("platformPassword").toString()));
                importedPlatformNames.add(credential.get("platformName").toString());
            });
            new ManageCredentialWindowController()
                    .updatePlatformCredentialsData(importedPlatformCredentials, 'C', userService);
            observablePlatformNamesList.addAll(importedPlatformNames);
            userService.updateSignedInUserProfile();
        } catch (Exception ignore) {
        }
    }

    private String[] decryptPlatformUsernameAndPassword(PlatformCredential credential) {
        String[] decryptedCredentials = new String[2];
        SecureDataRepo secureDataRepo = userService.getSecureDataRepo();
        secureDataRepo.decryptSelectedPlatformCredential(
                credential.getPlatformUsername(), signedInUserProfile.getProfileUsername());
        PasswordKeeperMain.getWindowCreationManager().showLoadingDialog(
                "Decrypting username. Platform: " + credential.getPlatformName(), "LoadingWindow.fxml");
        decryptedCredentials[0] = secureDataRepo.getStreamManager().getCommandResult();

        secureDataRepo.decryptSelectedPlatformCredential(
                credential.getPlatformPassword(), signedInUserProfile.getProfileUsername());
        PasswordKeeperMain.getWindowCreationManager().showLoadingDialog(
                "Decrypting password. Platform: " + credential.getPlatformName(), "LoadingWindow.fxml");
        decryptedCredentials[1] = secureDataRepo.getStreamManager().getCommandResult();
        secureDataRepo.getStreamManager().clearCommandResult();
        return decryptedCredentials;
    }

    @FXML
    public void handleSignOutAppButton() {
        handleClearCredentialSelectionButton();
        observablePlatformNamesList.clear();
        if (!profileFileWasDeleted) {
            userService.getSecureDataRepo().getStandardFileEncrypterDecrypter()
                    .encryptAndEncodeFile(signedInUserProfile.returnJSONObject().toJSONString(),
                            userService.getSecureDataRepo().getProfilesDirPath() +
                                    userService.getSecureDataRepo().getSignedInUserProfileFileName());
        }
        userService.signOutVariablesCleanUp();
        userService = null;
        observablePlatformNamesList = null;
        signedInUserProfile = null;
        profileFileWasDeleted = false;
        windowRootBorderPane.getScene().getWindow().hide();
        PasswordKeeperMain.getWindowCreationManager().showSignInWindow(windowRootBorderPane.getScene().getWindow());
        PasswordKeeperMain.continueProgramOrTerminate(this);
    }

    @FXML
    public void handleMinimizeAppButton() {
        ((Stage) windowRootBorderPane.getScene().getWindow()).setIconified(true);
    }

    @FXML
    public void handleMaximizeAppButton() {
        Stage mainStage = ((Stage) windowRootBorderPane.getScene().getWindow());
        if (mainStage.isMaximized()) {
            maximizeButton.setText("Maximize");
            mainStage.setMaximized(false);
        } else {
            maximizeButton.setText("Diminish");
            mainStage.setMaximized(true);
        }
    }

    @FXML
    public void handleAddNewCredentialButton() {
        boolean wasCredentialAdded = userService.managePlatformCredentialsData(
                windowRootBorderPane.getScene().getWindow(), 'C', selectedCredential);
        if (wasCredentialAdded) {
            int newlyAddedCredentialIdx = signedInUserProfile.getPlatformCredentials().size() - 1;
            observablePlatformNamesList.add(signedInUserProfile.getPlatformCredentials().get(newlyAddedCredentialIdx).getPlatformName());
            userService.updateSignedInUserProfile();
        }
    }

    @FXML
    public void handleUpdateCredentialButton() {
        boolean wasCredentialUpdated = userService.managePlatformCredentialsData(
                windowRootBorderPane.getScene().getWindow(), 'U', selectedCredential);
        if (wasCredentialUpdated) {
            PlatformCredential newPlatformCredential = signedInUserProfile.getPlatformCredentials().get(indexOfSelectedPlatform);
            observablePlatformNamesList.set(indexOfSelectedPlatform, newPlatformCredential.getPlatformName());
            userService.updateSignedInUserProfile();
            handleClearCredentialSelectionButton();
        }
    }

    @FXML
    public void handleRemoveCredentialButton() {
        String platformToDelete = platformNamesListView.getSelectionModel().getSelectedItem();
        if (platformToDelete == null) return;
        removeCredentialButton.setDisable(true);
        if (!isActionConfirmed("credential removal",
                "Your '" + platformToDelete + "' credential will be deleted")) {
            removeCredentialButton.setDisable(false);
            return;
        }
        PlatformCredential credentialToRemove = userService
                .findPlatformCredentialByPlatformName(platformToDelete);
        observablePlatformNamesList.remove(credentialToRemove.getPlatformName());
        signedInUserProfile.getPlatformCredentials().remove(credentialToRemove);
        userService.updateSignedInUserProfile();
        removeCredentialButton.setDisable(false);
        handleClearCredentialSelectionButton();
    }

    private boolean isActionConfirmed(String actionConfirmationTitle, String headerText) {
        return PasswordKeeperMain.getWindowCreationManager()
                .showConfirmationDialog("Confirmation - " + actionConfirmationTitle, headerText);
    }

    @FXML
    public void handleClearCredentialSelectionButton() {
        platformNamesListView.getSelectionModel().clearSelection();
        removeCredentialButton.setDisable(true);
        updateCredentialsButton.setDisable(true);
        platformNameLabel.setText(" Platform: ");
        selectedCredential.clearAllPlatformAttributes();
        indexOfSelectedPlatform = -1;
        usernameTextField.setText("");
        passwordTextField.setText("");
    }

    private void clearVisibleDataInFields() {
        platformNameLabel.setText(" Platform: ");
        usernameTextField.setText("");
        passwordTextField.setText("");
    }

}
