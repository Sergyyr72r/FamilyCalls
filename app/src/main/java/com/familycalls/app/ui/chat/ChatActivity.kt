package com.familycalls.app.ui.chat

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.familycalls.app.R
import com.familycalls.app.data.model.Message
import com.familycalls.app.data.model.MessageType
import com.familycalls.app.data.repository.ChatRepository
import com.familycalls.app.data.repository.StorageRepository
import com.familycalls.app.databinding.ActivityChatBinding
import com.familycalls.app.databinding.ItemMessageBinding
import com.familycalls.app.databinding.ItemMessageImageBinding
import com.familycalls.app.databinding.ItemMessageVideoBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val chatRepository = ChatRepository()
    private val storageRepository = StorageRepository()
    private lateinit var prefs: SharedPreferences
    private var currentUserId: String = ""
    private var contactId: String = ""
    private var contactName: String = ""
    private val messagesAdapter = MessagesAdapter()
    private val REQUEST_CODE_IMAGE = 100
    private val REQUEST_CODE_VIDEO = 101
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = getSharedPreferences("FamilyCalls", MODE_PRIVATE)
        currentUserId = prefs.getString("userId", "") ?: ""
        contactId = intent.getStringExtra("contactId") ?: ""
        contactName = intent.getStringExtra("contactName") ?: ""
        
        supportActionBar?.title = contactName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerViewMessages.adapter = messagesAdapter
        
        loadMessages()
        
        binding.btnSend.setOnClickListener {
            sendTextMessage()
        }
        
        binding.btnAttach.setOnClickListener {
            showMediaOptions()
        }
    }
    
    private fun loadMessages() {
        lifecycleScope.launch {
            chatRepository.getMessages(currentUserId, contactId).collectLatest { messages ->
                messagesAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }
    }
    
    private fun sendTextMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        
        val message = Message(
            senderId = currentUserId,
            receiverId = contactId,
            text = text,
            type = MessageType.TEXT
        )
        
        lifecycleScope.launch {
            val result = chatRepository.sendMessage(message)
            if (result.isSuccess) {
                binding.etMessage.text?.clear()
            } else {
                Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showMediaOptions() {
        val options = arrayOf("Image", "Video")
        AlertDialog.Builder(this)
            .setTitle("Select Media")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImage()
                    1 -> pickVideo()
                }
            }
            .show()
    }
    
    private fun pickImage() {
        if (checkStoragePermission()) {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_IMAGE)
        }
    }
    
    private fun pickVideo() {
        if (checkStoragePermission()) {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "video/*"
            }
            startActivityForResult(intent, REQUEST_CODE_VIDEO)
        }
    }
    
    private fun checkStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        return if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 200)
            false
        } else {
            true
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        
        val uri = data.data ?: return
        
        when (requestCode) {
            REQUEST_CODE_IMAGE -> uploadAndSendImage(uri)
            REQUEST_CODE_VIDEO -> uploadAndSendVideo(uri)
        }
    }
    
    private fun uploadAndSendImage(uri: Uri) {
        binding.btnAttach.isEnabled = false
        Snackbar.make(binding.root, "Uploading image...", Snackbar.LENGTH_INDEFINITE).show()
        
        lifecycleScope.launch {
            val result = storageRepository.uploadImage(uri, currentUserId)
            if (result.isSuccess) {
                val imageUrl = result.getOrNull() ?: return@launch
                val message = Message(
                    senderId = currentUserId,
                    receiverId = contactId,
                    imageUrl = imageUrl,
                    type = MessageType.IMAGE
                )
                chatRepository.sendMessage(message)
                binding.btnAttach.isEnabled = true
            } else {
                Toast.makeText(this@ChatActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                binding.btnAttach.isEnabled = true
            }
        }
    }
    
    private fun uploadAndSendVideo(uri: Uri) {
        binding.btnAttach.isEnabled = false
        Snackbar.make(binding.root, "Uploading video...", Snackbar.LENGTH_INDEFINITE).show()
        
        lifecycleScope.launch {
            val result = storageRepository.uploadVideo(uri, currentUserId)
            if (result.isSuccess) {
                val videoUrl = result.getOrNull() ?: return@launch
                val message = Message(
                    senderId = currentUserId,
                    receiverId = contactId,
                    videoUrl = videoUrl,
                    type = MessageType.VIDEO
                )
                chatRepository.sendMessage(message)
                binding.btnAttach.isEnabled = true
            } else {
                Toast.makeText(this@ChatActivity, "Failed to upload video", Toast.LENGTH_SHORT).show()
                binding.btnAttach.isEnabled = true
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    inner class MessagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var messages = listOf<Message>()
        
        fun submitList(newList: List<Message>) {
            messages = newList
            notifyDataSetChanged()
        }
        
        override fun getItemViewType(position: Int): Int {
            return when (messages[position].type) {
                MessageType.TEXT -> 0
                MessageType.IMAGE -> 1
                MessageType.VIDEO -> 2
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 -> {
                    val binding = ItemMessageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    TextMessageViewHolder(binding)
                }
                1 -> {
                    val binding = ItemMessageImageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    ImageMessageViewHolder(binding)
                }
                2 -> {
                    val binding = ItemMessageVideoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    VideoMessageViewHolder(binding)
                }
                else -> throw IllegalArgumentException("Unknown view type")
            }
        }
        
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = messages[position]
            val isSent = message.senderId == currentUserId
            
            when (holder) {
                is TextMessageViewHolder -> holder.bind(message, isSent)
                is ImageMessageViewHolder -> holder.bind(message, isSent)
                is VideoMessageViewHolder -> holder.bind(message, isSent)
            }
        }
        
        override fun getItemCount() = messages.size
        
        inner class TextMessageViewHolder(
            private val binding: ItemMessageBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(message: Message, isSent: Boolean) {
                binding.tvMessage.text = message.text
                binding.root.layoutParams = (binding.root.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    if (isSent) {
                        setMargins(100, 8, 8, 8)
                    } else {
                        setMargins(8, 8, 100, 8)
                    }
                }
            }
        }
        
        inner class ImageMessageViewHolder(
            private val binding: ItemMessageImageBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(message: Message, isSent: Boolean) {
                Glide.with(binding.root.context)
                    .load(message.imageUrl)
                    .into(binding.ivImage)
                binding.root.layoutParams = (binding.root.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    if (isSent) {
                        setMargins(100, 8, 8, 8)
                    } else {
                        setMargins(8, 8, 100, 8)
                    }
                }
            }
        }
        
        inner class VideoMessageViewHolder(
            private val binding: ItemMessageVideoBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(message: Message, isSent: Boolean) {
                // For video, we'll show a thumbnail or play button
                binding.tvVideoUrl.text = "Video message"
                binding.root.layoutParams = (binding.root.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    if (isSent) {
                        setMargins(100, 8, 8, 8)
                    } else {
                        setMargins(8, 8, 100, 8)
                    }
                }
            }
        }
    }
}

