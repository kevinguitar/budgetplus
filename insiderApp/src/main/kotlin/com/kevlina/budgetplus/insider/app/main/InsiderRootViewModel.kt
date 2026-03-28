package com.kevlina.budgetplus.insider.app.main

import androidx.lifecycle.ViewModel
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.nav.InsiderDest
import com.kevlina.budgetplus.core.common.nav.NavController
import dev.zacsweers.metro.ContributesIntoMap

@ViewModelKey(InsiderRootViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class InsiderRootViewModel(
    val navController: NavController<InsiderDest>,
    val snackbarSender: SnackbarSender,
) : ViewModel()