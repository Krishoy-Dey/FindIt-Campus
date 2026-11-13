package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object PhoneUtils {

    fun dialPhoneNumber(context: Context, phone: String) {
        val cleaned = phone.trim()
        if (cleaned.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No phone dialer found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        val trimmed = phone.trim()
        val digitsCount = trimmed.count { it.isDigit() }
        return trimmed.length in 5..25 && digitsCount >= 7
    }

    fun formatRelativeTime(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return "Just now"
        val diffMillis = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(date.time))
        }
    }
}
