package com.kevlina.budgetplus.core.common.nav

import com.kevlina.budgetplus.core.common.MutableEventFlow

//TODO: This should be gone, auth and welcome screen should just be part of the nav3 destination
fun interface NavigationAction {
    fun navigate()
}

typealias NavigationFlow = MutableEventFlow<NavigationAction>
typealias DeeplinkFlow = MutableEventFlow<String?>
