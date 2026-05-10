import common.implementation
import common.libs
import dev.zacsweers.metro.gradle.DelicateMetroGradleApi
import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import dev.zacsweers.metro.gradle.MetroPluginExtension
import dev.zacsweers.metro.gradle.OptionalBindingBehavior
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MetroConventionPlugin : Plugin<Project> {

    @OptIn(DelicateMetroGradleApi::class, ExperimentalMetroGradleApi::class)
    override fun apply(project: Project) {
        project.apply(plugin = project.libs.plugins.metro.get().pluginId)

        project.configure<MetroPluginExtension> {
            enableSwitchingProviders.set(true)
            optionalBindingBehavior.set(OptionalBindingBehavior.DISABLED)
            generateContributionProviders.set(true)
        }

        project.pluginManager.withPlugin(project.libs.plugins.kotlin.multiplatform.get().pluginId) {
            project.extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    commonMain.dependencies {
                        implementation(project.libs.metrox.viewmodel)
                    }
                }
            }
        }

        project.pluginManager.withPlugin(project.libs.plugins.android.application.get().pluginId) {
            project.dependencies {
                implementation(project.libs.metrox.viewmodel)
            }
        }
    }
}