package com.rejh.sharedr;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

/* JADX INFO: loaded from: classes.dex */
public class ActMain extends AppCompatActivity {
    private String APPTAG = "Sharedr";
    private Context context;

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i(this.APPTAG, "ActMain.onCreate()");
        setContentView(bin.p002mt.plus.TranslationData.R.layout.activity_act_main);
        this.context = this;
        ((Button) findViewById(bin.p002mt.plus.TranslationData.R.id.button_share)).setOnClickListener(new View.OnClickListener() { // from class: com.rejh.sharedr.ActMain.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("text/plain");
                intent.putExtra("android.intent.extra.SUBJECT", "Sharedr");
                intent.putExtra("android.intent.extra.TEXT", "Sharedr improves sharing on Android by extending its native functionality and adding some cherries on top.\n\nhttps://play.google.com/store/apps/details?id=com.rejh.sharedr");
                ActMain.this.startActivity(Intent.createChooser(intent, "Share via"));
            }
        });
        ((Button) findViewById(bin.p002mt.plus.TranslationData.R.id.button_faq)).setOnClickListener(new View.OnClickListener() { // from class: com.rejh.sharedr.ActMain.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setData(Uri.parse("https://sharedr.rejh.nl/#faq"));
                ActMain.this.startActivity(intent);
            }
        });
    }
}
