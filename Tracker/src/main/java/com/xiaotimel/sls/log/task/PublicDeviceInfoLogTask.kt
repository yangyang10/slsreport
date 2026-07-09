package com.xiaotimel.sls.log.task

import android.os.Build
import com.xiaotimel.sls.log.SLSReporter
import com.xiaotimel.sls.log.constants.ReportEvent
import com.xiaotimel.sls.log.model.AllDeviceId
import com.xiaotimel.sls.log.utils.SLSCommonUtils
import com.xiaotimel.sls.log.utils.SLSNetworkUtils
import com.xiaotimel.sls.log.utils.getScreenHeight
import com.xiaotimel.sls.log.utils.getScreenWidth
import com.xiaotimel.sls.log.utils.toJSON
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class PublicDeviceInfoLogTask : BaseLogTask() {

    private var reportCount = AtomicInteger(1)
    private var delayCount = AtomicInteger(1)

    @OptIn(DelicateCoroutinesApi::class)
    override fun initTask() {
        GlobalScope.launch(Dispatchers.IO) {
            launch {
                registerUpdate()
            }
            while (isActive) {
                try {
                    if (delayCount.get() > 3) {
                        return@launch
                    }
                    reportInternal()
                    delay(getDelayDuration())
                    delayCount.getAndIncrement()
                } catch (e: Exception) {
                    break
                }
            }
        }.setToRelease()
    }

    private suspend fun registerUpdate() {
        val updatableInfo = SLSReporter.instance.updatableInfo ?: return
        combine(updatableInfo.googleAdIdFlow, updatableInfo.userPseudoIdFlow) { values ->
            values
        }.distinctUntilChanged()
            .collect { combineValue ->
                SLSReporter.slsDebugLog("registerUpdate: ${combineValue.contentToString()}")
                if (combineValue.filterNotNull().isEmpty()) {
                    return@collect
                }
                reportInternal()
            }
    }

    override fun getDelayDuration(): Long {
        return 3 * 60 * 1000L
    }

    private fun reportInternal() {
        SLSReporter.instance.report(
            ReportEvent.PUBLIC_DEVICE_INFO,
            mapOf(
                "all_devices_id" to getAllDeviceId(),
                "model_os" to "android",
                "model_os_ver" to Build.VERSION.SDK_INT,
                "model_name" to Build.MODEL,
                "model_vendor" to Build.BRAND,
                "model_mac" to SLSNetworkUtils.getMacAddress(),
                "model_ram" to SLSCommonUtils.getRamMemory(),
                "model_rom" to SLSCommonUtils.getRomMemory(),
                "model_language" to SLSReporter.instance.getInitParam().getLocalLanguage(),
                "model_resolution" to ("${SLSReporter.instance.getApp().getScreenWidth()}-${
                    SLSReporter.instance.getApp().getScreenHeight()
                }"),
                "app_channel" to SLSReporter.instance.getFlavor(),
                "app_local_path" to SLSReporter.instance.getApp().packageCodePath,
                "media_source" to getAdjustFrom(),
                "report_amount" to reportCount.getAndIncrement()
            )
        )
    }

    private fun getAdjustFrom(): String {
        return SLSReporter.instance.getInitParam().getAdjustFrom()
    }

    private fun getAllDeviceId(): String {
        val updatableInfo = SLSReporter.instance.updatableInfo
            ?: return AllDeviceId(androidId = SLSReporter.instance.getInitParam().getDeviceId()).toJSON()
        val googleAdvertisingId = updatableInfo.googleAdIdFlow.value
        val userPseudoId = updatableInfo.userPseudoIdFlow.value
        return AllDeviceId(
            googleAdvertisingId,
            SLSReporter.instance.getInitParam().getDeviceId(),
             userPseudoId
        ).toJSON()
    }
}