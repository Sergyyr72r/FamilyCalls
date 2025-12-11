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
            android.util.Log.e("ChatRepository", "Error sending message", e)
            Result.failure(e)
        }
    }
    
    fun getMessages(userId1: String, userId2: String): Flow<List<Message>> = callbackFlow {
        android.util.Log.d("ChatRepository", "getMessages called: userId1=$userId1, userId2=$userId2")
        
        if (userId1.isEmpty() || userId2.isEmpty()) {
            android.util.Log.e("ChatRepository", "Empty user IDs: userId1='$userId1', userId2='$userId2'")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        // Use two separate queries to avoid index requirements
        var messages1 = emptyList<Message>()
        var messages2 = emptyList<Message>()
        
        fun updateMessages() {
            val allMessages = (messages1 + messages2)
                .distinctBy { it.id }
                .sortedBy { it.timestamp } // Sort in memory instead of using orderBy in query
            android.util.Log.d("ChatRepository", "Sending ${allMessages.size} messages to UI (sorted by timestamp)")
            trySend(allMessages)
        }
        
        // Query 1: Messages where userId1 is sender and userId2 is receiver
        // Note: Removed orderBy to avoid index requirement - we'll sort in memory
        val listener1 = messagesCollection
            .whereEqualTo("senderId", userId1)
            .whereEqualTo("receiverId", userId2)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatRepository", "Error in query 1 (sender=$userId1, receiver=$userId2): ${error.message}", error)
                    // If index missing, error will contain URL to create it
                    if (error.message?.contains("index") == true || error.message?.contains("requires") == true) {
                        android.util.Log.e("ChatRepository", "⚠️ MISSING FIRESTORE INDEX! The error message above should contain a URL to create it.")
                    }
                    messages1 = emptyList()
                    updateMessages()
                    return@addSnapshotListener
                }
                
                messages1 = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepository", "Error parsing message ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                android.util.Log.d("ChatRepository", "Query 1 returned ${messages1.size} messages")
                updateMessages()
            }
        
        // Query 2: Messages where userId2 is sender and userId1 is receiver
        // Note: Removed orderBy to avoid index requirement - we'll sort in memory
        val listener2 = messagesCollection
            .whereEqualTo("senderId", userId2)
            .whereEqualTo("receiverId", userId1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatRepository", "Error in query 2 (sender=$userId2, receiver=$userId1): ${error.message}", error)
                    // If index missing, error will contain URL to create it
                    if (error.message?.contains("index") == true || error.message?.contains("requires") == true) {
                        android.util.Log.e("ChatRepository", "⚠️ MISSING FIRESTORE INDEX! The error message above should contain a URL to create it.")
                    }
                    messages2 = emptyList()
                    updateMessages()
                    return@addSnapshotListener
                }
                
                messages2 = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepository", "Error parsing message ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                android.util.Log.d("ChatRepository", "Query 2 returned ${messages2.size} messages")
                updateMessages()
            }
        
        awaitClose {
            listener1.remove()
            listener2.remove()
        }
    }
}