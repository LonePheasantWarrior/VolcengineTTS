package com.github.lonepheasantwarrior.volcenginetts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.github.lonepheasantwarrior.volcenginetts.R

/**
 * 更新提示弹窗
 * @param version 新版本号
 * @param releaseNotes 更新说明
 * @param onDownload 下载按钮点击回调
 * @param onCancel 取消按钮点击回调
 */
@Composable
fun UpdateDialog(
    version: String,
    releaseNotes: String,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    // 使用更现代化的WindowInfo获取窗口尺寸（包含插入区域）
    val windowInfo = LocalWindowInfo.current
    val containerSize = windowInfo.containerSize
    val density = LocalDensity.current.density
    
    // 将像素尺寸转换为dp
    val screenWidth = (containerSize.width / density).dp
    val screenHeight = (containerSize.height / density).dp
    
    // 根据屏幕尺寸计算弹窗尺寸
    val dialogWidth = when {
        screenWidth < 360.dp -> screenWidth * 0.9f  // 小屏幕：宽度占90%
        screenWidth < 600.dp -> screenWidth * 0.8f  // 中等屏幕：宽度占80%
        else -> 480.dp  // 大屏幕：固定最大宽度
    }
    
    val dialogMaxHeight = screenHeight * 0.7f  // 最大高度为屏幕高度的70%
    
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(id = R.string.update_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(id = R.string.update_dialog_message, version),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (releaseNotes.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.update_dialog_release_notes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 更新内容区域 - 移除嵌套滚动，使用固定高度
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)  // 限制最大高度
                    ) {
                        Text(
                            text = releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            // 根据屏幕尺寸调整按钮布局
            if (screenWidth < 480.dp) {
                // 小屏幕：垂直布局
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.update_dialog_download))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.update_dialog_cancel))
                    }
                }
            } else {
                // 大屏幕：水平布局
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.update_dialog_cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.update_dialog_download))
                    }
                }
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        modifier = Modifier
            .padding(16.dp)
            .width(dialogWidth)
            .heightIn(max = dialogMaxHeight)
    )
}

/**
 * 更新检查错误弹窗
 * @param errorMessage 错误信息
 * @param onDismiss 关闭回调
 */
@Composable
fun UpdateErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.update_error_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(id = R.string.update_error_ok))
                }
            }
        }
    )
}