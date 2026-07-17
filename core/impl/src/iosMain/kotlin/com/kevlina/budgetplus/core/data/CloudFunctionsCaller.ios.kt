package com.kevlina.budgetplus.core.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.ios

internal actual fun firebaseProjectId(): String? =
    Firebase.app.ios.options.projectID
