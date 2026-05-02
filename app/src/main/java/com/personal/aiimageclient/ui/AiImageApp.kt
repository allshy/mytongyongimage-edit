package com.personal.aiimageclient.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.personal.aiimageclient.data.history.HistoryEntity
import com.personal.aiimageclient.data.history.localUriList
import com.personal.aiimageclient.data.model.ImageOperation
import com.personal.aiimageclient.data.model.ImageProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private enum class AppTab(val label: String) {
    Generate("生成"),
    Edit("编辑"),
    History("历史"),
    Settings("设置")
}

@Composable
fun AiImageApp(viewModel: AppViewModel) {
    var tab by remember { mutableStateOf(AppTab.Generate) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == AppTab.Generate,
                    onClick = { tab = AppTab.Generate },
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                    label = { Text(AppTab.Generate.label) }
                )
                NavigationBarItem(
                    selected = tab == AppTab.Edit,
                    onClick = { tab = AppTab.Edit },
                    icon = { Icon(Icons.Default.Brush, contentDescription = null) },
                    label = { Text(AppTab.Edit.label) }
                )
                NavigationBarItem(
                    selected = tab == AppTab.History,
                    onClick = { tab = AppTab.History },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(AppTab.History.label) }
                )
                NavigationBarItem(
                    selected = tab == AppTab.Settings,
                    onClick = { tab = AppTab.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(AppTab.Settings.label) }
                )
            }
        }
    ) { padding ->
        when (tab) {
            AppTab.Generate -> GenerateScreen(viewModel, padding)
            AppTab.Edit -> EditScreen(viewModel, padding)
            AppTab.History -> HistoryScreen(viewModel, padding)
            AppTab.Settings -> SettingsScreen(viewModel, padding)
        }
    }
}

@Composable
private fun GenerateScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.setOperation(ImageOperation.TextToImage)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("图像生成", style = MaterialTheme.typography.headlineMedium)
            Text("选择 Provider、模型和参数，然后提交生成。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { ProviderControls(state, viewModel, forceOperation = ImageOperation.TextToImage) }
        item {
            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::setPrompt,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("提示词") },
                minLines = 4
            )
        }
        item { ParameterControls(state, viewModel) }
        item { RunPanel(state, onRun = viewModel::runGeneration) }
        item { ResultGrid(state.resultUris) }
    }
}

@Composable
private fun EditScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        viewModel.setInputImage(uri?.toString())
        viewModel.setOperation(ImageOperation.Inpaint)
    }
    val maskState = remember { MaskState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("图像编辑", style = MaterialTheme.typography.headlineMedium)
        ProviderControls(state, viewModel, forceOperation = null)
        OutlinedTextField(
            value = state.prompt,
            onValueChange = viewModel::setPrompt,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("编辑说明") },
            minLines = 3
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launcher.launch("image/*") }) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("选择图片")
            }
            OutlinedButton(onClick = { maskState.clear(); viewModel.setMask(null) }) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("清空蒙版")
            }
        }
        val selectedImageUri = state.inputImageUri
        if (selectedImageUri != null) {
            MaskCanvas(
                imageUri = selectedImageUri,
                maskState = maskState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        } else {
            EmptyImageBox("请选择一张图片")
        }
        ParameterControls(state, viewModel)
        RunPanel(
            state = state,
            onRun = {
                if (maskState.hasMask()) {
                    viewModel.setMask(maskState.saveMask(context, state.inputImageUri).toString())
                }
                viewModel.runGeneration()
            }
        )
        ResultGrid(state.resultUris)
    }
}

