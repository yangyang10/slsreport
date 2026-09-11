package com.xiaotimel.sls.log

import android.app.Application
import android.view.MotionEvent
import android.webkit.WebView
import com.aliyun.sls.android.producer.Log
import com.aliyun.sls.android.producer.LogProducerClient
import com.aliyun.sls.android.producer.LogProducerConfig
import com.aliyun.sls.android.producer.LogProducerResult
import com.xiaotimel.sls.log.constants.ReportEvent
import com.xiaotimel.sls.log.constants.ReportKey
import com.xiaotimel.sls.log.model.LogConfig
import com.xiaotimel.sls.log.model.SLSLogConfig
import com.xiaotimel.sls.log.model.UpdatableInfo
import com.xiaotimel.sls.log.task.HeartBeatLogTask
import com.xiaotimel.sls.log.task.LogTask
import com.xiaotimel.sls.log.task.PageMonitorLogTask
import com.xiaotimel.sls.log.task.PingLogTask
import com.xiaotimel.sls.log.task.PublicAnomalyDetectLogTask
import com.xiaotimel.sls.log.task.PublicDeviceInfoLogTask
import com.xiaotimel.sls.log.task.StabDetectLogTask
import com.xiaotimel.sls.log.task.TouchEventLogTask
import com.xiaotimel.sls.log.utils.NetworkWIFI
import com.xiaotimel.sls.log.utils.SLSJSBridge
import com.xiaotimel.sls.log.utils.SLSNetworkUtils
import com.xiaotimel.sls.log.utils.getVersionName
import com.xiaotimel.sls.log.utils.toJSON
import kotlinx.coroutines.*
import java.io.File
import java.util.*

/**
 * Created by yangzaorong@mqjc.com on 2023/4/7
 * Desc:
 */
class SLSReporter private constructor(private val builder: Builder) {
    companion object {

        private const val MAX_ERROR_REPORT_COUNT = 3
        private const val STATE_UN_INIT = -1
        private const val STATE_LOG_ENABLE = 0
        private const val STATE_LOG_DISABLE = 1

        const val TAG = "SLSReporter"

        inline fun build(application: Application, block: (Builder.() -> Unit)) = Builder(application).apply(block).build()

        @JvmStatic
        lateinit var instance: SLSReporter
            private set

        fun slsDebugLog(msg: String) {
            if (instance.builder.isDebug) {
                android.util.Log.d(TAG, msg)
            }
        }

        fun report(key: String, params: Map<String, Any?>) {
            instance.report(key, params)
        }

        private var errorReportCount = 0
    }

    fun init() {
        if (firstInit) {
            return
        }
        init(builder)
        firstInit = true
    }

    private var logProducerClient: LogProducerClient? = null


    private var firstInit = false
    @Volatile
    private var logState = STATE_UN_INIT
    private var isFetchingToken = false
    private val uuid = UUID.randomUUID()
    private val sessionId = uuid.toString()
    private var sessionIdHash = uuid.hashCode()
    private val tasks = mutableListOf<LogTask>()
    private val pendingLogs = mutableListOf<Log>()
    private lateinit var pageMonitorLogTask: PageMonitorLogTask
    private val logConfigManager = LogConfigManager(sessionIdHash)
    private lateinit var app: Application
    internal var updatableInfo: UpdatableInfo? = null

    fun getInitParam() = builder

    fun getApp() = app

    private fun init(builder: Builder) {
        instance = this
        app = builder.application
        updatableInfo = UpdatableInfo()
        initLogTask(app)
        checkOfflinePush()
        checkRefreshToken()
    }

    /**
     * 目前 sdk 上报日志需要登录后的 token，所以需要外部监听登录事件之后再调用 token 检查
     */
    fun checkRefreshToken() {
        if (getInitParam().isLogin && logState == STATE_UN_INIT) {
            slsDebugLog("refresh token start")
            logConfigManager.initConfig()
            getSLSConfig {
                init(app, it)
            }
        }
    }

