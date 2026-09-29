package gt.com.ro.devumgapp.auth.dto;

/** Passwords exist only for the lifetime of this request and are never persisted. */
public class PasswordChangeRequest {
    public final String currentPassword;
    public final String newPassword;

    public PasswordChangeRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