@Composable
private fun HistoryScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val history by viewModel.history.collectAsState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("生成历史", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = viewModel::clearHistory) { Text("清空") }
            }
        }
        if (history.isEmpty()) {
            item { EmptyImageBox("暂无历史") }
        } else {
            items(history) { HistoryCard(it) }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val state by viewModel.uiState.collectAsState()
    var openAi by remember(state.openAiKey) { mutableStateOf(state.openAiKey) }
    var fal by remember(state.falKey) { mutableStateOf(state.falKey) }
    var replicate by remember(state.replicateKey) { mutableStateOf(state.replicateKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)
        Text("API Key 只保存在本机加密存储中。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SecretField("OpenAI API Key", openAi) { openAi = it }
        SecretField("fal.ai API Key", fal) { fal = it }
        SecretField("Replicate API Token", replicate) { replicate = it }
        Button(onClick = { viewModel.updateKeys(openAi, fal, replicate) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("保存密钥")
        }
    }
}

@Composable
private fun ProviderControls(state: AppUiState, viewModel: AppViewModel, forceOperation: ImageOperation?) {
    val operation = forceOperation ?: state.operation
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EnumDropdown("Provider", state.provider, ImageProvider.entries.toList(), { it.label }, viewModel::setProvider)
        EnumDropdown(
            "操作",
            operation,
            forceOperation?.let { listOf(it) } ?: ImageOperation.entries.toList(),
            { it.label },
            viewModel::setOperation
        )
        val models = state.availableModels.ifEmpty { ModelCatalogFallback.modelsFor(state.provider) }
        ValueDropdown(
            label = "模型",
            value = state.modelId,
            values = models.map { it.id },
            labelFor = { id -> models.firstOrNull { it.id == id }?.label ?: id },
            onChange = viewModel::setModel
        )
    }
}

@Composable
private fun ParameterControls(state: AppUiState, viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ValueDropdown(
            label = "尺寸",
            value = state.size,
            values = listOf("1024x1024", "1024x1536", "1536x1024", "1024x768", "768x1024"),
            labelFor = { it },
            onChange = viewModel::setSize
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("数量 ${state.count}")
            Slider(
                value = state.count.toFloat(),
                onValueChange = { viewModel.setCount(it.roundToInt()) },
                valueRange = 1f..4f,
                steps = 2,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.seed,
                onValueChange = viewModel::setSeed,
                label = { Text("种子（可空）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            ValueDropdown(
                label = "质量",
                value = state.quality,
                values = listOf("auto", "low", "medium", "high"),
                labelFor = { it },
                onChange = viewModel::setQuality,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RunPanel(state: AppUiState, onRun: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRun, enabled = !state.isRunning, modifier = Modifier.fillMaxWidth()) {
            if (state.isRunning) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Image, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (state.isRunning) "处理中..." else "开始生成")
        }
        Text(state.status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ResultGrid(uris: List<String>) {
    if (uris.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("结果", style = MaterialTheme.typography.titleMedium)
        uris.forEach { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun HistoryCard(entity: HistoryEntity) {
    val date = remember(entity.createdAtMillis) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entity.createdAtMillis))
    }
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${entity.provider} / ${entity.modelId}", style = MaterialTheme.typography.titleMedium)
            Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(entity.prompt.ifBlank { "无提示词" }, maxLines = 3)
            entity.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            entity.localUriList().firstOrNull()?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun EmptyImageBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SecretField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    value: T,
    values: List<T>,
    labelFor: (T) -> String,
    onChange: (T) -> Unit
) {
    ValueDropdown(label, value, values, labelFor, onChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ValueDropdown(
    label: String,
    value: T,
    values: List<T>,
    labelFor: (T) -> String,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = labelFor(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { item ->
                DropdownMenuItem(
                    text = { Text(labelFor(item)) },
                    onClick = {
                        onChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

private object ModelCatalogFallback {
    fun modelsFor(provider: ImageProvider) = com.personal.aiimageclient.data.model.ModelCatalog.models
        .filter { it.provider == provider }
}

private class MaskState {
    private val strokes = mutableStateListOf<List<Offset>>()
    private var current = mutableListOf<Offset>()
    private var size: IntSize = IntSize.Zero

    fun setSize(newSize: IntSize) {
        size = newSize
    }

    fun begin(offset: Offset) {
        current = mutableListOf(offset)
        strokes += current.toList()
    }

    fun add(offset: Offset) {
        current += offset
        if (strokes.isNotEmpty()) strokes[strokes.lastIndex] = current.toList()
    }

    fun clear() {
        strokes.clear()
        current.clear()
    }

    fun hasMask(): Boolean = strokes.any { it.size > 1 }

    fun draw(scope: DrawScope) {
        strokes.forEach { stroke ->
            for (index in 1 until stroke.size) {
                scope.drawLineCompat(stroke[index - 1], stroke[index])
            }
        }
    }

    fun saveMask(context: Context, sourceImageUri: String?): Uri {
        val sourceSize = sourceImageUri?.let { readImageSize(context, it) }
        val width = sourceSize?.width ?: size.width.coerceAtLeast(512)
        val height = sourceSize?.height ?: size.height.coerceAtLeast(512)
        val scaleX = width.toFloat() / size.width.coerceAtLeast(1)
        val scaleY = height.toFloat() / size.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLACK)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            strokeWidth = 48f * minOf(scaleX, scaleY)
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }
        strokes.forEach { stroke ->
            for (index in 1 until stroke.size) {
                canvas.drawLine(
                    stroke[index - 1].x * scaleX,
                    stroke[index - 1].y * scaleY,
                    stroke[index].x * scaleX,
                    stroke[index].y * scaleY,
                    paint
                )
            }
        }
        val file = File(context.cacheDir, "mask-${UUID.randomUUID()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    private fun readImageSize(context: Context, uriString: String): IntSize? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                IntSize(options.outWidth, options.outHeight)
            } else {
                null
            }
        }.getOrNull()
    }
}

@Composable
private fun MaskCanvas(imageUri: String, maskState: MaskState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { maskState.begin(it) },
                    onDrag = { change, _ -> maskState.add(change.position) }
                )
            }
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(Modifier.fillMaxSize()) {
            maskState.setSize(IntSize(size.width.roundToInt(), size.height.roundToInt()))
            maskState.draw(this)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineCompat(start: Offset, end: Offset) {
    drawLine(
        color = Color.White.copy(alpha = 0.65f),
        start = start,
        end = end,
        strokeWidth = 48f,
        cap = StrokeCap.Round
    )
}
