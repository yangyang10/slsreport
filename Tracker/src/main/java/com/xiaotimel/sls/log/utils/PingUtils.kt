package com.xiaotimel.sls.log.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object PingUtils {

    fun getPingDelay(host: String?): Float {
        host ?: return -1f
        val command = "ping -c 1 $host"
        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.apply {
                    if (contains("time=")) {
                        val start = indexOf("time=")
                        val end = indexOf(" ms")
                        val delayStr = substring(start + 5, end)
                        return delayStr.toFloat()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return (-1).toFloat()
    }
}