package com.kevlina.budgetplus.book.ui

// iPhone Duo: the status bar and the camera take a column on one side of the window. Other iPhones
// have symmetric side insets in landscape, so they never form a column.
internal actual val hasSideColumnForSystemUi: Boolean = true
