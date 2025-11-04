package org.streambox.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.heightIn
import android.content.ClipboardManager
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import org.streambox.datasource.xmbox.XmboxVideoSource
import org.streambox.datasource.xmbox.config.XmboxConfig
import org.streambox.datasource.xmbox.model.VideoItem
import androidx.navigation.compose.composable
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
            StreamBoxApp()
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

// ========== 源管理数据模型（参考 XMBOX，使用 Animeko 代码规范）==========
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
fun StreamBoxApp() {
    // 主题状态管理
    var darkMode by remember { mutableStateOf<DarkModeOption>(DarkModeOption.AUTO) }
    var selectedColor by remember { mutableStateOf(Color(0xFF4F378B)) }  // Animeko DefaultSeedColor
    var useDynamicColor by remember { mutableStateOf(false) }
    var useBlackBackground by remember { mutableStateOf(false) }
    var alwaysDarkInVideoPage by remember { mutableStateOf(false) }
    
    // 计算是否使用深色主题
    val isDark = when (darkMode) {
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
        DarkModeOption.AUTO -> isSystemInDarkTheme()
    }
    
    // 创建主题
    StreamBoxTheme(
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
                alwaysDarkInVideoPage = alwaysDarkInVideoPage,
                onDarkModeChange = { darkMode = it },
                onColorChange = { selectedColor = it },
                onDynamicColorChange = { useDynamicColor = it },
                onBlackBackgroundChange = { useBlackBackground = it },
                onAlwaysDarkInVideoPageChange = { alwaysDarkInVideoPage = it }
            )
        }
    }
}

