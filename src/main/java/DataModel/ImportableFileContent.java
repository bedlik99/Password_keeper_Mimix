package DataModel;

public class ImportableFileContent {

    private String password;
    private String securedProfileDetails;

    public ImportableFileContent() {
        this.password = "";
        this.securedProfileDetails = "";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecuredProfileDetails() {
        return securedProfileDetails;
    }

    public void setSecuredProfileDetails(String securedProfileDetails) {
        this.securedProfileDetails = securedProfileDetails;
    }

}