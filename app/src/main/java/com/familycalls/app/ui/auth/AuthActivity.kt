package com.familycalls.app.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.familycalls.app.R
import com.familycalls.app.data.repository.AuthRepository
import com.familycalls.app.databinding.ActivityAuthBinding
import com.familycalls.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val authRepository = AuthRepository()
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences("FamilyCalls", MODE_PRIVATE)
        
        // CRITICAL: Check if user is already logged in SYNCHRONOUSLY before showing UI
        // This prevents the login screen from flashing briefly for logged-in users
        val userId = prefs.getString("userId", "") ?: ""
        if (userId.isNotEmpty()) {
            // User is already logged in, navigate immediately without showing AuthActivity UI
            navigateToMain()
            return
        }
        
        // Only show the login UI if no user is logged in
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Additional check: verify user exists in Firestore (async backup)
        val deviceId = authRepository.getDeviceId(this)
        lifecycleScope.launch {
            val currentUser = authRepository.getCurrentUser(deviceId)
            if (currentUser != null) {
                // Save userId to SharedPreferences before navigating
                prefs.edit().putString("userId", currentUser.id).putString("deviceId", deviceId).apply()
                navigateToMain()
                return@launch
            }
        }
        
        binding.btnRegister.setOnClickListener {
            registerUser()
        }
    }
    
    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter name and phone number", Toast.LENGTH_SHORT).show()
            return
        }
        
        val deviceId = authRepository.getDeviceId(this)
        
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Registering..."
        
        lifecycleScope.launch {
            val result = authRepository.registerUser(name, phone, deviceId)
            result.getOrElse { error ->
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = getString(R.string.register)
                Toast.makeText(this@AuthActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                return@launch
            }
            val userId = result.getOrNull() ?: return@launch
            prefs.edit().putString("userId", userId).putString("deviceId", deviceId).apply()
            Toast.makeText(this@AuthActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

