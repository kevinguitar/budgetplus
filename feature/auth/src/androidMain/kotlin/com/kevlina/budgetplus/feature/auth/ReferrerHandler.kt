package com.kevlina.budgetplus.feature.auth

import androidx.activity.ComponentActivity
import co.touchlab.kermit.Logger
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.kevlina.budgetplus.core.data.BookRepo
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

@AssistedInject
class ReferrerHandler(
    @Assisted private val activity: ComponentActivity,
    private val bookRepo: BookRepo,
) : InstallReferrerStateListener {

    private val referrerClient by lazy {
        InstallReferrerClient
            .newBuilder(activity)
            .build()
    }

    fun retrieveReferrer() {
        referrerClient.startConnection(this)
    }

    override fun onInstallReferrerSetupFinished(responseCode: Int) {
        when (responseCode) {
            InstallReferrerResponse.OK -> {
                val joinId = referrerClient.installReferrer.installReferrer
                Logger.d { "ReferrerClient: Ok. JoinId=$joinId" }
                bookRepo.setPendingJoinRequest(joinId)
                referrerClient.endConnection()
            }

            InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                Logger.w { "ReferrerClient: Feature not supported" }
            }

            InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                Logger.w { "ReferrerClient: Service Unavailable" }
            }
        }
    }

    override fun onInstallReferrerServiceDisconnected() {
        Logger.d { "ReferrerClient: Disconnected" }
    }

    @AssistedFactory
    interface Factory {
        fun create(activity: ComponentActivity): ReferrerHandler
    }
}