package common

import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType

// Hack to support version catalog https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
inline val Project.libs
    get() = extensions.getByType<org.gradle.accessors.dm.LibrariesForLibs>()

fun DependencyHandlerScope.implementation(module: Any) {
    add("implementation", module)
}

fun DependencyHandlerScope.debugImplementation(module: Any) {
    add("debugImplementation", module)
}

fun DependencyHandlerScope.testImplementation(module: Any) {
    add("testImplementation", module)
}

fun DependencyHandlerScope.androidRuntimeClasspath(module: Any) {
    add("androidRuntimeClasspath", module)
}