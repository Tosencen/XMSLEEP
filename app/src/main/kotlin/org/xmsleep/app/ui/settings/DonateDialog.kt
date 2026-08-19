package org.xmsleep.app.ui.settings

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.xmsleep.app.R
import org.xmsleep.app.utils.Logger

/** 微信包名 */
private const val WECHAT_PACKAGE = "com.tencent.mm"
/**
 * 微信 URL Scheme。
 * 注意：只能拉起微信本体（weixin://），微信 8.0+ 已封禁外部直接拉起「扫一扫」的私有 scheme
 * （如 weixin://scanqrcode，会抛 ActivityNotFoundException），扫码需用户手动进入。
 */
private const val WECHAT_SCHEME = "weixin://"

/** 检测微信是否已安装（Android 11+ 需配合 manifest 中的 <queries> 声明才可见） */
private fun isWeChatInstalled(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(WECHAT_PACKAGE, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(WECHAT_PACKAGE, 0)
        }
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * 将打赏收款码保存到系统相册。
 * API 29+ 走 MediaStore（无需存储权限）；旧版本回退 insertImage。
 * @return 是否保存成功
 */
private fun saveDonateQrToGallery(context: Context): Boolean {
    return try {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.wechat_donate_qr)
            ?: return false
        val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "wechat_donate_qr.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/XMSLEEP")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            } else {
                false
            }
        } else {
            MediaStore.Images.Media.insertImage(
                context.contentResolver,
                bitmap,
                "wechat_donate_qr",
                "XMSLEEP donate QR code"
            ) != null
        }
        if (!bitmap.isRecycled) bitmap.recycle()
        saved
    } catch (e: Exception) {
        Logger.e("Donate", "保存收款码失败: ${e.message}")
        false
    }
}

/** 打开微信本体，成功返回 true */
private fun launchWeChat(context: Context): Boolean {
    return try {
        // LocalContext 是语言化包装的 application context，跨 Activity 启动必须带 NEW_TASK
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WECHAT_SCHEME))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Logger.e("Donate", "打开微信失败: ${e.message}")
        false
    }
}

/**
 * 给晓满加油弹窗（合并版）：
 * 左侧收款码 + 右侧三档金额；选中金额后自动保存收款码并出现「去微信扫码」主按钮。
 */
@Composable
fun DonateDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedAmount by remember { mutableIntStateOf(0) }
    var qrAutoSaved by remember { mutableStateOf(false) }

    val saveAndToast: () -> Unit = {
        if (!qrAutoSaved) {
            qrAutoSaved = saveDonateQrToGallery(context)
            val msgId = if (qrAutoSaved) R.string.qr_saved else R.string.qr_save_failed
            Toast.makeText(context, context.getString(msgId), Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(
                    text = context.getString(R.string.buy_me_a_coffee),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                // 记录按钮列高度，让二维码高度与三档按钮总高度一致
                var buttonsHeightPx by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                val qrHeight = if (buttonsHeightPx > 0) {
                    with(density) { buttonsHeightPx.toDp() }
                } else {
                    170.dp
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 收款码：高度与右侧三档按钮总高度一致
                    Box(
                        modifier = Modifier
                            .width(144.dp)
                            .height(qrHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(R.drawable.wechat_donate_qr),
                            contentDescription = context.getString(R.string.wechat_pay_qr),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // 三档金额
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onSizeChanged { buttonsHeightPx = it.height },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DonateAmountButton(
                            icon = Icons.Default.Restaurant,
                            title = context.getString(R.string.donate_amount_1),
                            amount = context.getString(R.string.donate_amount_value_1),
                            selected = selectedAmount == 1,
                            onClick = {
                                selectedAmount = 1
                                saveAndToast()
                            }
                        )
                        DonateAmountButton(
                            icon = Icons.Default.DirectionsSubway,
                            title = context.getString(R.string.donate_amount_2),
                            amount = context.getString(R.string.donate_amount_value_2),
                            selected = selectedAmount == 2,
                            onClick = {
                                selectedAmount = 2
                                saveAndToast()
                            }
                        )
                        DonateAmountButton(
                            icon = Icons.Default.LunchDining,
                            title = context.getString(R.string.donate_amount_3),
                            amount = context.getString(R.string.donate_amount_value_3),
                            selected = selectedAmount == 3,
                            onClick = {
                                selectedAmount = 3
                                saveAndToast()
                            }
                        )
                    }
                }

                // 未选金额时不显示，避免"没确认金额就跳走"的困惑
                if (selectedAmount != 0) {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (!qrAutoSaved && !saveDonateQrToGallery(context)) {
                                Toast.makeText(context, context.getString(R.string.qr_save_failed), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isWeChatInstalled(context)) {
                                Toast.makeText(context, context.getString(R.string.wechat_not_installed), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!launchWeChat(context)) {
                                Toast.makeText(context, context.getString(R.string.wechat_launch_failed), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.go_wechat_scan),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** 单档金额按钮，选中时呈现 FilterChip 高亮态 */
@Composable
private fun DonateAmountButton(
    icon: ImageVector,
    title: String,
    amount: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else contentColor
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
