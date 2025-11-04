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
                                searchable = true,
                                changeable = true,
                                quickSearch = true
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
        title = { 
            Text(
                "视频",
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 输入框（带文件夹图标）
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(8.dp)
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
                        tint = Color(0xFFFFC107), // 黄色，匹配 XMBOX
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
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

