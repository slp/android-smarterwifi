package net.kismetwireless.android.smarterwifimanager;

import android.util.Log;

/**
 * Created by dragorn on 5/20/14.
 */
public class LogAlias {
    private static boolean LOGGING_ENABLED = true;

    public static void d(String name, String text) {
        if (LOGGING_ENABLED)
            Log.d(name, text);
    }

}
