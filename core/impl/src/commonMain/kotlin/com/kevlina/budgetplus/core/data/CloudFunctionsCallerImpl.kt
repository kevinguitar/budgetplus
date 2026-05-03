package com.kevlina.budgetplus.core.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.functions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
internal class CloudFunctionsCallerImpl : CloudFunctionsCaller {

    override suspend fun call(functionName: String, region: String, data: Any?) {
        val functions = Firebase.functions(region)
        val callable = functions.httpsCallable(functionName)
        callable.invoke(data)
    }
}
