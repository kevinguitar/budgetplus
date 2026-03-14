import com.android.build.api.dsl.CommonExtension
import common.debugImplementation
import common.implementation
import common.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class ComposeConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<SetupComposeCompiler>()

        project.extensions.configure(CommonExtension::class.java) {
            buildFeatures.compose = true
        }

        project.dependencies {
            implementation(project.libs.bundles.compose)
            implementation(project.libs.android.activity.compose)
            debugImplementation(project.libs.compose.android.uiTooling)
        }
    }
}