    private fun init(application: Application, slsLogBean: SLSLogConfig) {
        slsDebugLog("init sls log: $slsLogBean")
        val config = LogProducerConfig(
            application,
            slsLogBean.endpoint,
            slsLogBean.project,
            slsLogBean.logStore,
            slsLogBean.accessKeyId,
            slsLogBean.accessKeySecret,
            slsLogBean.securityToken
        )

        config.setTopic("FULL_LINK_ANDROID")
        try {
            val logDir = getAppLogDirectory()
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            /**
             * 1表示开启断点续传功能，0表示关闭。默认值为0
             */
            config.setPersistent(1)
            config.setPersistentFilePath(logDir.path + "/log.dat")
            // 持久化文件滚动个数，建议设置为10
            config.setPersistentMaxFileCount(10)
            // 每个持久化文件的大小，单位为Byte，格式为N*1024*1024。建议N的取值范围为1~10
            config.setPersistentMaxFileSize(1024 * 1024)
            config.setPersistentMaxLogCount(65536)
            config.setCallbackFromSenderThread(false)
        } catch (e: Exception) {
            slsDebugLog("sls dir error")
        }
        try {
            logProducerClient = LogProducerClient(config) { resultCode, _, errorMessage, _, _ ->
                val producerResult = LogProducerResult.fromInt(resultCode)
                if (producerResult == LogProducerResult.LOG_PRODUCER_OK) {
                    slsDebugLog("log report success")
                    return@LogProducerClient
                }
                slsDebugLog("sls callback rc: $resultCode," + "msg: $errorMessage," + "pResult=${producerResult.name}")
                if (producerResult == LogProducerResult.LOG_PRODUCER_SEND_UNAUTHORIZED) {
                    getSLSConfig {
                        slsDebugLog("sls resetToken success")
                        config.resetSecurityToken(it.accessKeyId, it.accessKeySecret, it.securityToken)
                    }
                } else {
                    if (errorReportCount >= MAX_ERROR_REPORT_COUNT) {
                        // wocute 遇到一个问题，后台不断上报，停不下来
                        return@LogProducerClient
                    }
                    errorReportCount++
                    httpReport(
                        ReportEvent.FAIL_REPORT_EVENT, mutableMapOf(
                            ReportKey.NAME to producerResult.name,
                            ReportKey.VALUE to errorMessage
                        )
                    )
                }
            }
            checkPendingLog()
        } catch (e: Exception) {
            slsDebugLog("sls初始化失败 e=${e.message}")
            httpReport(ReportEvent.FAIL_INIT_EVENT, mutableMapOf(ReportKey.MSG to e.localizedMessage))
        }
    }

    private fun checkPendingLog() {
        synchronized(pendingLogs) {
            if (pendingLogs.isEmpty()) {
                return
            }
            pendingLogs.forEach {
                report(it)
            }
            pendingLogs.clear()
        }
    }

    private fun getAppLogDirectory(): File {
        return File(app.filesDir.absoluteFile, "log")
    }

    private fun initLogTask(application: Application) {
        pageMonitorLogTask = PageMonitorLogTask(application)
        tasks += pageMonitorLogTask
        tasks += PingLogTask()
//        tasks += StabDetectLogTask()
        tasks += PublicDeviceInfoLogTask()
        tasks += PublicAnomalyDetectLogTask()
//        tasks += TouchEventLogTask()
//        tasks += HeartBeatLogTask()
        tasks.forEach { it.initTask() }
    }