@Composable
fun StreamBoxTheme(
    isDark: Boolean,
    seedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // 设置系统状态栏样式（和 Animeko 一样）
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
    
    // 使用 MaterialKolor 生成完整的配色方案（和 Animeko 一样）
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
    alwaysDarkInVideoPage: Boolean,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onAlwaysDarkInVideoPageChange: (Boolean) -> Unit
) {
    val mainNavController = rememberNavController()
    var selectedItem by remember { mutableIntStateOf(0) }
    
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
    
    Scaffold(
        topBar = {
            // 只在视频源页面显示 TopBar
            if (isMainRoute && selectedItem == 0) {
                TopAppBar(
                    title = { 
                        if (showSourceSearch && selectedItem == 0) {
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
                            Text("视频源")
                        }
                    },
                    actions = {
                        // 只在视频源页面显示操作按钮
                        if (selectedItem == 0 && !showSourceSearch) {
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
            if (selectedItem == 0) {
                FloatingActionButton(
                    onClick = { showAddSourceDialog = true }
                ) {
                    Icon(Icons.Default.Add, "添加源")
                }
            }
        },
        bottomBar = {
            // 只在主页面显示底部导航栏
            if (isMainRoute) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.VideoLibrary, null) },
                        label = { Text("视频源") },
                        selected = selectedItem == 0,
                        onClick = { selectedItem = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("设置") },
                        selected = selectedItem == 1,
                        onClick = { selectedItem = 1 }
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
                // 主页面：直接根据 selectedItem 切换内容
                AnimatedContent(
                    targetState = selectedItem,
                    modifier = Modifier.fillMaxSize(),
                    label = "tab_switch"
                ) { currentTab ->
                    when (currentTab) {
                        0 -> {
                            SourcesScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                onShowAddDialog = { showAddSourceDialog = true }
                            )
                        }
                        1 -> {
                            var showAddSourceDialog2 by remember { mutableStateOf(false) }
                            var showHistoryDialog by remember { mutableStateOf(false) }
                            
                            SettingsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                onNavigateToTheme = { 
                                    mainNavController.navigate("theme") 
                                },
                                onNavigateToSourceManagement = {
                                    mainNavController.navigate("sourceManagement")
                                },
                                onShowAddSourceDialog = { showAddSourceDialog2 = true },
                                onShowHistoryDialog = { showHistoryDialog = true }
                            )
                            
                            // 设置页面的对话框
                            if (showAddSourceDialog2) {
                                AddSourceDialog(
                                    onDismiss = { showAddSourceDialog2 = false },
                                    onConfirm = { newSource: VideoSource ->
                                        VideoSourceManager.addSource(newSource)
                                        showAddSourceDialog2 = false
                                    }
                                )
                            }
                            
                            // 记录对话框
                            if (showHistoryDialog) {
                                HistoryDialog(
                                    sources = VideoSourceManager.sources.toList(),
                                    onDismiss = { showHistoryDialog = false },
                                    onDelete = { source: VideoSource ->
                                        VideoSourceManager.removeSource(source)
                                    }
                                )
                            }
                        }
                        else -> { /* 不应该到达这里 */ }
                    }
                }
                
                // 视频源页面的添加源对话框
                if (showAddSourceDialog && selectedItem == 0) {
                    AddSourceDialog(
                        onDismiss = { showAddSourceDialog = false },
                        onConfirm = { newSource: VideoSource ->
                            VideoSourceManager.addSource(newSource)
                            showAddSourceDialog = false
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
                    alwaysDarkInVideoPage = alwaysDarkInVideoPage,
                    onDarkModeChange = onDarkModeChange,
                    onColorChange = onColorChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onBlackBackgroundChange = onBlackBackgroundChange,
                    onAlwaysDarkInVideoPageChange = onAlwaysDarkInVideoPageChange,
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
}

@Composable
fun SourcesScreen(
    modifier: Modifier = Modifier,
    onShowAddDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val videoSource = remember { XmboxVideoSource.create(context) }
    
    // 当前选中的视频源（使用第一个）
    val currentSource = VideoSourceManager.sources.firstOrNull()
    
    // 视频列表状态
    var videoList by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    
    // 当视频源变化时，加载视频列表
    LaunchedEffect(currentSource?.id) {
        if (currentSource != null && currentSource.jsCode.isNotBlank()) {
            isLoading = true
            loadError = null
            try {
                // 构建 XmboxConfig
                val config = XmboxConfig.JavaScriptConfig(
                    url = currentSource.url,
                    name = currentSource.name,
                    jsCode = currentSource.jsCode,
                    type = if (currentSource.type == SourceType.VOD) {
                        org.streambox.datasource.xmbox.config.SourceType.VOD
                    } else {
                        org.streambox.datasource.xmbox.config.SourceType.LIVE
                    },
                    timeout = currentSource.timeout
                )
                
                // 获取首页视频列表
                val list = videoSource.getHomeVideoList(config)
                videoList = list
            } catch (e: Exception) {
                loadError = e.message ?: "加载失败"
            } finally {
                isLoading = false
            }
        } else {
            videoList = emptyList()
        }
    }
    
    // 如果没有视频源，显示添加提示
    if (VideoSourceManager.sources.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "还没有添加视频源",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(onClick = onShowAddDialog) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加第一个源")
                }
            }
        }
        return
    }
    
    // 显示视频列表
    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        loadError != null -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "加载失败: $loadError",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            loadError = null
                            // 触发重新加载
                        }
                    }) {
                        Text("重试")
                    }
                }
            }
        }
        videoList.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "暂无视频",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        else -> {
            // 使用网格布局显示视频列表
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(videoList.size) { index ->
                    val video = videoList[index]
                    VideoItemCard(
                        video = video,
                        onClick = {
                            // TODO: 播放视频
                        }
                    )
                }
            }
        }
    }
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
                    AsyncImage(
                        model = video.pic,
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
    onNavigateToTheme: () -> Unit,
    onNavigateToSourceManagement: () -> Unit,
    onShowAddSourceDialog: () -> Unit = {},
    onShowHistoryDialog: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top)
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 内容管理
        Text("内容管理", style = MaterialTheme.typography.titleLarge)
        
        // 3个水平的源管理卡片（水平排列）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 添加源
            Card(
                onClick = onShowAddSourceDialog,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "添加源",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "输入视频源",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 视频源列表
            Card(
                onClick = onNavigateToSourceManagement,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "视频",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "源列表",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 添加记录
            Card(
                onClick = onShowHistoryDialog,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "记录",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "添加历史",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // 外观设置
        Text("外观", style = MaterialTheme.typography.titleLarge)
        Card(
            onClick = onNavigateToTheme
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
        
        // 关于
        Text("关于", style = MaterialTheme.typography.titleLarge)
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("StreamBox", style = MaterialTheme.typography.titleMedium)
                Text("版本: 1.0.0")
                Text("基于 Animeko 架构")
                Text("参考 XMBOX 设计")
            }
        }
    }
}

