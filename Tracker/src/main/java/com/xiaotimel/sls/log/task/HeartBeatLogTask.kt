package com.xiaotimel.sls.log.task

import android.os.SystemClock
import com.xiaotimel.sls.log.SLSReporter
import com.xiaotimel.sls.log.constants.ReportEvent
import com.xiaotimel.sls.log.constants.ReportKey
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class HeartBeatLogTask : BaseLogTask() {
    private var count = AtomicInteger(1)

    @OptIn(DelicateCoroutinesApi::class)
    override fun initTask() {
        GlobalScope.launch(Dispatchers.IO) {
            while (isActive) {
                reportHeartBeat()
                delay(getDelayDuration())
            }
        }.setToRelease()
    }

    private fun reportHeartBeat() {
        SLSReporter.instance.apply {
            report(
                ReportEvent.HEART_BEAT,
                mapOf(
                    ReportKey.COUNT to count.getAndIncrement(),
                    "stay_time1" to getTotalForegroundTime(),
                    "stay_time2" to getTotalBackgroundTime(),
                    "current_class_staytime" to (SystemClock.uptimeMillis() - getCurrentClassResumeTime())
                )
            )
        }
    }

    override fun getDelayDuration(): Long {
        return 60 * 1000L
    }
}