package com.xiaotimel.sls.log.task

import com.xiaotimel.sls.log.SLSReporter
import com.xiaotimel.sls.log.constants.ReportEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class StabDetectLogTask : BaseLogTask() {

    private var count: AtomicInteger = AtomicInteger(1)
    private var maxValue = 100
    private lateinit var job: Job

    @OptIn(DelicateCoroutinesApi::class)
    override fun initTask() {
        GlobalScope.launch(Dispatchers.IO) {
            delay(Random.nextLong(30, 301) * 1000)
            while (isActive) {
                reportStableLog()
                delay(3 * 1000)
            }
        }.setToRelease()
    }

    override fun getDelayDuration(): Long {
        // 3s 一次
        return 3 * 1000L
    }

    private fun reportStableLog() {
        val value = count.getAndIncrement()
        if (value > maxValue) {
            release()
            return
        }
        SLSReporter.instance.report(
            ReportEvent.PUBLIC_STAB_DETECTION,
            mapOf(
                "count" to value
            )
        )
    }
}