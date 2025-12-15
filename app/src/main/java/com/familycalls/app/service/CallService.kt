package com.familycalls.app.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class CallService : Service() {
    
    private lateinit var notificationManager: CallNotificationManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtonePlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = CallNotificationManager(this)
        Log.d("CallService", "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            ACTION_SHOW_INCOMING_CALL -> {
                val callerId = intent.getStringExtra("callerId") ?: return START_NOT_STICKY
                val callerName = intent.getStringExtra("callerName") ?: ""
                val callerPhone = intent.getStringExtra("callerPhone") ?: ""
                
                Log.d("CallService", "Showing incoming call notification: callerId=$callerId, callerName=$callerName")
                
                // Acquire wake lock to wake device and keep screen on
                acquireWakeLock()
                
                // Start continuous ringtone playback and vibration
                startCallRingtone()
                startCallVibration()
                
                // Create the notification with buttons (this will be used for both display and foreground service)
                val notification = notificationManager.createIncomingCallNotification(
                    callerId,
                    callerName,
                    callerPhone
                )
                
                // Start as foreground service using the same notification
                try {
                    startForeground(NOTIFICATION_ID, notification)
                    Log.d("CallService", "Foreground service started successfully with incoming call notification")
                } catch (e: Exception) {
                    Log.e("CallService", "Failed to start foreground service", e)
                    releaseWakeLock()
                }
            }
            
            ACTION_END_CALL -> {
                stopCallRingtone()
                stopCallVibration()
                notificationManager.cancelNotification()
                releaseWakeLock()
                stopForeground(true)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            
            // Try to use screen wake lock (deprecated but still works for turning on screen)
            // The full-screen intent and activity's setTurnScreenOn should handle screen wake,
            // but this helps ensure the device wakes up
            var wakeLockFlags = PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
            
            // On older Android versions, we can use SCREEN_BRIGHT_WAKE_LOCK
            // This is deprecated but still functional for ensuring screen turns on
            @Suppress("DEPRECATION")
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O_MR1) {
                try {
                    wakeLockFlags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE
                    Log.d("CallService", "Using SCREEN_BRIGHT_WAKE_LOCK (deprecated but needed)")
                } catch (e: Exception) {
                    Log.w("CallService", "SCREEN_BRIGHT_WAKE_LOCK not available, using PARTIAL", e)
                }
            }
            
            wakeLock = powerManager.newWakeLock(
                wakeLockFlags,
                "FamilyCalls::IncomingCallWakeLock"
            )
            
            // Hold for 60 seconds - enough time for user to answer
            wakeLock?.acquire(60000)
            Log.d("CallService", "Wake lock acquired (flags: $wakeLockFlags)")
            Log.d("CallService", "Full-screen intent should wake screen via activity's setTurnScreenOn")
        } catch (e: Exception) {
            Log.e("CallService", "Failed to acquire wake lock", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("CallService", "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("CallService", "Failed to release wake lock", e)
        }
    }
    
    private fun startCallRingtone() {
        stopCallRingtone() // Stop any existing playback
        
        // Check ringer mode - don't play sound if device is in silent/vibrate mode
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = audioManager.ringerMode
        
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            Log.d("CallService", "Device is in silent mode - skipping ringtone, vibration only")
            return
        }
        
        try {
            val soundUri = android.provider.Settings.System.DEFAULT_RINGTONE_URI
            ringtonePlayer = MediaPlayer().apply {
                setDataSource(applicationContext, soundUri)
                
                // Configure audio attributes for call ringtone
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        // Remove FLAG_AUDIBILITY_ENFORCED to respect mute settings
                        .build()
                )
                
                // Set looping to continuously play the ringtone
                isLooping = true
                
                // Set audio stream to ringtone stream (respects ringer mode)
                setAudioStreamType(AudioManager.STREAM_RING)
                
                prepare()
                
                // Check again before starting (in case mode changed)
                val currentRingerMode = audioManager.ringerMode
                if (currentRingerMode != AudioManager.RINGER_MODE_SILENT) {
                    start()
                    Log.d("CallService", "Ringtone started and will loop continuously (ringer mode: $currentRingerMode)")
                } else {
                    Log.d("CallService", "Device is in silent mode - ringtone prepared but not started")
                }
            }
        } catch (e: Exception) {
            Log.e("CallService", "Failed to start ringtone", e)
            ringtonePlayer = null
        }
    }
    
    private fun stopCallRingtone() {
        try {
            ringtonePlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            ringtonePlayer = null
            Log.d("CallService", "Ringtone stopped")
        } catch (e: Exception) {
            Log.e("CallService", "Error stopping ringtone", e)
        }
    }
    
    private fun startCallVibration() {
        stopCallVibration() // Stop any existing vibration
        
        try {
            // Get vibrator service
            vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            // Standard call vibration pattern: 1s vibrate, 0.5s pause, 1s vibrate
            // Pattern format: [delay before start, vibrate duration, pause, vibrate duration, ...]
            val vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Repeat index 0 means repeat from the start of the pattern (after delay)
                // This will continuously loop: 1s vibrate, 0.5s pause, 1s vibrate, repeat
                val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                // Repeat index 0 means repeat from index 0 (skipping the initial delay)
                vibrator?.vibrate(vibrationPattern, 0)
            }
            
            Log.d("CallService", "Vibration started and will repeat continuously")
        } catch (e: Exception) {
            Log.e("CallService", "Failed to start vibration", e)
        }
    }
    
    private fun stopCallVibration() {
        try {
            vibrator?.cancel()
            vibrator = null
            Log.d("CallService", "Vibration stopped")
        } catch (e: Exception) {
            Log.e("CallService", "Error stopping vibration", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopCallRingtone()
        stopCallVibration()
        releaseWakeLock()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    companion object {
        const val ACTION_SHOW_INCOMING_CALL = "com.familycalls.app.SHOW_INCOMING_CALL"
        const val ACTION_END_CALL = "com.familycalls.app.END_CALL"
        // Use the same notification ID as CallNotificationManager to avoid duplicates
        private const val NOTIFICATION_ID = CallNotificationManager.NOTIFICATION_ID
    }
}

