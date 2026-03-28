package com.kevlina.budgetplus.insider.app.main

import androidx.lifecycle.ViewModel
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.nav.InsiderDest
import com.kevlina.budgetplus.core.common.nav.NavController
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.ViewModelScope

@ViewModelKey
@ContributesIntoMap(ViewModelScope::class)
class InsiderRootViewModel(
    val navController: NavController<InsiderDest>,
    val snackbarSender: SnackbarSender,
) : ViewModel()