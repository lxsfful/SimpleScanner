package com.lx.simplescanner.util

import android.content.Context
import android.widget.Toast
import com.lx.simplescanner.SimpleScannerApp

/**
 * Toast 工具：统一从任意位置弹 Toast（短/长）。
 */
object Toast {

    fun short(context: Context = SimpleScannerApp.appContext, msg: CharSequence) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun long(context: Context = SimpleScannerApp.appContext, msg: CharSequence) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
}
