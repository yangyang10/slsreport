package com.xiaotimel.sls.log.task

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import com.xiaotimel.sls.log.SLSReporter
import com.xiaotimel.sls.log.constants.LaunchSource
import com.xiaotimel.sls.log.constants.ReportEvent
import com.xiaotimel.sls.log.openApp

class PageMonitorLogTask(private val application: Application) : LogTask, Application.ActivityLifecycleCallbacks {

    private var activeClassCount = 0
    private var isRunInBackground = false
    private var lastForegroundTime = SystemClock.uptimeMillis()
    private var lastBackgroundTime = SystemClock.uptimeMillis()
    private var hasReportOpenApp = false

    var lastPageClass = ""
    var currentPageClass = ""
    var currentClassResumeTime = SystemClock.uptimeMillis()
    private var totalForegroundTime = 0L
    private var totalBackgroundTime = 0L

    override fun initTask() {
        application.registerActivityLifecycleCallbacks( this)
    }

    override fun release() {
        application.unregisterActivityLifecycleCallbacks(this)
    }

    fun isForeground(): Boolean {
        return activeClassCount > 0
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        SLSReporter.slsDebugLog("onActivityCreated: ${activity.javaClass.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
        SLSReporter.slsDebugLog("onActivityStarted: ${activity.javaClass.simpleName}, updateCurrent:${activity.getCurrentFullName()}")
        if (!hasActivities()) {
            if (!hasReportOpenApp) {
                SLSReporter.instance.openApp(LaunchSource.LAUNCH_SOURCE_ICON, true)
                hasReportOpenApp = true
            } else {
                SLSReporter.instance.openApp(LaunchSource.LAUNCH_SOURCE_ICON, false)
            }
        }
        activeClassCount++
        if (isRunInBackground) {
            isRunInBackground = false
            // 更新最后的后台时间时刻
            lastBackgroundTime = SystemClock.uptimeMillis()
            totalBackgroundTime += (lastBackgroundTime - lastForegroundTime)
        }
    }

    private fun hasActivities(): Boolean {
        return activeClassCount > 0
    }

    override fun onActivityResumed(activity: Activity) {
        SLSReporter.slsDebugLog("onActivityResumed: ${activity.javaClass.simpleName}")
        currentClassResumeTime = SystemClock.uptimeMillis()
        val current = activity.getCurrentFullName()
        if (currentPageClass == current) {
            return
        }
        lastPageClass = currentPageClass
        currentPageClass = current
    }

    override fun onActivityPaused(activity: Activity) {
        SLSReporter.slsDebugLog("onActivityPaused: ${activity.javaClass.simpleName}, " +
                "stayTime:${SystemClock.uptimeMillis() - currentClassResumeTime}")
        SLSReporter.instance.report(
            ReportEvent.VIEW_SCREEN,
            mapOf("last_class_staytime" to (SystemClock.uptimeMillis() - currentClassResumeTime))
        )
    }

    override fun onActivityStopped(activity: Activity) {
        SLSReporter.slsDebugLog("onActivityStopped: ${activity.javaClass.simpleName}")
        activeClassCount--
        if (!isForeground()) {
            isRunInBackground = true
            // 更新最后的前台时间时刻
            lastForegroundTime = SystemClock.uptimeMillis()
            totalForegroundTime += (lastForegroundTime - lastBackgroundTime)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        SLSReporter.slsDebugLog("onActivityDestroyed: ${activity.javaClass.simpleName}")
    }

    /**
     * 如果当前是在前台，总的前台时间就是之前总前台时间 +（当前时间 - 最后的后台时间）
     */
    fun getForegroundTime(): Long {
        return if (isForeground()) {
            totalForegroundTime + (SystemClock.uptimeMillis() - lastBackgroundTime)
        } else {
            totalForegroundTime
        }
    }

    /**
     * 如果当前是在后台，总的后台时间就是之前总后台时间 +（当前时间 - 最后的前台时间）
     */
    fun getBackgroundTime(): Long {
        return if (isForeground()) {
            totalBackgroundTime
        } else {
            totalBackgroundTime + (SystemClock.uptimeMillis() - lastForegroundTime)
        }
    }

    private fun Activity.getCurrentFullName(): String {
        return this::class.qualifiedName?:""
    }
}