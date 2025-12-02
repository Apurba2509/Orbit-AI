package com.apurba2509.chatbot

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

// --- 1. SONIC ELEGANCE PALETTE ---
val GeminiBlack = Color(0xFF050508) // Deep Midnight Black
val GeminiSurface = Color(0xFF16161E) // Rich Dark Gray
val OrbitBlue = Color(0xFF5379F6)     // Vibrant Electric Blue
val ElectricCyan = Color(0xFF00E5FF) // Bright Cyan Accent
val HotPink = Color(0xFFFF0080)      // Magenta Accent
val NebulaPurple = Color(0xFF6200EA) // Vivid Purple
val GlassSurface = Color(0xFF180E24).copy(alpha = 0.7f) // Translucent Dark Violet
val CodeBackground = Color(0xFF1E1E24) // Darker background for code blocks
val TextWhite = Color(0xFFFFFFFF)
val TextGrey = Color(0xFFB0A8C0)
val CodePink = Color(0xFFFF7EB3)

// --- 2. MODELS ---
enum class GeminiAgent(val displayName: String, val modelName: String, val icon: ImageVector) {
    GEMINI_FLASH("Orbit Flash", "gemini-2.5-flash", Icons.Default.Bolt),
    GEMINI_3_PRO("Orbit Pro", "gemini-3-pro-preview", Icons.Default.AutoAwesome)
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val image: Bitmap? = null,
    val modelName: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = GeminiBlack,
                    surface = Color.Transparent,
                    primary = NebulaPurple,
                    onBackground = TextWhite,
                    onSurface = TextWhite
                ),
                typography = Typography(
                    titleLarge = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    bodyLarge = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                )
            ) {
                OrbitApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitApp() {
    val context = LocalContext.current
    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showAttachMenu by remember { mutableStateOf(false) }

    var userName by remember { mutableStateOf("Traveler") }
    var showProfileDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedAgent by remember { mutableStateOf(GeminiAgent.GEMINI_FLASH) }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    // Safe API Key access
    val apiKey = try { BuildConfig.API_KEY } catch (e: Exception) { "" }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            selectedImage = bitmap
            showAttachMenu = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedImage = bitmap
            showAttachMenu = false
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF12081F).copy(alpha = 0.95f),
                drawerContentColor = TextWhite
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("ORBIT MENU", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge, color = ElectricCyan)
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                NavigationDrawerItem(
                    label = { Text("Clear Conversation") },
                    selected = false,
                    onClick = {
                        messages = emptyList()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Delete, null) },
                    modifier = Modifier.padding(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        showProfileDialog = true
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.padding(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.sonicBackground(),
            topBar = {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, "Menu", tint = TextWhite)
                    }

                    Text(
                        text = "Orbit AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GlassSurface)
                            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape)
                            .clickable { tempName = userName; showProfileDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userName.take(1).uppercase(), color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
            ) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier
                                .size(100.dp)
                                .background(Brush.linearGradient(listOf(NebulaPurple, ElectricCyan)), CircleShape)
                                .alpha(0.4f)
                            )
                            Icon(Icons.Default.AutoAwesome, null, tint = ElectricCyan, modifier = Modifier.size(64.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Hello, $userName",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "System Ready",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = ElectricCyan,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { message ->
                        MessageItem(message)
                    }
                    if (isLoading) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                                    label = "alpha"
                                )
                                Box(modifier = Modifier.size(8.dp).alpha(alpha).background(ElectricCyan, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Orbit is thinking...", color = ElectricCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(), // Removed navigationBarsPadding here to avoid double spacing
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(visible = selectedImage != null) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(GlassSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        ) {
                            selectedImage?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Selected Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.padding(8.dp).size(80.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                            IconButton(onClick = { selectedImage = null }, modifier = Modifier.align(Alignment.TopEnd)) {
                                Icon(Icons.Default.Close, "Remove", tint = TextWhite)
                            }
                        }
                    }

                    // --- INPUT PILL (Restored Solid Style) ---
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = GeminiSurface, // Solid Dark Color
                        shape = RoundedCornerShape(32.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(modifier = Modifier.padding(bottom = 6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { expanded = true }
                                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        selectedAgent.displayName.replace("Orbit ", ""),
                                        color = ElectricCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        null,
                                        tint = TextGrey,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(GlassSurface)
                                ) {
                                    GeminiAgent.entries.forEach { agent ->
                                        DropdownMenuItem(
                                            text = { Text(agent.displayName, color = TextWhite) },
                                            onClick = { selectedAgent = agent; expanded = false },
                                            leadingIcon = { Icon(agent.icon, null, tint = ElectricCyan) }
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 120.dp)
                                .padding(top = 8.dp, bottom = 14.dp)
                            ) {
                                if (userInput.isEmpty()) {
                                    Text("Ask Orbit...", color = TextGrey, fontSize = 16.sp)
                                }
                                BasicTextField(
                                    value = userInput,
                                    onValueChange = { userInput = it },
                                    textStyle = TextStyle(color = TextWhite, fontSize = 16.sp, lineHeight = 24.sp, fontFamily = FontFamily.SansSerif),
                                    cursorBrush = SolidColor(ElectricCyan),
                                    modifier = Modifier.fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(modifier = Modifier.padding(bottom = 4.dp)) {
                                if (userInput.isNotBlank() || selectedImage != null) {
                                    IconButton(
                                        onClick = {
                                            val q = userInput
                                            val img = selectedImage

                                            messages = messages + ChatMessage(q, true, img)
                                            userInput = ""
                                            selectedImage = null
                                            isLoading = true

                                            scope.launch {
                                                try {
                                                    val model = GenerativeModel(selectedAgent.modelName, apiKey)
                                                    val inputContent = content {
                                                        if (img != null) image(img)
                                                        text(q)
                                                    }
                                                    val res = model.generateContent(inputContent)
                                                    messages = messages + ChatMessage(res.text ?: "", false)
                                                } catch (e: Exception) {
                                                    val errorMsg = if (e.localizedMessage?.contains("quota", ignoreCase = true) == true) {
                                                        "Quota limit reached. Switch to Flash!"
                                                    } else {
                                                        "Error: ${e.localizedMessage}"
                                                    }
                                                    messages = messages + ChatMessage(errorMsg, false)
                                                } finally { isLoading = false }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                Brush.linearGradient(listOf(NebulaPurple, ElectricCyan)),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = TextWhite, modifier = Modifier.size(22.dp))
                                    }
                                } else {
                                    IconButton(onClick = { showAttachMenu = true }) {
                                        Icon(Icons.Default.Add, null, tint = TextWhite)
                                    }
                                    DropdownMenu(
                                        expanded = showAttachMenu,
                                        onDismissRequest = { showAttachMenu = false },
                                        modifier = Modifier.background(GlassSurface)
                                    ) {
                                        DropdownMenuItem(text = { Text("Camera", color = TextWhite) }, onClick = { cameraLauncher.launch(); showAttachMenu = false }, leadingIcon = { Icon(Icons.Default.CameraAlt, null, tint = TextWhite) })
                                        DropdownMenuItem(text = { Text("Gallery", color = TextWhite) }, onClick = { galleryLauncher.launch("image/*"); showAttachMenu = false }, leadingIcon = { Icon(Icons.Default.Image, null, tint = TextWhite) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        Dialog(onDismissRequest = { showProfileDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = GlassSurface, border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Identity", style = MaterialTheme.typography.titleLarge, color = TextWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = TextGrey,
                            cursorColor = ElectricCyan
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showProfileDialog = false }) { Text("Cancel", color = TextGrey) }
                        Button(
                            onClick = { if (tempName.isNotBlank()) { userName = tempName; showProfileDialog = false } },
                            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple)
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(Icons.Default.AutoAwesome, null, tint = ElectricCyan, modifier = Modifier.size(20.dp).padding(top = 4.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if(isUser) Alignment.End else Alignment.Start) {
            if (isUser && message.image != null) {
                Image(
                    bitmap = message.image.asImageBitmap(),
                    contentDescription = "Sent Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                )
            }

            Surface(
                color = if (isUser) Color(0xFF2A1F3D).copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
                border = if (isUser) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) else null,
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                SelectionContainer {
                    if (isUser) {
                        Text(message.text, color = TextWhite, modifier = Modifier.padding(14.dp), fontSize = 16.sp)
                    } else {
                        MarkdownText(message.text)
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val lines = text.split("\n")
            var inCodeBlock = false

            for (index in lines.indices) {
                val line = lines[index]
                if (line.trim().startsWith("```")) {
                    inCodeBlock = !inCodeBlock
                    continue
                }

                if (inCodeBlock) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = CodeBackground, color = CodePink)) {
                        append(line + "\n")
                    }
                } else {
                    when {
                        line.startsWith("### ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ElectricCyan)) {
                                append(line.removePrefix("### ") + "\n")
                            }
                        }
                        line.startsWith("## ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ElectricCyan)) {
                                append(line.removePrefix("## ") + "\n")
                            }
                        }
                        line.startsWith("# ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = ElectricCyan)) {
                                append(line.removePrefix("# ") + "\n")
                            }
                        }
                        line.trim().startsWith("* ") || line.trim().startsWith("- ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ElectricCyan)) {
                                append("• ")
                            }
                            appendFormattedLine(line.trim().substring(2))
                            append("\n")
                        }
                        else -> {
                            appendFormattedLine(line)
                            append("\n")
                        }
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        color = TextWhite,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFamily = FontFamily.SansSerif,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

fun androidx.compose.ui.text.AnnotatedString.Builder.appendFormattedLine(line: String) {
    val tokens = mutableListOf<MarkdownToken>()

    fun addTokens(regex: Regex, type: FormatType) {
        regex.findAll(line).forEach { result ->
            val range = result.range
            if (tokens.none { it.range.overlaps(range) }) {
                tokens.add(MarkdownToken(range, type, result.groupValues[1]))
            }
        }
    }

    addTokens(Regex("`([^`]+)`"), FormatType.CODE)
    addTokens(Regex("\\*\\*([^*]+)\\*\\*"), FormatType.BOLD)
    addTokens(Regex("\\*([^*]+)\\*"), FormatType.ITALIC)

    tokens.sortBy { it.range.first }

    var cursor = 0
    for (token in tokens) {
        if (token.range.first > cursor) {
            append(line.substring(cursor, token.range.first))
        }

        when (token.type) {
            FormatType.CODE -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = CodeBackground.copy(alpha = 0.8f), color = CodePink)) { append(token.text) }
            FormatType.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextWhite)) { append(token.text) }
            FormatType.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextWhite)) { append(token.text) }
        }
        cursor = token.range.last + 1
    }

    if (cursor < line.length) {
        append(line.substring(cursor))
    }
}

data class MarkdownToken(val range: IntRange, val type: FormatType, val text: String)
enum class FormatType { BOLD, ITALIC, CODE }
fun IntRange.overlaps(other: IntRange) = (first <= other.last && last >= other.first)

// --- VISIBLE ANIMATED BACKGROUND ---
fun Modifier.sonicBackground(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    this.drawBehind {
        val width = size.width
        val height = size.height

        drawRect(color = GeminiBlack)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricCyan.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(width * offsetX, height * 0.3f),
                radius = size.maxDimension * 0.8f
            ),
            radius = size.maxDimension * 0.8f,
            center = Offset(width * offsetX, height * 0.3f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(HotPink.copy(alpha = 0.2f), Color.Transparent),
                center = Offset(width * (1 - offsetX), height * 0.7f),
                radius = size.maxDimension * 0.9f
            ),
            radius = size.maxDimension * 0.9f,
            center = Offset(width * (1 - offsetX), height * 0.7f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NebulaPurple.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(width * 0.5f, height * offsetY),
                radius = size.maxDimension * 1.0f
            ),
            radius = size.maxDimension * 1.0f,
            center = Offset(width * 0.5f, height * offsetY)
        )
    }
}