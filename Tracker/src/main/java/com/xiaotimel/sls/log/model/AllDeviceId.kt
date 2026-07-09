package com.xiaotimel.sls.log.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName


// """{"google_advertising_id":"$googleAdvertisingId", "android_id":"${DeviceUtils.getAndroidId()}","adjust_id":"$adjustId","user_pseudo_id":"$userPseudoId"}""".trimMargin()
@Keep
data class AllDeviceId(
    @SerializedName("google_advertising_id")
    var googleAdvertisingId: String? = null,
    @SerializedName("android_id")
    var androidId: String? = null,
    @SerializedName("user_pseudo_id")
    var userPseudoId: String? = null
)