package com.xiaotimel.sls.log.constants

object ReportEvent {

    //sls上报失败
    const val FAIL_REPORT_EVENT = "sls_report_fail"

    //sls初始化失败
    const val FAIL_INIT_EVENT = "sls_init_fail"

    const val PUBLIC_DEVICE_INFO = "public_device_info"
    const val PUBLIC_NETWORK_DETECTION = "public_network_detection"
    const val PUBLIC_STAB_DETECTION = "public_stab_detection"
    const val PUBLIC_CLICK_DETECTION = "public_click_detection"
    const val PUBLIC_ANOMALY_DETECTION = "public_anomaly_detection"
    const val OPEN_APP = "open_app"
    const val PUSH_ARRIVE = "push_arrive"
    const val PUSH_CLICK = "push_click"

    const val HEART_BEAT = "heart_beat"
    const val VIEW_SCREEN = "view_screen"

    // TODO AOP，影响编译速度，需要花时间再看下
    const val BUTTON_CLICK = "button_click"
}