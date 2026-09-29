package gt.com.ro.devumgapp.auth.dto;

import com.google.gson.annotations.SerializedName;

/** Updated profile plus the backend instruction to authenticate again. */
public class ProfileUpdateResponse extends LoginResponse {
    @SerializedName("requiereNuevoLogin")
    public boolean requiereNuevoLogin;
}
