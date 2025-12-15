# FCM (Firebase Cloud Messaging) Setup Guide

This guide will help you set up Firebase Cloud Messaging to wake up phones when calls come in, even when the screen is off.

## Prerequisites

- Firebase project already created
- `google-services.json` already added to the app
- Node.js 18+ installed on your computer

## Step 1: Install Firebase CLI

1. Open Terminal
2. Run:
   ```bash
   npm install -g firebase-tools
   ```
3. Login to Firebase:
   ```bash
   firebase login
   ```

## Step 2: Initialize Firebase Functions

1. In your project directory (`/Users/sergeyromanov/Desktop/FamilyCalls`), run:
   ```bash
   firebase init functions
   ```

2. When prompted:
   - **Select an existing project**: Choose your "Family Calls" project
   - **Language**: JavaScript
   - **ESLint**: Yes (optional, but recommended)
   - **Install dependencies**: Yes

## Step 3: Deploy the Cloud Function

1. The Cloud Function code is already in `functions/index.js`
2. Deploy it:
   ```bash
   cd functions
   npm install
   cd ..
   firebase deploy --only functions
   ```

3. Wait for deployment to complete. You should see:
   ```
   ✔  functions[sendCallNotification(us-central1)] Successful create operation.
   ```

## Step 4: Verify the Function

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Functions** (left sidebar)
4. You should see `sendCallNotification` listed

## Step 5: Test the Setup

1. **Install the app** on two devices
2. **Sign up** both users
3. **Make sure both devices have FCM tokens registered**:
   - Open the app on both devices
   - Check Logcat for: `"FCM Token registered successfully"`
4. **Call from one device to the other**
5. **Turn off the screen** on the receiving device
6. **The notification should appear** and wake up the screen

## How It Works

1. **When a call is created** in Firestore (`calls` collection), the Cloud Function automatically triggers
2. **The function gets the receiver's FCM token** from the `users` collection
3. **It sends a push notification** to the receiver's device
4. **The device wakes up** and shows the notification, even if the screen is off
5. **MyFirebaseMessagingService** receives the notification and shows the call UI

## Troubleshooting

### Function Not Triggering

- Check Firebase Console → Functions → Logs for errors
- Verify the function is deployed: `firebase functions:list`
- Check Firestore rules allow the function to read `users` and `calls` collections

### No FCM Token

- Make sure the app is opened at least once after installation
- Check Logcat for: `"FCM Token registered successfully"`
- Verify the user document in Firestore has an `fcmToken` field

### Notification Not Appearing

- Check device notification settings
- Verify the notification channel is enabled
- Check Logcat for FCM message receipt: `"Received call FCM from: ..."`

## Cost

- **Free tier**: 2 million FCM messages/month
- **For 5 users, 3 calls/day**: ~450 messages/month (well within free tier)

## Next Steps

After deployment, the app will automatically:
- Register FCM tokens when users sign up
- Send push notifications when calls are created
- Wake up devices even when the screen is off

