package online.beapp.afpurchase;

import android.content.Context;
import android.content.SharedPreferences;

class SharedPrefs {

    private static String SHARED_PREFS_FILE_NAME = "name_on_cake_shared_prefs";
    private static final String USER_KEY = "USER_KEY";
    private static final String PURCHASE_KEY = "PURCHASE_KEY";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE);
    }

    static void setPurchased(Context context, boolean value) {
        getPrefs(context).edit().putBoolean(PURCHASE_KEY, value).apply();
    }

    static boolean getIfPurchased(Context context) {
        return getPrefs(context).getBoolean(PURCHASE_KEY, false);
    }

    static String getUser(Context context) {
        return getPrefs(context).getString(USER_KEY,"empty");
    }

    static void setUser(Context context, String user) {
        getPrefs(context).edit().putString(USER_KEY, user).apply();
    }

}
