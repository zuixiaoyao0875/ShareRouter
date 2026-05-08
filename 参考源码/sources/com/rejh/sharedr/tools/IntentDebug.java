package com.rejh.sharedr.tools;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.lang.reflect.Field;

/* JADX INFO: loaded from: classes.dex */
public class IntentDebug {
    private static String APPTAG = "Sharedr";

    public static void printIntent(Intent intent, String str) {
        Log.d(APPTAG, "tools.IntentDebug.printIntent(): " + str + "\n -> Action: " + intent.getAction() + "\n -> Component: " + intent.getComponent() + "\n -> Type: " + intent.getType());
    }

    private static void printIntentExtras(Intent intent) {
        StringBuilder sb = new StringBuilder();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String str : extras.keySet()) {
                Object obj = extras.get(str);
                Object[] objArr = new Object[3];
                objArr[0] = str;
                String name = "null";
                objArr[1] = obj != null ? obj.toString() : "null";
                if (obj != null) {
                    name = obj.getClass().getName();
                }
                objArr[2] = name;
                sb.append(String.format("\n -> %s %s (%s)", objArr));
            }
            if (sb.length() > 0) {
                Log.d(APPTAG, "tools.IntentDebug.printIntentExtras()" + sb.toString());
            }
        }
    }

    private static void printIntentData(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            Log.d(APPTAG, "tools.IntentDebug.printIntentData()\n -> " + data.toString());
        }
    }

    private static void printIntentClipData(Intent intent) {
        ClipData clipData = intent.getClipData();
        StringBuilder sb = new StringBuilder();
        if (clipData == null || clipData.getItemCount() == 0) {
            return;
        }
        for (int i = 0; i < clipData.getItemCount(); i++) {
            ClipData.Item itemAt = clipData.getItemAt(i);
            Uri uri = itemAt.getUri();
            String str = (String) itemAt.getText();
            Intent intent2 = itemAt.getIntent();
            if (uri != null) {
                sb.append("\n -> URI: ");
                sb.append(uri);
            } else if (str != null) {
                sb.append("\n -> Text: ");
                sb.append(str);
            } else if (intent2 != null) {
                sb.append("\n -> Intent: ");
                sb.append(intent2.toString());
            } else {
                sb.append("\n -> !uri, !text, !intent");
            }
        }
        if (sb.length() > 0) {
            Log.d(APPTAG, "tools.IntentDebug.printIntentClipData()" + sb.toString());
        }
    }

    private static void printIntentFlags(Intent intent) {
        Field[] declaredFields = Intent.class.getDeclaredFields();
        StringBuilder sb = new StringBuilder();
        for (Field field : declaredFields) {
            if (field.getName().startsWith("FLAG_")) {
                try {
                    if ((field.getInt(null) & intent.getFlags()) != 0) {
                        sb.append("\n -> ");
                        sb.append(field.getName());
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        if (sb.length() > 0) {
            Log.d(APPTAG, "tools.IntentDebug.printIntentFlags()" + sb.toString());
        }
    }
}
