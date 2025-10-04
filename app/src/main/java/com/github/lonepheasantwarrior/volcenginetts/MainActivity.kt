package com.github.lonepheasantwarrior.volcenginetts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import android.app.Application
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.github.lonepheasantwarrior.volcenginetts.ui.theme.VolcengineTTSTheme
import com.github.lonepheasantwarrior.volcenginetts.ui.theme.SynthButtonLight
import com.github.lonepheasantwarrior.volcenginetts.ui.theme.SynthButtonDark
import com.github.lonepheasantwarrior.volcenginetts.ui.theme.SaveButtonLight
import com.github.lonepheasantwarrior.volcenginetts.ui.theme.SaveButtonDark

class MainActivity : ComponentActivity() {
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
}

// 数据类 - 表示声音信息
data class SpeakerInfo(
    val name: String,  // 声音名称，如"甜美桃子"、"灿灿 / Shiny"
    val id: String     // 声音ID，如"zh_female_tianmeitaozi_mars_bigtts"
)

// ViewModel 类 - 负责状态管理和业务逻辑
class VolcengineTTSViewModel(application: Application) : AndroidViewModel(application) {
    // 应用配置状态
    var appId by mutableStateOf(" ")
    var token by mutableStateOf(" ")
    var textToSynthesize by mutableStateOf("")

    // UI 交互状态
    var selectedScene by mutableStateOf("")
    var selectedSpeakerId by mutableStateOf("") // 存储选中的声音ID
    var selectedSpeakerName by mutableStateOf("") // 存储选中的声音名称

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

    fun saveSettings() {
        // 保存设置的业务逻辑
        // 实际应用中这里会调用存储API
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
        // 配置输入
        TTSConfigurationInputs(
            appId = viewModel.appId,
            token = viewModel.token,
            onAppIdChange = { viewModel.appId = it },
            onTokenChange = { viewModel.token = it }
        )
        
        // 场景选择器
        TTSSceneSelector(
            selectedScene = viewModel.selectedScene,
            sceneCategories = sceneCategories,
            isExpanded = sceneDropdownExpanded,
            onExpandedChange = { sceneDropdownExpanded = it },
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
            }
        )
        
        // 声音选择器
        TTSSpeakerSelector(
            selectedSpeakerName = viewModel.selectedSpeakerName,
            speakers = filteredSpeakers,
            isExpanded = speakerDropdownExpanded,
            onExpandedChange = { speakerDropdownExpanded = it },
            onSpeakerSelect = { speakerInfo ->
                viewModel.selectedSpeakerName = speakerInfo.name
                viewModel.selectedSpeakerId = speakerInfo.id
                speakerDropdownExpanded = false
            }
        )

        // 保存设置按钮
        Button(
            onClick = { viewModel.saveSettings() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.save_settings))
        }
        
        // 待合成文本输入框
        TextSynthesisInput(
            text = viewModel.textToSynthesize,
            onTextChange = { viewModel.textToSynthesize = it }
        )

        // 朗读按钮
        SynthesisButton(onClick = { viewModel.synthesizeSpeech() })
    }
}

@Composable
fun TTSConfigurationInputs(
    appId: String,
    token: String,
    onAppIdChange: (String) -> Unit,
    onTokenChange: (String) -> Unit
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
        }
    }
}

@Composable
fun TTSSceneSelector(
    selectedScene: String,
    sceneCategories: Array<String>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSceneSelect: (String) -> Unit
) {
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
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Button(
                onClick = { onExpandedChange(true) },
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
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            val configuration = LocalConfiguration.current
            val menuMaxHeight = 0.7 * configuration.screenHeightDp.dp
            val menuMaxWidth = 0.8 * configuration.screenWidthDp.dp
            
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandedChange(false) },
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
}

@Composable
fun TTSSpeakerSelector(
    selectedSpeakerName: String,
    speakers: List<SpeakerInfo>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSpeakerSelect: (SpeakerInfo) -> Unit
) {
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
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Button(
                onClick = { onExpandedChange(true) },
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
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            val configuration = LocalConfiguration.current
            val menuMaxHeight = 0.7 * configuration.screenHeightDp.dp
            val menuMaxWidth = 0.8 * configuration.screenWidthDp.dp
            
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandedChange(false) },
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
}

@Composable
fun TextSynthesisInput(text: String, onTextChange: (String) -> Unit) {
    OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text(stringResource(id = R.string.input_text_for_speak)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
}

@Composable
fun SynthesisButton(onClick: () -> Unit) {
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
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null
            )
            Text(
                text = stringResource(id = R.string.button_speak),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
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