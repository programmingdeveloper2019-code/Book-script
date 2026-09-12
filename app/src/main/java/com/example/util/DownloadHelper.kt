package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.data.model.Book

object DownloadHelper {

    fun downloadBook(context: Context, book: Book) {
        try {
            val downloadUrl = "https://library.yatal.tech/api/download/${book.id}"
            val rawName = book.rawFilename?.trim().orEmpty()
            val filename = if (rawName.endsWith(".pdf", ignoreCase = true)) {
                rawName
            } else {
                "${book.cleanTitle.replace(Regex("[^a-zA-Z0-9_\\s\\u1000-\\u109F]"), "").replace(' ', '_')}.pdf"
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle(book.cleanTitle)
                setDescription("Yatal Digital Library မှ ဒေါင်းလုဒ်ရယူနေသည် (${book.formattedSize})")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (manager != null) {
                manager.enqueue(request)
                Toast.makeText(
                    context,
                    "«${book.cleanTitle}» ကို ဒေါင်းလုဒ် စတင်နေပါသည်… (Downloads ဖိုဒါတွင် သိမ်းဆည်းပါမည်)",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Fallback to browser download
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback to browser intent if permission or download manager issue
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://library.yatal.tech/api/download/${book.id}"))
                context.startActivity(browserIntent)
                Toast.makeText(
                    context,
                    "ဒေါင်းလုဒ် စတင်ရန် Browser သို့ လွှဲပြောင်းပေးထားပါသည်",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (ex: Exception) {
                Toast.makeText(
                    context,
                    "ဒေါင်းလုဒ် ရယူရာတွင် ချို့ယွင်းချက်ရှိပါသည်: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
