package DataModel;

import org.json.simple.JSONObject;

public class SecuredProfileDetails {

    private String profileFileContent;
    private String securedProfileUserName;

    public SecuredProfileDetails() {
        this.profileFileContent = "";
        this.securedProfileUserName = "";
    }

    public SecuredProfileDetails(String profileFileContent, String securedProfileUserName) {
        this.profileFileContent = profileFileContent;
        this.securedProfileUserName = securedProfileUserName;
    }

    public String getProfileFileContent() {
        return profileFileContent;
    }

    public void setProfileFileContent(String profileFileContent) {
        this.profileFileContent = profileFileContent;
    }

    public String getSecuredProfileUserName() {
        return securedProfileUserName;
    }

    public void setSecuredProfileUserName(String securedProfileUserName) {
        this.securedProfileUserName = securedProfileUserName;
    }

    public void clearVariables() {
        profileFileContent = "";
        securedProfileUserName = "";
    }

    public boolean isProfileDetailsInitialized() {
        return !profileFileContent.isBlank() && !securedProfileUserName.isBlank();
    }

    public JSONObject returnJSONObject() {
        JSONObject securedProfileCredentials = new JSONObject();
        securedProfileCredentials.put("profileFileContent", profileFileContent);
        securedProfileCredentials.put("securedProfileUserName", securedProfileUserName);
        return securedProfileCredentials;
    }

}
