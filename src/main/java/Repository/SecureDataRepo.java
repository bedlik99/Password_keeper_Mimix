package Repository;

import DataModel.ImportableFileContent;
import DataModel.PlatformCredential;
import DataModel.SecuredProfileDetails;
import DataModel.UserProfile;
import Main.PasswordKeeperMain;
import Util.FileEncrypterDecrypter;
import javafx.application.Platform;
import javafx.stage.Window;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SecureDataRepo {
    private final Charset charset = StandardCharsets.US_ASCII;
    private final StreamManager streamManager;
    private int applicationState;
    private final String secureBinaryPCProgramName;
    private final String secureBinaryPProgramName;
    private final String secureBinaryFSProgramName;
    private final String profilesDirPath;
    private final String orderedProfileNamesFilePath;
    private final String profileSuffix;
    private SecuredProfileDetails securedProfileDetails;
    private boolean isPasswordCorrect;
    private boolean isUserSignedIn;
    private String signedInUserOrderedUsername;
    private String signedInUserProfileFileName;
    private JSONArray orderedProfileUsernamesList;
    private FileEncrypterDecrypter standardFileEncrypterDecrypter;
    private FileEncrypterDecrypter customFileEncrypterDecrypter;
    private final LinkedList<Integer> profileOrderNumbers;

    private boolean scriptFinishedOk;

    public SecureDataRepo() {
        applicationState = 0;
        profilesDirPath = "data/profiles/";
        orderedProfileNamesFilePath = "data/secrets/secured_profile_names.json";
        profileSuffix = "_profile.json";
        streamManager = new StreamManager();
        orderedProfileUsernamesList = new JSONArray();
        profileOrderNumbers = new LinkedList<>();
        loadAllProfileOrderNumbers();
        loadOrderedProfileNames();
        secureBinaryPCProgramName = "binaries/secure_pcdata";
        secureBinaryPProgramName = "binaries/secure_pdata";
        secureBinaryFSProgramName = "binaries/secure_fsecrets";
        isPasswordCorrect = false;
        signedInUserOrderedUsername = null;
        signedInUserProfileFileName = null;
        securedProfileDetails = new SecuredProfileDetails();
        standardFileEncrypterDecrypter = null;
        scriptFinishedOk = false;
    }

    public void loadOrderedProfileNames() {
        orderedProfileUsernamesList.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(orderedProfileNamesFilePath))) {
            orderedProfileUsernamesList = (JSONArray) new JSONParser().parse(br);
        } catch (Exception ignored) {
        }
    }

    public void loadAllProfileOrderNumbers() {
        File[] listOfFiles = new File(profilesDirPath).listFiles(pathname -> pathname.getName().endsWith("_profile.json"));
        if (listOfFiles == null) return;
        profileOrderNumbers.clear();
        Arrays.stream(listOfFiles).forEach(el ->
                profileOrderNumbers.add(Integer.parseInt(el.getName().substring(0, el.getName().indexOf('_')))));
        Collections.sort(profileOrderNumbers);
    }

    public static class StreamManager implements Runnable {
        private InputStream inputStream;
        private String commandResult = "";

        private void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public void clearCommandResult() {
            commandResult = "";
        }

        public void clearVariables() {
            inputStream = null;
            commandResult = "";
        }

        public String getCommandResult() {
            return commandResult;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .findFirst()
                    .ifPresent(line -> commandResult = line);
        }
    }

    private boolean runSecureScript(int mode, String input, String subSecret, String programName) {
        if ((mode != 1 && mode != 2 && mode != 3 && mode != 4 && mode != 5) || (input.isBlank() || subSecret.isBlank()))
            return false;
        ProcessBuilder builder = new ProcessBuilder();
        Process process = null;
        ExecutorService service = null;
        if (mode == 3 || mode == 4 || mode == 5) {
            builder.command(programName, String.valueOf(mode), input);
        } else {
            builder.command(programName, String.valueOf(mode), input, subSecret);
        }
        try {
            process = builder.start();
            streamManager.setInputStream(process.getInputStream());
            service = Executors.newSingleThreadExecutor();
            service.submit(streamManager);
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            if (service != null) service.shutdown();
            if (process != null) process.destroyForcibly();
        }
        return !streamManager.commandResult.isBlank();
    }

    public void processNewProfileCredentials(String userName, String password) {
        applicationState = 1;
        Platform.runLater(() -> {
            String profileUsername = BCrypt.hashpw(userName, BCrypt.gensalt());
            String profilePassword = BCrypt.hashpw(password, BCrypt.gensalt());
            boolean scriptFinishedWithSuccess =
                    runSecureScript(1, profilePassword, profileUsername, secureBinaryPProgramName);
            if (!scriptFinishedWithSuccess) return;
            UserProfile newUserProfile = new UserProfile(profileUsername, streamManager.getCommandResult());
            String newProfileNumber = returnNewProfileNumber();
            String newProfilePath = profilesDirPath + newProfileNumber + profileSuffix;
            try (BufferedWriter newProfileBW = new BufferedWriter(new FileWriter(newProfilePath));
                 BufferedWriter orderedProfileNameBW = new BufferedWriter(new FileWriter(orderedProfileNamesFilePath))) {
                String orderedProfileUsername = BCrypt.hashpw((newProfileNumber + userName), BCrypt.gensalt());
                String jsonObjectToSave = newUserProfile.returnJSONObject().toJSONString();
                newProfileBW.write(jsonObjectToSave);
                newProfileBW.flush();
                orderedProfileUsernamesList.add(orderedProfileUsername);
                orderedProfileNameBW.write(orderedProfileUsernamesList.toJSONString());
                orderedProfileNameBW.flush();
                standardFileEncrypterDecrypter = instantiateFileEncrypterDecrypter(orderedProfileUsername);
                if (standardFileEncrypterDecrypter == null) return;
                standardFileEncrypterDecrypter.encryptAndEncodeFile(jsonObjectToSave, newProfilePath);
            } catch (Exception ignore) {
            } finally {
                applicationState = 0;
                streamManager.clearCommandResult();
            }
        });
    }

    public void decryptImportedFileAndCreateProfile(File selectedFile, Window windowOwner) {
        ImportableFileContent importableFileContent = new ImportableFileContent();
        try (BufferedReader importedFileBR = new BufferedReader(new FileReader(selectedFile.getAbsolutePath()))) {
            JSONObject jsonFileContent = (JSONObject) new JSONParser().parse(importedFileBR);
            importableFileContent.setPassword((String) jsonFileContent.get("password"));
            importableFileContent.setSecuredProfileDetails((String) jsonFileContent.get("securedProfileDetails"));
            decryptImportedFileContent(importableFileContent.getSecuredProfileDetails());
            PasswordKeeperMain.getWindowCreationManager()
                    .showLoadingDialog("Decrypting profile details file", "LoadingWindow.fxml");
            importableFileContent
                    .setSecuredProfileDetails(addDoubleQuotesToJavaJsonString(getStreamManager().getCommandResult()));
            JSONObject securedProfileDetailsJson =
                    (JSONObject) new JSONParser().parse(importableFileContent.getSecuredProfileDetails());
            SecuredProfileDetails securedProfileDetails = new SecuredProfileDetails();
            securedProfileDetails.setSecuredProfileUserName((String) securedProfileDetailsJson.get("securedProfileUserName"));
            securedProfileDetails.setProfileFileContent((String) securedProfileDetailsJson.get("profileFileContent"));
            String profileUsernameToVerify = PasswordKeeperMain.getWindowCreationManager()
                    .showVerifyProfileFileCredentialWindow(windowOwner, 'U', false);
            if (profileUsernameToVerify == null) return;
            if (!BCrypt.checkpw(profileUsernameToVerify, securedProfileDetails.getSecuredProfileUserName())) {
                PasswordKeeperMain.getWindowCreationManager()
                        .showInformationDialog("Unsuccessful username verification", "Wrong username",
                                "Incorrect username to existing profile.");
                return;
            }
            if (!doesProfileUserNameExist(profileUsernameToVerify)) {
                String newProfileOrderNumber = returnNewProfileNumber();
                String newProfileFilePath = getProfilesDirPath() + newProfileOrderNumber + getProfileSuffix();
                BufferedWriter orderedProfileNameBW = new BufferedWriter(new FileWriter(orderedProfileNamesFilePath));
                BufferedWriter newProfileBW = new BufferedWriter(new FileWriter(newProfileFilePath));
                String newOrderedProfileUsername = BCrypt.hashpw((newProfileOrderNumber + profileUsernameToVerify), BCrypt.gensalt());
                newProfileBW.write(securedProfileDetails.getProfileFileContent());
                newProfileBW.flush();
                newProfileBW.close();

                createFileEncrypterDecrypter(securedProfileDetails.getSecuredProfileUserName(), false);
                PasswordKeeperMain.getWindowCreationManager()
                        .showLoadingDialog("Initializing encrypting components", "LoadingWindow.fxml");
                UserProfile userProfile = decryptChosenProfileFileAndReadUserData(
                        newProfileFilePath, profileUsernameToVerify, customFileEncrypterDecrypter);

                createFileEncrypterDecrypter(newOrderedProfileUsername, true);
                PasswordKeeperMain.getWindowCreationManager()
                        .showLoadingDialog("Initializing encrypting components", "LoadingWindow.fxml");
                standardFileEncrypterDecrypter.encryptAndEncodeFile(userProfile.returnJSONObject().toJSONString(),
                        newProfileFilePath);

                orderedProfileUsernamesList.add(newOrderedProfileUsername);
                orderedProfileNameBW.write(orderedProfileUsernamesList.toJSONString());
                orderedProfileNameBW.flush();
                orderedProfileNameBW.close();
            } else {
                UserProfile existingUserProfile = getUserProfile('S', profileUsernameToVerify);
                if (existingUserProfile == null) return;
                String passwordToVerify = PasswordKeeperMain.getWindowCreationManager()
                        .showVerifyProfileFileCredentialWindow(windowOwner, 'P', false);
                if (passwordToVerify == null || applicationState != 0) return;
                verifyUserProfilePasswordWhenSigningIn(existingUserProfile, passwordToVerify);
                PasswordKeeperMain.getWindowCreationManager()
                        .showLoadingDialog("Existing profile password verification", "LoadingWindow.fxml");
                if (!isPasswordCorrect) {
                    PasswordKeeperMain.getWindowCreationManager()
                            .showInformationDialog("Unsuccessful file import", "File could not be imported",
                                    "Incorrect password to existing profile.");
                    return;
                }
                resetCredentialsCorrectionState();
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(profilesDirPath + "tmp_imported_file.json"))) {
                    bw.write(securedProfileDetails.getProfileFileContent());
                    bw.flush();
                }

                createFileEncrypterDecrypter(securedProfileDetails.getSecuredProfileUserName(), false);
                PasswordKeeperMain.getWindowCreationManager()
                        .showLoadingDialog("Initializing encrypting components", "LoadingWindow.fxml");
                customFileEncrypterDecrypter.decodeAndDecryptFile(profilesDirPath + "tmp_imported_file.json");

                UserProfile importedUserProfileData =
                        readPlainUserProfileFromJson(profileUsernameToVerify, profilesDirPath + "tmp_imported_file.json");
                if (importedUserProfileData == null) return;
                importedUserProfileData.getPlatformCredentials().forEach(platformCredential -> {
                    PlatformCredential foundCredential = existingUserProfile
                            .getPlatformCredentials()
                            .stream()
                            .filter(existingCredential -> existingCredential.getPlatformName()
                                    .equals(platformCredential.getPlatformName())).findFirst().orElse(null);
                    if (foundCredential == null) existingUserProfile.getPlatformCredentials().add(platformCredential);
                });
                updateUserProfile(existingUserProfile);
                new File(profilesDirPath + "tmp_imported_file.json").delete();
                standardFileEncrypterDecrypter.encryptAndEncodeFile(existingUserProfile.returnJSONObject().toJSONString(),
                        profilesDirPath + signedInUserProfileFileName);
            }
        } catch (Exception ignored) {
        } finally {
            eraseSignedInUserData();
        }
    }

    public void updateSignedInUserCredentials(Window windowOwner, UserProfile signedInUserProfile) {
        String[] initialSignedInUserPlainCredentials = verifySignedInUserCredentials(windowOwner, signedInUserProfile);
        if (initialSignedInUserPlainCredentials == null) return;
        String initialSignedInUserOrderedUsername = signedInUserOrderedUsername;
        String initialSignedInUserUsername = signedInUserProfile.getProfileUsername();
        String[] updatedSignedInUserPlainCredentials = PasswordKeeperMain
                .getWindowCreationManager()
                .showUpdateProfileCredentialsWindow(windowOwner, initialSignedInUserPlainCredentials);
        if (updatedSignedInUserPlainCredentials == null) return;
        if (!updatedSignedInUserPlainCredentials[0].equals(initialSignedInUserPlainCredentials[0]) &&
                doesProfileUserNameExist(updatedSignedInUserPlainCredentials[0])) {
            PasswordKeeperMain.getWindowCreationManager().showInformationDialog(
                    "Unsuccessful profile update",
                    "Occupied username",
                    "Profile with given username already exists.");
            return;
        }

        if (!updatedSignedInUserPlainCredentials[0].equals(initialSignedInUserPlainCredentials[0])) {
            orderedProfileUsernamesList.remove(signedInUserOrderedUsername);
            String newHashedProfileUsername = BCrypt.hashpw(updatedSignedInUserPlainCredentials[0], BCrypt.gensalt());
            signedInUserProfile.setProfileUsername(newHashedProfileUsername);
            String newHashedOrderedProfileUsername = BCrypt
                    .hashpw(signedInUserProfileFileName.charAt(0) + updatedSignedInUserPlainCredentials[0],
                            BCrypt.gensalt());
            orderedProfileUsernamesList.add(newHashedOrderedProfileUsername);
            try (BufferedWriter orderedProfileNameBW = new BufferedWriter(new FileWriter(orderedProfileNamesFilePath))) {
                orderedProfileNameBW.write(orderedProfileUsernamesList.toJSONString());
                orderedProfileNameBW.flush();

                int it = 1;
                LinkedList<PlatformCredential> initialPlatformCredentials = new LinkedList<>(signedInUserProfile.getPlatformCredentials());
                for (PlatformCredential platformCredential : signedInUserProfile.getPlatformCredentials()) {
                    decryptSelectedPlatformCredential(platformCredential.getPlatformUsername(), initialSignedInUserUsername);
                    PasswordKeeperMain.getWindowCreationManager()
                            .showLoadingDialog("Acquiring " + it + " platform username", "LoadingWindow.fxml");
                    String decryptedPlatformUsername = streamManager.commandResult;

                    decryptSelectedPlatformCredential(platformCredential.getPlatformPassword(), initialSignedInUserUsername);
                    PasswordKeeperMain.getWindowCreationManager()
                            .showLoadingDialog("Acquiring " + it + " platform password", "LoadingWindow.fxml");
                    String decryptedPlatformPassword = streamManager.commandResult;

                    encryptInputCredential(decryptedPlatformUsername, newHashedProfileUsername);
                    PasswordKeeperMain.getWindowCreationManager()
                            .showLoadingDialog("Securing " + it + " platform username after change", "LoadingWindow.fxml");
                    platformCredential.setPlatformUsername(streamManager.commandResult);

                    encryptInputCredential(decryptedPlatformPassword, newHashedProfileUsername);
                    PasswordKeeperMain.getWindowCreationManager()
                            .showLoadingDialog("Securing " + it + " platform password after change", "LoadingWindow.fxml");
                    platformCredential.setPlatformPassword(streamManager.commandResult);
                    it++;
                }
                String renewedPasswordHash = BCrypt.hashpw(updatedSignedInUserPlainCredentials[1], BCrypt.gensalt());
                Platform.runLater(() -> setScriptFinishedOk(
                        runSecureScript(1, renewedPasswordHash, newHashedProfileUsername, secureBinaryPProgramName)
                ));
                PasswordKeeperMain.getWindowCreationManager()
                        .showLoadingDialog("Generating password", "LoadingWindow.fxml");
                if (!scriptFinishedOk) {
                    signedInUserProfile.setProfileUsername(initialSignedInUserOrderedUsername);
                    orderedProfileUsernamesList.remove(newHashedOrderedProfileUsername);
                    orderedProfileUsernamesList.add(initialSignedInUserOrderedUsername);
                    signedInUserProfile.setPlatformCredentials(initialPlatformCredentials);
                    orderedProfileNameBW.write(orderedProfileUsernamesList.toJSONString());
                    orderedProfileNameBW.flush();
                    return;
                }
                signedInUserProfile.setProfilePassword(streamManager.commandResult);
                updateUserProfile(signedInUserProfile);
                createFileEncrypterDecrypter(newHashedOrderedProfileUsername, true);
            } catch (Exception ignored) {
            }
            return;
        }
        if (!updatedSignedInUserPlainCredentials[1].equals(initialSignedInUserPlainCredentials[1])) {
            String newPasswordHash = BCrypt.hashpw(updatedSignedInUserPlainCredentials[1], BCrypt.gensalt());
            Platform.runLater(() -> setScriptFinishedOk(
                    runSecureScript(1, newPasswordHash, signedInUserProfile.getProfileUsername(), secureBinaryPProgramName)
            ));
            PasswordKeeperMain.getWindowCreationManager()
                    .showLoadingDialog("Generating password", "LoadingWindow.fxml");
            signedInUserProfile.setProfilePassword(streamManager.commandResult);
            updateUserProfile(signedInUserProfile);
            createFileEncrypterDecrypter(initialSignedInUserOrderedUsername, true);
        }
    }

    private String[] verifySignedInUserCredentials(Window windowOwner, UserProfile signedInUserProfile) {
        String userNameToVerify = PasswordKeeperMain.getWindowCreationManager()
                .showVerifyProfileFileCredentialWindow(windowOwner, 'U', true);
        if (userNameToVerify == null) return null;
        if (!BCrypt.checkpw(userNameToVerify, signedInUserProfile.getProfileUsername())) {
            PasswordKeeperMain.getWindowCreationManager()
                    .showInformationDialog("Unsuccessful username verification", "Wrong username",
                            "Incorrect username to signed in profile. Try again.");
            return null;
        }
        String passwordToVerify = PasswordKeeperMain.getWindowCreationManager()
                .showVerifyProfileFileCredentialWindow(windowOwner, 'P', true);
        if (passwordToVerify == null || applicationState != 0) return null;
        verifyUserProfilePassword(signedInUserProfile, passwordToVerify);
        PasswordKeeperMain.getWindowCreationManager()
                .showLoadingDialog("Signed in profile password verification", "LoadingWindow.fxml");
        if (!isPasswordCorrect) {
            PasswordKeeperMain.getWindowCreationManager()
                    .showInformationDialog("Unsuccessful password verification", "Wrong password",
                            "Incorrect password to signed in profile. Try again.");
            return null;
        }
        resetCredentialsCorrectionState();
        return new String[]{userNameToVerify, passwordToVerify};
    }

    public UserProfile getUserProfile(char mode, String plainUsername) {
        String foundProfileFileName = null, orderedProfileName, modifiedUsername, profileFileName = null;
        loadOrderedProfileNames();
        loadAllProfileOrderNumbers();
        for (Object profileName : orderedProfileUsernamesList) {
            orderedProfileName = profileName.toString();
            for (Integer profileOrderNumber : profileOrderNumbers) {
                modifiedUsername = profileOrderNumber + plainUsername;
                if (BCrypt.checkpw(modifiedUsername, orderedProfileName)) {
                    foundProfileFileName = profileOrderNumber + profileSuffix;
                    profileFileName = foundProfileFileName;
                    signedInUserOrderedUsername = orderedProfileName;
                    break;
                }
            }
            if (foundProfileFileName != null) break;
        }
        if (profileFileName == null) return null;
        if (mode == 'C') return new UserProfile();
        if (mode == 'S') {
            createFileEncrypterDecrypter(signedInUserOrderedUsername, true);
            PasswordKeeperMain.getWindowCreationManager()
                    .showLoadingDialog("Initializing encrypting components", "LoadingWindow.fxml");
            return decryptChosenProfileFileAndReadUserData((profilesDirPath + profileFileName), plainUsername,
                    standardFileEncrypterDecrypter);
        }
        return null;
    }

    private UserProfile decryptChosenProfileFileAndReadUserData(String foundProfileFilePath, String plainUsername,
                                                                FileEncrypterDecrypter fileEncrypterDecrypter) {
        if (foundProfileFilePath == null || plainUsername == null || fileEncrypterDecrypter == null) return null;
        boolean decryptionWasSuccessful = fileEncrypterDecrypter
                .decodeAndDecryptFile(foundProfileFilePath);
        if (!decryptionWasSuccessful) return null;
        UserProfile userProfile = readPlainUserProfileFromJson(plainUsername, foundProfileFilePath);
        signedInUserProfileFileName = foundProfileFilePath.substring(foundProfileFilePath.lastIndexOf('/') + 1);
        return userProfile;
    }

    private boolean doesProfileUserNameExist(String plainUsername) {
        loadOrderedProfileNames();
        loadAllProfileOrderNumbers();
        if (orderedProfileUsernamesList == null || orderedProfileUsernamesList.isEmpty()) return false;
        String modifiedUsername;
        boolean wasProfileFound = false;
        loadOrderedProfileNames();
        for (Object profileName : orderedProfileUsernamesList) {
            for (Integer profileOrderNumber : profileOrderNumbers) {
                modifiedUsername = profileOrderNumber + plainUsername;
                wasProfileFound = BCrypt.checkpw(modifiedUsername, profileName.toString());
                if (wasProfileFound) break;
            }
            if (wasProfileFound) break;
        }
        return wasProfileFound;
    }

    private UserProfile readPlainUserProfileFromJson(String plainUsername, String foundProfileFilePath) {
        JSONParser jsonParser = new JSONParser();
        UserProfile searchedUserProfile = new UserProfile();
        JSONObject userProfileJsonObj = new JSONObject();
        try (BufferedReader br = new BufferedReader(new FileReader(foundProfileFilePath))) {
            userProfileJsonObj = (JSONObject) jsonParser.parse(br);
            if (BCrypt.checkpw(plainUsername, (String) userProfileJsonObj.get("profileUsername"))) {
                searchedUserProfile.setProfileUsername((String) userProfileJsonObj.get("profileUsername"));
                searchedUserProfile.setProfilePassword((String) userProfileJsonObj.get("profilePassword"));
                JSONArray jsonArray = (JSONArray) userProfileJsonObj.get("platformCredentials");
                jsonArray.forEach(platformCredentialJsonObj -> {
                    JSONObject platformCredentialJSON = (JSONObject) platformCredentialJsonObj;
                    searchedUserProfile.getPlatformCredentials().add(new PlatformCredential(
                            (String) platformCredentialJSON.get("platformName"),
                            (String) platformCredentialJSON.get("platformUsername"),
                            (String) platformCredentialJSON.get("platformPassword")
                    ));
                });
            } else {
                getStandardFileEncrypterDecrypter().encryptAndEncodeFile(userProfileJsonObj.toJSONString(),
                        profilesDirPath + foundProfileFilePath);
                eraseSignedInUserData();
                return null;
            }
        } catch (Exception e) {
            getStandardFileEncrypterDecrypter().encryptAndEncodeFile(userProfileJsonObj.toJSONString(),
                    profilesDirPath + foundProfileFilePath);
            eraseSignedInUserData();
            return null;
        }
        return searchedUserProfile;
    }

    public void updateUserProfile(UserProfile userProfile) {
        String profilePathToUpdate = profilesDirPath + signedInUserProfileFileName;
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(profilePathToUpdate))) {
            bufferedWriter.write(userProfile.returnJSONObject().toJSONString());
            bufferedWriter.flush();
        } catch (Exception ignored) {
        }
    }

    private FileEncrypterDecrypter instantiateFileEncrypterDecrypter(String secret) {
        String secretPassword, salt;
        byte[] iv;
        runSecureScript(3, secret, "-", secureBinaryFSProgramName);
        secretPassword = streamManager.getCommandResult();
        runSecureScript(4, secret, "-", secureBinaryFSProgramName);
        iv = charset.encode(streamManager.getCommandResult()).array();
        runSecureScript(5, secret, "-", secureBinaryFSProgramName);
        salt = streamManager.getCommandResult();
        try {
            return new FileEncrypterDecrypter(secretPassword, iv, salt);
        } catch (Exception e) {
            return null;
        }
    }

    public void createFileEncrypterDecrypter(String secret, boolean isStandardEncrypter) {
        if (secret == null || secret.isBlank()) return;
        applicationState = 1;
        Platform.runLater(() -> {
            if (isStandardEncrypter)
                standardFileEncrypterDecrypter = instantiateFileEncrypterDecrypter(secret);
            else
                customFileEncrypterDecrypter = instantiateFileEncrypterDecrypter(secret);
            applicationState = 0;
        });
    }

    public void verifyUserProfilePasswordWhenSigningIn(UserProfile signInUserProfile, String plainPassword) {
        applicationState = 1;
        Platform.runLater(() -> {
            boolean scriptFinishedWithSuccess = runSecureScript(2, signInUserProfile.getProfilePassword(),
                    signInUserProfile.getProfileUsername(), secureBinaryPProgramName);
            if (scriptFinishedWithSuccess) {
                isPasswordCorrect = BCrypt.checkpw(plainPassword, streamManager.getCommandResult());
                if (!isPasswordCorrect) {
                    standardFileEncrypterDecrypter = instantiateFileEncrypterDecrypter(signedInUserOrderedUsername);
                    standardFileEncrypterDecrypter.encryptAndEncodeFile(signInUserProfile.returnJSONObject().toJSONString(),
                            getProfilesDirPath() + signedInUserProfileFileName);
                    signInUserProfile.clearCredentials();
                    eraseSignedInUserData();
                }
            } else {
                signInUserProfile.clearCredentials();
                eraseSignedInUserData();
            }
            applicationState = 0;
            streamManager.clearCommandResult();
        });
    }

    private void verifyUserProfilePassword(UserProfile signInUserProfile, String plainPassword) {
        applicationState = 1;
        Platform.runLater(() -> {
            boolean scriptFinishedWithSuccess = runSecureScript(2, signInUserProfile.getProfilePassword(),
                    signInUserProfile.getProfileUsername(), secureBinaryPProgramName);
            if (scriptFinishedWithSuccess) {
                isPasswordCorrect = BCrypt.checkpw(plainPassword, streamManager.getCommandResult());
            } else {
                PasswordKeeperMain.getWindowCreationManager()
                        .showInformationDialog("Unsuccessful password verification import",
                                "Password could not be imported",
                                "Error during password verification. Try again.");
            }
            applicationState = 0;
            streamManager.clearCommandResult();
        });
    }

    public String returnNewProfileNumber() {
        List<File> fileList = returnProfileNames();
        if (fileList.size() == 0) return "1";
        File highestOrderedProfile = fileList.stream().reduce((file1, file2) -> {
            if (Integer.parseInt(file1.getName().substring(0, file1.getName().indexOf("_"))) >=
                    Integer.parseInt(file2.getName().substring(0, file2.getName().indexOf("_")))) {
                return file1;
            } else {
                return file2;
            }
        }).get();
        return String.valueOf(Integer.parseInt(
                highestOrderedProfile.getName().substring(0, highestOrderedProfile.getName().indexOf("_"))) + 1);
    }

    private List<File> returnProfileNames() {
        return Arrays.stream(Objects.requireNonNull(new File(profilesDirPath).listFiles()))
                .filter((file) -> file.getName().endsWith(profileSuffix)).collect(Collectors.toList());
    }

    public String addDoubleQuotesToJavaJsonString(String jsonStringWithoutDoubleQuotes) {
        jsonStringWithoutDoubleQuotes = jsonStringWithoutDoubleQuotes.replaceAll(":", "\":\"");
        jsonStringWithoutDoubleQuotes = jsonStringWithoutDoubleQuotes.replaceAll(",", "\",\"");
        jsonStringWithoutDoubleQuotes = jsonStringWithoutDoubleQuotes.replaceAll("\\{", "{\"");
        jsonStringWithoutDoubleQuotes = jsonStringWithoutDoubleQuotes.replaceAll("}", "\"}");
        return jsonStringWithoutDoubleQuotes;
    }

    public void encryptInputCredential(String input, String subSecret) {
        applicationState = 1;
        Platform.runLater(() -> {
            runSecureScript(1, input, subSecret, secureBinaryPCProgramName);
            applicationState = 0;
        });
    }

    public void decryptSelectedPlatformCredential(String input, String subSecret) {
        applicationState = 1;
        Platform.runLater(() -> {
            runSecureScript(2, input, subSecret, secureBinaryPCProgramName);
            applicationState = 0;
        });
    }

    public void encryptGeneratedFileContent(String content) {
        applicationState = 1;
        Platform.runLater(() -> {
            runSecureScript(1, content, "-", secureBinaryPProgramName);
            applicationState = 0;
        });
    }

    public void decryptImportedFileContent(String content) {
        applicationState = 1;
        Platform.runLater(() -> {
            runSecureScript(2, content, "-", secureBinaryPProgramName);
            applicationState = 0;
        });
    }

    public void eraseSignedInUserData() {
        securedProfileDetails.clearVariables();
        isUserSignedIn = false;
        if (signedInUserProfileFileName != null) signedInUserProfileFileName = null;
        if (signedInUserOrderedUsername != null) signedInUserOrderedUsername = null;
        if (standardFileEncrypterDecrypter != null) {
            standardFileEncrypterDecrypter.clearSecrets();
            standardFileEncrypterDecrypter = null;
        }
        if (customFileEncrypterDecrypter != null) {
            customFileEncrypterDecrypter.clearSecrets();
            customFileEncrypterDecrypter = null;
        }
    }


    public int getApplicationState() {
        return applicationState;
    }

    public void setApplicationState(int applicationState) {
        this.applicationState = applicationState;
    }

    public String getProfilesDirPath() {
        return profilesDirPath;
    }

    public boolean isPasswordCorrect() {
        return isPasswordCorrect;
    }

    public void resetCredentialsCorrectionState() {
        this.isPasswordCorrect = false;
    }

    public StreamManager getStreamManager() {
        return streamManager;
    }

    public String getProfileSuffix() {
        return profileSuffix;
    }

    public String getOrderedProfileNamesFilePath() {
        return orderedProfileNamesFilePath;
    }

    public String getSignedInUserOrderedUsername() {
        return signedInUserOrderedUsername;
    }

    public void setSignedInUserOrderedUsername(String signedInUserOrderedUsername) {
        this.signedInUserOrderedUsername = signedInUserOrderedUsername;
    }

    public String getSignedInUserProfileFileName() {
        return signedInUserProfileFileName;
    }

    public void setSignedInUserProfileFileName(String signedInUserProfileFileName) {
        this.signedInUserProfileFileName = signedInUserProfileFileName;
    }

    public SecuredProfileDetails getSecuredProfileDetails() {
        return securedProfileDetails;
    }

    public void setSecuredProfileDetails(SecuredProfileDetails securedProfileDetails) {
        this.securedProfileDetails = securedProfileDetails;
    }

    public JSONArray getOrderedProfileUsernamesList() {
        return orderedProfileUsernamesList;
    }

    public FileEncrypterDecrypter getStandardFileEncrypterDecrypter() {
        return standardFileEncrypterDecrypter;
    }

    public void setStandardFileEncrypterDecrypter(FileEncrypterDecrypter standardFileEncrypterDecrypter) {
        this.standardFileEncrypterDecrypter = standardFileEncrypterDecrypter;
    }

    public boolean isUserSignedIn() {
        return isUserSignedIn;
    }

    public void setUserSignedIn(boolean userSignedIn) {
        isUserSignedIn = userSignedIn;
    }

    public LinkedList<Integer> getProfileOrderNumbers() {
        return profileOrderNumbers;
    }

    public FileEncrypterDecrypter getCustomFileEncrypterDecrypter() {
        return customFileEncrypterDecrypter;
    }

    public void setScriptFinishedOk(boolean scriptFinishedOk) {
        this.scriptFinishedOk = scriptFinishedOk;
    }
}
