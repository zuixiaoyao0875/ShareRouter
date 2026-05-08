package com.rejh.sharedr;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.net.MailTo;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.rejh.sharedr.adapters.RecyclerItemsAdapter;
import com.rejh.sharedr.tools.DialogBuilder;
import com.rejh.sharedr.tools.IntentDebug;
import com.rejh.tools.RejhFileIO;
import com.rejh.tools.RejhJson;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class ActShareReplace extends AppCompatActivity implements RecyclerItemsAdapter.AdapterCallback {
    private static String APPTAG = "Sharedr";
    private static final int GRID_SPAN_COUNT = 4;
    private static final int GRID_SPAN_COUNT_480 = 5;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1;
    private static final int THEME_BLACK = 2;
    private static final int THEME_DARK = 1;
    private static final int THEME_LIGHT = 0;
    private Context context;
    private boolean mCanAccessContentUri;
    protected LayoutManagerType mCurrentLayoutManagerType;
    private ImageButton mDialogIconMoreVert;
    private RecyclerView mDialogListView;
    private TextView mDialogTextView;
    private RelativeLayout mDialogTopBar;
    private Intent mIntent;
    private SlidingUpPanelLayout mLayout;
    protected RecyclerView.LayoutManager mLayoutManager;
    private FrameLayout mMainLayout;
    private boolean mPanelIsSliding;
    private Intent[] mPayloadInitIntents;
    private SharedPreferences mSharedPrefs;
    private SharedPreferences.Editor mSharedPrefsEditor;
    private ArrayList<Item> mTargetItems;
    private ArrayList<Item> mTargetItemsAll;
    private JSONObject mTargetItemsJson;
    private IntentSender mChosenComponentSender = null;
    private String mPayloadTitle = null;
    private Intent mPayloadIntent = null;
    private String mPayloadAction = null;
    private boolean mFileShareMessageShown = false;
    private boolean mRequestedPermissionReadExternalStorage = false;
    private ArrayList<String> mIgnoreTargetNames = new ArrayList<>(Arrays.asList(com.sothree.slidinguppanel.library.BuildConfig.FLAVOR, "com.rejh.sharedr#com.rejh.sharedr.ActShareReplace", "com.google.android.apps.maps#com.google.android.apps.gmm.sharing.SendTextToClipboardActivity"));

    private enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    private String strFallback(String str, String str2) {
        return str == null ? str2 : str;
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        Log.i(APPTAG, "ActShareReplace.onCreate()");
        this.context = this;
        overridePendingTransition(bin.p002mt.plus.TranslationData.R.transition.activity_slide_from_bottom, bin.p002mt.plus.TranslationData.R.transition.activity_slide_to_bottom);
        SharedPreferences sharedPreferences = getSharedPreferences(APPTAG, 0);
        this.mSharedPrefs = sharedPreferences;
        this.mSharedPrefsEditor = sharedPreferences.edit();
        int i = this.mSharedPrefs.getInt("sharereplace_theme", this.mSharedPrefs.getBoolean("sharereplace_use_darkmode", false) ? 2 : 0);
        if (i == 1) {
            setTheme(2131755027);
            super.onCreate(bundle);
            setContentView(bin.p002mt.plus.TranslationData.R.layout.activity_act_share_replace_dark);
        } else if (i == 2) {
            setTheme(2131755027);
            super.onCreate(bundle);
            setContentView(bin.p002mt.plus.TranslationData.R.layout.activity_act_share_replace_black);
        } else {
            setTheme(2131755024);
            super.onCreate(bundle);
            setContentView(bin.p002mt.plus.TranslationData.R.layout.activity_act_share_replace);
        }
        getWindow().setStatusBarColor(ViewCompat.MEASURED_STATE_MASK);
        getWindow().setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK);
        this.mLayout = (SlidingUpPanelLayout) findViewById(bin.p002mt.plus.TranslationData.R.id.sliding_layout);
        this.mMainLayout = (FrameLayout) findViewById(bin.p002mt.plus.TranslationData.R.id.main);
        this.mDialogTopBar = (RelativeLayout) findViewById(bin.p002mt.plus.TranslationData.R.id.dialog_topbar);
        this.mDialogTextView = (TextView) findViewById(bin.p002mt.plus.TranslationData.R.id.dialog_title);
        this.mDialogIconMoreVert = (ImageButton) findViewById(bin.p002mt.plus.TranslationData.R.id.dialog_icon_more_vert);
        this.mDialogListView = (RecyclerView) findViewById(bin.p002mt.plus.TranslationData.R.id.dialog_list);
        this.mLayoutManager = new LinearLayoutManager(this);
        final ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mDialogTopBar, "elevation", 0.0f, 0.0f);
        this.mLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.rejh.sharedr.ActShareReplace.1
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                Rect rect = new Rect();
                ActShareReplace.this.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                int i2 = rect.top;
                DisplayMetrics displayMetrics = new DisplayMetrics();
                ActShareReplace.this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                ActShareReplace.this.getWindow().setLayout(displayMetrics.widthPixels, displayMetrics.heightPixels);
            }
        });
        this.mMainLayout.setOnClickListener(new View.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ActShareReplace.this.setResult(0);
                ActShareReplace.this.finish();
            }
        });
        this.mLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() { // from class: com.rejh.sharedr.ActShareReplace.3
            @Override // com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
            public void onPanelSlide(View view, float f) {
                ActShareReplace.this.mPanelIsSliding = true;
            }

            @Override // com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
            public void onPanelStateChanged(View view, SlidingUpPanelLayout.PanelState panelState, SlidingUpPanelLayout.PanelState panelState2) {
                Log.d(ActShareReplace.APPTAG, panelState + ", " + panelState2);
                if (panelState2 == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    objectAnimatorOfFloat.setFloatValues(0.0f, ActShareReplace.this.asDp(4));
                    objectAnimatorOfFloat.start();
                } else {
                    objectAnimatorOfFloat.setFloatValues(ActShareReplace.this.mDialogTextView.getElevation(), 0.0f);
                    objectAnimatorOfFloat.start();
                }
                ActShareReplace.this.mPanelIsSliding = false;
            }
        });
        this.mDialogIconMoreVert.setOnClickListener(new View.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.4
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(ActShareReplace.this.context, !ActShareReplace.this.mSharedPrefs.getBoolean("sharereplace_use_darkmode", false) ? bin.p002mt.plus.TranslationData.R.style.PopupMenuOverlapAnchor : bin.p002mt.plus.TranslationData.R.style.PopupMenuOverlapAnchorDark), view);
                popupMenu.setGravity(GravityCompat.END);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.rejh.sharedr.ActShareReplace.4.1
                    @Override // androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case bin.p002mt.plus.TranslationData.R.id.choose_theme /* 2131230827 */:
                                ActShareReplace.this.actionChooseTheme();
                                return false;
                            case bin.p002mt.plus.TranslationData.R.id.clear_defaults /* 2131230830 */:
                                ActShareReplace.this.actionClearDefaults();
                                return false;
                            case bin.p002mt.plus.TranslationData.R.id.send_feedback /* 2131231067 */:
                                ActShareReplace.this.actionSendFeedback();
                                return false;
                            case bin.p002mt.plus.TranslationData.R.id.toggle_grid_list /* 2131231141 */:
                                ActShareReplace.this.actionToggleGrid();
                                return false;
                            case bin.p002mt.plus.TranslationData.R.id.unhide_all /* 2131231154 */:
                                ActShareReplace.this.actionUnhideAll();
                                return false;
                            default:
                                Toast.makeText(ActShareReplace.this.context, "Selected Item: " + ((Object) menuItem.getTitle()), 0).show();
                                return false;
                        }
                    }
                });
                popupMenu.inflate(bin.p002mt.plus.TranslationData.R.menu.popupmenu_share_replace);
                popupMenu.show();
            }
        });
        handleUpdate();
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        Log.i(APPTAG, "ActShareReplace.onResume()");
        Log.d(APPTAG, " -> Invoke onNewIntent");
        onNewIntent(getIntent());
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(APPTAG, "ActShareReplace.onNewIntent()");
        setIntent(intent);
        handleIntent();
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        Log.i(APPTAG, "ActShareReplace.onRequestPermissionsResult()");
        if (i != 1) {
            return;
        }
        if (iArr.length > 0 && iArr[0] == 0) {
            handleIntent();
        } else {
            Toast.makeText(this.context, "Storage permission denied. This may complicate sharing files.", 1).show();
            handleIntent();
        }
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onDestroy() {
        List<ActivityManager.AppTask> appTasks;
        super.onDestroy();
        ActivityManager activityManager = (ActivityManager) getSystemService("activity");
        if (activityManager == null || (appTasks = activityManager.getAppTasks()) == null || appTasks.size() <= 0) {
            return;
        }
        for (int i = 0; i < appTasks.size(); i++) {
            appTasks.get(i).setExcludeFromRecents(true);
        }
    }

    @Override // android.app.Activity
    public void finish() {
        super.finish();
        overridePendingTransition(bin.p002mt.plus.TranslationData.R.transition.activity_fade_in, bin.p002mt.plus.TranslationData.R.transition.activity_slide_to_bottom);
    }

    @Override // com.rejh.sharedr.adapters.RecyclerItemsAdapter.AdapterCallback
    public void onRecycleViewItemClick(View view, int i) {
        actionSendPayload(this.mTargetItems.get(i));
    }

    @Override // com.rejh.sharedr.adapters.RecyclerItemsAdapter.AdapterCallback
    public boolean onRecycleViewItemLongClick(View view, int i) {
        if (this.mPanelIsSliding) {
            return true;
        }
        showLongpressDialog(i, this.mTargetItems.get(i));
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:135:0x0407  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void handleIntent() {
        /*
            Method dump skipped, instruction units count: 1186
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.rejh.sharedr.ActShareReplace.handleIntent():void");
    }

    private void actionSendPayload(Item item) {
        Log.d(APPTAG, "ActShareReplace.actionSendPayload()");
        Intent intent = item.intent != null ? item.intent : this.mPayloadIntent;
        if (!this.mCanAccessContentUri) {
            intent.removeExtra("android.intent.extra.STREAM");
            intent.removeExtra("output");
            intent.setData(null);
            intent.setClipData(null);
            if (Build.VERSION.SDK_INT >= 26) {
                intent.removeFlags(1);
                intent.removeFlags(2);
            } else {
                intent.setFlags(524288);
            }
        }
        Intent intent2 = new Intent(intent);
        ActivityInfo activityInfo = item.activityInfo;
        ComponentName componentName = new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
        intent2.addFlags(50331648);
        intent2.addFlags(524288);
        intent2.setComponent(componentName);
        IntentDebug.printIntent(intent2, "shareIntent");
        startActivity(intent2);
        activityStarted(intent2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionTogglePinned(int i) {
        Log.d(APPTAG, "ActShareReplace.actionTogglePinned()");
        Item item = this.mTargetItems.get(i);
        item.isPinned = !item.isPinned;
        int i2 = 0;
        item.isHidden = false;
        while (true) {
            if (i2 >= this.mTargetItemsAll.size()) {
                break;
            }
            boolean zEquals = this.mTargetItemsAll.get(i2).activityInfo.packageName.equals(item.activityInfo.packageName);
            boolean zEquals2 = this.mTargetItemsAll.get(i2).activityInfo.name.equals(item.activityInfo.name);
            if (zEquals && zEquals2) {
                this.mTargetItemsAll.set(i2, item);
                break;
            }
            i2++;
        }
        writeTargetItemsJson(this.mTargetItemsAll, this.mPayloadAction);
        handleIntent();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionToggleHidden(int i) {
        Log.d(APPTAG, "ActShareReplace.actionToggleHidden(position)");
        actionToggleHidden(this.mTargetItems.get(i));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionToggleHidden(final Item item) {
        Log.d(APPTAG, "ActShareReplace.actionToggleHidden(targetItem)");
        item.isHidden = !item.isHidden;
        item.isPinned = false;
        int i = 0;
        while (true) {
            if (i >= this.mTargetItemsAll.size()) {
                break;
            }
            boolean zEquals = this.mTargetItemsAll.get(i).activityInfo.packageName.equals(item.activityInfo.packageName);
            boolean zEquals2 = this.mTargetItemsAll.get(i).activityInfo.name.equals(item.activityInfo.name);
            if (zEquals && zEquals2) {
                this.mTargetItemsAll.set(i, item);
                break;
            }
            i++;
        }
        writeTargetItemsJson(this.mTargetItemsAll, this.mPayloadAction);
        handleIntent();
        if (item.isHidden) {
            Snackbar snackbarMake = Snackbar.make(findViewById(bin.p002mt.plus.TranslationData.R.id.sliding_layout), "App hidden from list", 0);
            snackbarMake.setAction("UNDO", new View.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.7
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    ActShareReplace.this.actionToggleHidden(item);
                }
            });
            snackbarMake.show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionRenameLabel(int i) {
        String string;
        Log.d(APPTAG, "ActShareReplace.actionRenameLabel()");
        final Item item = this.mTargetItems.get(i);
        try {
            string = getTargetItemJson(item, this.mTargetItemsJson).getString("label");
        } catch (NullPointerException | JSONException unused) {
            string = item.label;
        }
        boolean z = this.mSharedPrefs.getBoolean("sharereplace_use_darkmode", false);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this.context, !z ? bin.p002mt.plus.TranslationData.R.style.AlertDialog : bin.p002mt.plus.TranslationData.R.style.AlertDialogDark));
        View viewInflate = getLayoutInflater().inflate(bin.p002mt.plus.TranslationData.R.layout.alert_edittext, (ViewGroup) null);
        builder.setView(viewInflate);
        final EditText editText = (EditText) viewInflate.findViewById(bin.p002mt.plus.TranslationData.R.id.edittext);
        editText.setTextColor(!z ? ViewCompat.MEASURED_STATE_MASK : -1);
        editText.setText(string);
        builder.setTitle("\u91cd\u547d\u540d");
        builder.setMessage("\u8f93\u5165\u540d\u79f0 '" + item.label + "'");
        builder.setPositiveButton("\u786e\u5b9a", new DialogInterface.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.8
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i2) {
                String string2 = editText.getText().toString();
                item.labelOverride = string2;
                int i3 = 0;
                while (true) {
                    if (i3 >= ActShareReplace.this.mTargetItemsAll.size()) {
                        break;
                    }
                    boolean zEquals = ((Item) ActShareReplace.this.mTargetItemsAll.get(i3)).activityInfo.packageName.equals(item.activityInfo.packageName);
                    boolean zEquals2 = ((Item) ActShareReplace.this.mTargetItemsAll.get(i3)).activityInfo.name.equals(item.activityInfo.name);
                    if (zEquals && zEquals2) {
                        ActShareReplace.this.mTargetItemsAll.set(i3, item);
                        break;
                    }
                    i3++;
                }
                ActShareReplace actShareReplace = ActShareReplace.this;
                actShareReplace.writeTargetItemsJson(actShareReplace.mTargetItemsAll, ActShareReplace.this.mPayloadAction);
                ActShareReplace.this.handleIntent();
                Toast.makeText(ActShareReplace.this.context, "Renamed: " + string2, 0).show();
            }
        });
        builder.setNegativeButton("\u53d6\u6d88", new DialogInterface.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.9
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i2) {
            }
        });
        builder.create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionAppInfo(int i) {
        Log.d(APPTAG, "ActShareReplace.actionAppInfo()");
        Item item = this.mTargetItems.get(i);
        try {
            Intent intent = new Intent();
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.parse("package:" + item.activityInfo.packageName));
            startActivity(intent);
        } catch (ActivityNotFoundException unused) {
            Toast.makeText(this.context, "Unable to open app info", 0).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionUnhideAll() {
        Log.d(APPTAG, "ActShareReplace.actionUnhideAll()");
        for (int i = 0; i < this.mTargetItemsAll.size(); i++) {
            Item item = this.mTargetItemsAll.get(i);
            item.isHidden = false;
            this.mTargetItemsAll.set(i, item);
        }
        writeTargetItemsJson(this.mTargetItemsAll, this.mPayloadAction);
        handleIntent();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionClearDefaults() {
        Log.d(APPTAG, "ActShareReplace.actionClearDefaults()");
        getPackageManager().clearPackagePreferredActivities(this.context.getPackageName());
        setResult(0);
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionSendFeedback() {
        Log.d(APPTAG, "ActShareReplace.actionSendFeedback()");
        Intent intent = new Intent("android.intent.action.SENDTO");
        intent.setData(Uri.parse(MailTo.MAILTO_SCHEME));
        intent.putExtra("android.intent.extra.EMAIL", new String[]{"droidapps@rejh.nl"});
        intent.putExtra("android.intent.extra.SUBJECT", "Sharedr Feedback");
        intent.putExtra("android.intent.extra.TEXT", "[Please provide information like your device and OS version. More information is better!]");
        startActivity(Intent.createChooser(intent, "Send Feedback"));
        setResult(0);
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionChooseTheme() {
        Log.d(APPTAG, "ActShareReplace.actionChooseTheme()");
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this.context, !this.mSharedPrefs.getBoolean("sharereplace_use_darkmode", false) ? bin.p002mt.plus.TranslationData.R.style.AlertDialog : bin.p002mt.plus.TranslationData.R.style.AlertDialogDark));
        builder.setTitle("Apply theme");
        final ArrayAdapter arrayAdapter = new ArrayAdapter(this.context, bin.p002mt.plus.TranslationData.R.layout.listview_item_singleline);
        arrayAdapter.add("Light");
        arrayAdapter.add("Dark");
        arrayAdapter.add("Black");
        builder.setNegativeButton("\u786e\u8ba4", new DialogInterface.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.10
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.11
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (ActShareReplace.this.mSharedPrefs.getInt("sharereplace_theme", 0) == i) {
                    Log.d(ActShareReplace.APPTAG, " -> Theme didn't change");
                    return;
                }
                String str = (String) arrayAdapter.getItem(i);
                if (i != 0 && i != 1 && i != 2) {
                    Log.w(ActShareReplace.APPTAG, "Unhandled choice: " + i + ", " + str);
                    i = 0;
                }
                ActShareReplace.this.mSharedPrefsEditor.putBoolean("sharereplace_use_darkmode", i != 0);
                ActShareReplace.this.mSharedPrefsEditor.putInt("sharereplace_theme", i);
                ActShareReplace.this.mSharedPrefsEditor.commit();
                Toast.makeText(ActShareReplace.this.context, "Theme applied. Please restart Sharedr.", 0).show();
                ActShareReplace.this.setResult(0);
                ActShareReplace.this.finish();
            }
        });
        builder.create();
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void actionToggleGrid() {
        Log.d(APPTAG, "ActShareReplace.actionToggleGrid()");
        this.mSharedPrefsEditor.putBoolean("sharereplace_use_grid", !this.mSharedPrefs.getBoolean("sharereplace_use_grid", true));
        this.mSharedPrefsEditor.commit();
        handleIntent();
    }

    private void showLongpressDialog(final int i, Item item) {
        Log.d(APPTAG, "ActShareReplace.showLongpressDialog()");
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this.context, !this.mSharedPrefs.getBoolean("sharereplace_use_darkmode", false) ? bin.p002mt.plus.TranslationData.R.style.AlertDialog : bin.p002mt.plus.TranslationData.R.style.AlertDialogDark));
        builder.setTitle(item.label);
        final ArrayAdapter arrayAdapter = new ArrayAdapter(this.context, bin.p002mt.plus.TranslationData.R.layout.listview_item_singleline);
        if (item.isPinned) {
            arrayAdapter.add("\u7f6e\u9876\u5e94\u7528");
        } else {
            arrayAdapter.add("\u7f6e\u9876\u5e94\u7528");
        }
        if (item.isHidden) {
            arrayAdapter.add("\u7f6e\u9876\u5e94\u7528");
        } else {
            arrayAdapter.add("\u9690\u85cf\u5e94\u7528");
        }
        arrayAdapter.add("\u91cd\u547d\u540d");
        arrayAdapter.add("\u5e94\u7528\u4fe1\u606f");
        builder.setNegativeButton("\u786e\u5b9a", new DialogInterface.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.12
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i2) {
                dialogInterface.dismiss();
            }
        });
        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.13
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i2) {
                String str = (String) arrayAdapter.getItem(i2);
                if (i2 == 0) {
                    ActShareReplace.this.actionTogglePinned(i);
                    return;
                }
                if (i2 == 1) {
                    ActShareReplace.this.actionToggleHidden(i);
                    return;
                }
                if (i2 == 2) {
                    ActShareReplace.this.actionRenameLabel(i);
                } else if (i2 != 3) {
                    Log.w(ActShareReplace.APPTAG, "Unhandled choice: " + i + ", " + str);
                } else {
                    ActShareReplace.this.actionAppInfo(i);
                }
            }
        });
        builder.create();
        builder.show();
    }

    /* JADX INFO: renamed from: com.rejh.sharedr.ActShareReplace$16 */
    static /* synthetic */ class C082016 {
        static final /* synthetic */ int[] $SwitchMap$com$rejh$sharedr$ActShareReplace$LayoutManagerType;

        static {
            int[] iArr = new int[LayoutManagerType.values().length];
            $SwitchMap$com$rejh$sharedr$ActShareReplace$LayoutManagerType = iArr;
            try {
                iArr[LayoutManagerType.GRID_LAYOUT_MANAGER.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$rejh$sharedr$ActShareReplace$LayoutManagerType[LayoutManagerType.LINEAR_LAYOUT_MANAGER.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
        }
    }

    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int iAsDp;
        int i = C082016.$SwitchMap$com$rejh$sharedr$ActShareReplace$LayoutManagerType[layoutManagerType.ordinal()];
        if (i != 1) {
            if (i == 2) {
                this.mLayoutManager = new LinearLayoutManager(this.context);
                this.mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
            } else {
                this.mLayoutManager = new LinearLayoutManager(this.context);
                this.mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
            }
            iAsDp = 0;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            this.mLayoutManager = new GridLayoutManager(this.context, pixelToDp(displayMetrics.widthPixels) < 480 ? 4 : 5);
            this.mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;
            iAsDp = asDp(8);
        }
        this.mDialogListView.setPadding(iAsDp, iAsDp, iAsDp, asDp(32));
        this.mDialogListView.setLayoutManager(this.mLayoutManager);
    }

    public int getRecycleViewScrollPosition() {
        if (this.mDialogListView.getLayoutManager() != null) {
            return ((LinearLayoutManager) this.mDialogListView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }
        return 0;
    }

    public void setRecycleViewScrollPosition(int i) {
        this.mDialogListView.scrollToPosition(i);
    }

    public static class Item {
        public final ActivityInfo activityInfo;
        public final Drawable icon;
        public Intent intent;
        public boolean isHidden;
        public boolean isPinned;
        public final String label;
        public String labelOverride;

        public Item(String str, Drawable drawable, ActivityInfo activityInfo) {
            this(str, drawable, activityInfo, false, false);
        }

        public Item(String str, Drawable drawable, ActivityInfo activityInfo, boolean z) {
            this(str, drawable, activityInfo, z, false);
        }

        public Item(String str, Drawable drawable, ActivityInfo activityInfo, boolean z, boolean z2) {
            this.label = str;
            this.labelOverride = str;
            this.icon = drawable;
            this.activityInfo = activityInfo;
            this.isPinned = z;
            this.isHidden = z2;
            this.intent = new Intent();
        }

        public String toString() {
            return this.labelOverride;
        }

        public JSONObject toJSONObject() {
            JSONObject jSONObject = new JSONObject();
            try {
                jSONObject.put("label", this.label);
                jSONObject.put("labelOverride", this.labelOverride);
                jSONObject.put("icon", this.icon);
                jSONObject.put("isPinned", this.isPinned);
                jSONObject.put("isHidden", this.isHidden);
            } catch (JSONException e) {
                Log.e("Sharedr", "Item.toJSONObject().Error: " + e);
                e.printStackTrace();
            }
            return jSONObject;
        }
    }

    private Item getTargetItem(ResolveInfo resolveInfo, Intent intent) {
        PackageManager packageManager = getPackageManager();
        Item item = new Item((String) resolveInfo.loadLabel(packageManager), resolveInfo.loadIcon(packageManager), resolveInfo.activityInfo);
        item.intent = intent;
        try {
            JSONObject targetItemJson = getTargetItemJson(item, this.mTargetItemsJson);
            if (targetItemJson != null) {
                item.labelOverride = targetItemJson.getString("labelOverride");
                item.isPinned = targetItemJson.getBoolean("isPinned");
                item.isHidden = targetItemJson.getBoolean("isHidden");
            }
        } catch (JSONException unused) {
            Log.w(APPTAG, "JSONException");
        }
        return item;
    }

    private ArrayList<Item> sortItemsAlpha(ArrayList<Item> arrayList) {
        Collections.sort(arrayList, new Comparator<Item>() { // from class: com.rejh.sharedr.ActShareReplace.14
            @Override // java.util.Comparator
            public int compare(Item item, Item item2) {
                return item.toString().toLowerCase().compareTo(item2.toString().toLowerCase());
            }
        });
        return arrayList;
    }

    private void activityStarted(Intent intent) {
        ComponentName component;
        if (Build.VERSION.SDK_INT >= 22 && this.mChosenComponentSender != null && (component = intent.getComponent()) != null) {
            try {
                this.mChosenComponentSender.sendIntent(this, -1, new Intent().putExtra("android.intent.extra.CHOSEN_COMPONENT", component), null, null);
            } catch (IntentSender.SendIntentException e) {
                Log.e(APPTAG, "IntentSender.SendIntentException");
                Log.e(APPTAG, "Unable to launch supplied IntentSender to report the chosen component: " + e);
            }
        }
        setResult(-1);
        finish();
    }

    private boolean hasContentUri(Intent intent) throws BadParcelableException {
        Log.d(APPTAG, "ActShareReplace.hasContentUri()");
        if (intent == null) {
            return false;
        }
        try {
            return intent.hasExtra("android.intent.extra.STREAM") || intent.hasExtra("output") || intent.hasExtra("share_screenshot_as_stream") || (intent.getData() != null) || (intent.getClipData() != null);
        } catch (BadParcelableException e) {
            Log.w(APPTAG, "BadParcelableException: " + e.toString());
            return false;
        }
    }

    private boolean canAccessContentUri(Intent intent) {
        Log.d(APPTAG, "ActShareReplace.canAccessContentUri()");
        if (intent == null) {
            Log.d(APPTAG, " -> Intent is null");
            return true;
        }
        if (!hasContentUri(intent)) {
            Log.d(APPTAG, " -> No content URI");
            return true;
        }
        boolean zHasExtra = intent.hasExtra("android.intent.extra.STREAM");
        boolean zHasExtra2 = intent.hasExtra("output");
        boolean zHasExtra3 = intent.hasExtra("share_screenshot_as_stream");
        boolean z = intent.getData() != null;
        boolean z2 = intent.getClipData() != null;
        ArrayList arrayList = new ArrayList();
        try {
            String action = intent.getAction() != null ? intent.getAction() : com.sothree.slidinguppanel.library.BuildConfig.FLAVOR;
            if (((action.hashCode() == -58484670 && action.equals("android.intent.action.SEND_MULTIPLE")) ? (byte) 0 : (byte) -1) != 0) {
                if (zHasExtra) {
                    Uri uriFileToContentUri = (Uri) intent.getExtras().get("android.intent.extra.STREAM");
                    if (Objects.equals(uriFileToContentUri.getScheme(), "file")) {
                        uriFileToContentUri = fileToContentUri(uriFileToContentUri);
                        intent.putExtra("android.intent.extra.STREAM", uriFileToContentUri);
                    }
                    arrayList.add(uriFileToContentUri);
                }
                if (zHasExtra2) {
                    Uri uriFileToContentUri2 = (Uri) intent.getExtras().get("output");
                    if (Objects.equals(uriFileToContentUri2.getScheme(), "file")) {
                        uriFileToContentUri2 = fileToContentUri(uriFileToContentUri2);
                        intent.putExtra("output", uriFileToContentUri2);
                    }
                    arrayList.add(uriFileToContentUri2);
                }
                if (zHasExtra3) {
                    Uri uriFileToContentUri3 = (Uri) intent.getExtras().get("share_screenshot_as_stream");
                    if (Objects.equals(uriFileToContentUri3.getScheme(), "file")) {
                        uriFileToContentUri3 = fileToContentUri(uriFileToContentUri3);
                        intent.putExtra("share_screenshot_as_stream", uriFileToContentUri3);
                    }
                    arrayList.add(uriFileToContentUri3);
                }
                if (z) {
                    arrayList.add(intent.getData());
                }
                if (z2) {
                    ClipData.Item itemAt = intent.getClipData().getItemAt(0);
                    Uri uri = itemAt != null ? itemAt.getUri() : null;
                    if (uri != null) {
                        if (Objects.equals(uri.getScheme(), "file")) {
                            uri = fileToContentUri(uri);
                            intent.setClipData(ClipData.newRawUri(null, uri));
                        }
                        arrayList.add(uri);
                    }
                }
            } else {
                if (zHasExtra) {
                    ArrayList parcelableArrayListExtra = intent.getParcelableArrayListExtra("android.intent.extra.STREAM");
                    if (parcelableArrayListExtra != null) {
                        for (int i = 0; i < parcelableArrayListExtra.size(); i++) {
                            Uri uriFileToContentUri4 = (Uri) parcelableArrayListExtra.get(i);
                            if (Objects.equals(uriFileToContentUri4.getScheme(), "file")) {
                                uriFileToContentUri4 = fileToContentUri(uriFileToContentUri4);
                                parcelableArrayListExtra.set(i, uriFileToContentUri4);
                            }
                            arrayList.add(uriFileToContentUri4);
                        }
                    }
                    intent.putExtra("android.intent.extra.STREAM", parcelableArrayListExtra);
                }
                if (zHasExtra2) {
                    ArrayList parcelableArrayListExtra2 = intent.getParcelableArrayListExtra("output");
                    if (parcelableArrayListExtra2 != null) {
                        for (int i2 = 0; i2 < parcelableArrayListExtra2.size(); i2++) {
                            arrayList.add((Uri) parcelableArrayListExtra2.get(i2));
                        }
                    }
                    intent.putExtra("output", parcelableArrayListExtra2);
                }
                if (z2) {
                    ClipData clipData = intent.getClipData();
                    ClipData clipDataNewPlainText = ClipData.newPlainText(null, null);
                    for (int i3 = 0; i3 < clipData.getItemCount(); i3++) {
                        ClipData.Item itemAt2 = clipData.getItemAt(i3);
                        Uri uri2 = itemAt2 != null ? itemAt2.getUri() : null;
                        if (uri2 != null) {
                            if (Objects.equals(uri2.getScheme(), "file")) {
                                uri2 = fileToContentUri(uri2);
                            }
                            clipDataNewPlainText.addItem(new ClipData.Item(uri2));
                            arrayList.add(uri2);
                        }
                    }
                    intent.setClipData(clipDataNewPlainText);
                }
            }
        } catch (ClassCastException e) {
            Log.e(APPTAG, "ClassCastException");
            Log.e(APPTAG, e.toString());
            e.printStackTrace();
        }
        for (int i4 = 0; i4 < arrayList.size(); i4++) {
            Uri uri3 = (Uri) arrayList.get(i4);
            if (uri3 != null && (Objects.equals(uri3.getScheme(), "content") || Objects.equals(uri3.getScheme(), "file"))) {
                if (hasGrantReadUriPerm(uri3)) {
                    Log.d(APPTAG, " -> hasGrantReadUriPerm #" + i4 + ", " + uri3.toString());
                } else if (hasUriReadAccess(uri3)) {
                    Log.d(APPTAG, " -> hasUriReadAccess #" + i4 + ", " + uri3.toString());
                } else {
                    Log.d(APPTAG, " -> !hasGrantReadUriPerm && !hasUriReadAccess #" + i4 + ", " + uri3.toString());
                    return false;
                }
            }
        }
        Log.d(APPTAG, " -> Can access: true");
        return true;
    }

    private boolean hasGrantReadUriPerm(Uri uri) {
        return checkUriPermission(uri, Process.myPid(), Process.myUid(), 1) == 0;
    }

    private boolean hasUriReadAccess(Uri uri) {
        try {
            Cursor cursorQuery = getContentResolver().query(uri, null, null, null, null);
            if (cursorQuery == null) {
                return false;
            }
            cursorQuery.确定();
            return true;
        } catch (SecurityException e) {
            Log.e(APPTAG, "SecurityException");
            Log.e(APPTAG, e.toString());
            e.printStackTrace();
            return false;
        } catch (Exception e2) {
            Log.e(APPTAG, "Exception");
            Log.e(APPTAG, e2.toString());
            e2.printStackTrace();
            return false;
        }
    }

    private Uri fileToContentUri(Uri uri) {
        if (!hasPermission("android.permission.READ_EXTERNAL_STORAGE")) {
            return uri;
        }
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null || lastPathSegment.isEmpty()) {
            lastPathSegment = String.valueOf(System.currentTimeMillis());
        }
        try {
            if (!new RejhFileIO(this, APPTAG).cacheInputStream(lastPathSegment, getContentResolver().openInputStream(uri))) {
                return uri;
            }
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", new File(this.context.getCacheDir(), lastPathSegment));
        } catch (FileNotFoundException unused) {
            return uri;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeTargetItemsJson(ArrayList<Item> arrayList, String str) {
        JSONObject jSONObject = new JSONObject();
        for (int i = 0; i < arrayList.size(); i++) {
            try {
                Item item = arrayList.get(i);
                jSONObject.put(item.activityInfo.packageName + "#" + item.activityInfo.name, item.toJSONObject());
            } catch (JSONException e) {
                Log.e(APPTAG, "Item.writeTargetItemsCache().Error: " + e);
                e.printStackTrace();
            }
        }
        JSONObject jSONObject2 = this.mTargetItemsJson;
        if (jSONObject2 != null) {
            jSONObject = RejhJson.merge(jSONObject2, jSONObject);
        }
        Log.d(APPTAG, " -> Write data.targetItems.json");
        new RejhFileIO(this.context, APPTAG).writeTextFile("data.targetItems.json", jSONObject.toString());
    }

    private JSONObject readTargetItemsJson(String str) {
        Log.d(APPTAG, " -> Read data.targetItems.json");
        String textFile = new RejhFileIO(this.context, APPTAG).readTextFile("data.targetItems.json");
        if (textFile == null) {
            textFile = "{}";
        }
        try {
            return new JSONObject(textFile);
        } catch (JSONException e) {
            Log.e(APPTAG, "Item.readTargetItemsCache().Error: " + e);
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private JSONObject getTargetItemJson(Item item, JSONObject jSONObject) {
        String str = item.activityInfo.packageName + "#" + item.activityInfo.name;
        if (!jSONObject.has(str)) {
            return null;
        }
        try {
            return jSONObject.getJSONObject(str);
        } catch (JSONException e) {
            Log.e("\u5206\u4eab\u7ba1\u7406", "Item.readTargetItemsCache().Error: " + e);
            e.printStackTrace();
            return null;
        }
    }

    private boolean hasPermission(String str) {
        Log.d(APPTAG, "ActShareReplace.hasPermission()");
        return ContextCompat.checkSelfPermission(this.context, str) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestSinglePermission(String str, int i, String str2) {
        Log.d(APPTAG, "ActShareReplace.requestSinglePermission(): " + str);
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.READ_PHONE_STATE")) {
            showPermissionExplanation("Permission Needed", str2, str, i);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{str}, i);
        }
    }

    private void requestMultiplePermissions(String[] strArr, int i) {
        Log.d(APPTAG, "ActShareReplace.requestMultiplePermissions()");
        ActivityCompat.requestPermissions((Activity) this.context, strArr, i);
    }

    private void showPermissionExplanation(String str, final String str2, final String str3, final int i) {
        Log.d(APPTAG, "ActShareReplace.showPermissionExplanation()");
        int i2 = !this.mSharedPrefs.getBoolean("sharereplace_use_darkmode", false) ? bin.p002mt.plus.TranslationData.R.style.AlertDialog : bin.p002mt.plus.TranslationData.R.style.AlertDialogDark;
        ArrayList<DialogBuilder.ClickListenerOption> arrayList = new ArrayList<>();
        arrayList.add(new DialogBuilder.ClickListenerOption(-1, "OK", new DialogInterface.OnClickListener() { // from class: com.rejh.sharedr.ActShareReplace.15
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i3) {
                ActShareReplace.this.requestSinglePermission(str3, i, str2);
            }
        }));
        new DialogBuilder(this.context, i2).alert(str, str2, arrayList);
    }

    private void handleUpdate() {
        Log.d(APPTAG, "ActShareReplace.handleUpdate()");
        try {
            boolean z = this.mSharedPrefs.getBoolean("sharedr_was_updated", false);
            int i = this.mSharedPrefs.getInt("sharedr_versioncode", 0);
            int i2 = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionCode;
            this.mSharedPrefsEditor.putInt("sharedr_versioncode", i2);
            this.mSharedPrefsEditor.commit();
            if (!z || i >= i2) {
                Log.d(APPTAG, " -> No update, vc: " + i2);
                return;
            }
            if (i < 203040) {
                Log.d(APPTAG, " -> Update 203040, vc: " + i + ", " + i2);
                handleUpdate203040();
            }
            if (i < 204000) {
                Log.d(APPTAG, " -> Update 204000, vc: " + i + ", " + i2);
                handleUpdate204000();
            }
        } catch (PackageManager.NameNotFoundException unused) {
            Log.w(APPTAG, " -> NameNotFoundException");
        }
    }

    private void handleUpdate203040() {
        new DialogBuilder(this.context, !this.mSharedPrefs.getBoolean("sharereplace_use_darkmode", false) ? bin.p002mt.plus.TranslationData.R.style.AlertDialog : bin.p002mt.plus.TranslationData.R.style.AlertDialogDark).alert("Update notice: v2.3.4", "This update includes changes to how pinned, hidden and renamed apps are stored.<br><br>The good news is that this fixes at least one bug.<br><br>The bad news is that you may have lost some settings.<br><br>Sorry :(");
        new RejhFileIO(this.context, APPTAG).writeTextFile("data.targetItems.json", "{}");
    }

    private void handleUpdate204000() {
        this.mSharedPrefsEditor.putInt("sharereplace_theme", this.mSharedPrefs.getInt("sharereplace_theme", this.mSharedPrefs.getBoolean("sharereplace_use_darkmode", false) ? 2 : 0));
        this.mSharedPrefsEditor.commit();
    }

    public int asDp(int i) {
        return Math.round(getResources().getDisplayMetrics().density * i);
    }

    public int pixelToDp(int i) {
        return Math.round(i / getResources().getDisplayMetrics().density);
    }
}
