package com.kevlina.budgetplus.core.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.auth
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject

/**
 * Invokes a Firebase Callable Function by issuing the raw HTTPS request ourselves instead of
 * going through the GitLive/Firebase Functions SDK.
 *
 * Why: The Firebase iOS SDK's [callable] path calls `FunctionsContextProvider.context()`, which
 * gathers the auth/App Check/FCM tokens concurrently. Swift 6.3 (Xcode 26.4) miscompiles that
 * concurrency teardown in optimized/release builds, corrupting the Swift task allocator and
 * aborting the process (a native SIGABRT that Kotlin try/catch cannot intercept). See:
 *  - https://github.com/firebase/firebase-ios-sdk/issues/15974
 *  - https://github.com/swiftlang/swift/issues/87481
 *
 * Building the request manually avoids the crashing native code path entirely. Once the upstream
 * Swift compiler fix (https://github.com/swiftlang/swift/pull/87665) ships in a toolchain we build
 * with, we can restore the commented-out SDK-based implementation below.
 */
@ContributesBinding(AppScope::class)
internal class CloudFunctionsCallerImpl(
    private val httpClient: HttpClient,
    private val json: Json,
) : CloudFunctionsCaller {

    override suspend fun call(functionName: String, region: String, data: Map<String, String>?) {
        val projectId = requireNotNull(Firebase.app.options.projectId) {
            "Firebase projectId is not configured."
        }
        val idToken = Firebase.auth.currentUser?.getIdToken(false)

        // Firebase Callable Functions expect the payload wrapped in a top-level "data" field.
        val body = buildJsonObject {
            put(
                key = "data",
                element = json.encodeToJsonElement(
                    serializer = MapSerializer(String.serializer(), String.serializer()),
                    value = data.orEmpty(),
                )
            )
        }

        val url = "https://$region-$projectId.cloudfunctions.net/$functionName"
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            if (idToken != null) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
            }
            setBody(json.encodeToString(body))
        }

        if (!response.status.isSuccess()) {
            error("Cloud function $functionName failed: ${response.status}, ${response.bodyAsText()}")
        }
    }
}

// The original SDK-based implementation, kept for when the Swift compiler bug is resolved.
// See the KDoc above and https://github.com/firebase/firebase-ios-sdk/issues/15974
//
// @ContributesBinding(AppScope::class)
// internal class CloudFunctionsCallerImpl : CloudFunctionsCaller {
//
//     override suspend fun call(functionName: String, region: String, data: Map<String, String?>?) {
//         val functions = Firebase.functions(region)
//         val callable = functions.httpsCallable(functionName)
//         callable.invoke(data)
//     }
// }
