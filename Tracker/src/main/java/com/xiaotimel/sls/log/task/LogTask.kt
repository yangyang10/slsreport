package com.xiaotimel.sls.log.task

interface LogTask {

    fun initTask()

    fun getDelayDuration(): Long = 0L

    fun release()
}