package de.otaris.simsentest.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import de.otaris.simsentest.R;

/**
 * Just a simple class for storing the logs in a persistable manner, using the shared preferences.
 *
 * @author Rafael Miranda
 * @version 0.1
 * @since 22.03.2017
 */
public class LogPersistence {

    /**
     * Persists a string array using json in our default shared preferences with a given key.
     *
     * @param context for getting the default shared preferences
     * @param key     under which key the array should be saved
     * @param values  the values that will be saved.
     */
    public static void setStringArrayPref(Context context, String key, List<String> values) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray a = new JSONArray();
        for (int i = 0; i < values.size(); i++) {
            a.put(values.get(i));
        }
        if (!values.isEmpty()) {
            editor.putString(key, a.toString());
        } else {
            editor.putString(key, null);
        }
        editor.apply();
    }

    /**
     * Appends one String to the end of the already present string array.
     * @param context to get the shared preferences
     * @param key under which key the array is saved
     * @param value the value which should be appended
     */
    public static void appendStringToArrayPref(Context context, String key, String value) {
        List<String> logs = getStringArrayPref(context, key);
        logs.add(value);
        setStringArrayPref(context, key, logs);
    }

    /**
     * Gets a string array under the key from our default shared preferences
     *
     * @param context for getting the default shared preferences
     * @param key     the key from our array
     * @return the array list gotten from our shared preferences
     */
    public static ArrayList<String> getStringArrayPref(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String json = prefs.getString(key, null);
        ArrayList<String> strings = new ArrayList<String>();
        if (json != null) {
            try {
                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++) {
                    String url = a.optString(i);
                    strings.add(url);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return strings;
    }
}
