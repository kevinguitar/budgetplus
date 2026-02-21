package com.kevlina.budgetplus.book.di

import com.kevlina.budgetplus.feature.speak.record.RecordActor
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
class SpeakToRecordPlaceholderImpl : SpeakToRecord {
    override val isAvailableOnDevice: Boolean
        get() = false

    override fun startRecording(): RecordActor {
        TODO("Not yet implemented")
    }
}