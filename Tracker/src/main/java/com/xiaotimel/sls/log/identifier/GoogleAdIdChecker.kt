package com.xiaotimel.sls.log.identifier

import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.xiaotimel.sls.log.SLSReporter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GoogleAdIdChecker : BaseIdChecker() {

    override fun checkEnable(): Boolean {
        return try {
            Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
            true
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun checkIdentifier(callback: (String) -> Unit): Boolean {
        SLSReporter.slsDebugLog("GoogleIdAdId.checkIdentifier() isEnable: ${isEnable()}")
        if (super.checkIdentifier(callback)) {
            return true
        }
        GlobalScope.launch {
            try {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(SLSReporter.instance.getApp())
                SLSReporter.slsDebugLog("GoogleIdAdId.checkIdentifier() = $info")
                callback(info.id ?: "")
            } catch (e: Exception) {
                SLSReporter.slsDebugLog("GoogleIdAdId.checkIdentifier() = $e")
                callback("")
            }
        }
        return true
    }
}