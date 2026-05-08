package com.rejh.sharedr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.rejh.sharedr.tools.IntentDebug;

/* JADX INFO: loaded from: classes.dex */
public class ActShareCopyToClipboard extends AppCompatActivity {
    private String APPTAG = "Sharedr";

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(bin.p002mt.plus.TranslationData.R.layout.activity_act_share_copy_to_clipboard);
        Log.i(this.APPTAG, "ActShareCopyToClipboard.onCreate()");
        IntentDebug.printIntent(getIntent(), "Intent");
        ((ClipboardManager) getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText(null, getIntent().getCharSequenceExtra("android.intent.extra.TEXT")));
        Toast.makeText(this, "Copied to clipboard", 0).show();
        finish();
    }
}
