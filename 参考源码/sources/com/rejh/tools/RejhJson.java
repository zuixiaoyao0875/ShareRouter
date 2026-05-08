package com.rejh.tools;

import android.util.Log;
import com.sothree.slidinguppanel.library.BuildConfig;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class RejhJson {
    public static String join(JSONArray jSONArray, String str) {
        return join(jSONArray, str, null);
    }

    public static String join(JSONArray jSONArray, String str, String str2) {
        String strJoin = null;
        try {
            strJoin = jSONArray.join(str);
            strJoin.replaceAll("\"", BuildConfig.FLAVOR);
        } catch (NullPointerException e) {
            Log.w("RejhJson", "join() NullPointerException: " + e);
        } catch (JSONException e2) {
            Log.w("RejhJson", "join() JSONException: " + e2);
        }
        return strJoin == null ? str2 : strJoin;
    }

    public static String joinJsons(String str, String str2) {
        return joinJsons(str, str2, null);
    }

    public static String joinJsons(String str, String str2, String str3) {
        String strJoin;
        new JSONArray();
        try {
            strJoin = join(new JSONArray(str), str2, str3);
        } catch (NullPointerException e) {
            Log.w("RejhJson", "joinJsons() NullPointerException: " + e);
            strJoin = null;
        } catch (JSONException e2) {
            Log.w("RejhJson", "joinJsons() JSONException: " + e2);
            strJoin = null;
        }
        return strJoin == null ? str3 : strJoin;
    }

    public static JSONObject merge(JSONObject jSONObject, JSONObject jSONObject2) {
        JSONObject jSONObject3 = new JSONObject();
        JSONObject[] jSONObjectArr = {jSONObject, jSONObject2};
        for (int i = 0; i < 2; i++) {
            JSONObject jSONObject4 = jSONObjectArr[i];
            Iterator<String> itKeys = jSONObject4.keys();
            while (itKeys.hasNext()) {
                try {
                    String next = itKeys.next();
                    jSONObject3.put(next, jSONObject4.get(next));
                } catch (JSONException e) {
                    Log.w("RejhJson", "merge() JSONException: " + e);
                }
            }
        }
        return jSONObject3;
    }

    public static boolean contains(JSONArray jSONArray, String str) {
        for (int i = 0; i < jSONArray.length(); i++) {
            try {
                if (jSONArray.get(i).equals(str)) {
                    return true;
                }
            } catch (NullPointerException e) {
                Log.w("RejhJson", "clone() NullPointerException: " + e, e);
            } catch (JSONException e2) {
                Log.w("RejhJson", "clone() JSONException: " + e2, e2);
            }
        }
        return false;
    }

    public static JSONObject clone(JSONObject jSONObject) {
        JSONObject jSONObject2 = new JSONObject();
        try {
            return new JSONObject(jSONObject.toString());
        } catch (NullPointerException e) {
            Log.w("RejhJson", "clone() NullPointerException: " + e, e);
            return jSONObject2;
        } catch (JSONException e2) {
            Log.w("RejhJson", "clone() JSONException: " + e2, e2);
            return jSONObject2;
        }
    }

    public static JSONArray clone(JSONArray jSONArray) {
        JSONArray jSONArray2 = new JSONArray();
        try {
            return new JSONArray(jSONArray.toString());
        } catch (NullPointerException e) {
            Log.w("RejhJson", "contains() NullPointerException: " + e);
            return jSONArray2;
        } catch (JSONException e2) {
            Log.w("RejhJson", "contains() JSONException: " + e2);
            return jSONArray2;
        }
    }
}
