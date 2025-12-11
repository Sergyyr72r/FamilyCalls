# Firebase Pricing for Family Calls App

## Important: Storage Requires Blaze Plan

Firebase Storage is **not available** on the free Spark plan. You need to upgrade to the **Blaze (pay-as-you-go)** plan to use Storage.

**Good News:** The Blaze plan has generous free tier limits that should cover your family app at **$0 cost**!

---

## Cost Estimate for Your Family (5 users, 3 calls/day)

### Blaze Plan Free Tier Limits:

#### ✅ Firestore Database (FREE within limits):
- **Reads:** 50,000/day free → Your usage: ~500/day (well within limit)
- **Writes:** 20,000/day free → Your usage: ~250/day (well within limit)  
- **Storage:** 1 GB free → Your usage: ~50-100 MB (well within limit)

#### ✅ Cloud Storage (FREE within limits):
- **Storage:** 5 GB free
- **Downloads:** 1 GB/day free
- **Uploads:** 5 GB/day free
- **Your usage:** 
  - Images: ~10-50 MB/month
  - Videos: ~100-500 MB/month
  - **Total: Well under 1 GB/month** ✅

#### ✅ Authentication:
- **50,000 monthly active users free** → You have 5 users ✅

#### ✅ Cloud Messaging:
- **Unlimited** for your use case ✅

---

## Estimated Monthly Cost: **$0.00** 💰

For 5 users with 3 daily calls, you should **stay within all free tier limits** and pay **nothing**.

### Why it's free:
- 5 users is tiny compared to free tier limits
- 3 calls/day = ~90 calls/month = minimal data
- Messages and media sharing are lightweight
- All usage fits comfortably in free tiers

---

## What Happens If You Exceed Free Tier?

Even if you exceed free tier (unlikely), costs are very low:

- **Firestore reads:** $0.06 per 100,000 reads
- **Firestore writes:** $0.18 per 100,000 writes
- **Storage:** $0.026 per GB/month
- **Storage downloads:** $0.12 per GB

**Example:** Even if you used 10x your expected usage:
- Extra Firestore: ~$1-2/month
- Extra Storage: ~$0.50/month
- **Total: ~$2-3/month maximum**

---

## How to Set Up Blaze Plan (Pay-As-You-Go)

1. Go to Firebase Console
2. Click on your project
3. Go to **Usage and billing** (gear icon → Usage and billing)
4. Click **Modify plan**
5. Select **Blaze plan** (pay-as-you-go)
6. Add a payment method (credit card)
7. **Set up billing alerts** (important!)

### Set Up Billing Alerts (Recommended):

1. In Firebase Console → **Usage and billing**
2. Go to **Alerts** tab
3. Set alert at $5/month (or your comfort level)
4. This way you'll be notified if usage spikes

**Note:** You can set a budget cap to prevent unexpected charges.

---

## Alternative: Use Firestore Only (No Storage)

If you want to avoid the Blaze plan entirely, you can:

### Option 1: Store Images/Videos as Base64 in Firestore
- Convert images/videos to Base64 strings
- Store directly in Firestore documents
- **Limitation:** Firestore has 1MB document limit
- **Best for:** Small images only

### Option 2: Use External Storage (Free)
- Use **Imgur API** (free) for images
- Use **YouTube** or **Vimeo** (free) for videos
- Store only URLs in Firestore
- **Best for:** Avoiding Firebase Storage costs

### Option 3: Self-Hosted Storage
- Use your own server/cloud storage
- Store URLs in Firestore
- **Best for:** Full control

---

## Recommendation

**For your use case (5 users, 3 calls/day):**

✅ **Use Blaze Plan** - You'll pay $0/month and have full functionality
- Set billing alerts at $5/month for peace of mind
- Monitor usage in Firebase Console
- Costs will be $0 for your usage level

---

## Cost Monitoring

After setting up:

1. Go to Firebase Console → **Usage and billing**
2. Check **Usage** tab regularly
3. Set up **Alerts** (recommended: $5/month)
4. Review monthly billing statements

---

## Summary

- **Required:** Blaze plan (pay-as-you-go) for Storage
- **Your cost:** $0/month (stays within free tier)
- **Maximum cost:** ~$2-3/month even with 10x usage
- **Recommendation:** Use Blaze plan with billing alerts

**Bottom line:** Upgrade to Blaze plan, but you won't pay anything for your usage level! 🎉

