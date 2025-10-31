package com.teka.angaahewa.modules.chat_module

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import com.teka.angaahewa.ui.theme.TextSizeXXLarge
import com.teka.angaahewa.utils.ui_components.CustomTopAppBar
import com.teka.tsela.modules.chat_module.ChatMessage
import com.teka.tsela.modules.chat_module.ChatSession
import com.teka.tsela.modules.chat_module.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Track keyboard visibility
    val windowInsets = WindowInsets.ime
    val density = LocalDensity.current
    val keyboardHeight by remember {
        derivedStateOf {
            windowInsets.getBottom(density)
        }
    }
    val isKeyboardVisible = keyboardHeight > 0

    // Camera URI state
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera and gallery launchers (keeping your existing implementation)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, photoUri!!))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri!!)
                }
                viewModel.addImage(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            coroutineScope.launch {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    viewModel.addImage(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }

            photoUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            photoUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.currentSession?.messages?.size) {
        if (uiState.currentSession?.messages?.isNotEmpty() == true) {
            delay(100)
            listState.animateScrollToItem(uiState.currentSession!!.messages.size - 1)
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Handle error
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content with proper window insets handling
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // Compact top bar when keyboard is visible
                if (isKeyboardVisible) {
                    CompactTopAppBar(
                        title = "Angaa AI",
                        onMenuClick = { viewModel.toggleSidebar() },
                        onNewChatClick = { viewModel.createNewChatSession() },
                        onDismissKeyboard = { focusManager.clearFocus() }
                    )
                } else {
                    CustomTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.toggleSidebar() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open chat history",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Angaa AI",
                                        fontSize = TextSizeXXLarge,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (uiState.currentSession != null) {
                                        Text(
                                            text = uiState.currentSession!!.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.createNewChatSession() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New chat"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            bottomBar = {
                Column {
                    // Selected images preview
                    SelectedImagesPreview(
                        images = uiState.selectedImages,
                        onRemoveImage = viewModel::removeImage,
                        onClearAll = viewModel::clearSelectedImages
                    )

                    // Input section
                    ChatInputSection(
                        inputText = uiState.inputText,
                        isSending = uiState.isSending,
                        hasSelectedImages = uiState.selectedImages.isNotEmpty(),
                        onInputChange = viewModel::updateInputText,
                        onSendMessage = {
                            viewModel.sendMessage(uiState.inputText)
                            focusManager.clearFocus()
                        },
                        onCameraClick = {
                            when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    }

                                    photoUri = context.contentResolver.insert(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    )

                                    photoUri?.let { uri ->
                                        cameraLauncher.launch(uri)
                                    }
                                }
                                else -> {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        onGalleryClick = {
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            // This is crucial for proper keyboard handling
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            if (uiState.currentSession == null) {
                WelcomeContent(
                    onCreateChat = { viewModel.createNewChatSession() },
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                ChatContent(
                    session = uiState.currentSession!!,
                    listState = listState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Sidebar overlay
        ChatSidebar(
            isVisible = uiState.showSidebar,
            chatSessions = uiState.chatSessions,
            onSessionClick = viewModel::selectChatSession,
            onNewChatClick = {
                viewModel.createNewChatSession()
            },
            onDeleteSession = viewModel::deleteChatSession,
            onRenameSession = viewModel::renameChatSession,
            onDismiss = { viewModel.toggleSidebar() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactTopAppBar(
    title: String,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onDismissKeyboard: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open chat history",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Row {
                IconButton(
                    onClick = onNewChatClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New chat",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDismissKeyboard,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Hide keyboard",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun RichMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    RichText(
        modifier = modifier
    ) {
        Markdown(content = markdown)
    }
}

// Updated MessageItem to use Markdown rendering
@Composable
private fun MessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        if (message.isFromUser) {
            // User message (keep as is)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                // Images if present
                if (message.images.isNotEmpty()) {
                    MessageImagesGrid(
                        images = message.images,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Text message if present
                if (message.content.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        } else {
            // AI message with Markdown support
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI Avatar
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Surface(
                    modifier = Modifier.widthIn(max = 300.dp),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (message.isLoading) {
                        // Typing indicator
                        TypingIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        // Use one of the markdown solutions here
                        RichMarkdownText(
                            markdown = message.content,
                            modifier = Modifier.padding(16.dp),
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Alternative: Use SimpleMarkdownText for lightweight solution
                        // SimpleMarkdownText(
                        //     markdown = message.content,
                        //     modifier = Modifier.padding(16.dp),
                        //     style = MaterialTheme.typography.bodyMedium,
                        //     color = MaterialTheme.colorScheme.onSurfaceVariant
                        // )

                        // Alternative: Use RichMarkdownText for full features
                        // RichMarkdownText(
                        //     markdown = message.content,
                        //     modifier = Modifier.padding(16.dp)
                        // )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedImagesPreview(
    images: List<Bitmap>,
    onRemoveImage: (Int) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = images.isNotEmpty(),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${images.size} image${if (images.size > 1) "s" else ""} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = onClearAll,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = images,
                        key = { index, _ -> index }
                    ) { index, bitmap ->
                        SelectedImageItem(
                            bitmap = bitmap,
                            onRemove = { onRemoveImage(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedImageItem(
    bitmap: Bitmap,
    onRemove: () -> Unit
) {
    Box {
        Card(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Remove button
        Surface(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-8).dp)
                .size(24.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            shadowElevation = 4.dp
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.onError
            )
        }
    }
}

@Composable
private fun WelcomeContent(
    onCreateChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // AI Assistant Icon
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Welcome to Angaa AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "I can help with questions, analyze images, assist with coding, creative writing, and much more. Start a conversation to get started!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onCreateChat,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start New Chat")
            }

            // Feature highlights
            Column(
                modifier = Modifier.padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.QuestionAnswer,
                    title = "Ask Questions",
                    description = "Get answers on any topic"
                )
                FeatureItem(
                    icon = Icons.Default.Image,
                    title = "Image Analysis",
                    description = "Upload and discuss images"
                )
                FeatureItem(
                    icon = Icons.Default.Code,
                    title = "Coding Help",
                    description = "Programming assistance and debugging"
                )
                FeatureItem(
                    icon = Icons.Default.Create,
                    title = "Creative Writing",
                    description = "Stories, essays, and content creation"
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatContent(
    session: ChatSession,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = session.messages,
            key = { it.id }
        ) { message ->
            MessageItem(
                message = message,
                modifier = Modifier.animateItem()
            )
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessageItem2(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        if (message.isFromUser) {
            // User message
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                // Images if present
                if (message.images.isNotEmpty()) {
                    MessageImagesGrid(
                        images = message.images,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Text message if present
                if (message.content.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        } else {
            // AI message
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI Avatar
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Surface(
                    modifier = Modifier.widthIn(max = 300.dp),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (message.isLoading) {
                        // Typing indicator
                        TypingIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageImagesGrid(
    images: List<Bitmap>,
    modifier: Modifier = Modifier
) {
    val rows = images.chunked(2) // Show 2 images per row

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { rowImages ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowImages.forEach { bitmap ->
                    Card(
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Message image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Fill remaining space if odd number of images
                if (rowImages.size == 1) {
                    Spacer(modifier = Modifier.size(120.dp))
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AI is typing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_scale"
            )

            Surface(
                modifier = Modifier.size((4 * scale).dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            ) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputSection(
    inputText: String,
    isSending: Boolean,
    hasSelectedImages: Boolean,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    var showAttachmentOptions by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column {
            // Simple attachment options row
            AnimatedVisibility(
                visible = showAttachmentOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showAttachmentOptions = false
                            onCameraClick()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera")
                    }

                    OutlinedButton(
                        onClick = {
                            showAttachmentOptions = false
                            onGalleryClick()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery")
                    }
                }
            }

            // Main input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Simple attachment button
                IconButton(
                    onClick = { showAttachmentOptions = !showAttachmentOptions },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (showAttachmentOptions) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Attach files",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (hasSelectedImages) "Describe or ask about the images..." else "Type your message...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if ((inputText.isNotBlank() || hasSelectedImages) && !isSending) {
                                onSendMessage()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                FloatingActionButton(
                    onClick = onSendMessage,
                    modifier = Modifier.size(48.dp),
                    containerColor = if ((inputText.isNotBlank() || hasSelectedImages) && !isSending) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = if ((inputText.isNotBlank() || hasSelectedImages) && !isSending) 6.dp else 2.dp
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message",
                            tint = if ((inputText.isNotBlank() || hasSelectedImages)) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSidebar(
    isVisible: Boolean,
    chatSessions: List<ChatSession>,
    onSessionClick: (ChatSession) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(300, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Overlay
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
                color = Color.Black.copy(alpha = 0.5f)
            ) {}

            // Sidebar content
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Sidebar header
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chat History",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Row {
                                IconButton(onClick = onNewChatClick) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New chat"
                                    )
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close sidebar"
                                    )
                                }
                            }
                        }
                    }

                    // Chat sessions list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = chatSessions.sortedByDescending { it.lastUpdated },
                            key = { it.id }
                        ) { session ->
                            ChatSessionItem(
                                session = session,
                                onClick = { onSessionClick(session) },
                                onDelete = { onDeleteSession(session.id) },
                                onRename = { newTitle -> onRenameSession(session.id, newTitle) }
                            )
                        }
                    }

                    // Footer
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "${chatSessions.size} conversation${if (chatSessions.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSessionItem(
    session: ChatSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (session.isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (session.isActive) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (session.isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Text(
                    text = formatTimestamp(session.lastUpdated),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (session.isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (session.messages.isNotEmpty()) {
                    val lastMessage = session.messages.lastOrNull()
                    val preview = when {
                        lastMessage?.images?.isNotEmpty() == true && lastMessage.content.isNotBlank() ->
                            "🖼️ ${lastMessage.content.take(30)}"
                        lastMessage?.images?.isNotEmpty() == true ->
                            "🖼️ Image${if (lastMessage.images.size > 1) "s" else ""}"
                        else -> lastMessage?.content?.take(50) ?: ""
                    }

                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (session.isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(18.dp),
                        tint = if (session.isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(session.title) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Chat Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRename(newTitle.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}