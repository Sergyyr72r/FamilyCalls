# Create GitHub Repository

Follow these steps to create a GitHub repository and push your code:

## Step 1: Create Repository on GitHub

1. Go to [GitHub](https://github.com) and sign in
2. Click the **"+"** icon in the top right → **"New repository"**
3. Fill in the details:
   - **Repository name:** `FamilyCalls` (or your preferred name)
   - **Description:** "Private Family Messenger & Video Calls App"
   - **Visibility:** Choose **Private** (recommended for family app) or **Public**
   - **DO NOT** initialize with README, .gitignore, or license (we already have these)
4. Click **"Create repository"**

## Step 2: Push Your Code

After creating the repository, GitHub will show you commands. Run these in your terminal:

```bash
cd /Users/sergeyromanov/Desktop/FamilyCalls

# Add the remote (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/FamilyCalls.git

# Rename branch to main (if needed)
git branch -M main

# Push your code
git push -u origin main
```

## Step 3: Configure Git User (Optional but Recommended)

If you want to set your git user info:

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

## Alternative: Using SSH (if you have SSH keys set up)

If you prefer SSH instead of HTTPS:

```bash
git remote add origin git@github.com:YOUR_USERNAME/FamilyCalls.git
git branch -M main
git push -u origin main
```

## Quick One-Liner (after creating repo on GitHub)

Replace `YOUR_USERNAME` with your actual GitHub username:

```bash
git remote add origin https://github.com/YOUR_USERNAME/FamilyCalls.git && git branch -M main && git push -u origin main
```



