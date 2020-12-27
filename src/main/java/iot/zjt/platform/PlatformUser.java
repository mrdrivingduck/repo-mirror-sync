package iot.zjt.platform;

public class PlatformUser {
    private String username;
    private String token;

    public PlatformUser(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }
}
