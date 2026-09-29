package gt.com.ro.devumgapp.auth.ui;

/** Side-effect free profile validation, kept separate so it can be unit tested. */
public final class ProfileValidator {
    private ProfileValidator() {}

    public static boolean isProfileValid(String username, String email, String nombre, String apellido) {
        return notBlank(username) && notBlank(email) && notBlank(nombre) && notBlank(apellido)
                && email.indexOf('@') > 0 && email.indexOf('@') < email.length() - 1;
    }

    public static PasswordError validatePassword(String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) return PasswordError.CURRENT_REQUIRED;
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 72) {
            return PasswordError.INVALID_LENGTH;
        }
        if (newPassword.equals(currentPassword)) return PasswordError.SAME_PASSWORD;
        return PasswordError.NONE;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public enum PasswordError { NONE, CURRENT_REQUIRED, INVALID_LENGTH, SAME_PASSWORD }
}
