package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.CloudFunctionsCaller

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeCloudFunctionsCaller : CloudFunctionsCaller {

    var lastCall: Triple<String, String, Map<String, String>?>? = null
        private set

    var callError: Exception? = null

    override suspend fun call(functionName: String, region: String, data: Map<String, String>?) {
        callError?.let { throw it }
        lastCall = Triple(functionName, region, data)
    }
}
