package com.familycalls.app.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    suspend fun uploadImage(uri: Uri, userId: String): Result<String> {
        return try {
            val imageRef = storageRef.child("images/${userId}/${UUID.randomUUID()}.jpg")
            val uploadTask = imageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadVideo(uri: Uri, userId: String): Result<String> {
        return try {
            val videoRef = storageRef.child("videos/${userId}/${UUID.randomUUID()}.mp4")
            val uploadTask = videoRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

