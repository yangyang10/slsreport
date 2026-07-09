package com.xiaotimel.sls.log.task

import android.view.MotionEvent
import com.xiaotimel.sls.log.SLSReporter
import com.xiaotimel.sls.log.constants.ReportEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TouchEventLogTask : BaseLogTask() {

    private var clickPoints = hashSetOf<String>()
    private var clickCount = 0

    fun trackClick(event: MotionEvent?) {
        event ?: return
        if (event.action == MotionEvent.ACTION_DOWN) {
            val point = "${event.x},${event.y}"
            clickPoints.add(point)
            clickCount++
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun initTask() {
        GlobalScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(getDelayDuration()) // 每5分钟执行一次
                processClicks(clickPoints.size, clickCount)
                clickPoints.clear() // 清理点击坐标，准备下一个周期的收集
                clickCount = 0
            }
        }.setToRelease()
    }

    override fun getDelayDuration(): Long {
        return 5 * 60 * 1000L
    }

    private fun processClicks(size: Int, totalCount: Int) {
        SLSReporter.instance.report(
            ReportEvent.PUBLIC_CLICK_DETECTION,
            mapOf(
                "click_coord_amount" to size,
                "click_amount" to totalCount
            )
        )
    }
}