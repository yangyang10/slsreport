package com.xiaotimel.sls.log.utils

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.FileReader

object SLSCommonUtils {

    private var totalRawMemory = -1L

    private const val MEMORY_MEMORY_INFO: String = "/proc/meminfo"

    @JvmStatic
    fun getRamMemory(): Long {
        if (totalRawMemory > -1) {
            return totalRawMemory
        }
        var ramMemorySize: String? = null
        var br: BufferedReader? = null
        try {
            val fileReader = FileReader(MEMORY_MEMORY_INFO)
            br = BufferedReader(fileReader, 4096)
            var line = ""
            if ((br.readLine()?.also { line = it }) != null) {
                ramMemorySize = line.split(":").getOrNull(1)?.trim()?.split(" ")?.getOrNull(0)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            br?.closeSilently()
        }

        if (ramMemorySize != null) {
            totalRawMemory = ramMemorySize.toLongOrNull()?.let { it * 1024 } ?: 0L
        }
        return totalRawMemory
    }

    @JvmStatic
    fun getRomMemory(): Long {
        try {
            val dataDir = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            return totalBlocks * blockSize
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }
}

/**
 * 获取屏幕高度
 * */
fun Application.getScreenHeight(): Int {
    val p = Point()
    val display = (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    display?.getRealSize(p)
    return p.y
}

/**
 * 获取屏幕宽度
 * @return 屏幕宽度 px
 * */
fun Application.getScreenWidth(): Int {
    val p: Point = Point()
    val display = (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    display?.getRealSize(p)
    return p.x
}

fun Application.getVersionName(): String {
    try {
        val pm: PackageManager = this.packageManager
        val packageInfo = pm.getPackageInfo(this.packageName, PackageManager.GET_CONFIGURATIONS)
        return packageInfo?.versionName?:"0.0"
    } catch (e: java.lang.Exception) {
        return "0.0"
    }
}


/**
 * An object into a string
 *
 * @param object
 * @return
 */
fun Any.toJSON(): String {
    return try {
         Gson().toJson(this)
    } catch (e: Exception) {
        """{"classInstance": "${this::class.java.canonicalName}", "exception": "${e.message}"}"""
    }
}

fun String.parseJSON2Map(): Map<String, Any> {
    val gson = GsonBuilder().enableComplexMapKeySerialization()
        .create()
    val map = gson.fromJson<Map<String, Any>>(this, object : TypeToken<Map<String?, Any?>?>() {}.type)
    return map
}