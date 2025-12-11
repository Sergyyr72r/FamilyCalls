package com.familycalls.app.data.repository

import android.content.Context
import android.provider.Settings
import com.familycalls.app.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    
    // Maximum 5 family members
    private val MAX_FAMILY_MEMBERS = 5
    
    suspend fun registerUser(name: String, phone: String, deviceId: String): Result<String> {
        return try {
            // Check if device is already registered
            val existingUser = usersCollection
                .whereEqualTo("deviceId", deviceId)
                .get()
                .await()
            
            if (!existingUser.isEmpty) {
                return Result.success(existingUser.documents[0].id)
            }
            
            // Check if we've reached the limit
            val currentUsers = usersCollection.get().await()
            if (currentUsers.size() >= MAX_FAMILY_MEMBERS) {
                return Result.failure(Exception("Maximum family members limit reached"))
            }
            
            // Create new user
            val user = User(
                name = name,
                phone = phone,
                deviceId = deviceId
            )
            
            val docRef = usersCollection.add(user).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(deviceId: String): User? {
        return try {
            val query = usersCollection
                .whereEqualTo("deviceId", deviceId)
                .get()
                .await()
            
            if (query.isEmpty) null
            else {
                val doc = query.documents[0]
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = usersCollection.get().await()
            snapshot.documents.map { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id) ?: User()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
}

