package com.kevlina.budgetplus.core.common.nav

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavControllerTest {

    private sealed interface TestKey : NavKey {
        data object RootA : TestKey
        data object RootB : TestKey
        data object RootC : TestKey
        data object Screen1 : TestKey
        data object Screen2 : TestKey
    }

    private fun createNavController(startRoot: TestKey) = NavController(startRoot = startRoot)

    @Test
    fun `Initial state verification`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        assertEquals(listOf(startRoot), navController.backStack.toList())
        assertEquals(listOf(startRoot), navController.rootStack.toList())
    }

    @Test
    fun `Initial state current root check`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        // Indirectly check currentRoot. Navigation should happen on the startRoot.
        navController.navigate(TestKey.Screen1)
        assertEquals(listOf(startRoot, TestKey.Screen1), navController.backStack.toList())
    }

    @Test
    fun `navigate  Basic navigation on start root`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        val newKey = TestKey.Screen1
        navController.navigate(newKey)
        assertEquals(listOf(startRoot, newKey), navController.backStack.toList())
    }

    @Test
    fun `navigate  Multiple navigations`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        val key1 = TestKey.Screen1
        val key2 = TestKey.Screen2

        navController.navigate(key1)
        navController.navigate(key2)

        assertEquals(listOf(startRoot, key1, key2), navController.backStack.toList())
    }

    @Test
    fun `navigateUp  Basic navigation up`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        navController.navigate(TestKey.Screen1)
        navController.navigateUp()
        assertEquals(listOf(startRoot), navController.backStack.toList())
    }

    @Test
    fun `navigateUp  Emptying the current root s stack`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        navController.navigate(TestKey.Screen1)

        navController.navigateUp() // Removes Screen1
        assertEquals(listOf(startRoot), navController.backStack.toList())
        assertEquals(listOf(startRoot), navController.rootStack.toList())

        navController.navigateUp() // Removes RootA
        assertTrue(navController.rootStack.isEmpty())
    }

    @Test
    fun `navigateUp  On initial state`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        navController.navigateUp()
        assertTrue(navController.rootStack.isEmpty())
    }

    @Test
    fun `navigateUp  On an already empty back stack`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        navController.navigateUp() // first up, empties the stack
        navController.navigateUp() // second up, should not crash
        assertTrue(navController.rootStack.isEmpty())
    }

    @Test
    fun `selectRoot  Selecting a new root`() {
        val startRoot = TestKey.RootA
        val newRoot = TestKey.RootB
        val navController = createNavController(startRoot)
        navController.selectRoot(newRoot)
        assertEquals(listOf(startRoot, newRoot), navController.rootStack.toList())
        assertEquals(listOf(startRoot, newRoot), navController.backStack.toList())
    }

    @Test
    fun `selectRoot  Switching back to an existing root`() {
        val startRoot = TestKey.RootA
        val newRoot = TestKey.RootB
        val navController = createNavController(startRoot)
        navController.selectRoot(newRoot)
        navController.selectRoot(startRoot)
        assertContainsExactly(navController.rootStack, newRoot, startRoot)
    }

    @Test
    fun `selectRoot  Switching to a non top existing root`() {
        val r1 = TestKey.RootA
        val r2 = TestKey.RootB
        val r3 = TestKey.RootC
        val navController = createNavController(r1)

        navController.selectRoot(r2)
        navController.selectRoot(r3)
        assertEquals(listOf(r1, r2, r3), navController.rootStack.toList())

        navController.selectRoot(r1)
        assertContainsExactly(navController.rootStack, r2, r3, r1)
    }

    @Test
    fun `selectRoot  Selecting the current root`() {
        val r1 = TestKey.RootA
        val r2 = TestKey.RootB
        val navController = createNavController(r1)
        navController.selectRoot(r2)

        val rootStackBefore = navController.rootStack.toList()
        val backStackBefore = navController.backStack.toList()

        navController.selectRoot(r2)

        assertEquals(rootStackBefore, navController.rootStack.toList())
        assertEquals(backStackBefore, navController.backStack.toList())
    }

    @Test
    fun `selectRootAndClearAll  Selecting the new root`() {
        val r1 = TestKey.RootA
        val r2 = TestKey.RootB
        val navController = createNavController(r1)

        navController.navigate(TestKey.Screen1)
        navController.selectRootAndClearAll(r2)
        assertEquals(listOf(r2), navController.rootStack.toList())
        assertEquals(listOf(r2), navController.backStack.toList())
    }

    @Test
    fun `Complex scenario  Navigation within a non start root`() {
        val startRoot = TestKey.RootA
        val newRoot = TestKey.RootB
        val navController = createNavController(startRoot)
        navController.selectRoot(newRoot)
        navController.navigate(TestKey.Screen1)
        navController.navigate(TestKey.Screen2)

        assertEquals(listOf(startRoot, newRoot, TestKey.Screen1, TestKey.Screen2), navController.backStack.toList())
        assertEquals(listOf(startRoot, newRoot), navController.rootStack.toList())
    }

    @Test
    fun `Complex scenario  State preservation after switching roots`() {
        val r1 = TestKey.RootA
        val r2 = TestKey.RootB
        val n2 = TestKey.Screen2
        val navController = createNavController(r1)

        navController.selectRoot(r2)
        navController.navigate(n2)
        navController.selectRoot(r1)
        navController.selectRoot(r2)

        assertEquals(listOf(r1, r2, n2), navController.backStack.toList())
        assertEquals(listOf(r1, r2), navController.rootStack.toList())
    }

    @Test
    fun `Complex scenario  navigateUp within a deep root stack`() {
        val r1 = TestKey.RootA
        val n1 = TestKey.Screen1
        val r2 = TestKey.RootB
        val n2 = TestKey.Screen2
        val navController = createNavController(r1)

        navController.navigate(n1)
        navController.selectRoot(r2)
        navController.navigate(n2)
        navController.navigateUp()

        assertEquals(listOf(r1, n1, r2), navController.backStack.toList())
        assertEquals(listOf(r1, r2), navController.rootStack.toList())
    }

    @Test
    fun `Complex scenario  navigateUp to pop a root`() {
        val r1 = TestKey.RootA
        val r2 = TestKey.RootB
        val navController = createNavController(r1)

        navController.selectRoot(r2)
        navController.navigateUp()

        assertEquals(listOf(r1), navController.backStack.toList())
        assertEquals(listOf(r1), navController.rootStack.toList())
    }

    @Test
    fun `getBackStack  Direct modification attempt`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        // Direct modification of the returned list can lead to inconsistent states.
        navController.backStack.add(TestKey.Screen1)
        assertEquals(listOf(startRoot, TestKey.Screen1), navController.backStack.toList())

        // A subsequent navigation call will fix the stack.
        navController.navigate(TestKey.Screen2)
        assertEquals(listOf(startRoot, TestKey.Screen2), navController.backStack.toList())
    }

    @Test
    fun `getRootStack  Direct modification attempt`() {
        val startRoot = TestKey.RootA
        val navController = createNavController(startRoot)
        // Direct modification can corrupt the internal state.
        navController.rootStack.add(TestKey.RootB)
        assertEquals(listOf(startRoot, TestKey.RootB), navController.rootStack.toList())

        // backStack will be out of sync until updateBackStack() is triggered.
        assertEquals(listOf(startRoot), navController.backStack.toList())
    }

    /**
     * Asserts that the [list] contains exactly the given [elements] regardless of order.
     */
    private fun <T> assertContainsExactly(list: List<T>, vararg elements: T) {
        assertEquals(elements.size, list.size, "Expected size ${elements.size}, but was ${list.size}")
        assertEquals(elements.toSet(), list.toSet())
    }
}
