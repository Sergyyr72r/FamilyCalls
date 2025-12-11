package com.familycalls.app.ui.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.familycalls.app.R
import com.familycalls.app.databinding.ActivityVideoCallBinding
import com.familycalls.app.service.CallService
import com.familycalls.app.service.CallNotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import android.view.SurfaceView

class VideoCallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoCallBinding
    private lateinit var prefs: SharedPreferences
    private var currentUserId: String = ""
    private var contactId: String = ""
    private var contactName: String = ""
    private var contactPhone: String = ""
    private var isIncoming: Boolean = false
    private var isMuted: Boolean = false
    
    // Agora RTC Engine
    private var agoraEngine: RtcEngine? = null
    private val appId = "14db41c4c0ff4e96b4dd5972f5d6cfb4" // Updated App ID - no token required
    private var channelName: String = ""
    private var localUid: Int = 0
    
    private val db = FirebaseFirestore.getInstance()
    private val callsCollection = db.collection("calls")
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Wake up screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        binding = ActivityVideoCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        android.util.Log.d("VideoCallActivity", "onCreate - starting call activity")
        
        prefs = getSharedPreferences("FamilyCalls", MODE_PRIVATE)
        currentUserId = prefs.getString("userId", "") ?: ""
        contactId = intent.getStringExtra("contactId") ?: ""
        contactName = intent.getStringExtra("contactName") ?: ""
        contactPhone = intent.getStringExtra("contactPhone") ?: ""
        isIncoming = intent.getBooleanExtra("isIncoming", false)
        
        setupButtons()
        
        if (isIncoming) {
            // For incoming calls, show UI but do NOT initialize Agora or check permissions yet
            // Wait for user to explicitly tap Accept button
            showIncomingCallUI()
        } else {
            // For outgoing calls, start the call immediately
            startOutgoingCall()
            checkPermissions()
        }
    }
    
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            initializeAgora()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeAgora()
            } else {
                Toast.makeText(this, "Permissions required for video calls", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    // Agora RTC Engine initialization
    private fun initializeAgora() {
        try {
            // Validate App ID first - Agora App IDs are typically 32 characters (hex)
            if (appId == "YOUR_AGORA_APP_ID" || appId.isEmpty()) {
                android.util.Log.e("Agora", "Invalid App ID: $appId")
                runOnUiThread {
                    Toast.makeText(this, "Agora App ID not configured. Please add your App ID.", Toast.LENGTH_LONG).show()
                }
                return
            }
            
            // Verify App ID format (should be 32 hex characters)
            val trimmedAppId = appId.trim()
            if (trimmedAppId.length != 32 || !trimmedAppId.matches(Regex("[0-9a-fA-F]{32}"))) {
                android.util.Log.w("Agora", "App ID format warning: length=${trimmedAppId.length}, expected 32 hex characters")
                android.util.Log.w("Agora", "App ID: $trimmedAppId")
            }
            
            android.util.Log.d("Agora", "Initializing Agora with App ID: $trimmedAppId (length: ${trimmedAppId.length})")
            
            // Verify SDK classes are available
            try {
                val rtcEngineClass = RtcEngine::class.java
                android.util.Log.d("Agora", "RtcEngine class found: ${rtcEngineClass.name}")
            } catch (e: Exception) {
                android.util.Log.e("Agora", "RtcEngine class not found! SDK may not be loaded.", e)
                runOnUiThread {
                    Toast.makeText(this, "Agora SDK classes not found. Please sync Gradle and rebuild.", Toast.LENGTH_LONG).show()
                }
                return
            }
            
            // Create event handler first
            val eventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    android.util.Log.d("Agora", "onJoinChannelSuccess: channel=$channel, uid=$uid")
                    runOnUiThread {
                        binding.tvCallStatus.text = "Connected"
                        Toast.makeText(this@VideoCallActivity, "Joined channel successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    android.util.Log.d("Agora", "onUserJoined: uid=$uid")
                    runOnUiThread {
                        setupRemoteVideo(uid)
                        binding.tvCallStatus.text = "$contactName joined"
                    }
                }
                
                override fun onUserOffline(uid: Int, reason: Int) {
                    android.util.Log.d("Agora", "onUserOffline: uid=$uid, reason=$reason")
                    runOnUiThread {
                        binding.tvCallStatus.text = "$contactName left"
                    }
                }
                
                override fun onError(err: Int) {
                    android.util.Log.e("Agora", "onError: error code=$err")
                    runOnUiThread {
                        val errorMsg = when (err) {
                            101 -> "Invalid App ID or SDK initialization failed"
                            110 -> "Invalid token - App Certificate is enabled. Disable it in Agora Console or use token authentication."
                            -1 -> "General error"
                            -2 -> "Invalid App ID"
                            -3 -> "Invalid channel name"
                            -4 -> "Invalid token"
                            -5 -> "Token expired"
                            -7 -> "SDK not initialized"
                            -17 -> "Join channel failed"
                            else -> "Error code: $err"
                        }
                        Toast.makeText(this@VideoCallActivity, "Agora Error: $errorMsg", Toast.LENGTH_LONG).show()
                        binding.tvCallStatus.text = "Error: $errorMsg"
                    }
                }
            }
            
            // Create config with proper context
            val config = RtcEngineConfig().apply {
                mContext = applicationContext // Use application context
                mAppId = trimmedAppId // Use trimmed App ID
                mEventHandler = eventHandler
                // Note: LogConfig is not available in Agora SDK 4.2.0
                // Logging is handled via android.util.Log instead
            }
            
            android.util.Log.d("Agora", "Creating RtcEngine with config...")
            android.util.Log.d("Agora", "Context: ${config.mContext?.javaClass?.simpleName}")
            android.util.Log.d("Agora", "App ID: ${config.mAppId}")
            
            // Create Agora engine - must be called on main thread
            agoraEngine = try {
                val engine = RtcEngine.create(config)
                android.util.Log.d("Agora", "RtcEngine.create() returned: ${engine != null}")
                if (engine == null) {
                    android.util.Log.e("Agora", "RtcEngine.create() returned NULL - this usually means:")
                    android.util.Log.e("Agora", "1. Invalid App ID")
                    android.util.Log.e("Agora", "2. Native libraries not loaded")
                    android.util.Log.e("Agora", "3. SDK version mismatch")
                }
                engine
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e("Agora", "UnsatisfiedLinkError: Native library not found", e)
                android.util.Log.e("Agora", "Stack trace:", e)
                runOnUiThread {
                    Toast.makeText(this, """
                        Agora SDK native library error.
                        
                        Please:
                        1. Clean Project (Build → Clean)
                        2. Sync Gradle (File → Sync)
                        3. Rebuild Project (Build → Rebuild)
                        
                        Error: ${e.message}
                    """.trimIndent(), Toast.LENGTH_LONG).show()
                    binding.tvCallStatus.text = "SDK Library Error"
                }
                null
            } catch (e: IllegalArgumentException) {
                android.util.Log.e("Agora", "IllegalArgumentException: ${e.message}", e)
                runOnUiThread {
                    val errorMsg = e.message ?: "Unknown error"
                    Toast.makeText(this, """
                        Agora initialization failed.
                        
                        Error: $errorMsg
                        
                        Common causes:
                        - Invalid App ID
                        - SDK not properly integrated
                        - Missing native libraries
                    """.trimIndent(), Toast.LENGTH_LONG).show()
                    binding.tvCallStatus.text = "Init Error: $errorMsg"
                }
                null
            } catch (e: Exception) {
                android.util.Log.e("Agora", "Exception during engine creation: ${e.javaClass.simpleName}", e)
                android.util.Log.e("Agora", "Message: ${e.message}")
                android.util.Log.e("Agora", "Cause: ${e.cause?.message}")
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, """
                        Failed to create Agora engine
                        
                        Error: ${e.javaClass.simpleName}
                        Message: ${e.message}
                        
                        Check Logcat for details.
                    """.trimIndent(), Toast.LENGTH_LONG).show()
                    binding.tvCallStatus.text = "Init Error: ${e.message}"
                }
                null
            }
            
            if (agoraEngine == null) {
                android.util.Log.e("Agora", "Agora engine is NULL after creation attempt")
                android.util.Log.e("Agora", "App ID used: $appId")
                android.util.Log.e("Agora", "Please verify:")
                android.util.Log.e("Agora", "1. App ID is correct in Agora Console")
                android.util.Log.e("Agora", "2. SDK version matches (4.2.0)")
                android.util.Log.e("Agora", "3. Native libraries are included in APK")
                return
            }
            
            android.util.Log.d("Agora", "Engine created successfully, configuring...")
        
            // Enable video and audio
            agoraEngine?.enableVideo()
            agoraEngine?.enableAudio()
            
            android.util.Log.d("Agora", "Video and audio enabled")
            
            // Set video encoder configuration
            agoraEngine?.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x480,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                )
            )
            
            android.util.Log.d("Agora", "Video encoder configured")
            
            // Setup local video
            setupLocalVideo()
            
            android.util.Log.d("Agora", "Local video setup complete")
            
            // Join channel
            joinChannel()
            
        } catch (e: Exception) {
            android.util.Log.e("Agora", "Unexpected error during initialization", e)
            e.printStackTrace()
            runOnUiThread {
                val errorDetails = """
                    Failed to initialize Agora
                    
                    Error: ${e.javaClass.simpleName}
                    Message: ${e.message}
                    Cause: ${e.cause?.message ?: "Unknown"}
                    
                    Please check:
                    1. App ID is correct: $appId
                    2. Internet connection
                    3. Agora SDK is downloaded
                    4. Permissions are granted
                """.trimIndent()
                
                Toast.makeText(this, errorDetails, Toast.LENGTH_LONG).show()
                binding.tvCallStatus.text = "Error: ${e.message}"
            }
        }
    }
    
    private fun setupLocalVideo() {
        // Create SurfaceView for local video
        val surfaceView = SurfaceView(applicationContext)
        surfaceView.setZOrderMediaOverlay(true)
        binding.localVideoView.removeAllViews()
        binding.localVideoView.addView(surfaceView)
        
        agoraEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        agoraEngine?.startPreview()
    }
    
    private fun setupRemoteVideo(uid: Int) {
        // Create SurfaceView for remote video
        val surfaceView = SurfaceView(applicationContext)
        binding.remoteVideoView.removeAllViews()
        binding.remoteVideoView.addView(surfaceView)
        
        agoraEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }
    
    private fun joinChannel() {
        // Generate channel name from user IDs (ensures both users join same channel)
        channelName = if (currentUserId < contactId) {
            "${currentUserId}_${contactId}"
        } else {
            "${contactId}_${currentUserId}"
        }
        
        // Use user ID as UID (convert to Int)
        localUid = currentUserId.hashCode() and 0x7FFFFFFF // Ensure positive Int
        
        // Token handling:
        // New App ID doesn't require token (App Certificate is disabled)
        val token: String? = null
        
        android.util.Log.d("Agora", "Joining channel: $channelName, UID: $localUid, Token: null (App Certificate disabled)")
        
        val result = agoraEngine?.joinChannel(token, channelName, localUid, ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        })
        
        android.util.Log.d("Agora", "joinChannel result: $result (0 = success)")
        
        if (result != 0) {
            android.util.Log.e("Agora", "joinChannel failed with error code: $result")
            runOnUiThread {
                val errorMsg = when (result) {
                    110 -> "Invalid token - but token should not be required. Check App Certificate status."
                    -2 -> "Invalid App ID"
                    -3 -> "Invalid channel name"
                    -7 -> "SDK not initialized"
                    -17 -> "Join channel failed"
                    else -> "Join channel error: $result"
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                binding.tvCallStatus.text = errorMsg
            }
        }
    }
    
    /* WebRTC code commented out until library is added
    private fun initializeWebRTC() {
        val options = PeerConnectionFactory.InitializationOptions.builder(applicationContext)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        
        eglBase = EglBase.create()
        
        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase?.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
        
        createPeerConnection()
        startLocalVideo()
    }
    */
    
    /* WebRTC methods commented out - see WEBRTC_SETUP.md
    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )
        
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(p0: IceCandidate?) {
                    // Handle ICE candidate
                }
                
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onAddStream(p0: MediaStream?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onRenegotiationNeeded() {}
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                
                override fun onAddTrack(p0: RtpReceiver?, p0s: Array<out MediaStream>?) {
                    p0?.track()?.let { track ->
                        if (track is VideoTrack) {
                            remoteVideoTrack = track
                            runOnUiThread {
                                track.addSink(binding.remoteVideoView)
                            }
                        }
                    }
                }
            }
        )
    }
    
    private fun startLocalVideo() {
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread",
            eglBase?.eglBaseContext
        )
        
        videoCapturer = createCameraCapturer()
        videoCapturer?.initialize(
            surfaceTextureHelper,
            applicationContext,
            videoCapturer?.createCapturer(null, null)
        )
        
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        videoCapturer?.startCapture(1280, 720, 30)
        
        localVideoTrack = peerConnectionFactory?.createVideoTrack("video", videoSource)
        localVideoTrack?.addSink(binding.localVideoView)
        
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        audioTrack = peerConnectionFactory?.createAudioTrack("audio", audioSource)
        
        val localStream = peerConnectionFactory?.createLocalMediaStream("localStream")
        localVideoTrack?.let { localStream?.addTrack(it) }
        audioTrack?.let { localStream?.addTrack(it) }
        
        peerConnection?.addStream(localStream)
    }
    
    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(applicationContext)
        val deviceNames = enumerator.deviceNames
        
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }
    */
    
    private fun showIncomingCallUI() {
        binding.tvCallStatus.text = "Incoming call from $contactName"
        binding.btnAccept.visibility = View.VISIBLE
        binding.btnReject.visibility = View.VISIBLE
        binding.btnHangUp.visibility = View.GONE
        binding.btnMute.visibility = View.GONE
        
        // Listen for call acceptance/rejection
        listenForCallResponse()
    }
    
    private fun startOutgoingCall() {
        binding.tvCallStatus.text = "Calling $contactName..."
        binding.btnAccept.visibility = View.GONE
        binding.btnReject.visibility = View.GONE
        binding.btnHangUp.visibility = View.VISIBLE
        binding.btnMute.visibility = View.VISIBLE
        
        // Create call document
        createCallDocument()
    }
    
    private fun createCallDocument() {
        val callData = hashMapOf(
            "callerId" to currentUserId,
            "receiverId" to contactId,
            "status" to "ringing",
            "timestamp" to System.currentTimeMillis()
        )
        
        android.util.Log.d("VideoCallActivity", "Creating call document:")
        android.util.Log.d("VideoCallActivity", "  callerId: $currentUserId")
        android.util.Log.d("VideoCallActivity", "  receiverId: $contactId")
        android.util.Log.d("VideoCallActivity", "  status: ringing")
        
        if (currentUserId.isEmpty()) {
            android.util.Log.e("VideoCallActivity", "ERROR: currentUserId is empty! Call document won't be created correctly.")
        }
        if (contactId.isEmpty()) {
            android.util.Log.e("VideoCallActivity", "ERROR: contactId is empty! Call document won't be created correctly.")
        }
        
        callsCollection.add(callData)
            .addOnSuccessListener { documentReference ->
                android.util.Log.d("VideoCallActivity", "✓ Call document created successfully!")
                android.util.Log.d("VideoCallActivity", "  Document ID: ${documentReference.id}")
                android.util.Log.d("VideoCallActivity", "  The receiver's MainActivity listener should detect this now")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("VideoCallActivity", "✗ Failed to create call document", e)
                android.util.Log.e("VideoCallActivity", "  Error: ${e.message}")
            }
    }
    
    private fun listenForCallResponse() {
        callsCollection
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && !snapshot.isEmpty) {
                    // Call is ringing
                }
            }
    }
    
    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            acceptCall()
        }
        
        binding.btnReject.setOnClickListener {
            rejectCall()
        }
        
        binding.btnHangUp.setOnClickListener {
            hangUp()
        }
        
        binding.btnMute.setOnClickListener {
            toggleMute()
        }
    }
    
    private fun acceptCall() {
        binding.btnAccept.visibility = View.GONE
        binding.btnReject.visibility = View.GONE
        binding.btnHangUp.visibility = View.VISIBLE
        binding.btnMute.visibility = View.VISIBLE
        binding.tvCallStatus.text = "Connecting..."
        
        // Update call status
        updateCallStatus("accepted")
        
        // Check permissions first, then initialize Agora and join channel
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // Initialize Agora and join channel
            if (agoraEngine == null) {
                initializeAgora()
            } else {
                joinChannel()
            }
        }
    }
    
    private fun rejectCall() {
        updateCallStatus("rejected")
        finish()
    }
    
    private fun hangUp() {
        updateCallStatus("ended")
        cleanup()
        finish()
    }
    
    private fun toggleMute() {
        isMuted = !isMuted
        agoraEngine?.muteLocalAudioStream(isMuted)
        binding.btnMute.text = if (isMuted) getString(R.string.unmute) else getString(R.string.mute)
    }
    
    private fun updateCallStatus(status: String) {
        callsCollection
            .whereEqualTo("callerId", if (isIncoming) contactId else currentUserId)
            .whereEqualTo("receiverId", if (isIncoming) currentUserId else contactId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.reference?.update("status", status)
            }
    }
    
    private fun cleanup() {
        // Leave channel and cleanup Agora
        agoraEngine?.leaveChannel()
        agoraEngine?.stopPreview()
        RtcEngine.destroy()
        agoraEngine = null
        
        // Cancel notification
        CallNotificationManager(this).cancelNotification()
        
        // Stop call service
        val serviceIntent = Intent(this, CallService::class.java).apply {
            action = CallService.ACTION_END_CALL
        }
        stopService(serviceIntent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}

