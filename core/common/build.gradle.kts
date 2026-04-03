plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.metro)
    alias(budgetplus.plugins.compose.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.navigation3.runtime)
            implementation(libs.coil)
            api(libs.compose.resources)
        }

        androidMain.dependencies {
            implementation(libs.android.activity)
        }
    }
}

compose.resources {
    publicResClass = true
}

// Create a task to copy the TW resources to HK and MO
val copyTraditionalChinese = tasks.register<Copy>("copyTraditionalChinese") {
    val baseResDir = "src/commonMain/composeResources"

    into(baseResDir)

    // Use TW as the single source of truth
    from("$baseResDir/values-zh-rTW") {
        into("values-zh-rHK")
    }

    from("$baseResDir/values-zh-rTW") {
        into("values-zh-rMO")
    }
}

tasks
    .matching {
        it.name.startsWith("convertXmlValueResources") ||
            it.name.startsWith("copyNonXmlValueResources")
    }
    .configureEach {
        dependsOn(copyTraditionalChinese)
    }