// ========== 主题设置详情页（完全复刻 Animeko）==========
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    darkMode: DarkModeOption,
    selectedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    alwaysDarkInVideoPage: Boolean,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onAlwaysDarkInVideoPageChange: (Boolean) -> Unit,
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
                    // 复刻 Animeko 的 BackNavigationIconButton
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
                // 复刻 Animeko: forTopAppBar() = systemBars.union(displayCutout)
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 复刻 Animeko: 消费 TopAppBar 使用的 insets
                .consumeWindowInsets(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top)
                )
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 外观模式选择面板（复刻 Animeko 的 DarkModeSelectPanel）
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
            
            // 播放页始终使用深色模式（复刻 Animeko）
            SwitchItem(
                checked = alwaysDarkInVideoPage,
                onCheckedChange = onAlwaysDarkInVideoPageChange,
                title = "播放页始终使用深色模式",
                description = "观看视频时自动切换到深色，提供更好的观影体验"
            )
            
            // 调色板选择（复刻 Animeko 的 ColorButton 风格）
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
                        // 使用 HCT 色彩空间生成均匀分布的颜色（完全复刻 Animeko）
                        // 所有颜色统一使用 Chroma=40.0, Tone=40.0，确保明度一致
                        val colors = listOf(
                            Color(Hct.from(140.0, 40.0, 40.0).toInt()),  // base=4: 绿色偏青
                            Color(Hct.from(175.0, 40.0, 40.0).toInt()),  // base=5: 青绿
                            Color(Hct.from(210.0, 40.0, 40.0).toInt()),  // base=6: 青蓝
                            Color(Hct.from(245.0, 40.0, 40.0).toInt()),  // base=7: 蓝紫
                            Color(Hct.from(280.0, 40.0, 40.0).toInt()),  // base=8: 紫红
                            Color(0xFF4F378B),  // 默认紫色（Animeko DefaultSeedColor）
                            Color(Hct.from(315.0, 40.0, 40.0).toInt()),  // base=9: 粉红
                            Color(Hct.from(350.0, 40.0, 40.0).toInt()),  // base=10: 红
                            Color(Hct.from(35.0, 40.0, 40.0).toInt()),   // base=1: 橙黄
                            Color(Hct.from(70.0, 40.0, 40.0).toInt()),   // base=2: 黄
                            Color(Hct.from(105.0, 40.0, 40.0).toInt()),  // base=3: 黄绿
                        )
                        
                        colors.forEach { color ->
                            AnimekColorButton(
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

// ========== 深色模式选择面板（复刻 Animeko）==========
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

// ========== 主题预览面板（复刻 Animeko）==========
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

// ========== 开关项（复刻 Animeko）==========
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

// ========== Animeko 风格颜色按钮（完全复刻）==========
@Composable
fun AnimekColorButton(
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
    
    // 确定按钮点击处理 - 识别并保存
    val onConfirmClick: () -> Unit = {
        if (inputText.isNotBlank()) {
            scope.launch {
                try {
                    // 识别配置
                    val config = videoSource.identify(inputText, "")
                    val source = when (config) {
                        is XmboxConfig.VodConfig -> {
                            VideoSource(
                                name = config.name.ifBlank { "点播源" },
                                url = config.url,
                                type = SourceType.VOD,
                                searchable = config.searchable,
                                changeable = config.changeable,
                                quickSearch = config.quickSearch,
                                timeout = config.timeout
                            )
                        }
                        is XmboxConfig.LiveConfig -> {
                            VideoSource(
                                name = config.name.ifBlank { "直播源" },
                                url = config.url,
                                type = SourceType.LIVE,
                                timeout = config.timeout
                            )
                        }
                        is XmboxConfig.JavaScriptConfig -> {
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
                                jsCode = config.jsCode // 保存 JavaScript 代码
                            )
                        }
                        null -> {
                            // 如果无法识别，作为简单的 URL 处理
                            VideoSource(
                                name = "视频源",
                                url = inputText,
                                type = SourceType.VOD
                            )
                        }
                    }
                    onConfirm(source)
                } catch (e: Exception) {
                    // 识别失败，仍然创建简单的源
                    onConfirm(
                        VideoSource(
                            name = "视频源",
                            url = inputText,
                            type = SourceType.VOD
                        )
                    )
                }
            }
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
                // 输入框（带文件夹图标）- 使用标准 Material 3 样式
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            "请输入接口...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ) 
                    },
                    singleLine = false,
                    maxLines = 5,
                    trailingIcon = {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                
                // 点我粘贴按钮
                OutlinedButton(
                    onClick = onPasteClick,
                    modifier = Modifier.fillMaxWidth(),
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
                enabled = inputText.isNotBlank()
            ) {
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
 * 记录对话框 - 显示视频源列表（匹配 XMBOX 样式）
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
 * 记录列表项 - 匹配 XMBOX 样式
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
