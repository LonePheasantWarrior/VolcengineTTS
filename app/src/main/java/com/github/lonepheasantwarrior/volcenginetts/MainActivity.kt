package com.github.lonepheasantwarrior.volcenginetts

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.lonepheasantwarrior.volcenginetts.common.Constants
import com.github.lonepheasantwarrior.volcenginetts.common.LogTag
import com.github.lonepheasantwarrior.volcenginetts.engine.SynthesisEngine
import com.github.lonepheasantwarrior.volcenginetts.function.SettingsFunction
import com.github.lonepheasantwarrior.volcenginetts.tts.TTSContext
import com.github.lonepheasantwarrior.volcenginetts.tts.TtsVoiceSample
import com.github.lonepheasantwarrior.volcenginetts.ui.WelcomeDialog
import com.github.lonepheasantwarrior.volcenginetts.ui.theme.VolcengineTTSTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val synthesisEngine: SynthesisEngine get() = (applicationContext as TTSApplication).synthesisEngine
    private val settingsFunction: SettingsFunction get() = (applicationContext as TTSApplication).settingsFunction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VolcengineTTSTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) {
                        VolcengineTTSUI(modifier = Modifier.padding(it))
                    }

                    // 显示欢迎弹窗
                    var showWelcomeDialog by remember { mutableStateOf(settingsFunction.shouldShowWelcomeDialog()) }

                    if (showWelcomeDialog) {
                        WelcomeDialog(
                            onDismiss = {
                                showWelcomeDialog = false
                            },
                            onDontShowAgain = { dontShowAgain ->
                                settingsFunction.setShowWelcomeDialog(!dontShowAgain)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        synthesisEngine.destroy()
    }
}

// 数据类 - 表示声音信息
data class SpeakerInfo(
    val name: String,
    val id: String
)

// ViewModel 类 - 负责状态管理和业务逻辑
class VolcengineTTSViewModel(application: Application) : AndroidViewModel(application) {
    private val synthesisEngine: SynthesisEngine get() = (getApplication() as TTSApplication).synthesisEngine
    private val settingsFunction: SettingsFunction get() = (getApplication() as TTSApplication).settingsFunction
    private val ttsContext: TTSContext get() = (getApplication() as TTSApplication).ttsContext

    // 应用配置状态
    var appId by mutableStateOf("")
        private set
    var token by mutableStateOf("")
        private set
    var serviceCluster by mutableStateOf(Constants.DEFAULT_SERVICE_CLUSTER)
        private set
    var isEmotional by mutableStateOf(false)

    // UI 交互状态
    var selectedScene by mutableStateOf("")
    var selectedSpeakerId by mutableStateOf("")
    var selectedSpeakerName by mutableStateOf("")

    // 错误状态 - 用于UI提示
    var isAppIdError by mutableStateOf(false)
    var isTokenError by mutableStateOf(false)
    var isServiceClusterError by mutableStateOf(false)
    var isSpeakerError by mutableStateOf(false)

    // 自定义方法以在输入时清除错误状态
    fun updateAppId(value: String) {
        appId = value
        if (isAppIdError && value.isNotBlank()) {
            isAppIdError = false
        }
    }

    fun updateToken(value: String) {
        token = value
        if (isTokenError && value.isNotBlank()) {
            isTokenError = false
        }
    }

    fun updateServiceCluster(value: String) {
        serviceCluster = value
        if (isServiceClusterError && value.isNotBlank()) {
            isServiceClusterError = false
        }
    }

    fun updateSelectedSpeakerIdAndName(id: String, name: String) {
        selectedSpeakerId = id
        selectedSpeakerName = name
        if (isSpeakerError && id.isNotBlank()) {
            isSpeakerError = false
        }
    }

    init {
        // 初始化时加载保存的设置
        loadSettings()
    }

    // 从资源获取的静态数据
    fun getSceneCategories(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.scene_categories)
    }

    fun getSpeakerList(): Array<String> {
        return getApplication<Application>().resources.getStringArray(R.array.speaker_list)
    }

    // 业务逻辑
    fun filterSpeakersByScene(scene: String): List<SpeakerInfo> {
        return getSpeakerList()
            .map { it.split("|") }
            .filter { it.size >= 3 && it[0] == scene } // 场景在索引0的位置
            .map { SpeakerInfo(name = it[1], id = it[2]) }
    }

    /**
     * 验证设置是否有效
     * @return 验证结果
     */
    private fun validateSettings(): Boolean {
        // 重置错误状态
        isAppIdError = false
        isTokenError = false
        isServiceClusterError = false
        isSpeakerError = false

        var isValid = true

        // 检查App ID
        if (appId.isBlank()) {
            isAppIdError = true
            isValid = false
        }

        // 检查Token
        if (token.isBlank()) {
            isTokenError = true
            isValid = false
        }

        // 检查Service Cluster
        // 如果该字段未填写内容则填充默认值
        if (serviceCluster.isBlank()) {
            serviceCluster = Constants.DEFAULT_SERVICE_CLUSTER
            isServiceClusterError = false
            isValid = true
        }

        // 检查选中的声音ID
        if (selectedSpeakerId.isBlank()) {
            isSpeakerError = true
            isValid = false
        }

        return isValid
    }

    /**
     * 保存设置到持久化存储
     */
    fun saveSettings() {
        if (validateSettings()) {
            settingsFunction.saveSettings(
                appId,
                token,
                selectedSpeakerId,
                serviceCluster,
                isEmotional
            )
        }
    }

    /**
     * 从持久化存储加载设置
     */
    private fun loadSettings() {
        val settingsData = settingsFunction.getSettings()
        val (savedAppId, savedToken, savedSelectedSpeakerId, savedServiceCluster, savedIsEmotional) = settingsData
        if (savedAppId.isNotEmpty()) {
            appId = savedAppId
        }
        if (savedToken.isNotEmpty()) {
            token = savedToken
        }
        if (savedSelectedSpeakerId.isNotEmpty()) {
            selectedSpeakerId = savedSelectedSpeakerId
            // 根据保存的声音ID查找对应的声音名称
            findSpeakerNameById(savedSelectedSpeakerId)
        }
        if (savedServiceCluster.isNotEmpty()) {
            serviceCluster = savedServiceCluster
        }
        isEmotional = savedIsEmotional
    }

    /**
     * 根据声音ID查找声音名称
     */
    private fun findSpeakerNameById(speakerId: String) {
        val speakerList = getSpeakerList()
        val speakerInfo = speakerList
            .map { it.split("|") }
            .find { it.size >= 3 && it[2] == speakerId } // ID在索引2的位置

        if (speakerInfo != null) {
            selectedSpeakerName = speakerInfo[1] // 名称在索引1的位置
            selectedScene = speakerInfo[0] // 场景在索引0的位置
        }
    }

    /**
     * 播放演示声音
     */
    fun playSampleVoice() {
        // 使用现有的SynthesisEngine播放示例文本
        try {
            // 检查设置是否有效
            if (appId.isBlank() || token.isBlank() ||
                serviceCluster.isBlank() || selectedSpeakerId.isBlank()
            ) {
                Log.d(LogTag.INFO, "填写配置后可预览声音")
                Toast.makeText(
                    getApplication(),
                    "填写配置后可预览声音",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            val sampleText = TtsVoiceSample.getByLocate(getApplication(), Locale.getDefault())

            // 使用SynthesisEngine播放示例文本
            synthesisEngine.create(
                appId, token,
                selectedSpeakerId, serviceCluster, isEmotional
            )
            synthesisEngine.startEngine(sampleText, null, null, null)
        } catch (e: Exception) {
            Log.e(LogTag.ERROR, "播放演示声音失败: ${e.message}")
            Toast.makeText(
                getApplication(),
                "播放演示声音失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

// UI 组件 - 负责呈现和用户交互
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolcengineTTSUI(modifier: Modifier = Modifier) {
    val viewModel: VolcengineTTSViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application =
                checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
            return VolcengineTTSViewModel(application) as T
        }
    })

    // 获取数据
    val sceneCategories = viewModel.getSceneCategories()

    // 初始化默认场景（仅在未设置时）
    if (viewModel.selectedScene.isEmpty()) {
        viewModel.selectedScene = sceneCategories.firstOrNull() ?: ""
    }

    // 过滤当前场景的声音
    val filteredSpeakers = viewModel.filterSpeakersByScene(viewModel.selectedScene)

    // 初始化默认声音（仅在未设置时）
    if (viewModel.selectedSpeakerId.isEmpty() && filteredSpeakers.isNotEmpty()) {
        viewModel.selectedSpeakerId = filteredSpeakers.first().id
        viewModel.selectedSpeakerName = filteredSpeakers.first().name
    }

    // 下拉菜单状态
    var sceneDropdownExpanded by remember { mutableStateOf(false) }
    var speakerDropdownExpanded by remember { mutableStateOf(false) }

    // 使用垂直滚动布局以适应不同屏幕尺寸
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // 基础配置组 (appId, token, serviceCluster)
                TTSBasicConfigurationInputs(
                    appId = viewModel.appId,
                    token = viewModel.token,
                    serviceCluster = viewModel.serviceCluster,
                    isAppIdError = viewModel.isAppIdError,
                    isTokenError = viewModel.isTokenError,
                    isServiceClusterError = viewModel.isServiceClusterError,
                    onAppIdChange = { viewModel.updateAppId(it) },
                    onTokenChange = { viewModel.updateToken(it) },
                    onServiceClusterChange = { viewModel.updateServiceCluster(it) }
                )
            }

            item {
                // 场景和声音配置组 (selectedScene, selectedSpeakerName, isEmotional)
                TTSVoiceConfigurationInputs(
                    selectedScene = viewModel.selectedScene,
                    sceneCategories = sceneCategories,
                    selectedSpeakerName = viewModel.selectedSpeakerName,
                    speakers = filteredSpeakers,
                    isEmotional = viewModel.isEmotional,
                    isSpeakerError = viewModel.isSpeakerError,
                    sceneDropdownExpanded = sceneDropdownExpanded,
                    speakerDropdownExpanded = speakerDropdownExpanded,
                    onSceneExpandedChange = { sceneDropdownExpanded = it },
                    onSpeakerExpandedChange = { speakerDropdownExpanded = it },
                    onSceneSelect = { scene ->
                        viewModel.selectedScene = scene
                        sceneDropdownExpanded = false
                        // 获取新场景的声音列表并设置为第一个声音选项
                        val newFilteredSpeakers = viewModel.filterSpeakersByScene(scene)
                        if (newFilteredSpeakers.isNotEmpty()) {
                            viewModel.updateSelectedSpeakerIdAndName(
                                newFilteredSpeakers.first().id,
                                newFilteredSpeakers.first().name
                            )
                        } else {
                            viewModel.updateSelectedSpeakerIdAndName("", "")
                        }
                    },
                    onSpeakerSelect = { speakerInfo ->
                        viewModel.updateSelectedSpeakerIdAndName(speakerInfo.id, speakerInfo.name)
                        speakerDropdownExpanded = false
                        // 用户选择声音后自动播放演示声音
                        viewModel.playSampleVoice()
                    },
                    onIsEmotionalChange = { viewModel.isEmotional = it }
                )
            }

            item {
                // 保存设置按钮组
                TTSSaveSettingsButton(
                    onClick = { viewModel.saveSettings() }
                )
            }
        }
    }
}