    /**
     * 统计 sls 相关报错
     */
    private fun httpReport(id: String, value: MutableMap<String, Any?>) {
        slsDebugLog("HttpReport:$id, values:$value")
        builder.errorCallback(id, value)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getSLSConfig(success: (SLSLogConfig) -> Unit) {
        if (isFetchingToken) {
            return
        }
        isFetchingToken = true
        val exceptionHandler = CoroutineExceptionHandler { context, e ->
            isFetchingToken = false
            slsDebugLog("exception occurred in ${context[CoroutineName]}], $e")
        }
        GlobalScope.launch(context = exceptionHandler) {
            val slsLog: SLSLogConfig? = builder.getSLSLogConfig()
            isFetchingToken = false
            slsLog?.let {
                if (it.enabled) {
                    logState = STATE_LOG_ENABLE
                    success(slsLog)
                } else {
                    logState = STATE_LOG_DISABLE
                    release()
                }
            }
        }
    }

    private fun report(log: Log) {
        when (logState) {
            STATE_UN_INIT -> {
                synchronized(pendingLogs) {
                    if (pendingLogs.size > 1000) {
                        return
                    }
                    pendingLogs += log
                    slsDebugLog("pending log: ${pendingLogs.size}")
                }
            }
            STATE_LOG_ENABLE -> {
                if (logProducerClient != null) {
                    val result = logProducerClient?.addLog(log)
                    slsDebugLog("sls add log $result")
                }
            }
            else -> { return }
        }
    }

    /**
     * 释放
     */
    private fun release() {
        slsDebugLog("release SLSReporter")
        tasks.forEach { it.release() }
        tasks.clear()
        synchronized(pendingLogs){
            pendingLogs.clear()
        }
        logProducerClient = null
    }

    /**
     * 协议文档
     * https://alidocs.dingtalk.com/i/nodes/7NkDwLng8ZK1mXEDSDAP0wElWKMEvZBY?cid=51329914790&iframeQuery=utm_medium%3Dim_card%26utm_source%3Dim&utm_medium=im_group_card&utm_source=im&utm_scene=team_space&dontjump=true&corpId=dingf4a0b33d4983933135c2f4657eb6378f
     */
    private fun getCommonLog(): Log {
        val log = Log()
        //用户ID
        log.putContent("uid", if (builder.isLogin) builder.getLogin() else "unlogin")
        updatableInfo?.locationFlow?.value?.apply {
            log.putContent("longitude", this.longitude)
            log.putContent("latitude", this.latitude)
        }
        //会话uuid
        log.putContent("session_id", sessionId)
        //设备ID
        log.putContent("device_id", builder.getDeviceId())
        log.putContent("app_name", builder.appName)
        log.putContent("app_version", app.getVersionName())
        //设备网络情况
        SLSNetworkUtils.getNetworkType(app).let {
            log.putContent("net_type", it.desc)
            if (it is NetworkWIFI) {
                //设备连接的wifi名称
                log.putContent("wifi_name", it.name)
            } else {
                log.putContent("wifi_name", "none")
            }
        }
        //产生日志的时间戳（用户本地时间）
        log.putContent("local_timestamp", System.currentTimeMillis().toString())
        //产生日志的时间戳（服务器同步时间）
        log.putContent("report_time", builder.getServerTime().toString())
        if (this::pageMonitorLogTask.isInitialized) {
            log.putContent("current_pageclass", pageMonitorLogTask.currentPageClass)
            log.putContent("last_pageclass", pageMonitorLogTask.lastPageClass)
        }
        log.putContent(ReportKey.EVENT_ID, UUID.randomUUID().toString())
        return log
    }

    internal fun getTotalForegroundTime(): Long {
        if (!this::pageMonitorLogTask.isInitialized) {
            if (builder.isDebug) {
                throw Exception("init SLSReportHelper first!!!")
            }
            return -1L
        }
        return pageMonitorLogTask.getForegroundTime()
    }

    internal fun getTotalBackgroundTime(): Long {
        if (!this::pageMonitorLogTask.isInitialized) {
            if (builder.isDebug) {
                throw Exception("init SLSReportHelper first!!!")
            }
            return -1L
        }
        return pageMonitorLogTask.getBackgroundTime()
    }

    private fun needReport(event: String): Boolean = logConfigManager.shouldReport(event)

    fun getCurrentClassResumeTime(): Long {
        if (!this::pageMonitorLogTask.isInitialized) {
            throw Exception("init SLSReportHelper first!!!")
        }
        return pageMonitorLogTask.currentClassResumeTime
    }

    fun trackTouch(event: MotionEvent?) {
        tasks
            .filterIsInstance<TouchEventLogTask>()
            .getOrNull(0)?.trackClick(event)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun report(key: String, params: Map<String, Any?>) {
        if (!needReport(key)) {
            slsDebugLog("ignore report: $key, params: $params")
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            slsDebugLog("report log, key: $key, params: $params")
            val log = getCommonLog()
            log.putContent(ReportKey.EVENT_NAME, key)
            log.putContent(ReportKey.EVENT_TIME, builder.getServerTime().toString())
            log.putContent(ReportKey.EVENT_INFO, params.toJSON())
            report(log)
        }
    }

    fun getFlavor(): String = builder.flavor

    fun addSLSBridge(webView: WebView) {
        webView.addJavascriptInterface(SLSJSBridge(), "SLS")
    }

    class Builder(val application: Application) {

        var isDebug = true
        var flavor: String = ""
        var pingHost: String = ""
        var appName: String = ""
        var getLogin: () -> String? = { "" }
        var getDeviceId: () -> String = { "" }
        var getAdjustFrom: () -> String = { "" }
        var getServerTime: () -> Long = { 0L }
        var getLocalLanguage: () -> String? = { "" }
        var errorCallback: (String, Map<String, Any?>) -> Unit =
            { id, args -> slsDebugLog("id:$id, args: $args") }
        lateinit var getLogConfig: suspend () -> LogConfig?
        lateinit var getSLSLogConfig: suspend () -> SLSLogConfig?

        val isLogin: Boolean
            get() {
                return try {
                    !getLogin().isNullOrEmpty()
                } catch (e: Exception) {
                    false
                }
            }

        fun build() = SLSReporter(this)
    }
}
