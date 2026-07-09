package com.xiaotimel.sls.log.identifier

abstract class BaseIdChecker: IdChecker {

    private var hasInit = false
    private var enable = false

    override fun isEnable(): Boolean {
        if (hasInit) {
            return enable
        }
        enable = checkEnable()
        hasInit = true
        return enable
    }

    protected abstract fun checkEnable(): Boolean

    override fun checkIdentifier(callback: (String) -> Unit): Boolean {
        if (!isEnable()) {
            callback("")
            return true
        }
        return false
    }
}