package Service;

import DataModel.PlatformCredential;
import DataModel.SecuredProfileDetails;
import DataModel.UserProfile;
import Main.PasswordKeeperMain;
import Repository.SecureDataRepo;
import Util.FileEncrypterDecrypter;
import javafx.stage.Window;

import java.io.FileInputStream;
import java.io.IOException;

public class UserService {
    private static UserService userService = null;
    private final String[] responses;
    private UserProfile signedInUserProfile;
    private final SecureDataRepo secureDataRepo;

    private UserService() {
        secureDataRepo = new SecureDataRepo();
        responses = new String[]{
                "Fail. Signing in was unsuccessful.\n- Given credentials to existing profile have bad format.",
                "Fail. Signing in was unsuccessful.\n- Incorrect credentials.",
                "Fail. Profile creation was unsuccessful.\n- Username needs to be at least 3 characters and\npassword length needs to be between 8 and 40 characters" +
                        "\n- Username and password cannot contain white chars (e.g SPACE)",
                "Succeeded. Profile creation was successful.\n",
                "Succeeded. Signing in was successful.\n",
                "Fail. Profile creation was unsuccessful.\n- Profile name already exists.",
                "Fail. Profile creation was unsuccessful.\n- Profile creation process couldn't be started.",
                "Fail. Profile creation was unsuccessful.\n- Unknown error occurred during profile creation."
        };
        signedInUserProfile = null;
    }

    public static UserService getInstance() {
        if (userService == null) {
            userService = new UserService();
        }
        return userService;
    }

    public void signOutVariablesCleanUp() {
        signedInUserProfile.eraseVariablesData();
        secureDataRepo.getStreamManager().clearVariables();
        secureDataRepo.eraseSignedInUserData();
        userService = null;
    }

    public boolean managePlatformCredentialsData(Window ownerWindow, char windowMode,
                                                 PlatformCredential selectedCredential) {
        return PasswordKeeperMain.getWindowCreationManager()
                .showManagePlatformCredentialsWindow(ownerWindow, windowMode, selectedCredential);
    }

    public void updateSignedInUserProfile() {
        secureDataRepo.updateUserProfile(signedInUserProfile);
    }

    public synchronized int orderProfileCreation(String username, String password) {
        if (secureDataRepo.getApplicationState() != 0) return secureDataRepo.getApplicationState();
        if (secureDataRepo.getUserProfile('C', username) == null) {
            secureDataRepo.processNewProfileCredentials(username, password);
            return secureDataRepo.getApplicationState();
        }
        return -2;
    }

    public synchronized int verifyProfileCredentials(String username, String password) {
        if (secureDataRepo.getApplicationState() != 0) return secureDataRepo.getApplicationState();
        signedInUserProfile = secureDataRepo.getUserProfile('S', username);
        if (signedInUserProfile == null) return -1;
        secureDataRepo.verifyUserProfilePasswordWhenSigningIn(signedInUserProfile, password);
        return secureDataRepo.getApplicationState();
    }

    public boolean generateSecuredProfileCredentials() {
        secureDataRepo.createFileEncrypterDecrypter(signedInUserProfile.getProfileUsername(), false);
        PasswordKeeperMain.getWindowCreationManager()
                .showLoadingDialog("Initializing encrypting components", "LoadingWindow.fxml");
        FileEncrypterDecrypter fileEncrypterDecrypter = secureDataRepo.getCustomFileEncrypterDecrypter();
        String profilePath = secureDataRepo.getProfilesDirPath() + secureDataRepo.getSignedInUserProfileFileName();
        fileEncrypterDecrypter.encryptAndEncodeFile(signedInUserProfile.returnJSONObject().toJSONString(), profilePath);
        String encryptedProfileContent;
        byte[] encContent;
        try (FileInputStream fileIn = new FileInputStream(profilePath)) {
            encContent = new byte[fileIn.available()];
            fileIn.read(encContent);
            encryptedProfileContent = new String(encContent);
        } catch (Exception e) {
            return false;
        }
        SecuredProfileDetails securedProfileDetails = new SecuredProfileDetails(encryptedProfileContent,
                signedInUserProfile.getProfileUsername());
        secureDataRepo.setSecuredProfileDetails(securedProfileDetails);
        return fileEncrypterDecrypter.decodeAndDecryptFile(profilePath);
    }

    public PlatformCredential findPlatformCredentialByPlatformName(String platformName) {
        return (signedInUserProfile == null ? null : signedInUserProfile.getPlatformCredentials().stream().filter(
                platformCredential -> platformCredential.getPlatformName().equals(platformName)).findFirst().orElse(null));
    }

    public int validateInput(String username, String password) {
        if (username == null || password == null || username.length() < 3 || password.length() < 8 ||
                password.length() > 40 || username.matches(".*\\s.*") || password.matches(".*\\s.*")) {
            return -1;
        }
        return 0;
    }

    public SecureDataRepo getSecureDataRepo() {
        return secureDataRepo;
    }

    public String[] getResponses() {
        return responses;
    }

    public UserProfile getSignedInUserProfile() {
        return signedInUserProfile;
    }
}
