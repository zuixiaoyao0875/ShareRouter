package com.rejh.sharedr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public class RecvUpdated extends BroadcastReceiver {
    private static String APPTAG = "Sharedr";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Log.i(APPTAG, "RecvUpdated.onReceive()");
        SharedPreferences sharedPreferences = context.getSharedPreferences(APPTAG, 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        String action = intent.getAction();
        Log.i(APPTAG, " -> Action: " + action);
        if (action == null) {
            return;
        }
        if (action.equals("android.intent.action.MY_PACKAGE_REPLACED") || action.equals("android.intent.action.PACKAGE_REPLACED")) {
            Log.i(APPTAG, " -> Set sharedr_was_updated, currently " + sharedPreferences.getBoolean("sharedr_was_updated", false));
            editorEdit.putBoolean("sharedr_was_updated", true);
            editorEdit.commit();
        }
    }
}
