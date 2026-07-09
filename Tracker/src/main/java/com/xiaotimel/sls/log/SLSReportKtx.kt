package com.xiaotimel.sls.log

import com.xiaotimel.sls.log.constants.ReportEvent
import com.xiaotimel.sls.log.utils.SLSSpUtils

fun SLSReporter.openApp(source: Int, isColdStart: Boolean = false, linkUrl: String = "", pushId: String = "") {
    report(
        ReportEvent.OPEN_APP,
        mapOf(
            "open_source" to source,
            "cold_start" to if (isColdStart) 1 else 0,
            "link_url" to linkUrl,
            "push_id" to pushId
        )
    )
}
fun SLSReporter.pushArrive(pushId: String, isOffline: Boolean) {
    report(
        ReportEvent.PUSH_ARRIVE,
        mapOf(
            "push_id" to pushId,
            "is_offline" to if (isOffline) 1 else 0
        )
    )
}

fun SLSReporter.pushClick(pushId: String) {
    report(
        ReportEvent.PUSH_CLICK,
        mapOf(
            "push_id" to pushId,
        )
    )
}

fun SLSReporter.checkOfflinePush() {
    val offlinePushId = SLSSpUtils.getOfflinePushId()
    if (offlinePushId.isNotEmpty()) {
        this.pushArrive(offlinePushId, true)
        SLSSpUtils.saveOfflineId("")
    }
}