package com.teka.tsela.modules.chat_module

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val images: List<Bitmap> = emptyList() // Add images support
)

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
    val chat: Chat? = null
)

data class ChatUiState(
    val chatSessions: List<ChatSession> = emptyList(),
    val currentSession: ChatSession? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val showSidebar: Boolean = false,
    val inputText: String = "",
    val selectedImages: SnapshotStateList<Bitmap> = mutableStateListOf(), // Add selected images
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @Named("text_model") private val geminiTextModel: GenerativeModel,
    @Named("vision_model") private val geminiVisionModel: GenerativeModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        createNewChatSession()
    }

    fun createNewChatSession(title: String = "New Chat") {
        viewModelScope.launch {
            try {
                val chat = geminiTextModel.startChat()

                val newSession = ChatSession(
                    title = title,
                    isActive = true,
                    chat = chat
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        chatSessions = currentState.chatSessions.map { it.copy(isActive = false) } + newSession,
                        currentSession = newSession,
                        showSidebar = false,
                        selectedImages = mutableStateListOf(), // Clear selected images
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to create new chat session", e)
                _uiState.update { it.copy(error = "Failed to create new chat session: ${e.message}") }
            }
        }
    }

    fun selectChatSession(session: ChatSession) {
        _uiState.update { currentState ->
            currentState.copy(
                chatSessions = currentState.chatSessions.map {
                    it.copy(isActive = it.id == session.id)
                },
                currentSession = session,
                showSidebar = false,
                selectedImages = mutableStateListOf() // Clear selected images when switching sessions
            )
        }
    }

    fun deleteChatSession(sessionId: String) {
        _uiState.update { currentState ->
            val updatedSessions = currentState.chatSessions.filter { it.id != sessionId }
            val newCurrentSession = if (currentState.currentSession?.id == sessionId) {
                updatedSessions.firstOrNull()?.copy(isActive = true)
            } else {
                currentState.currentSession
            }

            currentState.copy(
                chatSessions = updatedSessions,
                currentSession = newCurrentSession
            )
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(showSidebar = !it.showSidebar) }
    }

    // Add image to selected images
    fun addImage(bitmap: Bitmap) {
        _uiState.update { currentState ->
            currentState.selectedImages.add(bitmap)
            currentState
        }
    }

    // Remove image from selected images
    fun removeImage(index: Int) {
        _uiState.update { currentState ->
            if (index in 0 until currentState.selectedImages.size) {
                currentState.selectedImages.removeAt(index)
            }
            currentState
        }
    }

    // Clear all selected images
    fun clearSelectedImages() {
        _uiState.update { currentState ->
            currentState.selectedImages.clear()
            currentState
        }
    }

    fun sendMessage(content: String, images: List<Bitmap> = emptyList()) {
        if ((content.isBlank() && images.isEmpty()) || _uiState.value.isSending) return

        viewModelScope.launch {
            val currentSession = _uiState.value.currentSession ?: return@launch

            // Use selected images if no images provided explicitly
            val imagesToSend = if (images.isNotEmpty()) images else _uiState.value.selectedImages.toList()

            // Add user message
            val userMessage = ChatMessage(
                content = content,
                isFromUser = true,
                images = imagesToSend
            )

            // Add loading AI message
            val loadingMessage = ChatMessage(
                content = "",
                isFromUser = false,
                isLoading = true
            )

            val updatedMessages = currentSession.messages + userMessage + loadingMessage
            val updatedSession = currentSession.copy(
                messages = updatedMessages,
                lastUpdated = System.currentTimeMillis(),
                title = if (currentSession.messages.isEmpty()) {
                    generateChatTitle(content, imagesToSend.isNotEmpty())
                } else currentSession.title
            )

            _uiState.update { currentState ->
                currentState.copy(
                    currentSession = updatedSession,
                    chatSessions = currentState.chatSessions.map { session ->
                        if (session.id == updatedSession.id) updatedSession else session
                    },
                    inputText = "",
                    selectedImages = mutableStateListOf(), // Clear selected images after sending
                    isSending = true,
                    error = null
                )
            }

            try {
                generateGeminiResponse(content, imagesToSend, currentSession.chat, updatedSession)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                handleGeminiError(e, updatedSession)
            }
        }
    }

    private suspend fun generateGeminiResponse(
        userMessage: String,
        images: List<Bitmap>,
        chat: Chat?,
        session: ChatSession
    ) {
        try {
            val stream = if (images.isNotEmpty()) {
                // Use vision model for image queries
                val inputContent = content {
                    images.forEach { bitmap ->
                        image(bitmap)
                    }
                    text(userMessage)
                }
                geminiVisionModel.generateContentStream(inputContent)
            } else {
                // Use regular text model with chat context
                if (chat == null) {
                    throw Exception("Chat session not initialized")
                }
                chat.sendMessageStream(userMessage)
            }

            var fullResponse = ""

            // Collect the streaming response
            stream.collect { chunk ->
                val chunkText = chunk.text ?: ""
                fullResponse += chunkText

                // Update UI with streaming response
                updateStreamingResponse(fullResponse, session.id)
            }

            // Final update to mark as completed
            finalizeResponse(
                fullResponse.ifEmpty { "I apologize, but I couldn't generate a response. Please try again." },
                session.id
            )

        } catch (e: Exception) {
            Log.e("ChatViewModel", "Gemini API error", e)
            val errorMessage = when {
                e.message?.contains("API_KEY", ignoreCase = true) == true ->
                    "API key error. Please check your Gemini API configuration."
                e.message?.contains("quota", ignoreCase = true) == true ->
                    "API quota exceeded. Please try again later."
                e.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
                    "Gemini service is temporarily unavailable. Please try again."
                else ->
                    "I encountered an error while processing your request. Please try again."
            }
            finalizeResponse(errorMessage, session.id)
        }
    }

    private fun updateStreamingResponse(partialResponse: String, sessionId: String) {
        _uiState.update { currentState ->
            val currentSession = currentState.currentSession ?: return@update currentState
            if (currentSession.id != sessionId) return@update currentState

            val messages = currentSession.messages.toMutableList()

            if (messages.isNotEmpty() && !messages.last().isFromUser) {
                // Update the last AI message with streaming content
                messages[messages.lastIndex] = messages.last().copy(
                    content = partialResponse,
                    isLoading = true
                )

                val updatedSession = currentSession.copy(messages = messages)

                currentState.copy(
                    currentSession = updatedSession,
                    chatSessions = currentState.chatSessions.map { session ->
                        if (session.id == updatedSession.id) updatedSession else session
                    }
                )
            } else {
                currentState
            }
        }
    }

    private fun finalizeResponse(response: String, sessionId: String) {
        _uiState.update { currentState ->
            val currentSession = currentState.currentSession ?: return@update currentState
            if (currentSession.id != sessionId) return@update currentState

            val messages = currentSession.messages.toMutableList()

            if (messages.isNotEmpty() && !messages.last().isFromUser) {
                // Finalize the last AI message
                messages[messages.lastIndex] = messages.last().copy(
                    content = response,
                    isLoading = false
                )

                val updatedSession = currentSession.copy(
                    messages = messages,
                    lastUpdated = System.currentTimeMillis()
                )

                currentState.copy(
                    currentSession = updatedSession,
                    chatSessions = currentState.chatSessions.map { session ->
                        if (session.id == updatedSession.id) updatedSession else session
                    },
                    isSending = false
                )
            } else {
                currentState.copy(isSending = false)
            }
        }
    }

    private fun handleGeminiError(error: Exception, session: ChatSession) {
        // Remove loading message on error
        val errorMessages = session.messages.dropLast(1)
        val errorSession = session.copy(
            messages = errorMessages,
            lastUpdated = System.currentTimeMillis()
        )

        _uiState.update { currentState ->
            currentState.copy(
                currentSession = errorSession,
                chatSessions = currentState.chatSessions.map { s ->
                    if (s.id == errorSession.id) errorSession else s
                },
                isSending = false,
                error = error.message ?: "Failed to send message"
            )
        }
    }

    private fun generateChatTitle(firstMessage: String, hasImages: Boolean): String {
        val prefix = if (hasImages) "🖼️ " else ""
        return prefix + firstMessage.take(30).trim().let {
            if (it.length < firstMessage.length) "$it..." else it
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun renameChatSession(sessionId: String, newTitle: String) {
        _uiState.update { currentState ->
            val updatedSessions = currentState.chatSessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(title = newTitle.take(50))
                } else session
            }

            currentState.copy(
                chatSessions = updatedSessions,
                currentSession = if (currentState.currentSession?.id == sessionId) {
                    currentState.currentSession?.copy(title = newTitle.take(50))
                } else currentState.currentSession
            )
        }
    }

    fun clearCurrentChatHistory() {
        viewModelScope.launch {
            val currentSession = _uiState.value.currentSession ?: return@launch

            try {
                // Create new chat instance for fresh context
                val newChat = geminiTextModel.startChat()

                val clearedSession = currentSession.copy(
                    messages = emptyList(),
                    chat = newChat,
                    lastUpdated = System.currentTimeMillis()
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        currentSession = clearedSession,
                        chatSessions = currentState.chatSessions.map { session ->
                            if (session.id == clearedSession.id) clearedSession else session
                        },
                        selectedImages = mutableStateListOf() // Clear selected images
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to clear chat history", e)
                _uiState.update { it.copy(error = "Failed to clear chat history: ${e.message}") }
            }
        }
    }

    fun retryLastMessage() {
        val currentSession = _uiState.value.currentSession ?: return
        val messages = currentSession.messages

        // Find the last user message
        val lastUserMessage = messages.lastOrNull { it.isFromUser }
        if (lastUserMessage != null && !_uiState.value.isSending) {
            sendMessage(lastUserMessage.content, lastUserMessage.images)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ChatViewModel", "ViewModel cleared")
    }
}