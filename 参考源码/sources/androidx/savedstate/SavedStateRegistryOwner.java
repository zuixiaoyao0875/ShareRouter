package androidx.savedstate;

import androidx.lifecycle.LifecycleOwner;

/* JADX INFO: loaded from: classes.dex */
public interface SavedStateRegistryOwner extends LifecycleOwner {
    SavedStateRegistry getSavedStateRegistry();
}
