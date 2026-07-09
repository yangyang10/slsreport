package com.xiaotimel.sls.log.model

import com.xiaotimel.sls.log.identifier.FirebaseIdChecker
import com.xiaotimel.sls.log.identifier.GoogleAdIdChecker
import kotlinx.coroutines.flow.MutableStateFlow

class UpdatableInfo {

    fun updateLocation(location: SimpleLocation) {
        locationFlow.value = location
    }

    internal var locationFlow = MutableStateFlow(SimpleLocation())

    internal var googleAdIdFlow = MutableStateFlow<String?>(null)
    internal var userPseudoIdFlow = MutableStateFlow<String?>(null)

    private val googleAdIdChecker = GoogleAdIdChecker()
    private val firebaseAdIdChecker = FirebaseIdChecker()

    init {
        firebaseAdIdChecker.checkIdentifier {
            userPseudoIdFlow.value = it
        }
        googleAdIdChecker.checkIdentifier {
            googleAdIdFlow.value = it
        }
    }
}