@Composable
fun TTSBasicConfigurationInputs(
    appId: String,
    token: String,
    serviceCluster: String,
    isAppIdError: Boolean,
    isTokenError: Boolean,
    isServiceClusterError: Boolean,
    onAppIdChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onServiceClusterChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "基础配置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )

            // App ID 输入框
            OutlinedTextField(
                value = appId,
                onValueChange = onAppIdChange,
                label = { Text(stringResource(id = R.string.input_app_id)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isAppIdError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isAppIdError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.5f
                    ),
                ),
                isError = isAppIdError,
                supportingText = if (isAppIdError) {
                    {
                        Text(
                            text = "App ID不能为空",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                }
            )

            // Token 输入框
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text(stringResource(id = R.string.input_token)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isTokenError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isTokenError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.5f
                    ),
                ),
                isError = isTokenError,
                supportingText = if (isTokenError) {
                    {
                        Text(
                            text = "Token不能为空",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                }
            )

            // API资源ID 输入框
            OutlinedTextField(
                value = serviceCluster,
                onValueChange = onServiceClusterChange,
                label = { Text(stringResource(id = R.string.input_service_cluster)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isServiceClusterError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isServiceClusterError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.5f
                    ),
                ),
                isError = isServiceClusterError,
                supportingText = if (isServiceClusterError) {
                    {
                        Text(
                            text = "Service Cluster不能为空",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
fun TTSVoiceConfigurationInputs(
    selectedScene: String,
    sceneCategories: Array<String>,
    selectedSpeakerName: String,
    speakers: List<SpeakerInfo>,
    isEmotional: Boolean,
    isSpeakerError: Boolean,
    sceneDropdownExpanded: Boolean,
    speakerDropdownExpanded: Boolean,
    onSceneExpandedChange: (Boolean) -> Unit,
    onSpeakerExpandedChange: (Boolean) -> Unit,
    onSceneSelect: (String) -> Unit,
    onSpeakerSelect: (SpeakerInfo) -> Unit,
    onIsEmotionalChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "语音配置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )

            // 场景选择器
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.select_voice_scene),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedButton(
                    onClick = { onSceneExpandedChange(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = selectedScene,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(
                    visible = sceneDropdownExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    val configuration = LocalConfiguration.current
                    val menuMaxHeight = 0.7 * configuration.screenHeightDp.dp
                    val menuMaxWidth = 0.8 * configuration.screenWidthDp.dp

                    DropdownMenu(
                        expanded = sceneDropdownExpanded,
                        onDismissRequest = { onSceneExpandedChange(false) },
                        modifier = Modifier
                            .widthIn(max = menuMaxWidth)
                            .heightIn(max = menuMaxHeight),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        sceneCategories.forEach { scene ->
                            DropdownMenuItem(
                                text = { Text(scene) },
                                onClick = { onSceneSelect(scene) },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // 声音选择器
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = R.string.select_voice_item),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedButton(
                    onClick = { onSpeakerExpandedChange(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = if (isSpeakerError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSpeakerError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = selectedSpeakerName.ifEmpty { "请选择声音" },
                        modifier = Modifier.weight(1f),
                        color = if (isSpeakerError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isSpeakerError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                // 声音选择错误提示
                if (isSpeakerError) {
                    Text(
                        text = "请选择一个声音",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                AnimatedVisibility(
                    visible = speakerDropdownExpanded,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    val configuration = LocalConfiguration.current
                    val menuMaxHeight = 0.7 * configuration.screenHeightDp.dp
                    val menuMaxWidth = 0.8 * configuration.screenWidthDp.dp

                    DropdownMenu(
                        expanded = speakerDropdownExpanded,
                        onDismissRequest = { onSpeakerExpandedChange(false) },
                        modifier = Modifier
                            .widthIn(max = menuMaxWidth)
                            .heightIn(max = menuMaxHeight),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        speakers.forEach { speakerInfo ->
                            DropdownMenuItem(
                                text = { Text(speakerInfo.name) },
                                onClick = { onSpeakerSelect(speakerInfo) },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // 情感朗读开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.emotional_speech_switch),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.Switch(
                    checked = isEmotional,
                    onCheckedChange = onIsEmotionalChange,
                    thumbContent = if (isEmotional) {
                        {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VolcengineTTSPreview() {
    VolcengineTTSTheme {
        VolcengineTTSUI()
    }
}


@Composable
fun TTSSaveSettingsButton(
    onClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = stringResource(id = R.string.save_settings),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}