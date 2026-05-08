package com.rejh.tools;

import android.content.Context;
import android.util.Log;
import com.sothree.slidinguppanel.library.BuildConfig;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;

/* JADX INFO: loaded from: classes.dex */
public class RejhLogger {
    private static final int MAX_LINES = 512;
    private String APPTAG;
    private Context context;
    private String logfilename;
    private JSONArray logjson;
    private RejhFileIO rejhFileIO;

    public RejhLogger(Context context, String str, String str2) {
        this.APPTAG = "RejhLogger";
        this.logfilename = "RejhLogger.json";
        this.context = context;
        if (str != null) {
            this.APPTAG = str;
        }
        if (str2 != null) {
            this.logfilename = str2;
        }
        this.rejhFileIO = new RejhFileIO(this.context, this.APPTAG);
        this.logjson = getLogFile();
    }

    public void log(String str) {
        log(str, true);
    }

    public void log(String str, boolean z) {
        this.logjson = getLogFile();
        String str2 = getDateStr("HH:mm") + "  " + str;
        this.logjson.put(str2);
        while (this.logjson.length() > 512) {
            this.logjson.remove(0);
        }
        if (z) {
            Log.d(this.APPTAG, "Logger -> " + str2);
        }
        writeLogFile();
    }

    public String getLogs(String str) {
        String strJoin;
        if (str == null) {
            str = "\n";
        }
        try {
            strJoin = getLog().join(str);
        } catch (JSONException e) {
            Log.e(this.APPTAG, " -> JSONException: " + e, e);
            strJoin = BuildConfig.FLAVOR;
        }
        return strJoin.replaceAll("\"", BuildConfig.FLAVOR);
    }

    public String getLogs(String str, int i) {
        String strJoin;
        if (str == null) {
            str = "\n";
        }
        try {
            strJoin = getLog(i).join(str);
        } catch (JSONException e) {
            Log.e(this.APPTAG, " -> JSONException: " + e, e);
            strJoin = BuildConfig.FLAVOR;
        }
        return strJoin.replaceAll("\"", BuildConfig.FLAVOR);
    }

    public JSONArray getLog() {
        JSONArray logFile = getLogFile();
        this.logjson = logFile;
        return reverse(logFile);
    }

    public JSONArray getLog(int i) {
        JSONArray jSONArrayClone = RejhJson.clone(getLogFile());
        while (jSONArrayClone.length() > i) {
            jSONArrayClone.remove(0);
        }
        return reverse(jSONArrayClone);
    }

    public void logdump() {
        Log.d(this.APPTAG, "RejhLogger LOGDUMP ---------------->");
        try {
            Log.d(this.APPTAG, this.logjson.join("\n"));
        } catch (JSONException e) {
            Log.e(this.APPTAG, " -> JSONException: " + e, e);
        }
    }

    public void clear() {
        this.logjson = new JSONArray();
        log("RejhLogger CLEARED ---------------->");
    }

    private JSONArray getLogFile() {
        return this.rejhFileIO.readJsonArrayFile(this.logfilename);
    }

    private void writeLogFile() {
        this.rejhFileIO.writeJsonsArrayFile(this.logfilename, this.logjson);
    }

    private String getDateStr(String str) {
        return new SimpleDateFormat(str, Locale.getDefault()).format(new Date());
    }

    private JSONArray reverse(JSONArray jSONArray) {
        JSONArray jSONArray2 = new JSONArray();
        try {
            for (int length = jSONArray.length() - 1; length >= 0; length--) {
                jSONArray2.put(jSONArray.get(length));
            }
            return jSONArray2;
        } catch (JSONException e) {
            Log.e(this.APPTAG, " -> JSONException: " + e, e);
            return jSONArray;
        }
    }
}
