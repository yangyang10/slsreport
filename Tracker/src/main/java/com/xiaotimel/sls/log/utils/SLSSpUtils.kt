package com.xiaotimel.sls.log.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.xiaotimel.sls.log.SLSReporter
import com.xiaotimel.sls.log.model.LogConfig
import com.xiaotimel.sls.log.model.LogConfigItem

object SLSSpUtils {

    private const val SP_NAME = "sls_log_sp"

    private const val KEY_CONFIG_VERSION = "key_config_version"

    private const val KEY_CONFIG_CONTENT = "key_config_content"

    private const val KEY_OFFLINE_PUSH_ID = "key_offline_push_id"

    private val sp: SharedPreferences by lazy {
        SLSReporter.instance.getApp().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    fun getConfigVersion() = sp.getString(KEY_CONFIG_VERSION, "") ?: ""

    fun getConfig(): List<LogConfigItem> {
        val content = sp.getString(KEY_CONFIG_CONTENT, "")
        if (content.isNullOrEmpty()) {
            return emptyList()
        }
        return content.split("||").map { configItem ->
            configItem.split(",,").run {
                LogConfigItem(this[0], this[1].toInt(), this.getOrNull(2)?.toInt())
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    fun saveLogConfig(config: LogConfig) {
        if (getConfigVersion() == config.version) {
            return
        }

        val content =
            config.eventConfigList.joinToString(separator = "||") { it.eventName + ",," + it.isReport + ",," + it.reportSamrate }
        SLSReporter.slsDebugLog("saveLogConfig: $content")

        sp.edit()
            .putString(KEY_CONFIG_VERSION, config.version)
            .putString(KEY_CONFIG_CONTENT,
                content)
            .commit()
    }

    @SuppressLint("ApplySharedPref")
    fun saveOfflineId(pushId: String) {
        sp.edit().putString(KEY_OFFLINE_PUSH_ID, pushId)
            .commit()
    }

    fun getOfflinePushId(): String {
        return sp.getString(KEY_OFFLINE_PUSH_ID, "")?:""
    }

}