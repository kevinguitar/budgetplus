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

    defaultConfigs {
        buildConfigField(STRING, "revenuecatApiKey", localProperty("REVENUECAT_API_KEY"))
    }
}