package com.kevlina.budgetplus.core.data

/**
 *  Wraps Firebase Cloud Functions calls for testability.
 */
interface CloudFunctionsCaller {

    suspend fun call(functionName: String, region: String, data: Any? = null)
}
