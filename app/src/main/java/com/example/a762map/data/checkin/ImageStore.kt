package com.example.a762map.data.checkin

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class ImageStore(private val context: Context) {

    private fun checkinDir(): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val dir = File(base, "checkins")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createNewCheckinImageUri(): Uri? {
        val dir = checkinDir()
        val file = File(dir, "checkin_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun resolvePathFromUri(uri: Uri): String? {
        // 这里的 uri 来自 FileProvider，path 即 app 内实际文件路径
        // content://... 不能直接取 path，所以通过 FileProvider 的目录结构反推不可靠
        // 最稳做法：用 file_paths 里配置的 external-files-path，FileProvider 会映射到该目录；
        // 我们只要遍历 checkins 目录里“最新文件”也行，但这里直接用 uri->file 不可取。
        // 解决：createNewCheckinImageUri() 时我们自己就知道 file，但 TakePicture 只返回 bool。
        // 因此这里采用：从 uri 的 lastPathSegment 推导文件名，然后拼回 checkins 目录。
        val name = uri.lastPathSegment ?: return null
        // lastPathSegment 可能包含路径片段，取最后一段
        val fileName = name.substringAfterLast('/')
        val file = File(checkinDir(), fileName)
        return if (file.exists()) file.absolutePath else null
    }

    fun copyFromUriToCheckins(inputUri: Uri): String? {
        val dir = checkinDir()
        val outFile = File(dir, "checkin_${System.currentTimeMillis()}.jpg")

        return try {
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun filePathToContentUri(filePath: String): Uri? {
        val f = File(filePath)
        if (!f.exists()) return null
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            f
        )
    }
}
