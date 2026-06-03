package com.nuevoso.launcher.agent.executors

import android.content.Context
import android.content.Intent
import android.net.Uri

class SearchWebExecutor(private val context: Context) {
    fun execute(query: String): String {
        val encoded = Uri.encode(query)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Buscando \"$query\" en el navegador."
    }
}
