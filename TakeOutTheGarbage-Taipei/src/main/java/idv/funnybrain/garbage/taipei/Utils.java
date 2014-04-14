package idv.funnybrain.garbage.taipei;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Freeman on 2014/3/18.
 */
public class Utils {
    private static final boolean D = true;
    private static final String TAG = "Utils";

    public static final String GARBAGE_TAIPEI_URL =
            "http://data.taipei.gov.tw/opendata/apply/query/MTQ2ODU5NjMtREQxNS00MTczLUE4NDktOTlCNEI0MzZDQjcy?$format=json";
    public static final String PREFERENCE_FILE_NAME = "garbage_taipei";

    private static String selectedDist = "";

    public void saveDataToSharedPreference(Context context, String key, int value) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public int getDataFromSharedPreference(Context context, String key) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        return pref.getInt(key, Integer.MIN_VALUE);
    }

    public void setSelectedDist(String dist) {
        if(D) { Log.d(TAG, "setSelectedDist: " + dist); }
        selectedDist = dist;
    }

    public String getSelectedDist() {
        if(D) { Log.d(TAG, "getSelectedDist: " + selectedDist); }
        return selectedDist;
    }
}
