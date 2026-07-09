package com.xiaotimel.sls.log

import com.xiaotimel.sls.log.model.LogConfigItem
import com.xiaotimel.sls.log.utils.SLSSpUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class LogConfigManager(private val sessionHash: Int) {

    private var eventMap = ConcurrentHashMap<String, LogConfigItem>()

    @OptIn(DelicateCoroutinesApi::class)
    fun initConfig() {
        val exceptionHandler = CoroutineExceptionHandler { context, e ->
            checkLocalConfig()
            SLSReporter.slsDebugLog("exception occurred in ${context[CoroutineName]}], $e")
        }
        GlobalScope.launch(context = exceptionHandler) {
            val data = SLSReporter.instance.getInitParam().getLogConfig()
            SLSReporter.slsDebugLog("data: $data")
            if (data == null || data.version.isEmpty()) {
                checkLocalConfig()
            } else {
                SLSSpUtils.saveLogConfig(data)
                updateEventMap(data.eventConfigList)
            }
        }
    }

    private fun checkLocalConfig() {
        val config = SLSSpUtils.getConfig()
        if (config.isNotEmpty()) {
            updateEventMap(config)
        }
    }

    private fun updateEventMap(configList: List<LogConfigItem>) {
        val map = ConcurrentHashMap<String, LogConfigItem>()
        configList.forEach {
            map[it.eventName] = it
        }
        eventMap = map
    }

    fun shouldReport(event: String): Boolean {
        if (eventMap.contains(event)) {
            val item = eventMap[event] ?: return true
            if (!item.needReport(sessionHash)) {
                return false
            }
        }
        return true
    }
}