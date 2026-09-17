package app.noor.prayer.core.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import app.noor.prayer.domain.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AudioRepository @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun import(uri: Uri, voice: Voice): String = withContext(Dispatchers.IO) {
        val target = File(context.filesDir,"adhan_${voice.name}_${System.currentTimeMillis()}.audio")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192); var total = 0
                    while(true) { val n = input.read(buffer); if(n < 0) break; total += n; require(total <= 25 * 1024 * 1024); output.write(buffer,0,n) }
                }
            }
            val retriever = MediaMetadataRetriever()
            try { retriever.setDataSource(target.absolutePath); require((retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0) in 1_000..900_000) }
            finally { retriever.release() }
            Uri.fromFile(target).toString()
        } catch(e: Exception) { target.delete(); throw e }
    }
}
