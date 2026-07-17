package com.kevlina.budgetplus.core.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app

internal actual fun firebaseProjectId(): String? = Firebase.app.options.projectId
