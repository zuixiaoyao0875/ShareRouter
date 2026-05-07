package com.zxy.sharerouter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sourceforge.pinyin4j.PinyinHelper
import org.json.JSONArray
import org.json.JSONObject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

data class AppTarget(
    val name: String,
    val packageName: String,
    val componentName: ComponentName,
    val isPinned: Boolean,
    val pinyinPrefix: String
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences("ShareRouterPrefs", Context.MODE_PRIVATE)

        val action = intent.action
        val isShareIntent = action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE

        setContent {
            MaterialTheme {
                var pinnedSet by remember { mutableStateOf(getPinnedComponents()) }
                var hiddenSet by remember { mutableStateOf(getHiddenComponents()) }
                var pinnedOrder by remember { mutableStateOf(getPinnedOrder()) }

                var columnCount by remember { mutableStateOf(getColumnCount()) }
                var showAppName by remember { mutableStateOf(getShowAppName()) }
                var fontSize by remember { mutableStateOf(getFontSize()) }
                var lineSpacing by remember { mutableStateOf(getLineSpacing()) }

                if (isShareIntent) {
                    var showBottomSheet by remember { mutableStateOf(true) }
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                    
                    var isTempUnhideEnabled by remember { mutableStateOf(false) }

                    // 1. Initial State: Load from JSON cache IMMEDIATELY (Synchronously)
                    // This ensures the first frame already contains the app list, avoiding flickers.
                    var targetApps by remember(pinnedSet, hiddenSet, pinnedOrder, isTempUnhideEnabled) {
                        val startTime = System.currentTimeMillis()
                        val cached = getCachedTargets()
                        val initial = if (cached != null) {
                            sortTargets(cached, pinnedSet, hiddenSet, pinnedOrder, isTempUnhideEnabled)
                        } else {
                            emptyList()
                        }
                        android.util.Log.d("ShareRouter", "Cache load took ${System.currentTimeMillis() - startTime}ms, size: ${initial.size}")
                        mutableStateOf(initial)
                    }

                    // 2. Background silent sync (Only for refreshing the system list)
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            val startTime = System.currentTimeMillis()
                            val realApps = fetchSystemTargets()
                            android.util.Log.d("ShareRouter", "System scan took ${System.currentTimeMillis() - startTime}ms")
                            
                            val realCompStr = realApps.map { it.componentName.flattenToString() }.toSet()
                            val cachedCompStr = targetApps.map { it.componentName.flattenToString() }.toSet()
                            
                            // If the installed apps changed (added or removed)
                            if (realCompStr != cachedCompStr) {
                                android.util.Log.d("ShareRouter", "System targets changed, updating cache...")
                                saveTargetsToCache(realApps)
                                val sorted = sortTargets(realApps, pinnedSet, hiddenSet, pinnedOrder, isTempUnhideEnabled)
                                withContext(Dispatchers.Main) {
                                    targetApps = sorted
                                }
                            }
                        }
                    }

                    if (showBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                showBottomSheet = false
                                finish()
                            },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface,
                            dragHandle = {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 10.dp, bottom = 4.dp)
                                        .width(32.dp)
                                        .height(4.dp)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), MaterialTheme.shapes.extraLarge)
                                )
                            }
                        ) {
                            ShareTargetScreen(
                                targets = targetApps,
                                columnCount = columnCount,
                                showAppName = showAppName,
                                fontSize = fontSize,
                                lineSpacing = lineSpacing,
                                isTempUnhideEnabled = isTempUnhideEnabled,
                                onColumnCountChange = { c -> 
                                    columnCount = c
                                    saveColumnCount(c) 
                                },
                                onShowAppNameChange = { s -> 
                                    showAppName = s
                                    saveShowAppName(s) 
                                },
                                onFontSizeChange = { f ->
                                    fontSize = f
                                    saveFontSize(f)
                                },
                                onLineSpacingChange = { s ->
                                    lineSpacing = s
                                    saveLineSpacing(s)
                                },
                                onTempUnhideToggle = { 
                                    isTempUnhideEnabled = !isTempUnhideEnabled 
                                },
                                onTargetClick = { target -> forwardIntent(intent, target) },
                                onPinToggle = { target ->
                                    val compStr = target.componentName.flattenToString()
                                    val newPinned = pinnedSet.toMutableSet()
                                    if (newPinned.contains(compStr)) {
                                        newPinned.remove(compStr)
                                    } else {
                                        newPinned.add(compStr)
                                    }
                                    savePinnedComponents(newPinned)
                                    pinnedSet = newPinned
                                },
                                onHide = { target ->
                                    val compStr = target.componentName.flattenToString()
                                    val newHidden = hiddenSet.toMutableSet()
                                    newHidden.add(compStr)
                                    saveHiddenComponents(newHidden)
                                    hiddenSet = newHidden
                                }
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(
                            hiddenSet = hiddenSet,
                            pinnedSet = pinnedSet,
                            pinnedOrder = pinnedOrder,
                            columnCount = columnCount,
                            showAppName = showAppName,
                            fontSize = fontSize,
                            lineSpacing = lineSpacing,
                            onUnhide = { compStr ->
                                val newHidden = hiddenSet.toMutableSet()
                                newHidden.remove(compStr)
                                saveHiddenComponents(newHidden)
                                hiddenSet = newHidden
                            },
                            onPinnedOrderChange = { newList ->
                                savePinnedOrder(newList)
                                pinnedOrder = newList
                            }
                        )
                    }
                }
            }
        }
    }

    private fun getPinnedComponents(): Set<String> {
        return prefs.getStringSet("pinned_components", emptySet()) ?: emptySet()
    }

    private fun savePinnedComponents(set: Set<String>) {
        prefs.edit().putStringSet("pinned_components", set).apply()
    }

    private fun getHiddenComponents(): Set<String> {
        return prefs.getStringSet("hidden_components", emptySet()) ?: emptySet()
    }

    private fun saveHiddenComponents(set: Set<String>) {
        prefs.edit().putStringSet("hidden_components", set).apply()
    }

    private fun getPinnedOrder(): List<String> {
        val str = prefs.getString("pinned_order", "") ?: ""
        return if (str.isBlank()) emptyList() else str.split(",")
    }

    private fun savePinnedOrder(list: List<String>) {
        prefs.edit().putString("pinned_order", list.joinToString(",")).apply()
    }

    private fun getColumnCount(): Int = prefs.getInt("column_count", 7)
    private fun saveColumnCount(count: Int) = prefs.edit().putInt("column_count", count).apply()

    private fun getShowAppName(): Boolean = prefs.getBoolean("show_app_name", true)
    private fun saveShowAppName(show: Boolean) = prefs.edit().putBoolean("show_app_name", show).apply()

    private fun getFontSize(): Float = prefs.getFloat("font_size", 10f)
    private fun saveFontSize(size: Float) = prefs.edit().putFloat("font_size", size).apply()

    private fun getLineSpacing(): Float = prefs.getFloat("line_spacing", 10f)
    private fun saveLineSpacing(spacing: Float) = prefs.edit().putFloat("line_spacing", spacing).apply()

    private fun recordUsage(target: AppTarget) {
        val compStr = target.componentName.flattenToString()
        prefs.edit().putString("last_used_app", compStr).apply()
        val count = prefs.getInt("usage_count_$compStr", 0)
        prefs.edit().putInt("usage_count_$compStr", count + 1).apply()
    }

    private fun getPinyinPrefix(text: String): String {
        val builder = java.lang.StringBuilder()
        for (c in text) {
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c)
            if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                builder.append(pinyinArray[0][0])
            } else {
                builder.append(c)
            }
        }
        return builder.toString().lowercase()
    }

    fun getCachedTargets(): List<AppTarget>? {
        val jsonStr = prefs.getString("cached_targets_json", null) ?: return null
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AppTarget>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val compStr = obj.getString("comp")
                val comp = ComponentName.unflattenFromString(compStr) ?: continue
                list.add(
                    AppTarget(
                        name = obj.getString("name"),
                        packageName = comp.packageName,
                        componentName = comp,
                        isPinned = false, // Will be overridden
                        pinyinPrefix = obj.getString("pinyin")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    fun saveTargetsToCache(targets: List<AppTarget>) {
        val jsonArray = JSONArray()
        for (target in targets) {
            val obj = JSONObject()
            obj.put("comp", target.componentName.flattenToString())
            obj.put("name", target.name)
            obj.put("pinyin", target.pinyinPrefix)
            jsonArray.put(obj)
        }
        prefs.edit().putString("cached_targets_json", jsonArray.toString()).apply()
    }

    fun fetchSystemTargets(): List<AppTarget> {
        val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "*/*" }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                shareIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        return resolveInfos
            .filter { it.activityInfo.packageName != packageName } // Exclude self
            .map {
                val componentName = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
                val compStr = componentName.flattenToString()
                
                val label = prefs.getString("label_$compStr", null) ?: run {
                    val fetchedLabel = it.loadLabel(packageManager).toString()
                    prefs.edit().putString("label_$compStr", fetchedLabel).apply()
                    fetchedLabel
                }
                val pinyin = prefs.getString("pinyin_$compStr", null) ?: run {
                    val fetchedPinyin = getPinyinPrefix(label)
                    prefs.edit().putString("pinyin_$compStr", fetchedPinyin).apply()
                    fetchedPinyin
                }
                
                AppTarget(
                    name = label,
                    packageName = it.activityInfo.packageName,
                    componentName = componentName,
                    isPinned = false,
                    pinyinPrefix = pinyin
                )
            }
            .distinctBy { it.componentName.flattenToString() }
    }

    private fun sortTargets(
        rawApps: List<AppTarget>,
        pinnedSet: Set<String>, 
        hiddenSet: Set<String>,
        pinnedOrder: List<String>,
        isTempUnhideEnabled: Boolean
    ): List<AppTarget> {
        val lastUsedApp = prefs.getString("last_used_app", "") ?: ""
        val effectiveHiddenSet = if (isTempUnhideEnabled) emptySet() else hiddenSet

        return rawApps
            .map { it.copy(isPinned = pinnedSet.contains(it.componentName.flattenToString())) }
            .filter { !effectiveHiddenSet.contains(it.componentName.flattenToString()) }
            .sortedWith { a, b ->
                val aComp = a.componentName.flattenToString()
                val bComp = b.componentName.flattenToString()
                if (a.isPinned && b.isPinned) {
                    val aIndex = pinnedOrder.indexOf(aComp).let { if (it == -1) Int.MAX_VALUE else it }
                    val bIndex = pinnedOrder.indexOf(bComp).let { if (it == -1) Int.MAX_VALUE else it }
                    if (aIndex != bIndex) aIndex.compareTo(bIndex) else a.name.compareTo(b.name)
                } else if (!a.isPinned && !b.isPinned) {
                    val aIsLastUsed = aComp == lastUsedApp
                    val bIsLastUsed = bComp == lastUsedApp
                    if (aIsLastUsed != bIsLastUsed) {
                        if (aIsLastUsed) -1 else 1
                    } else {
                        val aCount = prefs.getInt("usage_count_$aComp", 0)
                        val bCount = prefs.getInt("usage_count_$bComp", 0)
                        if (aCount != bCount) {
                            bCount.compareTo(aCount)
                        } else {
                            a.name.compareTo(b.name)
                        }
                    }
                } else {
                    if (a.isPinned) -1 else 1
                }
            }
    }

    private fun forwardIntent(originalIntent: Intent, target: AppTarget) {
        try {
            val forwardIntent = Intent(originalIntent).apply {
                component = target.componentName
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
            }
            startActivity(forwardIntent)
            recordUsage(target)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun ShareTargetScreen(
    targets: List<AppTarget>,
    columnCount: Int,
    showAppName: Boolean,
    fontSize: Float,
    lineSpacing: Float,
    isTempUnhideEnabled: Boolean,
    onColumnCountChange: (Int) -> Unit,
    onShowAppNameChange: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onTempUnhideToggle: () -> Unit,
    onTargetClick: (AppTarget) -> Unit,
    onPinToggle: (AppTarget) -> Unit,
    onHide: (AppTarget) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    
    val filteredTargets = remember(searchQuery, targets) {
        if (searchQuery.isBlank()) {
            targets
        } else {
            val query = searchQuery.lowercase()
            targets.filter { 
                it.name.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true) ||
                it.pinyinPrefix.contains(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择分享目标",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onTempUnhideToggle) {
                Text(
                    text = if (isTempUnhideEnabled) "隐藏已忽略" else "显示已隐藏",
                    color = if (isTempUnhideEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showSettings = !showSettings }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }

        AnimatedVisibility(visible = showSettings) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("列数: $columnCount", modifier = Modifier.width(70.dp))
                        Slider(
                            value = columnCount.toFloat(),
                            onValueChange = { onColumnCountChange(it.toInt()) },
                            valueRange = 1f..20f,
                            steps = 18,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("字号: ${fontSize.toInt()}", modifier = Modifier.width(70.dp))
                        Slider(
                            value = fontSize,
                            onValueChange = { onFontSizeChange(it) },
                            valueRange = 1f..24f,
                            steps = 22,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("行距: ${lineSpacing.toInt()}", modifier = Modifier.width(70.dp))
                        Slider(
                            value = lineSpacing,
                            onValueChange = { onLineSpacingChange(it) },
                            valueRange = 0f..32f,
                            steps = 31,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("显示名称", modifier = Modifier.weight(1f))
                        Switch(
                            checked = showAppName,
                            onCheckedChange = { onShowAppNameChange(it) }
                        )
                    }
                }
            }
        }
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
            placeholder = { Text("搜索应用名称...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search Icon")
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(lineSpacing.dp)
        ) {
            items(filteredTargets, key = { it.componentName.flattenToString() }) { target ->
                AppTargetGridItem(
                    target = target,
                    showAppName = showAppName,
                    fontSize = fontSize,
                    showMenuEnabled = true,
                    onClick = { onTargetClick(target) },
                    onPinToggle = { onPinToggle(target) },
                    onHide = { onHide(target) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTargetGridItem(
    target: AppTarget,
    showAppName: Boolean,
    fontSize: Float,
    showMenuEnabled: Boolean,
    onClick: () -> Unit = {},
    onPinToggle: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, target) {
        value = withContext(Dispatchers.IO) {
            try {
                packageManager.getActivityIcon(target.componentName).toBitmap().asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showMenuEnabled) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = { showMenu = true }
                    )
                } else {
                    Modifier.clickable { onClick() }
                }
            )
            .then(dragModifier)
            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .padding(vertical = 0.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap!!,
                    contentDescription = target.name,
                    modifier = Modifier
                        .widthIn(max = 48.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(max = 48.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                )
            }
            
            if (showAppName) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = target.name,
                    fontSize = fontSize.sp,
                    lineHeight = fontSize.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (target.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        if (showMenuEnabled && onPinToggle != null && onHide != null) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (target.isPinned) "取消置顶" else "设为置顶") },
                    onClick = {
                        showMenu = false
                        onPinToggle()
                    }
                )
                DropdownMenuItem(
                    text = { Text("隐藏该应用", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onHide()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    hiddenSet: Set<String>,
    pinnedSet: Set<String>,
    pinnedOrder: List<String>,
    columnCount: Int,
    showAppName: Boolean,
    fontSize: Float,
    lineSpacing: Float,
    onUnhide: (String) -> Unit,
    onPinnedOrderChange: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val mainActivity = context as MainActivity

    // 1. Synchronous cache load for the settings screen
    var rawApps by remember {
        val cached = mainActivity.getCachedTargets()
        mutableStateOf(cached ?: emptyList())
    }

    // 2. Background sync
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val realApps = mainActivity.fetchSystemTargets()
            val realCompStr = realApps.map { it.componentName.flattenToString() }.toSet()
            val cachedCompStr = rawApps.map { it.componentName.flattenToString() }.toSet()
            
            if (realCompStr != cachedCompStr) {
                mainActivity.saveTargetsToCache(realApps)
                withContext(Dispatchers.Main) {
                    rawApps = realApps
                }
            }
        }
    }

    val appsWithPinState = remember(rawApps, pinnedSet) {
        rawApps.map { it.copy(isPinned = pinnedSet.contains(it.componentName.flattenToString())) }
    }

    val hiddenApps = remember(appsWithPinState, hiddenSet) {
        appsWithPinState.filter { hiddenSet.contains(it.componentName.flattenToString()) }.sortedBy { it.name }
    }

    var mutablePinnedApps by remember(appsWithPinState, pinnedSet, pinnedOrder, hiddenSet) {
        mutableStateOf(
            appsWithPinState.filter { it.isPinned && !hiddenSet.contains(it.componentName.flattenToString()) }
                   .sortedWith { a, b ->
                       val aComp = a.componentName.flattenToString()
                       val bComp = b.componentName.flattenToString()
                       val aIndex = pinnedOrder.indexOf(aComp).let { if (it == -1) Int.MAX_VALUE else it }
                       val bIndex = pinnedOrder.indexOf(bComp).let { if (it == -1) Int.MAX_VALUE else it }
                       if (aIndex != bIndex) aIndex.compareTo(bIndex) else a.name.compareTo(b.name)
                   }
        )
    }

    val normalApps = remember(appsWithPinState, pinnedSet, hiddenSet) {
        val prefs = mainActivity.getSharedPreferences("ShareRouterPrefs", Context.MODE_PRIVATE)
        val lastUsedApp = prefs.getString("last_used_app", "") ?: ""
        
        appsWithPinState.filter { !it.isPinned && !hiddenSet.contains(it.componentName.flattenToString()) }
               .sortedWith { a, b ->
                   val aComp = a.componentName.flattenToString()
                   val bComp = b.componentName.flattenToString()
                   val aIsLastUsed = aComp == lastUsedApp
                   val bIsLastUsed = bComp == lastUsedApp
                   if (aIsLastUsed != bIsLastUsed) {
                       if (aIsLastUsed) -1 else 1
                   } else {
                       val aCount = prefs.getInt("usage_count_$aComp", 0)
                       val bCount = prefs.getInt("usage_count_$bComp", 0)
                       if (aCount != bCount) {
                           bCount.compareTo(aCount)
                       } else {
                           a.name.compareTo(b.name)
                       }
                   }
               }
    }

    val lazyGridState = rememberLazyGridState()
    
    val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        val fromKey = from.key.toString()
        val toKey = to.key.toString()

        if (fromKey.startsWith("pinned_") && toKey.startsWith("pinned_")) {
            val fromIndex = mutablePinnedApps.indexOfFirst { "pinned_" + it.componentName.flattenToString() == fromKey }
            val toIndex = mutablePinnedApps.indexOfFirst { "pinned_" + it.componentName.flattenToString() == toKey }
            if (fromIndex != -1 && toIndex != -1) {
                mutablePinnedApps = mutablePinnedApps.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                onPinnedOrderChange(mutablePinnedApps.map { it.componentName.flattenToString() })
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("ShareRouter 设置") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            state = lazyGridState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(lineSpacing.dp)
        ) {
            // -- Pinned Apps Section --
            if (mutablePinnedApps.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "置顶应用 (长按图标拖拽)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                items(mutablePinnedApps, key = { "pinned_" + it.componentName.flattenToString() }) { app ->
                    ReorderableItem(reorderableState, key = "pinned_" + app.componentName.flattenToString()) { isDragging ->
                        AppTargetGridItem(
                            target = app,
                            showAppName = showAppName,
                            fontSize = fontSize,
                            showMenuEnabled = false,
                            isDragging = isDragging,
                            dragModifier = Modifier.longPressDraggableHandle()
                        )
                    }
                }
            }

            // -- Normal Apps Section --
            if (normalApps.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "常规应用 (按使用频次智能排序)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                items(normalApps, key = { "normal_" + it.componentName.flattenToString() }) { app ->
                    AppTargetGridItem(
                        target = app,
                        showAppName = showAppName,
                        fontSize = fontSize,
                        showMenuEnabled = false
                    )
                }
            }

            // -- Hidden Apps Section --
            if (hiddenApps.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "已隐藏的应用",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                items(hiddenApps, key = { "hidden_" + it.componentName.flattenToString() }) { app ->
                    val packageManager = context.packageManager
                    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app) {
                        value = withContext(Dispatchers.IO) {
                            try {
                                packageManager.getActivityIcon(app.componentName).toBitmap().asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp, horizontal = 2.dp)
                    ) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap!!, 
                                contentDescription = app.name, 
                                modifier = Modifier
                                    .widthIn(max = 48.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 48.dp)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                            )
                        }
                        if (showAppName) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = app.name,
                                fontSize = fontSize.sp,
                                lineHeight = fontSize.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                        TextButton(onClick = { onUnhide(app.componentName.flattenToString()) }) {
                            Text("取消隐藏", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
