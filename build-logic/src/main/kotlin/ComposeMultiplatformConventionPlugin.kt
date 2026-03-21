import common.debugImplementation
import common.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply(plugin = project.libs.plugins.jetbrains.compose.get().pluginId)
        project.apply<SetupComposeCompiler>()

        project.extensions.configure<KotlinMultiplatformExtension> {
            sourceSets {
                commonMain.dependencies {
                    implementation(project.libs.bundles.compose)
                    implementation(project.libs.coil.compose)
                    implementation(project.libs.coil.ktor)
                }
                commonTest.dependencies {
                    implementation(kotlin("test"))
                }
                androidMain.dependencies {
                    implementation(project.libs.android.activity.compose)
                    implementation(project.libs.ktor.android)
                }
                iosMain.dependencies {
                    implementation(project.libs.ktor.ios)
                }
            }
        }

        // See https://youtrack.jetbrains.com/issue/CMP-4885
        project.tasks
            .matching { it.name == "syncComposeResourcesForIos" }
            .configureEach { enabled = false }

        project.dependencies {
            debugImplementation(project.libs.compose.android.uiTooling)
        }
    }
}
