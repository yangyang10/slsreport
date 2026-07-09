package com.xiaotimel.sls.log.utils

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.text.TextUtils
import com.xiaotimel.sls.log.utils.CheckResult.Companion.RESULT_EMULATOR
import com.xiaotimel.sls.log.utils.CheckResult.Companion.RESULT_MAYBE_EMULATOR
import com.xiaotimel.sls.log.utils.CheckResult.Companion.RESULT_UNKNOWN
import java.util.Locale

object EmulatorCheckUtil {
    private var suspectCount = 0
    private var emulatorCount = 0

    fun readSysProperty(context: Context, callback: (Map<String, Any?>) -> Unit) {
        val resultMap = mutableMapOf<String, Any?>()
        // 检测硬件
        checkFeaturesByHardware(resultMap).updateSuspect()
        //检测渠道
        checkFeaturesByFlavor(resultMap).updateSuspect()
        //检测设备型号
        checkFeaturesByModel(resultMap).updateSuspect()
        //检测硬件制造商
        checkFeaturesByManufacturer(resultMap).updateSuspect()
        //检测主板名称
        checkFeaturesByBoard(resultMap).updateSuspect()
        //检测主板平台
        checkFeaturesByPlatform(resultMap).updateSuspect()
        //检测基带信息
        checkFeaturesByBaseBand(resultMap).updateSuspect()
        //检测进程组信息
        checkFeaturesByCgroup(resultMap).updateSuspect()
        //检测传感器数量
        val sensorNumber = getSensorNumber(context)
        resultMap["sensor_number"] = sensorNumber
        if (sensorNumber <= 7) ++suspectCount
        //检测是否支持闪光灯
        val supportCameraFlash = supportCameraFlash(context)
        resultMap["support_camera_flash"] = supportCameraFlash
        if (!supportCameraFlash) ++suspectCount
        //检测是否支持相机
        val supportCamera = supportCamera(context)
        resultMap["support_camera"] = supportCamera
        if (!supportCamera) ++suspectCount
        //检测是否支持蓝牙
        val supportBluetooth = supportBluetooth(context)
        resultMap["support_bluetootsh"] = supportBluetooth
        if (!supportBluetooth) ++suspectCount
        //检测光线传感器
        val hasLightSensor = hasLightSensor(context)
        resultMap["has_light_sensor"] = hasLightSensor
        if (!hasLightSensor) ++suspectCount
        resultMap["suspect_count"] = suspectCount
        resultMap["emulator_count"] = emulatorCount
        callback(resultMap)
    }

    private fun CheckResult.updateSuspect() {
        when (this.result) {
            RESULT_MAYBE_EMULATOR -> ++suspectCount
            RESULT_EMULATOR -> ++emulatorCount
        }
    }

