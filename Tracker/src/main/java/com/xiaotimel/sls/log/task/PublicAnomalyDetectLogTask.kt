package com.xiaotimel.sls.log.task

import com.xiaotimel.sls.log.SLSReporter
import com.xiaotimel.sls.log.constants.ReportEvent
import com.xiaotimel.sls.log.utils.EmulatorCheckUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PublicAnomalyDetectLogTask : BaseLogTask() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun initTask() {
        GlobalScope.launch(Dispatchers.IO) {
            EmulatorCheckUtil.readSysProperty(SLSReporter.instance.getApp()) {
                SLSReporter.instance.report(ReportEvent.PUBLIC_ANOMALY_DETECTION, it)
            }
        }
    }
}