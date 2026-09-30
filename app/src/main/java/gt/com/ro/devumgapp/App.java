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
    private static int startedActivities;
    public static boolean isForeground() { return startedActivities > 0; }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(android.app.Activity activity) { startedActivities++; }
            @Override public void onActivityStopped(android.app.Activity activity) { startedActivities=Math.max(0,startedActivities-1); }
            @Override public void onActivityCreated(android.app.Activity a,android.os.Bundle b){}
            @Override public void onActivityResumed(android.app.Activity a){}
            @Override public void onActivityPaused(android.app.Activity a){}
            @Override public void onActivitySaveInstanceState(android.app.Activity a,android.os.Bundle b){}
            @Override public void onActivityDestroyed(android.app.Activity a){}
        });
    }

    /** Returns the global application context. */
    public static Context getContext() {
        return context;
    }
}
