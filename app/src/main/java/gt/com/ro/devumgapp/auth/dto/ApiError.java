package gt.com.ro.devumgapp.auth.dto;

import com.google.gson.annotations.SerializedName;

/** Error payload returned by the backend on failed requests. */
public class ApiError {

    @SerializedName("status")
    public Integer status;

    @SerializedName("error")
    public String error;

    @SerializedName("message")
    public String message;

    @SerializedName("path")
    public String path;

    @SerializedName("timestamp")
    public String timestamp;
}
