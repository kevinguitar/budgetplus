package com.kevlina.budgetplus.core.common.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.serialization.saved
import androidx.navigation3.runtime.NavKey
import co.touchlab.kermit.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A navigation controller that has a back stack for complex navigation trees like bottom navigation.
 *
 * @param startRoot which root of navigation tree that is initially displayed.
 */
class NavController<T : NavKey>(
    private val startRoot: T,
    private val serializer: KSerializer<T>,
    savedStateHandle: SavedStateHandle,
) {

    private val stateListSerializer = object : KSerializer<SnapshotStateList<T>> {
        private val delegate = ListSerializer(serializer)
        override val descriptor: SerialDescriptor = delegate.descriptor

        override fun serialize(encoder: Encoder, value: SnapshotStateList<T>) {
            delegate.serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder): SnapshotStateList<T> {
            return delegate.deserialize(decoder).toMutableStateList()
        }
    }

    private val mutableMapSerializer = object : KSerializer<MutableMap<T, SnapshotStateList<T>>> {
        private val delegate = MapSerializer(serializer, stateListSerializer)
        override val descriptor: SerialDescriptor = delegate.descriptor

        override fun serialize(encoder: Encoder, value: MutableMap<T, SnapshotStateList<T>>) {
            delegate.serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder): MutableMap<T, SnapshotStateList<T>> {
            return delegate.deserialize(decoder).toMutableMap()
        }
    }

    private val rootBackStacks by savedStateHandle.saved(serializer = mutableMapSerializer) {
        mutableMapOf<T, SnapshotStateList<T>>(
            startRoot to mutableStateListOf(startRoot)
        )
    }

    val backStack by savedStateHandle.saved(stateListSerializer) { mutableStateListOf(startRoot) }
    val rootStack by savedStateHandle.saved(stateListSerializer) { mutableStateListOf(startRoot) }

    init {
        // Force initialization of delegated properties to avoid "Cannot modify a state object in a read-only snapshot"
        // when they are first accessed inside snapshotFlow or other read-only snapshots.
        rootBackStacks
        backStack
        rootStack
    }

    private val currentRoot: T
        get() = rootStack.lastOrNull() ?: startRoot

    fun selectRoot(rootKey: T) {
        if (rootKey == currentRoot) return

        if (rootKey in rootStack) {
            // If the root is already in the stack, remove it and add it again to bring it to the top
            rootStack.remove(rootKey)
            rootStack.add(rootKey)
        } else {
            // If the root was never added before, simply add it
            rootStack.add(rootKey)
            rootBackStacks[rootKey] = mutableStateListOf(rootKey)
        }
        updateBackStack()
    }

    fun selectRootAndClearAll(rootKey: T) {
        if (rootKey == currentRoot) return

        rootStack.clear()
        rootStack.add(rootKey)

        rootBackStacks.clear()
        rootBackStacks[rootKey] = mutableStateListOf(rootKey)
        updateBackStack()
    }

    fun navigate(key: T) {
        rootBackStacks[currentRoot]?.add(key)
        updateBackStack()
    }

    fun navigateUp() {
        val currentStack = rootBackStacks[currentRoot] ?: return
        currentStack.removeLastOrNull()

        // Move on to the previous root if the current stack is empty
        if (currentStack.isEmpty()) {
            rootStack.removeLastOrNull()
        }
        updateBackStack()
    }

    private fun updateBackStack() {
        backStack.clear()
        rootStack.forEach { root ->
            val stack = rootBackStacks[root] ?: return@forEach
            backStack.addAll(stack)
        }
        Logger.i { "NavController: BackStack=${backStack.toList()}" }

        if (backStack.isEmpty()) {
            backStack.add(startRoot)
            Logger.w { "NavController: BackStack is empty, added $startRoot to avoid crash" }
        }
    }

    companion object {
        val preview = NavController(
            startRoot = BottomNavTab.Add.root,
            serializer = BookDest.serializer(),
            savedStateHandle = SavedStateHandle()
        )
    }
}