    private fun getUserAppNum(userApps: String): Int {
        if (TextUtils.isEmpty(userApps)) return 0
        val result = userApps.split("package:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return result.size
    }

    private fun getProperty(propName: String): String? {
        val property = CommandUtil.getProperty(propName)
        return if (TextUtils.isEmpty(property)) null else property
    }

    /**
     * 特征参数-硬件名称
     *
     * @return 0表示可能是模拟器，1表示模拟器，2表示可能是真机
     */
    private fun checkFeaturesByHardware(resultMap: MutableMap<String, Any?>): CheckResult {
        val hardware = getProperty("ro.hardware") ?: return CheckResult(RESULT_MAYBE_EMULATOR, null)
        val result: Int
        val tempValue = hardware.lowercase(Locale.getDefault())
        resultMap["hardware"] = tempValue
        result = when (tempValue) {
            "ttvm", "nox", "cancro", "intel", "vbox", "vbox86", "android_x86" -> RESULT_EMULATOR
            else -> RESULT_UNKNOWN
        }
        return CheckResult(result, hardware)
    }

    /**
     * 特征参数-渠道
     *
     * @return 0表示可能是模拟器，1表示模拟器，2表示可能是真机
     */
    private fun checkFeaturesByFlavor(resultMap: MutableMap<String, Any?>): CheckResult {
        val flavor = getProperty("ro.build.flavor") ?: return CheckResult(RESULT_MAYBE_EMULATOR, null)
        val result: Int
        val tempValue = flavor.lowercase(Locale.getDefault())
        resultMap["flavor"] = flavor
        result = if (tempValue.contains("vbox")) RESULT_EMULATOR
        else if (tempValue.contains("sdk_gphone")) RESULT_EMULATOR
        else RESULT_UNKNOWN
        return CheckResult(result, flavor)
    }

    /**
     * 特征参数-设备型号
     *
     * @return 0表示可能是模拟器，1表示模拟器，2表示可能是真机
     */
    private fun checkFeaturesByModel(resultMap: MutableMap<String, Any?>): CheckResult {
        val model = getProperty("ro.product.model") ?: return CheckResult(RESULT_MAYBE_EMULATOR, null)
        val result: Int
        val tempValue = model.lowercase(Locale.getDefault())
        resultMap["model"] = tempValue
        result = if (tempValue.contains("google_sdk")) RESULT_EMULATOR
        else if (tempValue.contains("emulator")) RESULT_EMULATOR
        else if (tempValue.contains("android sdk built for x86")) RESULT_EMULATOR
        else RESULT_UNKNOWN
        return CheckResult(result, model)
    }

    /**
     * 特征参数-硬件制造商
     *
     * @return 0表示可能是模拟器，1表示模拟器，2表示可能是真机
     */
    private fun checkFeaturesByManufacturer(resultMap: MutableMap<String, Any?>): CheckResult {
        val manufacturer = getProperty("ro.product.manufacturer") ?: return CheckResult(RESULT_MAYBE_EMULATOR, null)
        val result: Int
        val tempValue = manufacturer.lowercase(Locale.getDefault())
        resultMap["manufacturer"] = manufacturer
        result = if (tempValue.contains("genymotion")) RESULT_EMULATOR
        else if (tempValue.contains("netease")) RESULT_EMULATOR //网易MUMU模拟器
        else RESULT_UNKNOWN
        return CheckResult(result, manufacturer)
    }

    /**
     * 特征参数-主板名称
     *
     * @return 0表示可能是模拟器，1表示模拟器，2表示可能是真机
     */
    private fun checkFeaturesByBoard(resultMap: MutableMap<String, Any?>): CheckResult {
        val board = getProperty("ro.product.board") ?: return CheckResult(RESULT_MAYBE_EMULATOR, null)
        val result: Int
        val tempValue = board.lowercase(Locale.getDefault())
        resultMap["board"] = board
        result = if (tempValue.contains("android")) RESULT_EMULATOR
        else if (tempValue.contains("goldfish")) RESULT_EMULATOR
        else RESULT_UNKNOWN
        return CheckResult(result, board)
    }

    /**
     * 特征参数-主板平台
     *
     * @return 0表示可能是模拟器，1表示模拟器，2表示可能是真机
     */
    private fun checkFeaturesByPlatform(resultMap: MutableMap<String, Any?>): CheckResult {
        val platform = getProperty("ro.board.platform") ?: return CheckResult(RESULT_MAYBE_EMULATOR, null)
        val result: Int
        val tempValue = platform.lowercase(Locale.getDefault())
        resultMap["platform"] = tempValue
        result = if (tempValue.contains("android")) RESULT_EMULATOR
        else RESULT_UNKNOWN
        return CheckResult(result, platform)
    }

    /**
     * 特征参数-基带信息
     *
     * @return 0表示可能是模拟器，1表示模拟器，2表示可能是真机
     */
    private fun checkFeaturesByBaseBand(resultMap: MutableMap<String, Any?>): CheckResult {
        val baseBandVersion = getProperty("gsm.version.baseband") ?: return CheckResult(RESULT_MAYBE_EMULATOR, null)
        resultMap["base_band"] = baseBandVersion
        val result: Int = if (baseBandVersion.contains("1.0.0.0")) RESULT_EMULATOR
        else RESULT_UNKNOWN
        return CheckResult(result, baseBandVersion)
    }

    /**
     * 获取传感器数量
     */
    private fun getSensorNumber(context: Context): Int {
        val sm = context.getSystemService(SENSOR_SERVICE) as SensorManager
        return sm.getSensorList(Sensor.TYPE_ALL).size
    }

    /**
     * 是否支持相机
     */
    private fun supportCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    /**
     * 是否支持闪光灯
     */
    private fun supportCameraFlash(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    /**
     * 是否支持蓝牙
     */
    private fun supportBluetooth(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    /**
     * 判断是否存在光传感器来判断是否为模拟器
     * 部分真机也不存在温度和压力传感器。其余传感器模拟器也存在。
     *
     * @return false为模拟器
     */
    private fun hasLightSensor(context: Context): Boolean {
        val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) //光线传感器
        return null != sensor
    }

    /**
     * 特征参数-进程组信息
     */
    private fun checkFeaturesByCgroup(resultMap: MutableMap<String, Any?>): CheckResult {
        val filter: String = CommandUtil.exec("cat /proc/self/cgroup")
            ?: return CheckResult(RESULT_MAYBE_EMULATOR, null)
        resultMap["cgroup_result"] = filter
        return CheckResult(RESULT_UNKNOWN, filter)
    }
}

class CheckResult(var result: Int, var value: String?) {
    companion object {
        const val RESULT_MAYBE_EMULATOR: Int = 0 //可能是模拟器
        const val RESULT_EMULATOR: Int = 1 //模拟器
        const val RESULT_UNKNOWN: Int = 2 //可能是真机
    }
}