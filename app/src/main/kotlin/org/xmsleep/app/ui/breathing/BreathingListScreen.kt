package org.xmsleep.app.ui.breathing

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.xmsleep.app.R
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.meditation.MeditationPlayerManager
import org.xmsleep.app.ui.components.PlayingAnimation
import org.xmsleep.app.ui.components.FrostedPopup
import org.xmsleep.app.ui.components.FrostedGlassSheet
import org.xmsleep.app.ui.meditation.MeditationCategory
import org.xmsleep.app.ui.meditation.MeditationManifest
import java.io.InputStreamReader
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 无限水平滑动的 PagerState：虚拟大页数 + 取模映射，
 * 初始页对齐到 itemCount 的倍数，保证从第 0 项起左右均可滑动
 */
@Composable
private fun rememberInfinitePagerState(itemCount: Int): PagerState {
    val mid = Int.MAX_VALUE / 2
    return rememberPagerState(
        initialPage = if (itemCount > 0) mid - mid % itemCount else 0,
        pageCount = { if (itemCount > 0) Int.MAX_VALUE else 0 }
    )
}

@Composable
fun BreathingListScreen(
    onMethodClick: (String) -> Unit,
    onMeditationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val methods = remember { BreathingMethods.getMethods(context) }
    val pagerState = rememberInfinitePagerState(methods.size)
    var showTutorialMethod by remember { mutableStateOf<BreathingMethod?>(null) }
    val languageCode = LanguageManager.getCurrentLanguage(context).code

    val meditationManifest = remember {
        loadMeditationManifest(context)
    }
    val meditationCategories = remember(meditationManifest) {
        meditationManifest?.categories?.sortedBy { it.order } ?: emptyList()
    }

    val playerManager = remember { MeditationPlayerManager.getInstance() }
    val isMeditationPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val playingCategoryId by playerManager.currentCategoryId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        playerManager.initialize(context)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // 冥想模块（上方）
            Text(
                text = context.getString(R.string.meditation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
            )
            MeditationCategoriesRow(
                categories = meditationCategories,
                languageCode = languageCode,
                onClick = onMeditationClick,
                isPlaying = isMeditationPlaying,
                playingCategoryId = playingCategoryId
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 呼吸模块（下方）
            Text(
                text = context.getString(R.string.breathing_exercise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp
            ) { page ->
                val method = methods[page % methods.size]
                BreathingMethodCard(
                    method = method,
                    onClick = { onMethodClick(method.id) },
                    onHelpClick = { showTutorialMethod = method }
                )
            }
        }
    }

    showTutorialMethod?.let { method ->
        BreathingMethodTutorialDialog(method = method, onDismiss = { showTutorialMethod = null })
    }
}

@Composable
private fun MeditationCategoriesRow(
    categories: List<MeditationCategory>,
    languageCode: String,
    onClick: (String) -> Unit,
    isPlaying: Boolean,
    playingCategoryId: String?
) {
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    HorizontalPager(
        state = rememberInfinitePagerState(categories.size),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentPadding = PaddingValues(horizontal = 32.dp),
        pageSpacing = 16.dp
    ) { page ->
        val category = categories[page % categories.size]
        val showAnimation = isPlaying && playingCategoryId == category.id

        Box(
            modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(backgroundColor, backgroundColor.copy(alpha = 0.9f))))
                .clickable { onClick(category.id) }
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = category.getLocalizedName(languageCode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getMeditationCategorySubtitle(context, category.id),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = contentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = getCategoryTag(context, category.id),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (showAnimation) {
                        PlayingAnimation(
                            color = contentColor,
                            width = 16.dp,
                            height = 14.dp
                        )
                    }
                }
            }
        }
    }
}

private fun getMeditationCategorySubtitle(context: Context, categoryId: String): String {
    return when (categoryId) {
        "meditation_sleep" -> context.getString(R.string.meditation_sleep_subtitle)
        "meditation_practice" -> context.getString(R.string.meditation_practice_subtitle)
        "meditation_relax" -> context.getString(R.string.meditation_relax_subtitle)
        "mindfulness" -> context.getString(R.string.mindfulness_subtitle)
        else -> ""
    }
}

