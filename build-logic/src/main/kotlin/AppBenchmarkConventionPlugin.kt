import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import common.implementation
import common.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class AppBenchmarkConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply(plugin = project.libs.plugins.android.test.get().pluginId)

        val appId: String by project

        project.extensions.configure<TestExtension> {

            namespace = "$appId.benchmark"
            compileSdk = project.libs.versions.compileAndroidSdk.get().toInt()

            compileOptions {
                val javaVersion = JavaVersion.toVersion(project.libs.versions.jvmTarget.get())
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }

            project.tasks.withType<KotlinCompile>().configureEach {
                compilerOptions {
                    jvmTarget.set(project.libs.versions.jvmTarget.map(JvmTarget::fromTarget))
                }
            }

            defaultConfig {
                minSdk = project.libs.versions.minAndroidSdk.get().toInt()
                targetSdk = project.libs.versions.compileAndroidSdk.get().toInt()

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            buildTypes {
                register("release") {
                    isDebuggable = true
                    signingConfig = signingConfigs.getByName("debug")

                    matchingFallbacks.add("release")
                    matchingFallbacks.add("debug")
                }
            }

            targetProjectPath = ":androidApp"

            @Suppress("UnstableApiUsage")
            experimentalProperties["android.experimental.self-instrumenting"] = true
        }

        project.extensions.configure<TestAndroidComponentsExtension> {
            beforeVariants(selector().all()) {
                it.enable = it.buildType == "release"
            }
        }

        project.dependencies {
            implementation(project.libs.junit.android)
            implementation(project.libs.espresso)
            implementation(project.libs.test.uiautomator)
            implementation(project.libs.macro.benchmark)
        }
    }
}