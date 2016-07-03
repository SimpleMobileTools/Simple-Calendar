package com.simplemobiletools.calendar;

import android.graphics.Color;
import android.util.Log;
import android.util.SparseBooleanArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static int adjustAlpha(int color, float factor) {
        final int alpha = Math.round(Color.alpha(color) * factor);
        final int red = Color.red(color);
        final int green = Color.green(color);
        final int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static String serializeArray(SparseBooleanArray arr) {
        JSONObject json = new JSONObject();
        try {
            for (int i = 0; i < arr.size(); i++) {
                int key = arr.keyAt(i);
                json.put(String.valueOf(key), arr.get(key));
            }
        } catch (JSONException e) {
            Log.e(TAG, "serializeArray " + e.getMessage());
        }
        return json.toString();
    }

    public static SparseBooleanArray deserializeJson(String string) {
        final SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
        try {
            final JSONObject json = new JSONObject(string);
            final Iterator<String> iter = json.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Boolean value = (Boolean) json.get(key);
                sparseBooleanArray.put(Integer.parseInt(key), value);
            }
        } catch (JSONException e) {
            Log.e(TAG, "deserializeJson " + e.getMessage());
        }
        return sparseBooleanArray;
    }
}
