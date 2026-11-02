package com.example.chronicle.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri

object MediaIntents {

    fun copyToClipboard(context: Context, uris: List<String>) {

        for (uri in uris) {
            val uri = uri.toUri()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(context.contentResolver, "Image Uri", uri)
            clipboard.setPrimaryClip(clip)
        }
        if (uris.size == 1) {
            Toast.makeText(context, "Image copied to Clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "${uris.size} Images copied to Clipboard", Toast.LENGTH_LONG).show()
        }
    }

    fun shareMedia(context: Context, uriString: String, mimeType: String) {
        val uri = uriString.toUri()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
    }

    fun editMedia(context: Context, uriString: String, mimeType: String) {
        val uri = uriString.toUri()
        val editIntent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, mimeType)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(editIntent, "Edit Media"))
    }

    fun shareMediaBatch(context: Context, uris: List<String>) {
        if (uris.isEmpty()) return

        val parsedUris = ArrayList(uris.map { it.toUri() })
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, parsedUris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
    }
}