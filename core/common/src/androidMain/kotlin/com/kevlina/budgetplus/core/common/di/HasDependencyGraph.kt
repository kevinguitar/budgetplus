package com.kevlina.budgetplus.core.common.di

import android.content.Context

interface HasDependencyGraph {
    fun <T> resolve(): T
}

fun <T> Context.resolveGraphExtensionFactory(): T {
    return (applicationContext as HasDependencyGraph).resolve()
}