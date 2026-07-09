package com.xiaotimel.sls.log.task

import com.xiaotimel.sls.log.SLSReporter
import com.xiaotimel.sls.log.constants.ReportEvent
import com.xiaotimel.sls.log.utils.PingUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PingLogTask : BaseLogTask() {

    private val GOOGLE = "www.google.com"
    private var preAverageTime = 0L
    private var needMoreFrequency = false

    @OptIn(DelicateCoroutinesApi::class)
    override fun initTask() {
        GlobalScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    doPingTest()
                    delay(getDelayDuration())
                } catch (e: Exception) {
                    break
                }
            }
        }.setToRelease()
    }

    override fun getDelayDuration(): Long {
        return if (needMoreFrequency) {
            60 * 1000L
        } else {
            3 * 60 * 1000L
        }
    }

    private fun doPingTest() {
        val googlePing = PingUtils.getPingDelay(GOOGLE)
        val mqjcPing = PingUtils.getPingDelay(SLSReporter.instance.getInitParam().pingHost)
        val currentAverage = if (googlePing < 0) {
            mqjcPing
        } else {
            (googlePing + mqjcPing) / 2
        }
        needMoreFrequency = preAverageTime > 100 && currentAverage > 100
        preAverageTime = currentAverage.toLong()
        SLSReporter.report(
            ReportEvent.PUBLIC_NETWORK_DETECTION,
            mapOf(
                "gg_ping" to googlePing,
                "mq_ping" to mqjcPing
            )
        )
    }
}