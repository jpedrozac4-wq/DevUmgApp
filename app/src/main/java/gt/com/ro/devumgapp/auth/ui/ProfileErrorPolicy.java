package gt.com.ro.devumgapp.auth.ui;

/** HTTP error classification independent from Android UI. */
public final class ProfileErrorPolicy {
    private ProfileErrorPolicy() {}

    public static Kind classify(int status, boolean passwordRequest) {
        switch (status) {
            case 400: return Kind.INVALID_DATA;
            case 401: return passwordRequest ? Kind.CURRENT_PASSWORD : Kind.EXPIRED_SESSION;
            case 409: return Kind.DUPLICATE;
            case 429: return Kind.RATE_LIMIT;
            default: return Kind.SERVER;
        }
    }

    public enum Kind { INVALID_DATA, EXPIRED_SESSION, CURRENT_PASSWORD, DUPLICATE, RATE_LIMIT, SERVER }
}
