package com.familycalls.app.data.repository

import com.familycalls.app.data.model.Message
import com.familycalls.app.data.model.MessageType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val messagesCollection = db.collection("messages")
    
    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val docRef = messagesCollection.add(message).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getMessages(userId1: String, userId2: String): Flow<List<Message>> = callbackFlow {
        // Get messages where userId1 is sender or receiver
        val query1 = messagesCollection
            .whereIn("senderId", listOf(userId1, userId2))
            .orderBy("timestamp", Query.Direction.ASCENDING)
        
        val listenerRegistration = query1.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            val messages = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            
            // Filter to only messages between these two users
            val filteredMessages = messages.filter { msg ->
                (msg.senderId == userId1 && msg.receiverId == userId2) ||
                (msg.senderId == userId2 && msg.receiverId == userId1)
            }
            
            trySend(filteredMessages)
        }
        
        awaitClose { listenerRegistration.remove() }
    }
}

