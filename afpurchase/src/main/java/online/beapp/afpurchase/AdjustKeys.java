package online.beapp.afpurchase;

public class AdjustKeys {


    private String successToken;
    private String errorToken;
    private String restoreToken;

    AdjustKeys(String successToken, String errorToken, String restoreToken) {
        this.successToken = successToken;
        this.errorToken = errorToken;
        this.restoreToken = restoreToken;
    }

    String getSuccessToken() {
        return successToken;
    }

    public void setSuccessToken(String successToken) {
        this.successToken = successToken;
    }

    String getErrorToken() {
        return errorToken;
    }

    public void setErrorToken(String errorToken) {
        this.errorToken = errorToken;
    }

    String getRestoreToken() {
        return restoreToken;
    }

    public void setRestoreToken(String restoreToken) {
        this.restoreToken = restoreToken;
    }



}
