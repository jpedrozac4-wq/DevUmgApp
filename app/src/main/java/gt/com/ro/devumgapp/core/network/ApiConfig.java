package gt.com.ro.devumgapp.core.network;

import gt.com.ro.devumgapp.BuildConfig;

/** Central API configuration. */
public final class ApiConfig {

    /** Base URL of the REST backend (with trailing slash). */
    public static final String BASE_URL = BuildConfig.BASE_URL;

    private ApiConfig() {
        // Utility class: no instances.
    }
}
