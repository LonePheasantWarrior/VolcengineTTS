package com.github.lonepheasantwarrior.volcenginetts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import android.app.Application
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.github.lonepheasantwarrior.volcenginetts.engine.SynthesisEngine
import com.github.lonepheasantwarrior.volcenginetts.ui.theme.VolcengineTTSTheme
import com.github.lonepheasantwarrior.volcenginetts.function.SettingsFunction

class MainActivity : ComponentActivity() {
    private val synthesisEngine: SynthesisEngine get() = (applicationContext as TTSApplication).synthesisEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VolcengineTTSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    VolcengineTTSUI(modifier = Modifier.padding(it))
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
    private val settingsFunction: SettingsFunction get() = (getApplication() as TTSApplication).settingsFunction

    // 应用配置状态
    var appId by mutableStateOf(" ")
    var token by mutableStateOf(" ")
    var serviceCluster by mutableStateOf("") //接口区域ID
    var textToSynthesize by mutableStateOf("")
    var isEmotional by mutableStateOf(false) // 感情朗读开关

    // UI 交互状态
    var selectedScene by mutableStateOf("")
    var selectedSpeakerId by mutableStateOf("") // 存储选中的声音ID
    var selectedSpeakerName by mutableStateOf("") // 存储选中的声音名称
    
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
     * 保存设置到持久化存储
     */
    fun saveSettings() {
        settingsFunction.saveSettings(appId, token, selectedSpeakerId, serviceCluster, isEmotional)
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
        }
        if (serviceCluster.isNotEmpty()) {
            serviceCluster = savedServiceCluster
        }
        isEmotional = savedIsEmotional
    }

    fun synthesizeSpeech() {
        // 语音合成的业务逻辑
        // 实际应用中这里会调用火山引擎API
    }
}

// UI 组件 - 负责呈现和用户交互
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolcengineTTSUI(modifier: Modifier = Modifier) {
    val viewModel: VolcengineTTSViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
            return VolcengineTTSViewModel(application) as T
        }
    })
    
    // 获取数据
    val sceneCategories = viewModel.getSceneCategories()
    
    // 初始化默认场景
    if (viewModel.selectedScene.isEmpty()) {
        viewModel.selectedScene = sceneCategories.firstOrNull() ?: ""
    }
    
    // 过滤当前场景的声音
    val filteredSpeakers = viewModel.filterSpeakersByScene(viewModel.selectedScene)
    
    // 初始化默认声音
    if (viewModel.selectedSpeakerId.isEmpty() && filteredSpeakers.isNotEmpty()) {
        viewModel.selectedSpeakerId = filteredSpeakers.first().id
        viewModel.selectedSpeakerName = filteredSpeakers.first().name
    }
    
    // 下拉菜单状态
    var sceneDropdownExpanded by remember { mutableStateOf(false) }
    var speakerDropdownExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 基础配置组 (appId, token, serviceCluster)
        TTSBasicConfigurationInputs(
            appId = viewModel.appId,
            token = viewModel.token,
            serviceCluster = viewModel.serviceCluster,
            onAppIdChange = { viewModel.appId = it },
            onTokenChange = { viewModel.token = it },
            onServiceClusterChange = { viewModel.serviceCluster = it }
        )
        
        // 场景和声音配置组 (selectedScene, selectedSpeakerName, isEmotional)
        TTSVoiceConfigurationInputs(
            selectedScene = viewModel.selectedScene,
            sceneCategories = sceneCategories,
            selectedSpeakerName = viewModel.selectedSpeakerName,
            speakers = filteredSpeakers,
            isEmotional = viewModel.isEmotional,
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
                    viewModel.selectedSpeakerId = newFilteredSpeakers.first().id
                    viewModel.selectedSpeakerName = newFilteredSpeakers.first().name
                } else {
                    viewModel.selectedSpeakerId = ""
                    viewModel.selectedSpeakerName = ""
                }
            },
            onSpeakerSelect = { speakerInfo ->
                viewModel.selectedSpeakerName = speakerInfo.name
                viewModel.selectedSpeakerId = speakerInfo.id
                speakerDropdownExpanded = false
            },
            onIsEmotionalChange = { viewModel.isEmotional = it }
        )

        // 保存设置按钮组
        TTSSaveSettingsButton(
            onClick = { viewModel.saveSettings() }
        )
    }
}

@Composable
fun TTSBasicConfigurationInputs(
    appId: String,
    token: String,
    serviceCluster: String,
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
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            )
            
            // Token 输入框
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text(stringResource(id = R.string.input_token)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            )

            // API资源ID 输入框
            OutlinedTextField(
                value = serviceCluster,
                onValueChange = onServiceClusterChange,
                label = { Text(stringResource(id = R.string.input_service_cluster)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
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
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                ) {
                    Button(
                        onClick = { onSceneExpandedChange(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
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
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                ) {
                    Button(
                        onClick = { onSpeakerExpandedChange(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = selectedSpeakerName,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
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