package com.kevlina.budgetplus.feature.add.record

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_invite
import budgetplus.core.common.generated.resources.menu_invite_to_book
import budgetplus.core.common.generated.resources.permission_hint
import budgetplus.core.common.generated.resources.record_empty_category
import budgetplus.core.common.generated.resources.record_empty_price
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.EventTrigger
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.ShareHelper
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.common.parseToPrice
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.common.withCurrentTime
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.RecordRepo
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.toAuthor
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleRepo
import com.kevlina.budgetplus.feature.add.record.RecordViewModel.Companion.RECORD_COUNT_CYCLE
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import com.kevlina.budgetplus.feature.freeze.FreezeBookViewModel
import com.kevlina.budgetplus.inapp.review.InAppReviewManager
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

@ViewModelKey(RecordViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class RecordViewModel(
    val calculatorVm: CalculatorViewModel,
    val categoriesVm: CategoriesViewModel,
    val freezeBookVm: FreezeBookViewModel,
    val bookRepo: BookRepo,
    private val recordRepo: RecordRepo,
    private val bubbleRepo: BubbleRepo,
    private val authManager: AuthManager,
    private val interstitialAdsHandler: InterstitialAdsHandler,
    private val inAppReviewManager: InAppReviewManager,
    private val snackbarSender: SnackbarSender,
    private val shareHelper: ShareHelper,
    private val preference: Preference,
) : ViewModel() {

    val type: StateFlow<RecordType>
        field = MutableStateFlow(RecordType.Expense)

    val recordDate: StateFlow<RecordDateState>
        field = MutableStateFlow<RecordDateState>(RecordDateState.Now)

    val note = TextFieldState()

    val recordEvent = EventTrigger<Unit>()

    val requestReviewEvent: EventFlow<Unit>
        field = MutableEventFlow<Unit>()

    val requestPermissionEvent: EventFlow<Unit>
        field = MutableEventFlow<Unit>()

    private val recordCountKey = intPreferencesKey("recordCount")
    private val recordCount = preference.of(recordCountKey)

    init {
        calculatorVm.recordFlow
            .consumeEach { record() }
            .launchIn(viewModelScope)

        calculatorVm.speakToRecordVm.speakResultFlow
            .consumeEach {
                note.setTextAndPlaceCursorAtEnd(it.name)
                it.price?.let(calculatorVm::setPrice)
            }
            .launchIn(viewModelScope)
    }

    fun setType(newType: RecordType) {
        type.value = newType
    }

    fun setDate(date: LocalDate) {
        recordDate.value = if (date == LocalDate.now()) {
            RecordDateState.Now
        } else {
            RecordDateState.Other(date)
        }
    }

    fun shareJoinLink() {
        viewModelScope.launch {
            try {
                val joinLink = bookRepo.generateJoinLink()
                shareHelper.share(
                    title = Res.string.cta_invite,
                    text = getString(Res.string.menu_invite_to_book, joinLink)
                )
                requestPermissionEvent.sendEvent()
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    fun highlightInviteButton(dest: BubbleDest) {
        viewModelScope.launch { bubbleRepo.addBubbleToQueue(dest) }
    }

    fun launchReviewFlow() {
        viewModelScope.launch {
            inAppReviewManager.launchReviewFlow()
        }
    }

    fun rejectReview() {
        inAppReviewManager.rejectReviewing()
    }

    suspend fun showNotificationPermissionHint() {
        snackbarSender.send(Res.string.permission_hint)
    }

    private fun record() {
        val category = categoriesVm.category.value
        val price = calculatorVm.priceText.text.parseToPrice()

        if (category == null) {
            viewModelScope.launch { snackbarSender.send(message = Res.string.record_empty_category) }
            return
        }

        if (price == 0.0) {
            viewModelScope.launch { snackbarSender.send(Res.string.record_empty_price) }
            return
        }

        val record = Record(
            type = type.value,
            date = recordDate.value.date.toEpochDays(),
            timestamp = recordDate.value.date.withCurrentTime,
            category = category,
            name = note.text.trim().ifEmpty { category }.toString(),
            price = price,
            author = authManager.userState.value?.toAuthor()
        )

        recordRepo.createRecord(record)
        recordEvent.sendEvent(Unit)
        resetScreen()

        viewModelScope.launch {
            preference.update(recordCountKey, (recordCount.first() ?: 0) + 1)
            onRecordCreated()
        }
    }

    private fun resetScreen() {
        categoriesVm.setCategory(null)
        note.clearText()
        calculatorVm.clearPrice()
    }

    /**
     *  This callback does several things
     *  - Show full screen Ad on every [RECORD_COUNT_CYCLE] records
     *  - Request notification permission after the 2nd record
     *  - Request in-app review after the 4th record
     */
    private suspend fun onRecordCreated() {
        when ((recordCount.first() ?: 0) % RECORD_COUNT_CYCLE) {
            RECORD_SHOW_AD -> interstitialAdsHandler.showAd()
            RECORD_REQUEST_PERMISSION -> requestPermissionEvent.sendEvent()
            // Request the in-app review when almost reach the next fullscreen ad,
            // just to have a better UX while user reviewing.
            RECORD_REQUEST_REVIEW -> if (inAppReviewManager.isEligibleForReview()) {
                requestReviewEvent.sendEvent()
            }
        }
    }

    private companion object {
        /**
         *  Show full screen Ad on every [RECORD_COUNT_CYCLE] records
         */
        const val RECORD_COUNT_CYCLE = 7
        const val RECORD_SHOW_AD = 0
        const val RECORD_REQUEST_PERMISSION = 2
        const val RECORD_REQUEST_REVIEW = 4
    }
}