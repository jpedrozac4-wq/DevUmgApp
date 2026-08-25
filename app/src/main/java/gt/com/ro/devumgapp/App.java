package gt.com.ro.devumgapp;

import android.app.Application;
import android.content.Context;

/**
 * Application entry point.
 * Keeps a global reference to the application context so core classes
 * (e.g. SessionManager) can access SharedPreferences without an Activity.
 */
public class App extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    /** Returns the global application context. */
    public static Context getContext() {
        return context;
    }
}
