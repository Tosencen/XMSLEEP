package org.streambox.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.heightIn
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.dynamicDarkColorScheme
import kotlinx.serialization.json.*
import androidx.compose.material3.dynamicLightColorScheme
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import org.streambox.datasource.xmbox.XmboxVideoSource
import org.streambox.datasource.xmbox.config.XmboxConfig
import org.streambox.datasource.xmbox.model.VideoItem
import androidx.navigation.compose.composable
import org.streambox.app.update.UpdateDialog
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XMSLEEPApp()
        }
    }
}

enum class DarkModeOption {
    LIGHT, DARK, AUTO
}

// Shape 对象必须在顶层定义（在所有函数之前）
object TopLeftDiagonalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            },
        )
    }
}

object BottomRightDiagonalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            Path().apply {
                moveTo(size.width, size.height)
                lineTo(0f, size.height)
                lineTo(size.width, 0f)
                close()
            },
        )
    }
}

// ========== 源管理数据模型 ==========
enum class SourceType {
    VOD,  // 点播
    LIVE  // 直播
}

data class VideoSource(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val type: SourceType = SourceType.VOD,
    val searchable: Boolean = true,
    val changeable: Boolean = true,
    val quickSearch: Boolean = true,
    val timeout: Int = 15,
    val jsCode: String = "" // 保存 JavaScript 代码（如果有）
)

// 全局视频源列表状态管理
object VideoSourceManager {
    val sources = mutableStateListOf<VideoSource>()
    
    fun addSource(source: VideoSource) {
        // 检查是否已存在（根据 ID）
        if (!sources.any { it.id == source.id }) {
            sources.add(source)
        }
    }
    
    fun removeSource(source: VideoSource) {
        sources.remove(source)
    }
    
    fun updateSource(updatedSource: VideoSource) {
        val index = sources.indexOfFirst { it.id == updatedSource.id }
        if (index >= 0) {
            sources[index] = updatedSource
        }
    }
    
    fun getSourceById(id: String): VideoSource? {
        return sources.firstOrNull { it.id == id }
    }
}

@Composable
fun XMSLEEPApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 请求存储权限
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 权限请求结果处理
        android.util.Log.d("MainActivity", "存储权限请求结果: $permissions")
    }
    
    LaunchedEffect(Unit) {
        // 检查并请求存储权限
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // Android 10及以下需要存储权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    // 调色板颜色列表（提取为共享常量）
    val paletteColors = remember {
        listOf(
            Color(Hct.from(140.0, 40.0, 40.0).toInt()),  // base=4: 绿色偏青（第一个）
            Color(Hct.from(175.0, 40.0, 40.0).toInt()),  // base=5: 青绿
            Color(Hct.from(210.0, 40.0, 40.0).toInt()),  // base=6: 青蓝
            Color(Hct.from(245.0, 40.0, 40.0).toInt()),  // base=7: 蓝紫
            Color(Hct.from(280.0, 40.0, 40.0).toInt()),  // base=8: 紫红
            Color(0xFF4F378B),  // 默认紫色
            Color(Hct.from(315.0, 40.0, 40.0).toInt()),  // base=9: 粉红
            Color(Hct.from(350.0, 40.0, 40.0).toInt()),  // base=10: 红
            Color(Hct.from(35.0, 40.0, 40.0).toInt()),   // base=1: 橙黄
            Color(Hct.from(70.0, 40.0, 40.0).toInt()),   // base=2: 黄
            Color(Hct.from(105.0, 40.0, 40.0).toInt()),  // base=3: 黄绿
        )
    }
    
    // 主题状态管理
    var darkMode by remember { mutableStateOf<DarkModeOption>(DarkModeOption.AUTO) }
    var selectedColor by remember { mutableStateOf(paletteColors.first()) }  // 默认使用调色板第一个颜色
    var useDynamicColor by remember { mutableStateOf(false) }
    var useBlackBackground by remember { mutableStateOf(false) }
    var hideAnimation by remember { mutableStateOf(false) }  // 隐藏动画文件
    var soundCardsColumnsCount by remember { mutableIntStateOf(2) }  // 声音卡片列数（2或3），不受hideAnimation影响
    
    // 计算是否使用深色主题
    val isDark = when (darkMode) {
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
        DarkModeOption.AUTO -> isSystemInDarkTheme()
    }
    
    // 创建主题
    XMSLEEPTheme(
        isDark = isDark,
        seedColor = selectedColor,
        useDynamicColor = useDynamicColor,
        useBlackBackground = useBlackBackground
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(
                darkMode = darkMode,
                selectedColor = selectedColor,
                useDynamicColor = useDynamicColor,
                useBlackBackground = useBlackBackground,
                hideAnimation = hideAnimation,
                soundCardsColumnsCount = soundCardsColumnsCount,
                onDarkModeChange = { darkMode = it },
                onColorChange = { selectedColor = it },
                onDynamicColorChange = { useDynamicColor = it },
                onBlackBackgroundChange = { useBlackBackground = it },
                onHideAnimationChange = { hideAnimation = it },
                onSoundCardsColumnsCountChange = { soundCardsColumnsCount = it }
            )
        }
    }
}

