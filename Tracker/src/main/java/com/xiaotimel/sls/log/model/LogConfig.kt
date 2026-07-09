package com.xiaotimel.sls.log.model

import androidx.annotation.Keep


@Keep
data class LogConfig(
    val eventConfigList: List<LogConfigItem> = emptyList(),
    val version: String = ""
)

@Keep
data class LogConfigItem(
    val eventName: String,
    val isReport: Int = 1,
    val reportSamrate: Int? = 0
){

    /**
     * 当事件名不在这个配置中，默认走上报，
     * 当在这个配置中，is_report = 0不上报，is_report = 1
     * 上报并且根据后面的report_samrate/10000来确定上报比率，
     * 对session_id进行哈希，然后/10000取模，< report_samrate的进行上报，>= 的不上报
     */
    fun needReport(sessionHash: Int): Boolean {
        if (isReport == 0) {
            return false
        }
        return (sessionHash % 10000) < (reportSamrate ?: 0)
    }
}