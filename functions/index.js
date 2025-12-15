/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
setGlobalOptions({maxInstances: 10});

// Cloud Function to send FCM notification when a call is created
exports.sendCallNotification = onDocumentCreated(
    {
      document: "calls/{callId}",
      region: "us-central1",
    },
    async (event) => {
      const callData = event.data.data();
      const callId = event.params.callId;

      // Only send notification for new calls with "ringing" status
      if (callData.status !== "ringing") {
        console.log(
            `Call ${callId} status is "${callData.status}", ` +
            "skipping notification",
        );
        return null;
      }

      const receiverId = callData.receiverId;
      const callerId = callData.callerId;

      if (!receiverId) {
        console.error(`Call ${callId} has no receiverId`);
        return null;
      }

      if (!callerId) {
        console.error(`Call ${callId} has no callerId`);
        return null;
      }

      try {
        // Get receiver's user document to get FCM token
        const receiverDoc = await admin.firestore()
            .collection("users")
            .doc(receiverId)
            .get();

        if (!receiverDoc.exists) {
          console.error(`Receiver ${receiverId} not found`);
          return null;
        }

        const receiverData = receiverDoc.data();
        const fcmToken = receiverData && receiverData.fcmToken;

        if (!fcmToken) {
          console.error(`Receiver ${receiverId} has no FCM token`);
          return null;
        }

        // Get caller's name and phone for the notification
        let callerName = "Someone";
        let callerPhone = "";
        if (callerId) {
          const callerDoc = await admin.firestore()
              .collection("users")
              .doc(callerId)
              .get();
          if (callerDoc.exists) {
            const callerData = callerDoc.data();
            callerName = (callerData && callerData.name) || callerName;
            callerPhone = (callerData && callerData.phone) || callerPhone;
          }
        }

        // Send FCM notification
        // Use data-only payload so onMessageReceived is always called
        // This ensures our service handles it and can start the activity
        const message = {
          data: {
            type: "incoming_call",
            callId: callId,
            callerId: callerId,
            receiverId: receiverId,
            callerName: callerName,
            callerPhone: callerPhone,
            title: "Incoming Call",
            body: `${callerName} is calling you`,
          },
          token: fcmToken,
          android: {
            priority: "high",
          },
        };

        const response = await admin.messaging().send(message);
        console.log(
            `Successfully sent notification for call ${callId}:`,
            response,
        );
        return null;
      } catch (error) {
        console.error(`Error sending notification for call ${callId}:`, error);
        return null;
      }
    },
);

// Cloud Function to send FCM notification when a message is created
exports.sendMessageNotification = onDocumentCreated(
    {
      document: "messages/{messageId}",
      region: "us-central1",
    },
    async (event) => {
      const messageData = event.data.data();
      const messageId = event.params.messageId;

      const receiverId = messageData.receiverId;
      const senderId = messageData.senderId;

      if (!receiverId) {
        console.error(`Message ${messageId} has no receiverId`);
        return null;
      }

      if (!senderId) {
        console.error(`Message ${messageId} has no senderId`);
        return null;
      }

      try {
        // Get receiver's user document to get FCM token
        const receiverDoc = await admin.firestore()
            .collection("users")
            .doc(receiverId)
            .get();

        if (!receiverDoc.exists) {
          console.error(`Receiver ${receiverId} not found`);
          return null;
        }

        const receiverData = receiverDoc.data();
        const fcmToken = receiverData && receiverData.fcmToken;

        if (!fcmToken) {
          console.error(`Receiver ${receiverId} has no FCM token`);
          return null;
        }

        // Get sender's name for the notification
        let senderName = "Someone";
        if (senderId) {
          const senderDoc = await admin.firestore()
              .collection("users")
              .doc(senderId)
              .get();
          if (senderDoc.exists) {
            const senderData = senderDoc.data();
            senderName = (senderData && senderData.name) || senderName;
          }
        }

        // Determine full message text based on type (don't truncate - let app handle it)
        let fullMessageText = "";
        const messageType = messageData.type || "text";
        if (messageType === "text") {
          fullMessageText = messageData.text || "";
        } else if (messageType === "image") {
          fullMessageText = "📷 Image";
        } else if (messageType === "video") {
          fullMessageText = "🎥 Video";
        }

        // Don't default to "New message" - send empty if no text, app will handle it
        // But ensure we have something for notification preview
        const messagePreview = fullMessageText || "New message";

        // Send FCM notification
        // Use data-only payload so onMessageReceived is always called
        // This ensures notifications work even when app is closed
        // IMPORTANT: Send full message text, not truncated preview
        const message = {
          data: {
            type: "message",
            messageId: messageId,
            senderId: senderId,
            receiverId: receiverId,
            messageText: fullMessageText, // Send FULL message text, not truncated
            senderName: senderName,
            title: senderName,
            body: messagePreview, // Preview for compatibility
          },
          token: fcmToken,
          android: {
            priority: "high",
          },
        };

        const response = await admin.messaging().send(message);
        console.log(
            `Successfully sent message notification for ${messageId}:`,
            response,
        );
        return null;
      } catch (error) {
        console.error(
            `Error sending message notification for ${messageId}:`,
            error,
        );
        return null;
      }
    },
);
