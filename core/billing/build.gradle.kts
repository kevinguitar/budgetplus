import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import common.localProperty

plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.metro)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(libs.revenuecat.core)
        }
    }
}

buildkonfig {
    packageName = "com.kevlina.budgetplus.core.billing"

    val revenuecatApiKey = "revenuecatApiKey"
    defaultConfigs {
        buildConfigField(STRING, revenuecatApiKey, "")
    }

    targetConfigs {
        create("android") {
            buildConfigField(
                type = STRING,
                name = revenuecatApiKey,
                value = localProperty("REVENUECAT_ANDROID_API_KEY")
            )
        }

        create("ios") {
            buildConfigField(
                type = STRING,
                name = revenuecatApiKey,
                value = localProperty("REVENUECAT_IOS_API_KEY")
            )
        }
    }
}