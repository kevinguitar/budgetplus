package com.kevlina.budgetplus.core.common.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

/**
 * A navigation controller that has a back stack for complex navigation trees like bottom navigation.
 *
 * @param startRoot which root of navigation tree that is initially displayed.
 */
class NavController<T : NavKey>(
    private val startRoot: T,
) {

    private val rootBackStacks = mutableMapOf<T, SnapshotStateList<T>>(
        startRoot to mutableStateListOf(startRoot)
    )

    val backStack = mutableStateListOf(startRoot)
    val rootStack = mutableStateListOf(startRoot)

    val currentNavKeyFlow: Flow<T> = snapshotFlow { backStack.lastOrNull() }.filterNotNull()

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
        val preview = NavController(startRoot = BottomNavTab.Add.root)
    }
}