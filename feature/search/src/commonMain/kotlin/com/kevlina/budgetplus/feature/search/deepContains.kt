package com.kevlina.budgetplus.feature.search

/**
 * Checks if the characters of the `search` string appear in the `target` string
 * in the same order, potentially interleaved with other characters.
 *
 * Examples:
 * deepContains("1a2b3c", "123") == true
 * deepContains("abc", "ac") == true
 * deepContains("abc", "ca") == false
 * deepContains("banana", "bna") == true
 * deepContains("apple", "aple") == true
 * deepContains("apple", "aplex") == false
 * deepContains("abc", "") == true // Empty search string is always contained
 * deepContains("", "a") == false // Cannot find non-empty search in empty target
 */
fun deepContains(target: String, search: CharSequence): Boolean {
    // 1. Handle edge case: An empty search string is always "found".
    if (search.isEmpty()) {
        return true
    }
    // 2. Handle edge case: Cannot find a non-empty search string in an empty target.
    if (target.isEmpty()) {
        return false
    }

    var searchIndex = 0 // Index for the character we are currently looking for in 'search'
    var targetIndex = 0 // Index for the character we are currently checking in 'target'

    // 3. Iterate through the target string
    while (targetIndex < target.length) {
        // 4. Check if the current target character matches the current search character
        if (target[targetIndex].equals(search[searchIndex], ignoreCase = true)) {
            // 5. Match found! Move to the next character in the search string
            searchIndex++
            // 6. If we've found all characters in the search string, we're done.
            if (searchIndex == search.length) {
                return true
            }
        }
        // 7. Always move to the next character in the target string
        targetIndex++
    }

    // 8. If we reached the end of the target string but haven't found all
    //    characters from the search string, then it's not a match.
    return false
}