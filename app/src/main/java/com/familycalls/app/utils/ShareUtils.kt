package com.familycalls.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

object ShareUtils {
    fun shareDownloadLink(context: Context, downloadLink: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join our family calls app! Download here: $downloadLink")
            putExtra(Intent.EXTRA_SUBJECT, "Family Calls App")
        }
        context.startActivity(Intent.createChooser(intent, "Share download link"))
    }
    
    fun copyToClipboard(context: Context, text: String, label: String = "Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    fun generateDownloadLink(packageName: String): String {
        // Replace with your actual download link
        // This could be:
        // 1. Google Play Store link
        // 2. Direct APK download link
        // 3. Firebase App Distribution link
        return "https://play.google.com/store/apps/details?id=$packageName"
    }
}

