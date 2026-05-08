package com.zxy.sharerouter

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.content.ClipboardManager
import android.content.ClipData
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.pinyin4j.PinyinHelper
import org.json.JSONArray
import org.json.JSONObject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import android.util.LruCache
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import java.io.File
import java.io.FileOutputStream

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
    private val iconCache = LruCache<String, ImageBitmap>(200)
    private val iconSemaphore = Semaphore(4) // 进一步收紧，给 UI 留出更多资源

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. 立即显示 XML 骨架屏，抢占冷启动首帧
        setContentView(R.layout.skeleton_main)
        
        prefs = getSharedPreferences("ShareRouterPrefs", Context.MODE_PRIVATE)

        // 异步初始化图标缓存目录
        val iconCacheDir = File(cacheDir, "icon_cache")
        if (!iconCacheDir.exists()) iconCacheDir.mkdirs()

        val action = intent.action
        val isShareIntent = action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE

        // 稍微延迟 Compose 的挂载，确保 XML 骨架屏至少渲染一帧，消除 Compose 初始化时的瞬时白屏
        window.decorView.post {
            setContent {
                MaterialTheme(
                    colorScheme = if (isSystemInDarkTheme()) darkColorScheme(surface = Color(0xFF1C1B1F)) else lightColorScheme()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        var pinnedSet by remember { mutableStateOf(getPinnedComponents()) }
                        var hiddenSet by remember { mutableStateOf(getHiddenComponents()) }
                        var pinnedOrder by remember { mutableStateOf(getPinnedOrder()) }

                        var columnCount by remember { mutableStateOf(getColumnCount()) }
                        var showAppName by remember { mutableStateOf(getShowAppName()) }
                        var fontSize by remember { mutableStateOf(getFontSize()) }
                        var lineSpacing by remember { mutableStateOf(getLineSpacing()) }

                        val onConfigImported = { newOrder: List<String>, newHidden: Set<String> ->
                            pinnedOrder = newOrder
                            hiddenSet = newHidden
                            pinnedSet = newOrder.toSet()
                        }

                        if (isShareIntent) {
                            var showBottomSheet by remember { mutableStateOf(true) }
                            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                            var isTempUnhideEnabled by remember { mutableStateOf(false) }
                            var isFirstLoad by remember { mutableStateOf(true) }

                            // 1. 【极致优化】分阶段加载：冷启动阶梯式载入，热更新（隐藏/置顶）瞬间响应
                            val targetApps by produceState<List<AppTarget>>(
                                initialValue = if (isFirstLoad) {
                                    getCachedTargets()?.let { cached ->
                                        val lastUsed = prefs.getString("last_used_app", "") ?: ""
                                        cached.filter { 
                                            pinnedSet.contains(it.componentName.flattenToString()) || 
                                            it.componentName.flattenToString() == lastUsed 
                                        }.let { sortTargets(it, pinnedSet, hiddenSet, pinnedOrder, isTempUnhideEnabled) }
                                    } ?: emptyList()
                                } else emptyList(),
                                pinnedSet, hiddenSet, pinnedOrder, isTempUnhideEnabled
                            ) {
                                value = withContext(Dispatchers.IO) {
                                    // 2. 后台获取精准过滤的全量列表
                                    val filtered = fetchSystemTargets(intent)
                                    val sorted = sortTargets(filtered, pinnedSet, hiddenSet, pinnedOrder, isTempUnhideEnabled)
                                    
                                    if (isFirstLoad) {
                                        // 首次加载路径：阶梯式注入，平滑首帧
                                        val lastUsed = prefs.getString("last_used_app", "") ?: ""
                                        val minimal = sorted.filter { 
                                            pinnedSet.contains(it.componentName.flattenToString()) || 
                                            it.componentName.flattenToString() == lastUsed 
                                        }
                                        withContext(Dispatchers.Main) {
                                            value = minimal
                                        }
                                        
                                        delay(450) 
                                        withContext(Dispatchers.Main) {
                                            value = sorted.take(30)
                                        }
                                        
                                        delay(800)
                                        android.util.Log.d("ShareRouter", "Staged loading completed.")
                                        isFirstLoad = false // 之后的所有操作都进入快车道
                                        sorted
                                    } else {
                                        // 热更新路径：无延迟即时加载
                                        sorted
                                    }
                                }
                            }

                            // 2. 背景同步：避开启动峰值更新缓存
                            LaunchedEffect(Unit) {
                                withContext(Dispatchers.IO) {
                                    delay(1500)
                                    val realFullApps = fetchSystemTargets(null)
                                    saveTargetsToCache(realFullApps)
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
                                        pinnedSet = pinnedSet,
                                        pinnedOrder = pinnedOrder,
                                        hiddenSet = hiddenSet,
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
                                            val newOrder = pinnedOrder.toMutableList()
                                            if (newPinned.contains(compStr)) {
                                                newPinned.remove(compStr)
                                                newOrder.remove(compStr)
                                            } else {
                                                newPinned.add(compStr)
                                                if (!newOrder.contains(compStr)) newOrder.add(compStr)
                                            }
                                            savePinnedComponents(newPinned)
                                            savePinnedOrder(newOrder)
                                            pinnedSet = newPinned
                                            pinnedOrder = newOrder
                                        },
                                        onHide = { target ->
                                            val compStr = target.componentName.flattenToString()
                                            val newHidden = hiddenSet.toMutableSet()
                                            newHidden.add(compStr)
                                            saveHiddenComponents(newHidden)
                                            hiddenSet = newHidden
                                        },
                                        onConfigImported = onConfigImported
                                    )
                                }
                            }
                        } else {
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
                                },
                                onConfigImported = onConfigImported
                            )
                        }
                    }
                }
            }
        }
    }

    fun getPinnedComponents(): Set<String> {
        return prefs.getStringSet("pinned_components", emptySet()) ?: emptySet()
    }

    private fun savePinnedComponents(set: Set<String>) {
        prefs.edit().putStringSet("pinned_components", set).apply()
    }

    // --- 导入导出逻辑 ---
    fun exportConfigToClipboard(pinnedOrder: List<String>, hiddenSet: Set<String>) {
        try {
            val json = JSONObject().apply {
                put("version", 1)
                put("pinned", JSONArray(pinnedOrder))
                put("hidden", JSONArray(hiddenSet.toList()))
            }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ShareRouter Config", json.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "配置已复制到剪贴板", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun parseConfigFromClipboard(): JSONObject? {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) return null
        val text = clip.getItemAt(0).text?.toString() ?: return null
        return try {
            val obj = JSONObject(text)
            if (obj.has("pinned") || obj.has("hidden")) obj else null
        } catch (e: Exception) {
            null
        }
    }

    fun applyConfig(config: JSONObject, onComplete: (List<String>, Set<String>) -> Unit) {
        try {
            val pinnedArr = config.optJSONArray("pinned")
            val hiddenArr = config.optJSONArray("hidden")

            val newPinnedOrder = mutableListOf<String>()
            if (pinnedArr != null) {
                for (i in 0 until pinnedArr.length()) {
                    newPinnedOrder.add(pinnedArr.getString(i))
                }
            }

            val newHiddenSet = mutableSetOf<String>()
            if (hiddenArr != null) {
                for (i in 0 until hiddenArr.length()) {
                    newHiddenSet.add(hiddenArr.getString(i))
                }
            }

            savePinnedOrder(newPinnedOrder)
            saveHiddenComponents(newHiddenSet)
            
            onComplete(newPinnedOrder, newHiddenSet)
            Toast.makeText(this, "配置导入成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getHiddenComponents(): Set<String> {
        return prefs.getStringSet("hidden_components", emptySet()) ?: emptySet()
    }

    private fun saveHiddenComponents(set: Set<String>) {
        prefs.edit().putStringSet("hidden_components", set).apply()
    }

    fun getPinnedOrder(): List<String> {
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
            if (c.isWhitespace()) continue
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c)
            if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                builder.append(pinyinArray[0][0])
            } else if (c.isLetterOrDigit()) {
                builder.append(c)
            }
        }
        val result = builder.toString().lowercase()
        android.util.Log.d("ShareRouter", "Pinyin mapping: [$text] -> [$result]")
        return result
    }

    fun clearPinyinCache() {
        val allEntries = prefs.all
        val editor = prefs.edit()
        for (key in allEntries.keys) {
            if (key.startsWith("pinyin_")) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    /**
     * Optimized icon loader with memory cache, disk cache, and concurrency control.
     */
    suspend fun loadIcon(target: AppTarget): ImageBitmap? {
        val key = target.componentName.flattenToString()
        
        // 1. Check memory cache
        iconCache.get(key)?.let { return it }
        
        // 2. Load from disk cache (fast)
        val diskIcon = withContext(Dispatchers.IO) { loadIconFromDisk(key) }
        if (diskIcon != null) {
            iconCache.put(key, diskIcon)
            return diskIcon
        }

        // 3. Load from system with concurrency limit (slowest)
        return withContext(Dispatchers.IO) {
            iconSemaphore.withPermit {
                // Double check memory cache after acquiring permit
                iconCache.get(key)?.let { return@withPermit it }
                
                try {
                    val drawable = packageManager.getActivityIcon(target.componentName)
                    val targetSize = (56 * resources.displayMetrics.density).toInt().coerceIn(120, 192)
                    val bitmap = drawable.toBitmap(width = targetSize, height = targetSize)
                    
                    // Save to disk for next cold start
                    saveIconToDisk(key, bitmap)
                    
                    val imageBitmap = bitmap.asImageBitmap()
                    iconCache.put(key, imageBitmap)
                    imageBitmap
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private fun loadIconFromDisk(key: String): ImageBitmap? {
        val safeKey = key.replace("/", "_").replace("#", "_")
        val file = File(cacheDir, "icon_cache/$safeKey.webp")
        if (!file.exists()) return null
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                // 改为 VERBOSE 级别，避免全量加载时刷屏
                if (android.util.Log.isLoggable("ShareRouter", android.util.Log.VERBOSE)) {
                    android.util.Log.v("ShareRouter", "Disk cache HIT for: $safeKey")
                }
                bitmap.asImageBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveIconToDisk(key: String, bitmap: Bitmap) {
        val safeKey = key.replace("/", "_").replace("#", "_")
        val file = File(cacheDir, "icon_cache/$safeKey.webp")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
                android.util.Log.d("ShareRouter", "Saved to disk cache: $safeKey")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            list.distinctBy { it.packageName + "|" + it.name }
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

    fun fetchSystemTargets(filterIntent: Intent? = null): List<AppTarget> {
        val queryIntent = if (filterIntent != null) {
            // 必须构造一个新的 Intent，剥离掉指向自身的 Component 信息
            Intent(filterIntent.action).apply {
                setDataAndType(filterIntent.data, filterIntent.type)
                filterIntent.categories?.forEach { addCategory(it) }
            }
        } else {
            Intent(Intent.ACTION_SEND).apply { type = "*/*" }
        }
        
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                queryIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(queryIntent, PackageManager.MATCH_DEFAULT_ONLY)
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
            // 核心改进：按包名 + 名称去重，解决“多个一模一样的入口”问题
            .distinctBy { it.packageName + "|" + it.name }
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
    pinnedSet: Set<String>,
    pinnedOrder: List<String>,
    hiddenSet: Set<String>,
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
    onConfigImported: (List<String>, Set<String>) -> Unit,
    onTargetClick: (AppTarget) -> Unit,
    onPinToggle: (AppTarget) -> Unit,
    onHide: (AppTarget) -> Unit
) {
    val context = LocalContext.current
    val mainActivity = remember(context) {
        var c = context
        while (c is ContextWrapper) {
            if (c is MainActivity) break
            c = c.baseContext
        }
        c as MainActivity
    }
    var searchQuery by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    
    val filteredTargets = remember(searchQuery, targets) {
        if (searchQuery.isBlank()) {
            targets
        } else {
            val query = searchQuery.trim().lowercase()
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

        var showImportConfirm by remember { mutableStateOf(false) }
        var pendingConfig by remember { mutableStateOf<JSONObject?>(null) }

        if (showImportConfirm && pendingConfig != null) {
            AlertDialog(
                onDismissRequest = { showImportConfirm = false },
                title = { Text("确认导入配置？") },
                text = { Text("导入配置将完全替换您当前的置顶顺序和隐藏应用列表，此操作不可撤销。") },
                confirmButton = {
                    TextButton(onClick = {
                        mainActivity.applyConfig(pendingConfig!!) { newOrder, newHidden ->
                            onConfigImported(newOrder, newHidden)
                        }
                        showImportConfirm = false
                    }) {
                        Text("确认导入", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportConfirm = false }) {
                        Text("取消")
                    }
                }
            )
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { 
                            mainActivity.exportConfigToClipboard(pinnedOrder, hiddenSet)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("导出配置")
                        }
                        TextButton(onClick = { 
                            val config = mainActivity.parseConfigFromClipboard()
                            if (config != null) {
                                pendingConfig = config
                                showImportConfirm = true
                            } else {
                                Toast.makeText(context, "剪贴板中没有有效的配置信息", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("导入配置")
                        }
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
                    onHide = { onHide(target) },
                    isSearchActive = searchQuery.isNotBlank()
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
    isSearchActive: Boolean = false,
    onClick: () -> Unit = {},
    onPinToggle: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainActivity = remember(context) {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is MainActivity) break
            currentContext = currentContext.baseContext
        }
        currentContext as MainActivity
    }
    
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, target.componentName) {
        // 避峰加载：延迟 50ms 等待面板滑出动画最吃性能的前几帧过去
        delay(50)
        value = mainActivity.loadIcon(target)
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
            
            // 搜索状态下显示拼音索引用于调试
            if (isSearchActive) {
                Text(
                    text = target.pinyinPrefix,
                    fontSize = (fontSize * 0.7f).sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    onPinnedOrderChange: (List<String>) -> Unit,
    onConfigImported: (List<String>, Set<String>) -> Unit
) {
    val context = LocalContext.current
    val mainActivity = remember(context) {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is MainActivity) break
            currentContext = currentContext.baseContext
        }
        currentContext as MainActivity
    }

    // 1. Synchronous cache load for the settings screen
    var rawApps by remember {
        val cached = mainActivity.getCachedTargets()
        mutableStateOf<List<AppTarget>>(cached ?: emptyList())
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
                mutablePinnedApps = mutablePinnedApps.toMutableList().also { 
                    it.add(toIndex, it.removeAt(fromIndex)) 
                }
                onPinnedOrderChange(mutablePinnedApps.map { it.componentName.flattenToString() })
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("ShareRouter 设置") },
            actions = {
                val scope = rememberCoroutineScope()
                IconButton(onClick = {
                    mainActivity.exportConfigToClipboard(pinnedOrder, hiddenSet)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "导出配置")
                }

                var showImportConfirm by remember { mutableStateOf(false) }
                var pendingConfig by remember { mutableStateOf<JSONObject?>(null) }
                
                IconButton(onClick = {
                    val config = mainActivity.parseConfigFromClipboard()
                    if (config != null) {
                        pendingConfig = config
                        showImportConfirm = true
                    } else {
                        Toast.makeText(context, "剪贴板中没有有效的配置信息", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "导入配置")
                }

                if (showImportConfirm && pendingConfig != null) {
                    AlertDialog(
                        onDismissRequest = { showImportConfirm = false },
                        title = { Text("确认导入配置？") },
                        text = { Text("导入配置将完全替换您当前的置顶顺序和隐藏应用列表，此操作不可撤销。") },
                        confirmButton = {
                            TextButton(onClick = {
                                mainActivity.applyConfig(pendingConfig!!) { newOrder, newHidden ->
                                    onConfigImported(newOrder, newHidden)
                                }
                                showImportConfirm = false
                            }) {
                                Text("确认导入", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportConfirm = false }) {
                                Text("取消")
                            }
                        }
                    )
                }

                IconButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        mainActivity.clearPinyinCache()
                        val realApps = mainActivity.fetchSystemTargets()
                        mainActivity.saveTargetsToCache(realApps)
                        
                        // 从 SharedPreferences 重新加载最新的配置（处理多实例同步问题）
                        val latestPinned = mainActivity.getPinnedComponents()
                        val latestHidden = mainActivity.getHiddenComponents()
                        val latestOrder = mainActivity.getPinnedOrder()

                        withContext(Dispatchers.Main) {
                            rawApps = realApps
                            // 通过导入回调更新父作用域的状态变量
                            onConfigImported(latestOrder, latestHidden)
                            Toast.makeText(context, "列表与配置已同步刷新", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新列表")
                }
            },
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
