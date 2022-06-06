package DataModel;

public class PlatformCredential {
    private String platformName;
    private String platformUsername;
    private String platformPassword;

    public PlatformCredential() {
        platformName = "";
        platformUsername = "";
        platformPassword = "";
    }

    public PlatformCredential(String platformName, String platformUsername, String platformPassword) {
        this.platformName = platformName;
        this.platformUsername = platformUsername;
        this.platformPassword = platformPassword;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformUsername() {
        return platformUsername;
    }

    public void setPlatformUsername(String platformUsername) {
        this.platformUsername = platformUsername;
    }

    public String getPlatformPassword() {
        return platformPassword;
    }

    public void setPlatformPassword(String platformPassword) {
        this.platformPassword = platformPassword;
    }

    public void setAllPlatformAttributes(String platformName, String platformUsername, String platformPassword) {
        this.platformName = platformName;
        this.platformUsername = platformUsername;
        this.platformPassword = platformPassword;
    }

    public void clearAllPlatformAttributes() {
        this.platformName = "";
        this.platformUsername = "";
        this.platformPassword = "";
    }

    @Override
    public String toString() {
        return "{" +
                "\"platformName\":\"" + platformName + "\"," +
                "\"platformUsername\":\"" + platformUsername + "\"," +
                "\"platformPassword\":\"" + platformPassword + "\"" +
                "}";
    }
}
