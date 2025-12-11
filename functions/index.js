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

        // Get caller's name for the notification
        let callerName = "Someone";
        if (callerId) {
          const callerDoc = await admin.firestore()
              .collection("users")
              .doc(callerId)
              .get();
          if (callerDoc.exists) {
            const callerData = callerDoc.data();
            callerName = (callerData && callerData.name) || callerName;
          }
        }

        // Send FCM notification
        const message = {
          notification: {
            title: "Incoming Call",
            body: `${callerName} is calling you`,
          },
          data: {
            type: "incoming_call",
            callId: callId,
            callerId: callerId,
            receiverId: receiverId,
          },
          token: fcmToken,
          android: {
            priority: "high",
            notification: {
              channelId: "incoming_calls",
              sound: "default",
              priority: "high",
            },
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
