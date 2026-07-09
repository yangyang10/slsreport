package com.xiaotimel.sls.log.model

import androidx.annotation.Keep
import java.io.Serializable

/**
 * Created by HHY on 2023/4/20 16:01
 * Desc:sls日志站点信息
 **/
@Keep
data class SLSLogConfig(
    var enabled:Boolean = false,
    var endpoint:String?,//上报SLS的端点
    var project:String?,//上报SLS的项目
    var logStore:String?,//上报SLS的库
    var accessKeyId:String?,//STS accessKeyId
    var accessKeySecret:String?,//STS accessKeySecret
    var securityToken:String?,//STS securityToken
    var expiration:String?,//STS securityToken 的过期时间
): Serializable