@Composable
fun XMSLEEPTheme(
    isDark: Boolean,
    seedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // 设置系统状态栏样式
    val activity = context as? ComponentActivity
    DisposableEffect(activity, isDark) {
        if (activity != null) {
            if (isDark) {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                )
            } else {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ),
                    navigationBarStyle = SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ),
                )
            }
        }
        onDispose { }
    }
    
    // 使用 MaterialKolor 生成完整的配色方案
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ 使用系统动态颜色（使用原生 Material3 动态颜色 API）
        if (isDark) {
            val baseScheme = dynamicDarkColorScheme(context)
            if (useBlackBackground) {
                baseScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF1C1C1C)
                )
            } else {
                baseScheme
            }
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        // 使用自定义主题色
        dynamicColorScheme(
            primary = seedColor,
            isDark = isDark,
            isAmoled = useBlackBackground,  // 纯黑背景
            style = PaletteStyle.TonalSpot,  // Material 3 推荐的风格
            modifyColorScheme = { scheme ->
                if (useBlackBackground && isDark) {
                    // 高对比度：纯黑背景
                    scheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceVariant = Color(0xFF1C1C1C),
                        surfaceContainer = Color(0xFF121212),
                        surfaceContainerLow = Color(0xFF0A0A0A),
                        surfaceContainerHigh = Color(0xFF1F1F1F)
                    )
                } else {
                    scheme
                }
            }
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    darkMode: DarkModeOption,
    selectedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    hideAnimation: Boolean,
    soundCardsColumnsCount: Int,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onHideAnimationChange: (Boolean) -> Unit,
    onSoundCardsColumnsCountChange: (Int) -> Unit
) {
    val mainNavController = rememberNavController()
    var selectedItem by remember { mutableIntStateOf(1) } // 默认显示白噪音页面
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 自动更新检查（全局共享）
    val updateViewModel = remember { org.streambox.app.update.UpdateViewModel(context) }
    val updateState by updateViewModel.updateState.collectAsState()
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    
    // 获取当前版本号
    val currentVersion = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val version = packageInfo?.versionName ?: "0.0.0"
            android.util.Log.d("UpdateCheck", "读取到的版本号: $version")
            version
        } catch (e: Exception) {
            android.util.Log.e("UpdateCheck", "读取版本号失败，使用默认值", e)
            "0.0.0" // 使用最低版本号作为默认值，确保能检测到更新
        }
    }
    
    // 每次进入主页时自动检查更新（使用LaunchedEffect确保只在组合时执行一次）
    LaunchedEffect(Unit) {
        android.util.Log.d("UpdateCheck", "开始自动检查更新，当前版本: $currentVersion")
        
        // 先检查初始状态，如果已经是HasUpdate状态，立即显示弹窗（处理从后台恢复的情况）
        val initialState = updateViewModel.updateState.value
        if (initialState is org.streambox.app.update.UpdateState.HasUpdate) {
            android.util.Log.d("UpdateCheck", "初始状态已是HasUpdate，显示弹窗")
            kotlinx.coroutines.delay(500)
            showAutoUpdateDialog = true
        } else {
            // 否则开始检查更新
            updateViewModel.startAutomaticCheckLatestVersion(currentVersion)
        }
    }
    
    // 当检测到有新版本时，自动显示更新弹窗
    // 使用LaunchedEffect监听updateState变化，并在状态为HasUpdate时显示弹窗
    LaunchedEffect(updateState) {
        val state = updateState
        when (state) {
            is org.streambox.app.update.UpdateState.HasUpdate -> {
                android.util.Log.d("UpdateCheck", "检测到新版本: ${state.version.version}")
                // 延迟一小段时间确保UI已准备好
                kotlinx.coroutines.delay(300)
                showAutoUpdateDialog = true
            }
            is org.streambox.app.update.UpdateState.CheckFailed -> {
                android.util.Log.e("UpdateCheck", "检查更新失败: ${state.error}")
                // 如果是rate limit错误，也显示弹窗提示用户
                if (state.error.contains("rate limit", ignoreCase = true) || 
                    state.error.contains("请求次数", ignoreCase = true)) {
                    android.util.Log.d("UpdateCheck", "Rate limit错误，显示弹窗提示用户")
                    kotlinx.coroutines.delay(300)
                    showAutoUpdateDialog = true
                }
            }
            is org.streambox.app.update.UpdateState.UpToDate -> {
                android.util.Log.d("UpdateCheck", "当前已是最新版本")
            }
            is org.streambox.app.update.UpdateState.Checking -> {
                android.util.Log.d("UpdateCheck", "正在检查更新...")
            }
            else -> {}
        }
    }
    
    // 置顶和收藏状态管理（提升到MainScreen级别，确保切换tab时状态不丢失）
    val pinnedSounds = remember { mutableStateOf(mutableSetOf<org.streambox.app.audio.AudioManager.Sound>()) }
    val favoriteSounds = remember { mutableStateOf(mutableSetOf<org.streambox.app.audio.AudioManager.Sound>()) }
    
    // 监听当前路由，判断是否在二级页面
    val currentBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isInSecondaryPage = currentRoute in listOf("theme", "sourceManagement")
    val isMainRoute = !isInSecondaryPage  // 主页面 = 不在二级页面
    
    // 视频源页面的搜索状态
    var showSourceSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 切换页面时自动关闭搜索框
    LaunchedEffect(selectedItem) {
        showSourceSearch = false
        searchQuery = ""
    }
    
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showSourceSelectorDialog by remember { mutableStateOf(false) }
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    // 当前选中的子源（sub-source），用于多源配置中的 sites 选择
    var selectedSubSourceKey by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            // 只在视频源页面显示 TopBar
            if (false) { // 首页tab已删除
                TopAppBar(
                    title = { 
                        if (false) { // 首页tab已删除
                            // 视频源页面显示搜索框
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("搜索视频源") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showSourceSearch = false }) {
                                        Icon(Icons.Default.Close, "关闭")
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        } else {
                            // 视频源标题 + 圆形图标按钮
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 圆形图标按钮（使用外部状态）
                                IconButton(
                                    onClick = { showSourceSelectorDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.VideoLibrary,
                                            contentDescription = "选择视频源",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Text("视频源")
                            }
                        }
                    },
                    actions = {
                        // 只在视频源页面显示操作按钮
                        if (false) { // 首页tab已删除
                            IconButton(onClick = { showSourceSearch = true }) {
                                Icon(Icons.Default.Search, "搜索")
                            }
                            IconButton(onClick = { /* TODO: 显示收藏 */ }) {
                                Icon(Icons.Default.Star, "收藏")
                            }
                            IconButton(onClick = { /* TODO: 显示历史 */ }) {
                                Icon(Icons.Default.History, "历史")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = {
            // 只在视频源页面显示添加按钮
            // 首页FAB已删除
        },
        bottomBar = {
            // 只在主页面显示底部导航栏
            if (isMainRoute) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.GraphicEq, null) },
                        label = { Text("白噪音") },
                        selected = selectedItem == 1,
                        onClick = { selectedItem = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("设置") },
                        selected = selectedItem == 2,
                        onClick = { selectedItem = 2 }
                    )
                }
            }
        }
    ) { paddingValues ->
        // NavHost 始终存在，用于处理二级页面导航
        NavHost(
            navController = mainNavController,
            startDestination = "main",  // 主页面路由
            modifier = Modifier.fillMaxSize()
        ) {
            // 主页面路由（显示 AnimatedContent）
            composable("main") {
                // 主页面：直接根据 selectedItem 切换内容（支持左右滑动切换）
                AnimatedContent(
                    targetState = selectedItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var accumulatedDrag = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    // 手势结束时判断是否切换页面
                                    val threshold = size.width * 0.25f
                                    when {
                                        // 向右滑动（从设置页面回到白噪音页面）
                                        accumulatedDrag > threshold && selectedItem == 2 -> {
                                            selectedItem = 1
                                        }
                                        // 向左滑动（从白噪音页面到设置页面）
                                        accumulatedDrag < -threshold && selectedItem == 1 -> {
                                            selectedItem = 2
                                        }
                                    }
                                    accumulatedDrag = 0f
                                }
                            ) { change, dragAmount ->
                                accumulatedDrag += dragAmount
                            }
                        },
                    label = "tab_switch"
                ) { currentTab ->
                    when (currentTab) {
                        1 -> {
                            // 白噪音页面
                            org.streambox.app.ui.SoundsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                hideAnimation = hideAnimation,
                                darkMode = darkMode,
                                onDarkModeChange = onDarkModeChange,
                                columnsCount = soundCardsColumnsCount,
                                onColumnsCountChange = onSoundCardsColumnsCountChange,
                                pinnedSounds = pinnedSounds,
                                favoriteSounds = favoriteSounds
                            )
                        }
                        2 -> {
                            SettingsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                hideAnimation = hideAnimation,
                                onHideAnimationChange = onHideAnimationChange,
                                updateViewModel = updateViewModel,
                                onNavigateToTheme = { 
                                    mainNavController.navigate("theme") 
                                },
                                onNavigateToSourceManagement = {
                                    mainNavController.navigate("sourceManagement")
                                },
                                onNavigateToSounds = {
                                    // 不再需要导航到声音页面，因为已经是独立tab
                                }
                            )
                        }
                        else -> { /* 不应该到达这里 */ }
                    }
                }
                
                // 视频源页面的添加源对话框
                // 首页相关对话框已删除
                if (false) {
                    AddSourceDialog(
                        onDismiss = { showAddSourceDialog = false },
                        onConfirm = { newSource: VideoSource ->
                            VideoSourceManager.addSource(newSource)
                            showAddSourceDialog = false
                            
                            // 自动选中新添加的源
                            selectedSourceId = newSource.id
                            selectedSubSourceKey = null // 清空子源选择，让SourcesScreen自动选择第一个
                            android.util.Log.d("MainScreen", "自动选中新添加的源: id=${newSource.id}, name=${newSource.name}")
                            
                            // 如果是多源配置，记录日志
                            if (newSource.jsCode.isNotBlank()) {
                                try {
                                    val json = Json {
                                        ignoreUnknownKeys = true
                                        coerceInputValues = true
                                    }
                                    val jsonElement: JsonElement = json.parseToJsonElement(newSource.jsCode)
                                    if (jsonElement is JsonObject) {
                                        val sitesArray = jsonElement["sites"]?.jsonArray
                                        if (sitesArray != null && sitesArray.isNotEmpty()) {
                                            sitesArray.firstOrNull()?.let { firstSite ->
                                                if (firstSite is JsonObject) {
                                                    val firstKey = firstSite["key"]?.jsonPrimitive?.content
                                                        ?: firstSite["name"]?.jsonPrimitive?.content
                                                    if (firstKey != null) {
                                                        selectedSubSourceKey = firstKey
                                                        android.util.Log.d("MainScreen", "自动选择第一个子源: key=$firstKey")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainScreen", "解析子源失败", e)
                                }
                            }
                        }
                    )
                }
                
                // 分源选择对话框
                // 首页相关对话框已删除
                if (false) {
                    val currentSource = selectedSourceId?.let { id ->
                        VideoSourceManager.sources.firstOrNull { it.id == id }
                    } ?: VideoSourceManager.sources.firstOrNull()
                    
                    android.util.Log.d("MainScreen", "打开 SourceSelectorDialog - selectedSourceId=$selectedSourceId, currentSource=${currentSource?.name}, sources.size=${VideoSourceManager.sources.size}")
                    if (currentSource != null) {
                        android.util.Log.d("MainScreen", "currentSource - id=${currentSource.id}, name=${currentSource.name}, url=${currentSource.url}, jsCode.length=${currentSource.jsCode.length}")
                    }
                    
                    SourceSelectorDialog(
                        currentSource = currentSource,
                        allSources = VideoSourceManager.sources.toList(),
                        currentSourceId = selectedSourceId,
                        currentSubSourceKey = selectedSubSourceKey,
                        onDismiss = { showSourceSelectorDialog = false },
                        onSelectSource = { source ->
                            selectedSourceId = source.id
                            selectedSubSourceKey = null // 切换到新源时，清空子源选择
                            showSourceSelectorDialog = false
                        },
                        onSelectSubSource = { subSourceKey ->
                            selectedSubSourceKey = subSourceKey
                            showSourceSelectorDialog = false
                        }
                    )
                }
            }
            
            // 二级页面路由
            composable("theme") {
                ThemeSettingsScreen(
                    darkMode = darkMode,
                    selectedColor = selectedColor,
                    useDynamicColor = useDynamicColor,
                    useBlackBackground = useBlackBackground,
                    onDarkModeChange = onDarkModeChange,
                    onColorChange = onColorChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onBlackBackgroundChange = onBlackBackgroundChange,
                    onBack = { mainNavController.popBackStack() }
                )
            }
            composable("sourceManagement") {
                SourceManagementScreen(
                    onBack = { mainNavController.popBackStack() }
                )
            }
        }
    }
    
    // 自动更新弹窗（检测到新版本时自动显示）
    // 注意：只在HasUpdate状态时显示自动弹窗，避免在Idle状态时重复检查
    val currentUpdateState by updateViewModel.updateState.collectAsState()
    
    // 当状态为HasUpdate时，显示自动更新弹窗
    if (showAutoUpdateDialog && currentUpdateState is org.streambox.app.update.UpdateState.HasUpdate) {
        UpdateDialog(
            onDismiss = { 
                showAutoUpdateDialog = false
                android.util.Log.d("UpdateCheck", "用户关闭了自动更新弹窗")
            },
            updateViewModel = updateViewModel,
            context = context
        )
    }
    
    // 当检查失败时，如果错误信息包含rate limit，也显示弹窗提示用户
    if (showAutoUpdateDialog && currentUpdateState is org.streambox.app.update.UpdateState.CheckFailed) {
        val errorState = currentUpdateState as org.streambox.app.update.UpdateState.CheckFailed
        if (errorState.error.contains("rate limit", ignoreCase = true) || 
            errorState.error.contains("请求次数", ignoreCase = true)) {
            UpdateDialog(
                onDismiss = { 
                    showAutoUpdateDialog = false
                    android.util.Log.d("UpdateCheck", "用户关闭了更新检查失败弹窗")
                },
                updateViewModel = updateViewModel,
                context = context
            )
        } else {
            // 其他错误不显示弹窗，静默失败
            showAutoUpdateDialog = false
        }
    }
}

@Composable
fun SourcesScreen(
    modifier: Modifier = Modifier,
    onShowAddDialog: () -> Unit = {},
    currentSourceId: String? = null,
    currentSubSourceKey: String? = null,
    onSubSourceKeyChange: ((String?) -> Unit)? = null
) {
    // 空状态页面
    Box(
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun VideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 封面图片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f) // 4:3 比例
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (video.pic.isNotBlank()) {
                    // 使用ImageUrlUtil处理图片URL（支持@Headers、@Cookie等自定义格式）
                    val (imageUrl, _) = org.streambox.datasource.xmbox.utils.ImageUrlUtil.getImageUrlWithHeaders(video.pic)
                    AsyncImage(
                        model = imageUrl.ifBlank { video.pic },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 视频信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.name.ifBlank { "未知" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (video.note.isNotBlank()) {
                    Text(
                        text = video.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    hideAnimation: Boolean = false,
    onHideAnimationChange: (Boolean) -> Unit = {},
    updateViewModel: org.streambox.app.update.UpdateViewModel,
    onNavigateToTheme: () -> Unit,
    onNavigateToSourceManagement: () -> Unit,
    onNavigateToSounds: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { org.streambox.app.audio.AudioManager.getInstance() }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(0L) }
    var isCalculatingCache by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val updateState by updateViewModel.updateState.collectAsState()
    
    // 定期更新缓存大小
    LaunchedEffect(Unit) {
        while (true) {
            isCalculatingCache = true
            cacheSize = calculateCacheSize(context)
            isCalculatingCache = false
            
            // 检查缓存是否超过200M (200 * 1024 * 1024 字节)
            val thresholdBytes = 200L * 1024 * 1024
            if (cacheSize > thresholdBytes && !isClearingCache) {
                // 自动清理缓存
                isClearingCache = true
                scope.launch {
                    try {
                        clearApplicationCache(context)
                        cacheSize = 0L
                        Toast.makeText(context, "缓存已超过200M，自动清理完成", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "自动清理失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isClearingCache = false
                        // 清理完成后重新计算缓存大小
                        cacheSize = calculateCacheSize(context)
                    }
                }
            }
            
            // 每5秒更新一次缓存大小
            kotlinx.coroutines.delay(5000)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 外观设置
        Text("外观", style = MaterialTheme.typography.titleLarge)
            Card(
            onClick = onNavigateToTheme,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("主题与色彩", style = MaterialTheme.typography.titleMedium)
                    Text(
                            "外观模式、主题色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 系统设置
        Text("系统", style = MaterialTheme.typography.titleLarge)
        
        // 隐藏动画文件
        SwitchItem(
            checked = hideAnimation,
            onCheckedChange = onHideAnimationChange,
            title = "隐藏动画文件",
            description = "隐藏声音卡片中的Lottie动画"
        )
        
        // 一键调整音量
            Card(
            onClick = { showVolumeDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("一键调整音量", style = MaterialTheme.typography.titleMedium)
                    Text(
                            "统一调整所有声音音量",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
        
            Card(
            onClick = { showClearCacheDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("缓存清理", style = MaterialTheme.typography.titleMedium)
                    Text(
                            "清理应用缓存数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCalculatingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            formatBytes(cacheSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 其他
        Text("其他", style = MaterialTheme.typography.titleLarge)
        
        // 软件更新
        Card(
            onClick = { showUpdateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("软件更新", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "检查并更新到最新版本",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
        
        // 关于 XMSLEEP
        Card(
            onClick = { showAboutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("关于 XMSLEEP", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "查看应用信息、版本和版权",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
        
        // 软件更新对话框
        if (showUpdateDialog) {
            UpdateDialog(
                onDismiss = { showUpdateDialog = false },
                updateViewModel = updateViewModel,
                context = context
            )
        }
        
        // 关于对话框
        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false },
                context = context
            )
    }
    
    // 缓存清理对话框
    if (showClearCacheDialog) {
        ClearCacheDialog(
            onDismiss = { 
                if (!isClearingCache) {
                    showClearCacheDialog = false
                }
            },
            onConfirm = {
                // 立即关闭对话框，避免重复显示
                showClearCacheDialog = false
                isClearingCache = true
                scope.launch {
                    try {
                        clearApplicationCache(context)
                        cacheSize = 0L  // 清理后重置缓存大小
                        Toast.makeText(context, "缓存清理成功", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "缓存清理失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isClearingCache = false
                        // 清理完成后重新计算缓存大小
                        cacheSize = calculateCacheSize(context)
                    }
                }
            },
            isClearing = isClearingCache
        )
    }
        
        // 一键调整音量对话框
        if (showVolumeDialog) {
            var volume by remember { 
                mutableStateOf(
                // 获取第一个正在播放的声音的音量作为默认值，如果没有则使用0.5
                audioManager.getPlayingSounds().firstOrNull()?.let { 
                    audioManager.getVolume(it) 
                } ?: 0.5f
            )
        }
        
        AlertDialog(
            onDismissRequest = { showVolumeDialog = false },
            title = { Text("一键调整音量") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "将应用到所有正在播放的声音",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 音量滑块
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = volume,
                            onValueChange = { 
                                volume = it
                                // 实时应用到所有声音
                                audioManager.getPlayingSounds().forEach { sound ->
                                    audioManager.setVolume(sound, volume)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            valueRange = 0f..1f,
                            steps = 19  // 0到100，步长5%
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(volume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    // 应用到所有声音（包括未播放的，以便下次播放时使用）
                    listOf(
                        org.streambox.app.audio.AudioManager.Sound.RAIN,
                        org.streambox.app.audio.AudioManager.Sound.CAMPFIRE,
                        org.streambox.app.audio.AudioManager.Sound.THUNDER,
                        org.streambox.app.audio.AudioManager.Sound.CAT_PURRING,
                        org.streambox.app.audio.AudioManager.Sound.BIRD_CHIRPING,
                        org.streambox.app.audio.AudioManager.Sound.NIGHT_INSECTS
                    ).forEach { sound ->
                        audioManager.setVolume(sound, volume)
                    }
                    showVolumeDialog = false
                    Toast.makeText(context, "音量已设置为${(volume * 100).toInt()}%", Toast.LENGTH_SHORT).show()
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVolumeDialog = false }) {
                    Text("取消")
                }
            }
        )
        }
        }
    }
}

// ========== 主题设置详情页 ==========
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    darkMode: DarkModeOption,
    selectedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    // 固定 TopAppBar，不随滚动隐藏
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "主题与色彩",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    // 返回导航按钮
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-4).dp)
                    ) {
                        Box(Modifier.size(24.dp)) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                // TopAppBar 使用系统栏和显示区域切口
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 消费 TopAppBar 使用的 insets
                .consumeWindowInsets(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top)
                )
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 外观模式选择面板
            DarkModeSelectPanel(
                currentMode = darkMode,
                onModeSelected = onDarkModeChange
            )
            
            // 动态颜色开关（Android 12+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchItem(
                    checked = useDynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    title = "动态颜色",
                    description = "使用壁纸颜色作为主题色"
                )
            }
            
            // 高对比度开关
            SwitchItem(
                checked = useBlackBackground,
                onCheckedChange = onBlackBackgroundChange,
                title = "高对比度",
                description = "在深色模式下使用纯黑背景"
            )
            
            // 调色板选择
            Box(
                modifier = Modifier.alpha(if (useDynamicColor) 0.5f else 1f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("调色板", style = MaterialTheme.typography.titleMedium)
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 使用 HCT 色彩空间生成均匀分布的颜色
                        // 所有颜色统一使用 Chroma=40.0, Tone=40.0，确保明度一致
                        val colors = listOf(
                            Color(Hct.from(140.0, 40.0, 40.0).toInt()),  // base=4: 绿色偏青
                            Color(Hct.from(175.0, 40.0, 40.0).toInt()),  // base=5: 青绿
                            Color(Hct.from(210.0, 40.0, 40.0).toInt()),  // base=6: 青蓝
                            Color(Hct.from(245.0, 40.0, 40.0).toInt()),  // base=7: 蓝紫
                            Color(Hct.from(280.0, 40.0, 40.0).toInt()),  // base=8: 紫红
                            Color(0xFF4F378B),  // 默认紫色
                            Color(Hct.from(315.0, 40.0, 40.0).toInt()),  // base=9: 粉红
                            Color(Hct.from(350.0, 40.0, 40.0).toInt()),  // base=10: 红
                            Color(Hct.from(35.0, 40.0, 40.0).toInt()),   // base=1: 橙黄
                            Color(Hct.from(70.0, 40.0, 40.0).toInt()),   // base=2: 黄
                            Color(Hct.from(105.0, 40.0, 40.0).toInt()),  // base=3: 黄绿
                        )
                        
                        colors.forEach { color ->
                            ColorButton(
                                baseColor = color,
                                selected = selectedColor == color && !useDynamicColor,
                                onClick = {
                                    onColorChange(color)
                                    onDynamicColorChange(false)
                                }
                            )
                        }
                    }
                }
            }
            
            // 底部留出安全空间
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ========== 深色模式选择面板 ==========
@Composable
fun DarkModeSelectPanel(
    currentMode: DarkModeOption,
    onModeSelected: (DarkModeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val panelModifier = Modifier.size(96.dp, 146.dp)
    
    val themePanelItem: @Composable (DarkModeOption) -> Unit = { mode ->
        ColorSchemePreviewItem(
            onClick = { onModeSelected(mode) },
            panel = {
                if (mode != DarkModeOption.AUTO) {
                    ThemePreviewPanel(
                        isDark = mode == DarkModeOption.DARK,
                        modifier = panelModifier,
                    )
                } else {
                    DiagonalMixedThemePreviewPanel(
                        modifier = panelModifier,
                    )
                }
            },
            text = {
                Text(
                    when (mode) {
                        DarkModeOption.LIGHT -> "浅色"
                        DarkModeOption.DARK -> "深色"
                        DarkModeOption.AUTO -> "自动"
                    }
                )
            },
            selected = currentMode == mode,
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        themePanelItem(DarkModeOption.LIGHT)
        themePanelItem(DarkModeOption.DARK)
        themePanelItem(DarkModeOption.AUTO)
    }
}

@Composable
fun ColorSchemePreviewItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    panel: @Composable () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        panel()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                RadioButton(
                    selected = selected,
                    interactionSource = interactionSource,
                    onClick = null,
                )
            }
            text()
        }
    }
}

// ========== 主题预览面板 ==========
@Composable
fun ThemePreviewPanel(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // 使用更柔和的紫色（降低饱和度和亮度）
    val previewColor = Color(Hct.from(280.0, 36.0, 50.0).toInt())
    val colorScheme = dynamicColorScheme(
        primary = previewColor,
        isDark = isDark,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
    )
    
    Box(modifier) {
        Row(
            modifier = Modifier.fillMaxSize()
                .background(
                    color = colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = colorScheme.background, shape = RoundedCornerShape(9.dp)),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(
                                    color = colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(colorScheme.tertiary),
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(
                                    color = colorScheme.secondary,
                                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                                ),
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(8.dp)
                                .background(color = colorScheme.primaryContainer, shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(8.dp)
                                .background(color = colorScheme.secondaryContainer, shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(8.dp)
                                .background(color = colorScheme.tertiaryContainer, shape = CircleShape),
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .height(height = 26.dp)
                        .background(
                            color = colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        )
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(color = colorScheme.primary, shape = CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .background(color = colorScheme.primaryContainer, shape = CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
fun DiagonalMixedThemePreviewPanel(
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        ThemePreviewPanel(
            isDark = false,
            modifier = Modifier
                .fillMaxSize()
                .clip(TopLeftDiagonalShape),
        )
        ThemePreviewPanel(
            isDark = true,
            modifier = Modifier
                .fillMaxSize()
                .clip(BottomRightDiagonalShape),
        )
    }
}

// ========== 开关项 ==========
@Composable
fun SwitchItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

// ========== 颜色按钮 ==========
@Composable
fun ColorButton(
    baseColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerSize by animateDpAsState(targetValue = if (selected) 28.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp)
    
    Surface(
        modifier = modifier
            .sizeIn(maxHeight = 80.dp, maxWidth = 80.dp, minHeight = 64.dp, minWidth = 64.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize()) {
            val hct = baseColor.toHct()
            val color1 = Color(Hct.from(hct.hue, 40.0, 80.0).toInt())
            val color2 = Color(Hct.from(hct.hue, 40.0, 90.0).toInt())
            val color3 = Color(Hct.from(hct.hue, 40.0, 60.0).toInt())
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .drawBehind { drawCircle(color1) }
                    .align(Alignment.Center),
            ) {
                Surface(
                    color = color2,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp),
                ) {}
                Surface(
                    color = color3,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp),
                ) {}
                if (selected) {
                    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .size(containerSize)
                            .drawBehind { drawCircle(primaryContainer) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

// ========== 旧组件（保留以免破坏其他地方）==========
@Composable
fun ThemeModeCard(
    option: DarkModeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(0.7f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (option) {
                        DarkModeOption.LIGHT -> Icons.Default.LightMode
                        DarkModeOption.DARK -> Icons.Default.DarkMode
                        DarkModeOption.AUTO -> Icons.Default.Brightness4
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Text(
                when (option) {
                    DarkModeOption.LIGHT -> "浅色"
                    DarkModeOption.DARK -> "深色"
                    DarkModeOption.AUTO -> "自动"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun EnhancedColorButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerSize by animateDpAsState(targetValue = if (selected) 24.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp)
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .drawBehind { drawCircle(color) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Surface(
                        modifier = Modifier.size(containerSize),
                        shape = CircleShape,
                        color = primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = onPrimaryContainer,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .drawBehind { drawCircle(color) },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "已选择",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceManagementScreen(
    onBack: () -> Unit
) {
    // 使用全局视频源管理器
    val sources = VideoSourceManager.sources
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<VideoSource?>(null) }
    
    // 标签页状态
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("添加源", "视频", "记录")
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "源管理",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.offset(x = (-4).dp)
                        ) {
                            Box(Modifier.size(24.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                // 标签页
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { 
                                if (index == 0) {
                                    // 点击"添加源"直接弹出对话框
                                    showAddDialog = true
                                } else {
                                    selectedTabIndex = index
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // 根据选中的标签页显示不同内容
        when (selectedTabIndex) {
            0 -> {
                // 添加源标签页（点击后会弹窗，这里显示说明）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .consumeWindowInsets(
                            WindowInsets.systemBars.union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.Top)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "添加新的视频源",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(onClick = { showAddDialog = true }) {
                            Text("点击添加")
                        }
                    }
                }
            }
            1 -> {
                // 视频源列表
                if (sources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .consumeWindowInsets(
                                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                                    .only(WindowInsetsSides.Top)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "暂无视频源",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "请先添加视频源",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(
                                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                                    .only(WindowInsetsSides.Top)
                            ),
                        contentPadding = paddingValues
                    ) {
                        items(sources.toList()) { source ->
                            SourceItem(
                                source = source,
                                onEdit = { editingSource = source },
                                onDelete = { VideoSourceManager.removeSource(source) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // 添加历史记录（显示所有已添加的视频源）
                if (sources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .consumeWindowInsets(
                                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                                    .only(WindowInsetsSides.Top)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "暂无添加记录",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(
                                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                                    .only(WindowInsetsSides.Top)
                            ),
                        contentPadding = paddingValues
                    ) {
                        items(sources.toList()) { source ->
                            HistoryItem(
                                source = source,
                                onRestore = { 
                                    // 如果源不在列表中，则添加（实际上已经在列表中）
                                    selectedTabIndex = 1
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 添加源对话框
    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { 
                showAddDialog = false
                // 切换回视频标签页
                if (selectedTabIndex == 0) {
                    selectedTabIndex = 1
                }
            },
            onConfirm = { newSource: VideoSource ->
                VideoSourceManager.addSource(newSource)
                showAddDialog = false
                selectedTabIndex = 1  // 切换到视频列表
            }
        )
    }
    
    // 编辑源对话框
    editingSource?.let { source ->
        EditSourceDialog(
            source = source,
            onDismiss = { editingSource = null },
            onConfirm = { updatedSource: VideoSource ->
                // 更新全局列表中的源
                VideoSourceManager.updateSource(updatedSource)
                editingSource = null
            }
        )
    }
}

@Composable
fun HistoryItem(
    source: VideoSource,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = source.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(
                        onClick = { },
                        label = { Text(if (source.type == SourceType.VOD) "点播" else "直播") }
                    )
                }
                Button(onClick = onRestore) {
                    Text("恢复")
                }
            }
        }
    }
}

@Composable
fun SourceItem(
    source: VideoSource,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = source.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(if (source.type == SourceType.VOD) "点播" else "直播") }
                        )
                        if (source.searchable) {
                            AssistChip(
                                onClick = { },
                                label = { Text("可搜索") }
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, "编辑")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (VideoSource) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val videoSource = remember { XmboxVideoSource.create(context) }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }
    
    var inputText by remember { mutableStateOf("") }
    
    // 粘贴功能
    val onPasteClick: () -> Unit = {
        clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()?.let { clipText ->
            inputText = clipText
        }
    }
    
    // 识别状态
    var isIdentifying by remember { mutableStateOf(false) }
    var identifyError by remember { mutableStateOf<String?>(null) }
    
    // 确定按钮点击处理 - 识别并保存
    val onConfirmClick: () -> Unit = {
        if (inputText.isNotBlank()) {
            scope.launch {
                isIdentifying = true
                identifyError = null
                
                try {
                    android.util.Log.d("AddSourceDialog", "开始识别视频源，输入: ${inputText.take(100)}")
                    
                    // 识别配置
                    val config = videoSource.identify(inputText, "")
                    
                    android.util.Log.d("AddSourceDialog", "识别结果: ${config?.let { it::class.simpleName } ?: "null"}")
                    if (config is XmboxConfig.JavaScriptConfig) {
                        android.util.Log.d("AddSourceDialog", "JavaScriptConfig - url=${config.url}, name=${config.name}, jsCode.length=${config.jsCode.length}")
                        android.util.Log.d("AddSourceDialog", "jsCode 前200字符: ${config.jsCode.take(200)}")
                    }
                    
                    if (config == null) {
                        // 识别失败，显示错误提示
                        identifyError = "无法识别视频源格式，请检查输入内容是否正确"
                        Toast.makeText(context, "无法识别视频源格式，请检查输入内容是否正确", Toast.LENGTH_LONG).show()
                        isIdentifying = false
                        return@launch
                    }
                    
                    val source = when (config) {
                        is XmboxConfig.VodConfig -> {
                            android.util.Log.d("AddSourceDialog", "创建 VodConfig")
                            VideoSource(
                                name = config.name.ifBlank { "点播源" },
                                url = config.url,
                                type = SourceType.VOD,
                                searchable = config.searchable,
                                changeable = config.changeable,
                                quickSearch = config.quickSearch,
                                timeout = config.timeout,
                                jsCode = "" // VodConfig 没有 jsCode
                            )
                        }
                        is XmboxConfig.LiveConfig -> {
                            android.util.Log.d("AddSourceDialog", "创建 LiveConfig")
                            VideoSource(
                                name = config.name.ifBlank { "直播源" },
                                url = config.url,
                                type = SourceType.LIVE,
                                timeout = config.timeout,
                                jsCode = "" // LiveConfig 没有 jsCode
                            )
                        }
                        is XmboxConfig.JavaScriptConfig -> {
                            android.util.Log.d("AddSourceDialog", "创建 JavaScriptConfig，保存 jsCode.length=${config.jsCode.length}")
                            VideoSource(
                                name = config.name.ifBlank { "JavaScript源" },
                                url = config.url,
                                type = if (config.type == org.streambox.datasource.xmbox.config.SourceType.VOD) {
                                    SourceType.VOD
                                } else {
                                    SourceType.LIVE
                                },
                                searchable = true, // JavaScript源默认可搜索
                                changeable = true, // JavaScript源默认可换源
                                quickSearch = true, // JavaScript源默认快速搜索
                                timeout = config.timeout,
                                jsCode = config.jsCode // 保存 JavaScript 代码（整个 JSON 配置）
                            )
                        }
                    }
                    
                    android.util.Log.d("AddSourceDialog", "创建 VideoSource 成功，id=${source.id}, name=${source.name}, jsCode.length=${source.jsCode.length}")
                    
                    // 识别成功，保存并关闭对话框
                    onConfirm(source)
                    android.util.Log.d("AddSourceDialog", "已调用 onConfirm，VideoSourceManager.sources.size=${VideoSourceManager.sources.size}")
                    Toast.makeText(context, "视频源添加成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // 识别过程中发生异常
                    identifyError = "识别失败: ${e.localizedMessage ?: e.message}"
                    Toast.makeText(context, "识别失败: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isIdentifying = false
                }
            }
        } else {
            Toast.makeText(context, "请输入视频源内容", Toast.LENGTH_SHORT).show()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("视频") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 错误提示
                identifyError?.let { error ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // 输入框（使用与编辑视频源弹窗相同的样式）
                val density = LocalDensity.current
                var inputFieldHeight by remember { mutableStateOf(0.dp) }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("源地址") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            inputFieldHeight = with(density) {
                                coordinates.size.height.toDp()
                            }
                        },
                    singleLine = false,
                    maxLines = 5,
                    trailingIcon = {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                
                // 点我粘贴按钮（高度与输入框同步）
                OutlinedButton(
                    onClick = onPasteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (inputFieldHeight > 0.dp) inputFieldHeight else 56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("点我粘贴")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                enabled = inputText.isNotBlank() && !isIdentifying
            ) {
                if (isIdentifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSourceDialog(
    source: VideoSource,
    onDismiss: () -> Unit,
    onConfirm: (VideoSource) -> Unit
) {
    var name by remember { mutableStateOf(source.name) }
    var url by remember { mutableStateOf(source.url) }
    var type by remember { mutableStateOf(source.type) }
    var searchable by remember { mutableStateOf(source.searchable) }
    var changeable by remember { mutableStateOf(source.changeable) }
    var quickSearch by remember { mutableStateOf(source.quickSearch) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑视频源") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("源名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("源地址") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("类型", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = type == SourceType.VOD,
                            onClick = { type = SourceType.VOD }
                        )
                    ) {
                        RadioButton(
                            selected = type == SourceType.VOD,
                            onClick = { type = SourceType.VOD }
                        )
                        Text("点播")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = type == SourceType.LIVE,
                            onClick = { type = SourceType.LIVE }
                        )
                    ) {
                        RadioButton(
                            selected = type == SourceType.LIVE,
                            onClick = { type = SourceType.LIVE }
                        )
                        Text("直播")
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("支持搜索")
                    Switch(
                        checked = searchable,
                        onCheckedChange = { searchable = it }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("支持换源")
                    Switch(
                        checked = changeable,
                        onCheckedChange = { changeable = it }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("快速搜索")
                    Switch(
                        checked = quickSearch,
                        onCheckedChange = { quickSearch = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onConfirm(
                            source.copy(
                                name = name,
                                url = url,
                                type = type,
                                searchable = searchable,
                                changeable = changeable,
                                quickSearch = quickSearch
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


/**
 * 子源数据（从多源配置的 sites 数组中解析）
 */
data class SubSource(
    val key: String,
    val name: String,
    val type: Int, // 0=点播, 2=直播, 3=其他
    val api: String? = null,
    val jar: String? = null // Spider JAR URL
)

/**
 * 分源选择对话框 - 优先显示当前视频源的子源，如果没有子源则显示所有视频源
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectorDialog(
    currentSource: VideoSource?,
    allSources: List<VideoSource>,
    currentSourceId: String?,
    currentSubSourceKey: String?,
    onDismiss: () -> Unit,
    onSelectSource: (VideoSource) -> Unit,
    onSelectSubSource: (String) -> Unit
) {
    // 解析当前源中的子源（如果是多源配置）
    val subSources: List<SubSource> = remember(currentSource?.id, currentSource?.jsCode) {
        val result = (currentSource?.let { source ->
            android.util.Log.d("SourceSelector", "开始解析子源，source.name=${source.name}, jsCode.length=${source.jsCode.length}")
            
            if (source.jsCode.isNotBlank()) {
                try {
                    // 尝试解析 JSON 配置中的 sites 数组
                    val json = Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    }
                    
                    android.util.Log.d("SourceSelector", "jsCode 前200字符: ${source.jsCode.take(200)}")
                    
                    val jsonElement: JsonElement = json.parseToJsonElement(source.jsCode)
                    android.util.Log.d("SourceSelector", "解析 JSON 成功，类型: ${jsonElement::class.simpleName}")
                    
                    if (jsonElement is JsonObject) {
                        val sitesArray = jsonElement["sites"]?.jsonArray
                        android.util.Log.d("SourceSelector", "sites 数组: ${if (sitesArray != null) "存在，长度=${sitesArray.size}" else "不存在"}")
                        
                        val parsedSubSources = sitesArray?.mapNotNull { siteJson ->
                            if (siteJson is JsonObject) {
                                // 优先使用 key，如果没有 key 则使用 name
                                val key = siteJson["key"]?.jsonPrimitive?.content
                                val name = siteJson["name"]?.jsonPrimitive?.content
                                
                                android.util.Log.d("SourceSelector", "解析子源项: key=$key, name=$name")
                                
                                // key 是必需的，如果没有 key 也没有 name，则跳过
                                val finalKey = key ?: name ?: return@mapNotNull null
                                val finalName = name ?: key ?: ""
                                
                                val type = siteJson["type"]?.jsonPrimitive?.intOrNull ?: 0
                                val api = siteJson["api"]?.jsonPrimitive?.content
                                
                                SubSource(
                                    key = finalKey,
                                    name = finalName,
                                    type = type,
                                    api = api,
                                    jar = null
                                )
                            } else {
                                null
                            }
                        } ?: emptyList()
                        
                        // 调试输出
                        android.util.Log.d("SourceSelector", "解析完成，共 ${parsedSubSources.size} 个子源: ${parsedSubSources.take(3).map { "${it.key}(${it.name})" }}")
                        
                        parsedSubSources
                    } else {
                        android.util.Log.w("SourceSelector", "JSON 元素不是 JsonObject，而是: ${jsonElement::class.simpleName}")
                        emptyList()
                    }
                } catch (e: Exception) {
                    // 解析失败，打印错误信息
                    android.util.Log.e("SourceSelector", "解析子源失败: ${e.message}", e)
                    e.printStackTrace()
                    emptyList<SubSource>()
                }
            } else {
                android.util.Log.w("SourceSelector", "jsCode 为空")
                emptyList<SubSource>()
            }
        }) ?: run {
            android.util.Log.w("SourceSelector", "currentSource 为 null")
            emptyList<SubSource>()
        }
        
        android.util.Log.d("SourceSelector", "最终结果: ${result.size} 个子源")
        result
    }
    
    val hasSubSources = subSources.isNotEmpty()
    
    // 调试：如果没有子源但 jsCode 不为空，输出警告
    LaunchedEffect(hasSubSources, currentSource?.jsCode) {
        if (!hasSubSources && currentSource != null && currentSource.jsCode.isNotBlank()) {
            android.util.Log.w("SourceSelector", "警告：未能解析子源，jsCode 长度: ${currentSource.jsCode.length}, 前100字符: ${currentSource.jsCode.take(100)}")
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasSubSources) "选择分源" else "选择视频源") },
        text = {
            // 优先显示子源列表（如果当前源有多源配置）
            if (hasSubSources) {
                // 显示子源列表（分源）
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subSources.size) { index ->
                        val subSource = subSources[index]
                        val isSelected = subSource.key == currentSubSourceKey
                        
                        SubSourceSelectorItem(
                            subSource = subSource,
                            isSelected = isSelected,
                            onClick = {
                                onSelectSubSource(subSource.key)
                            },
                            currentSource = currentSource
                        )
                    }
                }
            } else if (currentSource != null && currentSource.jsCode.isNotBlank()) {
                // 如果有 jsCode 但没有解析出子源，显示错误提示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "无法解析子源列表",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "请检查视频源配置是否正确",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (allSources.isEmpty()) {
                // 没有源也没有子源
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "暂无视频源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "请先添加视频源",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 显示所有视频源列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allSources.size) { index ->
                        val source = allSources[index]
                        val isSelected = source.id == currentSourceId
                        
                        SourceSelectorItem(
                            source = source,
                            isSelected = isSelected,
                            onClick = {
                                onSelectSource(source)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 子源选择列表项
 */
@Composable
fun SubSourceSelectorItem(
    subSource: SubSource,
    isSelected: Boolean,
    onClick: () -> Unit,
    currentSource: VideoSource? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subSource.name.ifBlank { subSource.key },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                // 显示子源的 key（如果和 name 不同）
                if (subSource.key != subSource.name && subSource.key.isNotBlank()) {
                    Text(
                        text = subSource.key,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                if (subSource.type == 2) "直播" else "点播",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                    )
                    if (subSource.api != null) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    subSource.api,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            )
                        )
                    }
                }
            }
            
            // 选中状态指示
            if (isSelected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 分源选择列表项
 */
@Composable
fun SourceSelectorItem(
    source: VideoSource,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name.ifBlank { source.url },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (source.url != source.name && source.url.isNotBlank()) {
                    Text(
                        text = source.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                if (source.type == SourceType.VOD) "点播" else "直播",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                    )
                    if (source.jsCode.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    "JavaScript",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            )
                        )
                    }
                }
            }
            
            // 选中状态指示
            if (isSelected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 计算应用缓存大小
 */
suspend fun calculateCacheSize(context: Context): Long {
    return withContext(Dispatchers.IO) {
        var totalSize = 0L
        try {
            // 计算内部缓存目录
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                totalSize += getDirectorySize(cacheDir)
            }
            
            // 计算外部缓存目录
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                totalSize += getDirectorySize(externalCacheDir)
            }
        } catch (e: Exception) {
            // 计算失败返回0
        }
        totalSize
    }
}

/**
 * 递归计算目录大小
 */
private fun getDirectorySize(directory: File): Long {
    var size = 0L
    try {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
    } catch (e: Exception) {
        // 忽略错误，继续计算其他文件
    }
    return size
}

/**
 * 格式化字节数为可读格式
 */
fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * 清理应用缓存
 */
suspend fun clearApplicationCache(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            // 清理缓存目录
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir)
                cacheDir.mkdirs()
            }
            
            // 清理外部缓存目录
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteRecursive(externalCacheDir)
                externalCacheDir.mkdirs()
            }
            
            // 清理 WebView 缓存
            try {
                android.webkit.WebView(context).apply {
                    clearCache(true)
                    clearHistory()
                    destroy()
                }
            } catch (e: Exception) {
                // WebView 清理失败不影响整体清理
            }
        } catch (e: Exception) {
            throw e
        }
    }
}

/**
 * 递归删除目录
 */
fun deleteRecursive(fileOrDirectory: File) {
    if (fileOrDirectory.isDirectory) {
        fileOrDirectory.listFiles()?.forEach { child ->
            deleteRecursive(child)
        }
    }
    fileOrDirectory.delete()
}

/**
 * 缓存清理对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isClearing: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!isClearing) onDismiss() },
        title = { Text("缓存清理") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("确定要清理所有缓存数据吗？")
                if (isClearing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            "正在清理...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        "这将清理应用的所有缓存数据，包括图片、视频、临时文件等。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isClearing
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isClearing
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 关于对话框 - 显示应用信息、版本、版权和使用说明
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    // 获取应用版本信息
    val packageInfo = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    } catch (e: Exception) {
        null
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "1"
    } else {
        @Suppress("DEPRECATION")
        (packageInfo?.versionCode ?: 1).toString()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("关于 XMSLEEP", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 应用名称和版本
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "XMSLEEP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "版本: $versionName (Build $versionCode)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // 应用说明
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "应用说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "XMSLEEP 是一款专注于白噪音播放的应用，提供多种自然声音帮助您放松、专注和入眠。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 使用说明
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "使用说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "• 选择您喜欢的声音卡片开始播放\n" +
                        "• 点击音量图标可以单独调节每个声音的音量\n" +
                        "• 使用倒计时功能可以设置自动停止播放的时间\n" +
                        "• 在设置中可以调整主题、隐藏动画等",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                HorizontalDivider()
                
                // 技术信息
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "技术信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "• 使用 Jetpack Compose 构建界面\n" +
                        "• 采用 Material Design 3 设计规范",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // 版权说明
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "版权说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "© 2024 XMSLEEP. All rights reserved.\n\n" +
                        "本应用提供的音频内容仅供个人学习和娱乐使用。\n" +
                        "请尊重相关音频资源的版权，不得用于商业用途。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

/**
 * 记录对话框 - 显示视频源列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDialog(
    sources: List<VideoSource>,
    onDismiss: () -> Unit,
    onDelete: (VideoSource) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("视频源记录") },
        text = {
            if (sources.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("暂无记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sources.size) { index ->
                        val source = sources[index]
                        HistoryListItem(source = source, isFirst = index == 0, onCopy = { clipboardManager?.let { cm -> val clip = android.content.ClipData.newPlainText("视频源", source.url); cm.setPrimaryClip(clip) } }, onDelete = { onDelete(source) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

/**
 * 记录列表项
 */
@Composable
fun HistoryListItem(
    source: VideoSource,
    isFirst: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左边的框 - 显示视频源名称或URL
        Card(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = source.url,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // 右边的复制和删除按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 复制按钮
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 删除按钮（第一个不可删除）
            IconButton(
                onClick = onDelete,
                enabled = !isFirst,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = if (isFirst) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}
