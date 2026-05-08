package androidx.lifecycle;

import android.app.Application;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;
import java.lang.reflect.Constructor;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class SavedStateViewModelFactory extends ViewModelProvider.KeyedFactory {
    private static final Class<?>[] ANDROID_VIEWMODEL_SIGNATURE = {Application.class, SavedStateHandle.class};
    private static final Class<?>[] VIEWMODEL_SIGNATURE = {SavedStateHandle.class};
    private final Application mApplication;
    private final Bundle mDefaultArgs;
    private final ViewModelProvider.Factory mFactory;
    private final Lifecycle mLifecycle;
    private final SavedStateRegistry mSavedStateRegistry;

    public SavedStateViewModelFactory(Application application, SavedStateRegistryOwner owner) {
        this(application, owner, null);
    }

    public SavedStateViewModelFactory(Application application, SavedStateRegistryOwner owner, Bundle defaultArgs) {
        ViewModelProvider.Factory newInstanceFactory;
        this.mSavedStateRegistry = owner.getSavedStateRegistry();
        this.mLifecycle = owner.getLifecycle();
        this.mDefaultArgs = defaultArgs;
        this.mApplication = application;
        if (application != null) {
            newInstanceFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application);
        } else {
            newInstanceFactory = ViewModelProvider.NewInstanceFactory.getInstance();
        }
        this.mFactory = newInstanceFactory;
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x0046 A[Catch: InvocationTargetException -> 0x005a, InstantiationException -> 0x0078, IllegalAccessException -> 0x0098, TryCatch #2 {IllegalAccessException -> 0x0098, InstantiationException -> 0x0078, InvocationTargetException -> 0x005a, blocks: (B:13:0x0030, B:15:0x0034, B:17:0x0054, B:16:0x0046), top: B:28:0x0030 }] */
    @Override // androidx.lifecycle.ViewModelProvider.KeyedFactory
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public <T extends androidx.lifecycle.ViewModel> T create(java.lang.String r6, java.lang.Class<T> r7) {
        /*
            r5 = this;
            java.lang.Class<androidx.lifecycle.AndroidViewModel> r0 = androidx.lifecycle.AndroidViewModel.class
            boolean r0 = r0.isAssignableFrom(r7)
            if (r0 == 0) goto L13
            android.app.Application r1 = r5.mApplication
            if (r1 == 0) goto L13
            java.lang.Class<?>[] r1 = androidx.lifecycle.SavedStateViewModelFactory.ANDROID_VIEWMODEL_SIGNATURE
            java.lang.reflect.Constructor r1 = findMatchingConstructor(r7, r1)
            goto L19
        L13:
            java.lang.Class<?>[] r1 = androidx.lifecycle.SavedStateViewModelFactory.VIEWMODEL_SIGNATURE
            java.lang.reflect.Constructor r1 = findMatchingConstructor(r7, r1)
        L19:
            if (r1 != 0) goto L22
            androidx.lifecycle.ViewModelProvider$Factory r6 = r5.mFactory
            androidx.lifecycle.ViewModel r6 = r6.create(r7)
            return r6
        L22:
            androidx.savedstate.SavedStateRegistry r2 = r5.mSavedStateRegistry
            androidx.lifecycle.Lifecycle r3 = r5.mLifecycle
            android.os.Bundle r4 = r5.mDefaultArgs
            androidx.lifecycle.SavedStateHandleController r6 = androidx.lifecycle.SavedStateHandleController.create(r2, r3, r6, r4)
            r2 = 0
            r3 = 1
            if (r0 == 0) goto L46
            android.app.Application r0 = r5.mApplication     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            if (r0 == 0) goto L46
            r4 = 2
            java.lang.Object[] r4 = new java.lang.Object[r4]     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            r4[r2] = r0     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            androidx.lifecycle.SavedStateHandle r0 = r6.getHandle()     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            r4[r3] = r0     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            java.lang.Object r0 = r1.newInstance(r4)     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            androidx.lifecycle.ViewModel r0 = (androidx.lifecycle.ViewModel) r0     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            goto L54
        L46:
            java.lang.Object[] r0 = new java.lang.Object[r3]     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            androidx.lifecycle.SavedStateHandle r3 = r6.getHandle()     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            r0[r2] = r3     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            java.lang.Object r0 = r1.newInstance(r0)     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            androidx.lifecycle.ViewModel r0 = (androidx.lifecycle.ViewModel) r0     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
        L54:
            java.lang.String r1 = "androidx.lifecycle.savedstate.vm.tag"
            r0.setTagIfAbsent(r1, r6)     // Catch: java.lang.reflect.InvocationTargetException -> L5a java.lang.InstantiationException -> L78 java.lang.IllegalAccessException -> L98
            return r0
        L5a:
            r6 = move-exception
            java.lang.RuntimeException r0 = new java.lang.RuntimeException
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "An exception happened in constructor of "
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r7 = r1.append(r7)
            java.lang.String r7 = r7.toString()
            java.lang.Throwable r6 = r6.getCause()
            r0.<init>(r7, r6)
            throw r0
        L78:
            r6 = move-exception
            java.lang.RuntimeException r0 = new java.lang.RuntimeException
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "A "
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r7 = r1.append(r7)
            java.lang.String r1 = " cannot be instantiated."
            java.lang.StringBuilder r7 = r7.append(r1)
            java.lang.String r7 = r7.toString()
            r0.<init>(r7, r6)
            throw r0
        L98:
            r6 = move-exception
            java.lang.RuntimeException r0 = new java.lang.RuntimeException
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "Failed to access "
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r7 = r1.append(r7)
            java.lang.String r7 = r7.toString()
            r0.<init>(r7, r6)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.lifecycle.SavedStateViewModelFactory.create(java.lang.String, java.lang.Class):androidx.lifecycle.ViewModel");
    }

    @Override // androidx.lifecycle.ViewModelProvider.KeyedFactory, androidx.lifecycle.ViewModelProvider.Factory
    public <T extends ViewModel> T create(Class<T> cls) {
        String canonicalName = cls.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return (T) create(canonicalName, cls);
    }

    private static <T> Constructor<T> findMatchingConstructor(Class<T> cls, Class<?>[] clsArr) {
        for (Object obj : cls.getConstructors()) {
            Constructor<T> constructor = (Constructor<T>) obj;
            if (Arrays.equals(clsArr, constructor.getParameterTypes())) {
                return constructor;
            }
        }
        return null;
    }

    @Override // androidx.lifecycle.ViewModelProvider.OnRequeryFactory
    void onRequery(ViewModel viewModel) {
        SavedStateHandleController.attachHandleIfNeeded(viewModel, this.mSavedStateRegistry, this.mLifecycle);
    }
}
