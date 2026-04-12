package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.data.CloudFunctionsCaller

@VisibleForTesting
class FakeCloudFunctionsCaller : CloudFunctionsCaller {

    var lastCall: Triple<String, String, Any?>? = null
        private set

    var callError: Exception? = null

    override suspend fun call(functionName: String, region: String, data: Any?) {
        callError?.let { throw it }
        lastCall = Triple(functionName, region, data)
    }
}
