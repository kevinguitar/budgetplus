package com.kevlina.budgetplus.core.data

interface CloudFunctionsCaller {

    suspend fun call(functionName: String, region: String, data: Any? = null)
}
