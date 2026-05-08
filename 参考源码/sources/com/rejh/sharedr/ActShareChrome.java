package com.rejh.sharedr;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class ActShareChrome extends AppCompatActivity {
    private static final int RESULT_SHARE_ACTION_SEND = 1;
    private Context context;
    private String APPTAG = "Sharedr";
    private long newIntentTime = -1;

    private String ifNotNullStr(String str, String str2) {
        return str == null ? str2 : str;
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(bin.p002mt.plus.TranslationData.R.layout.activity_act_share_chrome);
        Log.i(this.APPTAG, "ActShareChrome.onCreate()");
        this.context = this;
        Log.d(this.APPTAG, " -> Invoke onNewIntent");
        onNewIntent(getIntent());
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        Log.i(this.APPTAG, "ActShareChrome.onResume()");
        handleIntent();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(this.APPTAG, "ActShareChrome.onNewIntent()");
        this.newIntentTime = System.currentTimeMillis();
        setIntent(intent);
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        Log.i(this.APPTAG, "ActShareChrome.onActivityResult()");
        if (i == 1) {
            finish();
        } else {
            Log.w(this.APPTAG, " > requestCode unhandled: " + i);
            finish();
        }
    }

    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private void handleIntent() {
        String stringExtra;
        String strUrldecoder;
        Log.d(this.APPTAG, "ActShareChrome.handleIntent()");
        Intent intent = getIntent();
        long jCurrentTimeMillis = System.currentTimeMillis() - this.newIntentTime;
        if (intent != null && intent.hasExtra("action") && jCurrentTimeMillis < 1000) {
            String stringExtra2 = intent.getStringExtra("action");
            stringExtra2.hashCode();
            if (stringExtra2.equals("share")) {
                String stringExtra3 = intent.hasExtra("type") ? intent.getStringExtra("type") : "url";
                stringExtra3.hashCode();
                byte b = -1;
                switch (stringExtra3.hashCode()) {
                    case -528744463:
                        if (stringExtra3.equals("ACTION_SEND")) {
                            b = 0;
                        }
                        break;
                    case -528651506:
                        if (stringExtra3.equals("ACTION_VIEW")) {
                            b = 1;
                        }
                        break;
                    case -120765253:
                        if (stringExtra3.equals("ACTION_SEND_TEXT")) {
                            b = 2;
                        }
                        break;
                    case 1104484353:
                        if (stringExtra3.equals("ACTION_SEND_URL")) {
                            b = 3;
                        }
                        break;
                }
                switch (b) {
                    case 0:
                        stringExtra = intent.hasExtra("title") ? intent.getStringExtra("title") : "NO_TITLE";
                        strUrldecoder = intent.hasExtra("text") ? urldecoder(intent.getStringExtra("text")) : null;
                        if (strUrldecoder == null) {
                            strUrldecoder = intent.hasExtra("url") ? urldecoder(intent.getStringExtra("url")) : "NO_TEXT";
                        }
                        if (strUrldecoder == null) {
                            finish();
                        }
                        startActivityForResult(Intent.createChooser(buildIntentActionSend(stringExtra, strUrldecoder), ifNotNullStr("Share:", stringExtra, "..")), 1);
                        break;
                    case 1:
                        stringExtra = intent.hasExtra("title") ? intent.getStringExtra("title") : "NO_TITLE";
                        strUrldecoder = intent.hasExtra("text") ? urldecoder(intent.getStringExtra("text")) : null;
                        if (strUrldecoder == null) {
                            strUrldecoder = intent.hasExtra("url") ? urldecoder(intent.getStringExtra("url")) : "NO_TEXT";
                        }
                        if (strUrldecoder == null) {
                            finish();
                        }
                        Intent intentBuildIntentActionView = buildIntentActionView(strUrldecoder, true);
                        if (!defaultAppIsSetForIntent(intentBuildIntentActionView)) {
                            intentBuildIntentActionView = Intent.createChooser(intentBuildIntentActionView, ifNotNullStr("View:", stringExtra, ".."));
                        }
                        startActivityForResult(intentBuildIntentActionView, 1);
                        finish();
                        break;
                    case 2:
                        stringExtra = intent.hasExtra("title") ? intent.getStringExtra("title") : "NO_TITLE";
                        String strUrldecoder2 = intent.hasExtra("text") ? urldecoder(intent.getStringExtra("text")) : "NO_TEXT";
                        if (strUrldecoder2 == null) {
                            finish();
                        }
                        startActivityForResult(Intent.createChooser(buildIntentActionSend(stringExtra, strUrldecoder2), ifNotNullStr("Share:", stringExtra, "..")), 1);
                        break;
                    case 3:
                        stringExtra = intent.hasExtra("title") ? intent.getStringExtra("title") : "NO_TITLE";
                        String strUrldecoder3 = intent.hasExtra("url") ? urldecoder(intent.getStringExtra("url")) : "http://google.com";
                        if (strUrldecoder3 == null) {
                            finish();
                        }
                        startActivityForResult(Intent.createChooser(buildIntentActionSend(stringExtra, strUrldecoder3), ifNotNullStr("Share:", stringExtra, "..")), 1);
                        break;
                    default:
                        Log.e(this.APPTAG, " > intent.type not handled: '" + stringExtra3 + "' for action: '" + stringExtra2 + "'");
                        Toast.makeText(this.context, "intent.type not handled: '" + stringExtra3 + "' for action: '" + stringExtra2 + "'", 1).show();
                        finish();
                        break;
                }
            }
            Log.e(this.APPTAG, " > intent.action not handled: '" + stringExtra2 + "'");
            Toast.makeText(this.context, "intent.action not handled: '" + stringExtra2 + "'", 1).show();
            finish();
            return;
        }
        finish();
    }

    private Intent buildIntentActionSend(String str, String str2) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.addFlags(524288);
        intent.putExtra("android.intent.extra.SUBJECT", str);
        intent.putExtra("android.intent.extra.TEXT", str2);
        return intent;
    }

    private Intent buildIntentActionView(String str) {
        return buildIntentActionView(str, false);
    }

    private Intent buildIntentActionView(String str, boolean z) {
        Log.d(this.APPTAG, " -> buildIntentActionView()");
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(524288);
        intent.setData(Uri.parse(str));
        if (defaultAppIsSetForIntent(intent) && z) {
            Log.d(this.APPTAG, " -> Found default app");
            ActivityInfo activityInfo = getDefaultResolvedInfoForIntent(intent).activityInfo;
            String str2 = activityInfo.applicationInfo.packageName;
            String str3 = activityInfo.name;
            Log.d(this.APPTAG, " --> " + str2 + ", " + str3);
            intent.setComponent(new ComponentName(str2, str3));
        }
        return intent;
    }

    private boolean defaultAppIsSetForIntent(Intent intent) {
        return getDefaultResolvedInfoForIntent(intent) != null;
    }

    private ResolveInfo getDefaultResolvedInfoForIntent(Intent intent) {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intent, 0);
        ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(intent, 0);
        if (listQueryIntentActivities.size() > 0) {
            Iterator<ResolveInfo> it = listQueryIntentActivities.iterator();
            while (it.hasNext()) {
                if (it.next().activityInfo.applicationInfo.packageName.equals(resolveInfoResolveActivity.activityInfo.applicationInfo.packageName)) {
                    return resolveInfoResolveActivity;
                }
            }
        }
        return null;
    }

    private String urldecoder(String str) {
        try {
            return URLDecoder.decode(str.replace("+", "%2B"), "UTF-8").replace("%2B", "+");
        } catch (UnsupportedEncodingException e) {
            Log.e(this.APPTAG, "ActShareChrome.urldecoder.UnsupportedEncodingException", e);
            Toast.makeText(this.context, "Sharedr Error: UnsupportedEncodingException", 1).show();
            return null;
        } catch (IllegalArgumentException e2) {
            Log.e(this.APPTAG, "ActShareChrome.urldecoder.IllegalArgumentException", e2);
            Toast.makeText(this.context, "Sharedr Error: IllegalArgumentException", 1).show();
            return null;
        }
    }

    private String ifNotNullStr(String str, String str2, String str3) {
        if (str2 == null) {
            return str.trim() + " " + str3;
        }
        return str.trim() + " " + str2;
    }
}
