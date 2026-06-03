package com.nuevoso.launcher.agent.executors

import android.content.Context
import android.content.Intent
import android.net.Uri

class DialExecutor(private val context: Context) {
    fun execute(target: String): String {
        val uri = if (target.all { it.isDigit() || it == '+' || it == '-' || it == ' ' })
            Uri.parse("tel:${target.replace(" ", "")}")
        else
            Uri.parse("tel:")
        val intent = Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Abriendo marcador para llamar a $target."
    }
}
