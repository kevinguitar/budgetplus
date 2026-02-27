import common.libs
import dev.zacsweers.metro.gradle.DelicateMetroGradleApi
import dev.zacsweers.metro.gradle.DiagnosticSeverity
import dev.zacsweers.metro.gradle.MetroPluginExtension
import dev.zacsweers.metro.gradle.OptionalBindingBehavior
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class MetroConventionPlugin : Plugin<Project> {

    @OptIn(DelicateMetroGradleApi::class)
    override fun apply(project: Project) {
        project.apply(plugin = project.libs.plugins.metro.get().pluginId)

        project.configure<MetroPluginExtension> {
            enableSwitchingProviders.set(true)
            optionalBindingBehavior.set(OptionalBindingBehavior.DISABLED)
            unusedGraphInputsSeverity.set(DiagnosticSeverity.NONE)
        }
    }
}