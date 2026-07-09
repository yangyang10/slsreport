package com.xiaotimel.sls.log.task

import kotlinx.coroutines.Job

abstract class BaseLogTask: LogTask {

    private var job: Job? = null

    fun Job.setToRelease() {
        job = this
    }

    override fun release() {
        job?.cancel()
    }
}