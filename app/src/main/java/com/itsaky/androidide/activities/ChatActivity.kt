package com.itsaky.androidide.activities

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.itsaky.androidide.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ChatActivity : AppCompatActivity() {
    private data class ChatMessage(val role: String, val content: String)

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var messagesContainer: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var input: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private var thinkingView: TextView? = null

    private val functionUrl = "https://kgbnygrwhjzkodkiesbq.supabase.co/functions/v1/chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        findViewById<MaterialToolbar>(R.id.chatToolbar).setNavigationOnClickListener { finish() }
        messagesContainer = findViewById(R.id.messagesContainer)
        chatScroll = findViewById(R.id.chatScroll)
        input = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        sendButton.setOnClickListener { sendCurrentMessage() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage()
                true
            } else false
        }

        addMessage("assistant", getString(R.string.pilgrix_bot_welcome))
        renderMessages()
    }

    private fun sendCurrentMessage() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty() || thinkingView != null) return

        input.setText("")
        messages += ChatMessage("user", text)
        renderMessages()
        showThinking()

        lifecycleScope.launch {
            requestReply().onSuccess { reply ->
                hideThinking()
                messages += ChatMessage("assistant", reply)
                renderMessages()
            }.onFailure { error ->
                hideThinking()
                messages.removeLastOrNull()
                renderMessages()
                Toast.makeText(this@ChatActivity, error.message ?: "Could not reach PilgrixBot.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun requestReply(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(functionUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                // The chat Edge Function is intentionally configured as a public endpoint.\n            }

            val payload = JSONObject().apply {
                put("messages", JSONArray().apply {
                    messages.takeLast(24).forEach { message ->
                        put(JSONObject().apply {
                            put("role", message.role)
                            put("content", message.content)
                        })
                    }
                })
            }

            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            val json = runCatching { JSONObject(body) }.getOrNull()
            if (status !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException(json?.optString("error").takeUnless { it.isNullOrBlank() }
                        ?: "PilgrixBot returned HTTP $status.")
                )
            }

            val reply = json?.optString("reply").orEmpty().trim()
            if (reply.isEmpty()) return@withContext Result.failure(IllegalStateException("PilgrixBot returned an empty response."))
            Result.success(reply)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addMessage(role: String, content: String) {
        messages += ChatMessage(role, content)
    }

    private fun showThinking() {
        thinkingView = TextView(this).apply {
            text = getString(R.string.pilgrix_bot_thinking)
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            setPadding(16, 8, 16, 8)
        }
        messagesContainer.addView(thinkingView)
        scrollToBottom()
        sendButton.isEnabled = false
    }

    private fun hideThinking() {
        thinkingView?.let { messagesContainer.removeView(it) }
        thinkingView = null
        sendButton.isEnabled = true
    }

    private fun renderMessages() {
        messagesContainer.removeAllViews()
        messages.forEach { message ->
            val bubble = TextView(this).apply {
                text = message.content
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(18, 13, 18, 13)
                setLineSpacing(0f, 1.08f)
                background = GradientDrawable().apply {
                    cornerRadius = 22f
                    setColor(if (message.role == "user") Color.rgb(68, 92, 220) else Color.rgb(42, 46, 55))
                }
            }
            val params = LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.82f).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = if (message.role == "user") Gravity.END else Gravity.START
                topMargin = 8
                bottomMargin = 2
            }
            messagesContainer.addView(bubble, params)
        }
        scrollToBottom()
    }

    private fun scrollToBottom() {
        chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
    }
}