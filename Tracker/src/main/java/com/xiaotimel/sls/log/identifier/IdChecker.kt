package com.xiaotimel.sls.log.identifier

interface IdChecker {
    fun isEnable(): Boolean

    fun checkIdentifier(callback: (String) -> Unit): Boolean
}