private fun getCategoryTag(context: Context, categoryId: String): String {
    return when (categoryId) {
        "meditation_sleep" -> context.getString(R.string.tag_sleep)
        "meditation_practice" -> context.getString(R.string.tag_practice)
        "meditation_relax" -> context.getString(R.string.tag_relax)
        "mindfulness" -> context.getString(R.string.tag_mindfulness)
        else -> ""
    }
}

private fun loadMeditationManifest(context: Context): MeditationManifest? {
    return try {
        context.assets.open("meditation_remote.json").use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                val type = object : TypeToken<MeditationManifest>() {}.type
                Gson().fromJson(reader, type)
            }
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun BreathingMethodCard(
    method: BreathingMethod,
    onClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    // 主题色系：容器色（柔和）+ 主色反色，随主题/黑白模式自动适配
    val (rhythmColor, contentColor) = when (method.id) {
        "sleep_478" -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        "box_4444" -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        "belly_46" -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        "stress_426" -> colorScheme.inversePrimary to colorScheme.onSurface
        else -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(rhythmColor, rhythmColor.copy(alpha = 0.85f))))
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                if (method.isPrimary) {
                    Surface(
                        color = contentColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.breathing_method_primary),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = method.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = method.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }

            RhythmVisualization(
                inhale = method.inhale, hold = method.hold,
                exhale = method.exhale, holdAfter = method.holdAfter, color = contentColor
            )

            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    method.tags.take(2).forEach { tag ->
                        Surface(color = contentColor.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(text = tag, style = MaterialTheme.typography.labelSmall, color = contentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    IconButton(onClick = onHelpClick, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = context.getString(R.string.tutorial),
                            tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (method.inhale > 0) RhythmParam(label = context.getString(R.string.inhale).removeSuffix("..."), value = "${method.inhale}s", contentColor = contentColor)
                    if (method.hold > 0) RhythmParam(label = context.getString(R.string.hold).removeSuffix("..."), value = "${method.hold}s", contentColor = contentColor)
                    if (method.exhale > 0) RhythmParam(label = context.getString(R.string.exhale).removeSuffix("..."), value = "${method.exhale}s", contentColor = contentColor)
                    if (method.holdAfter > 0) RhythmParam(label = context.getString(R.string.hold_after).removeSuffix("..."), value = "${method.holdAfter}s", contentColor = contentColor)
                }
            }
        }
    }
}

@Composable
private fun RhythmVisualization(inhale: Int, hold: Int, exhale: Int, holdAfter: Int, color: Color) {
    val context = LocalContext.current
    val total = (inhale + hold + exhale + holdAfter).toFloat()
    if (total == 0f) return
    val segments = listOf(
        Triple(inhale, context.getString(R.string.inhale).removeSuffix("..."), 0.8f),
        Triple(hold, context.getString(R.string.hold).removeSuffix("..."), 1f),
        Triple(exhale, context.getString(R.string.exhale).removeSuffix("..."), 0.5f),
        Triple(holdAfter, context.getString(R.string.hold_after).removeSuffix("..."), 1f)
    ).filter { it.first > 0 }
    Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        segments.forEach { (seconds, label, heightRatio) ->
            Box(
                modifier = Modifier.weight(seconds / total).fillMaxHeight(heightRatio)
                    .align(Alignment.CenterVertically).clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$label${seconds}s", style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.9f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RhythmParam(label: String, value: String, contentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
    }
}

@Composable
private fun BreathingMethodTutorialDialog(method: BreathingMethod, onDismiss: () -> Unit) {
    val context = LocalContext.current
    FrostedPopup(visible = true) {
        FrostedGlassSheet(
            visible = true,
            onDismissRequest = onDismiss
        ) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Text(
                method.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(method.subtitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Divider()
                Text(method.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Divider()
                Text(context.getString(R.string.operation_steps), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                method.steps.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Divider()
                Text(context.getString(R.string.breathing_rhythm), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val rhythmDetail = listOf(
                    context.resources.getQuantityString(R.plurals.breathing_rhythm_inhale, method.inhale, method.inhale),
                    context.resources.getQuantityString(R.plurals.breathing_rhythm_hold, method.hold, method.hold),
                    context.resources.getQuantityString(R.plurals.breathing_rhythm_exhale, method.exhale, method.exhale),
                    context.resources.getQuantityString(R.plurals.breathing_rhythm_hold, method.holdAfter, method.holdAfter)
                ).joinToString(" → ")
                Text(rhythmDetail,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(context.getString(R.string.got_it))
            }
        }
    }
}
