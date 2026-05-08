package com.rejh.sharedr.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.ContextThemeWrapper;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class DialogBuilder {
    private Context mContext;
    private int mDialogStyleResource;

    public DialogBuilder(Context context, int i) {
        this.mContext = context;
        this.mDialogStyleResource = i;
    }

    public AlertDialog alert(String str, String str2) {
        ArrayList<ClickListenerOption> arrayList = new ArrayList<>();
        arrayList.add(new ClickListenerOption(-1, "OK", new DialogInterface.OnClickListener() { // from class: com.rejh.sharedr.tools.DialogBuilder.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }));
        return alert(str, str2, arrayList);
    }

    public AlertDialog alert(String str, String str2, ArrayList<ClickListenerOption> arrayList) {
        AlertDialog alertDialogBuilder = getAlertDialogBuilder();
        alertDialogBuilder.setTitle(str);
        alertDialogBuilder.setMessage(Html.fromHtml(str2));
        for (int i = 0; i < arrayList.size(); i++) {
            ClickListenerOption clickListenerOption = arrayList.get(i);
            alertDialogBuilder.setButton(clickListenerOption.whichButton, clickListenerOption.text, clickListenerOption.listener);
        }
        alertDialogBuilder.show();
        return alertDialogBuilder;
    }

    public static class ClickListenerOption {
        public DialogInterface.OnClickListener listener;
        public CharSequence text;
        public int whichButton;

        public ClickListenerOption(int i, CharSequence charSequence, DialogInterface.OnClickListener onClickListener) {
            this.whichButton = i;
            this.text = charSequence;
            this.listener = onClickListener;
        }
    }

    private AlertDialog getAlertDialogBuilder() {
        return new AlertDialog.Builder(new ContextThemeWrapper(this.mContext, this.mDialogStyleResource)).create();
    }
}
