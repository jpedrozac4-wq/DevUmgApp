package gt.com.ro.devumgapp.auth.dto;

import com.google.gson.annotations.SerializedName;

/** Payload sent to POST api/auth/login. */
public class LoginRequest {

    @SerializedName("username")
    public String username;

    @SerializedName("password")
    public String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
