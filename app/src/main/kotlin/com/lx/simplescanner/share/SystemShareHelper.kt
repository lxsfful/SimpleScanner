package com.lx.simplescanner.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * 系统分享：使用 Intent.createChooser 列出所有可分享 PDF 的 App
 * （微信、QQ、邮件、Drive、AirDrop 等任选其一）。
 *
 * 不需要任何 SDK 注册或 AppID。
 */
object SystemShareHelper {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun sharePdf(context: Context, file: File, chooserTitle: String = "分享 PDF") {
        val authority = "${context.packageName}$AUTHORITY_SUFFIX"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
