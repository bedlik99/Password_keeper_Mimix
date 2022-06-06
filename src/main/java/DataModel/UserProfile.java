package DataModel;

import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class UserProfile {

    private String profileUsername;
    private String profilePassword;
    private List<PlatformCredential> platformCredentials = new LinkedList<>();

    public UserProfile() {}
    public UserProfile(String profileUsername, String profilePassword) {
        this.profileUsername = profileUsername;
        this.profilePassword = profilePassword;
    }

    public String getProfileUsername() {
        return profileUsername;
    }

    public void setProfileUsername(String profileUsername) {
        this.profileUsername = profileUsername;
    }

    public String getProfilePassword() {
        return profilePassword;
    }

    public void setProfilePassword(String profilePassword) {
        this.profilePassword = profilePassword;
    }

    public List<PlatformCredential> getPlatformCredentials() {
        return platformCredentials;
    }

    public void setPlatformCredentials(List<PlatformCredential> platformCredentials) {
        this.platformCredentials = platformCredentials;
    }

    public JSONObject returnJSONObject() {
        JSONObject userProfileDetails = new JSONObject();
        userProfileDetails.put("profileUsername", profileUsername);
        userProfileDetails.put("profilePassword", profilePassword);
        userProfileDetails.put("platformCredentials", platformCredentials);
        return userProfileDetails;
    }

    public void clearCredentials() {
        profileUsername = "";
        profilePassword = "";
        platformCredentials.clear();
    }

    public void eraseVariablesData() {
        profileUsername = null;
        profilePassword = null;
        platformCredentials.clear();
        platformCredentials = null;
    }

}
