import com.android.build.api.dsl.CommonExtension
import common.implementation
import common.libs
import common.testImplementation
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class KotlinAndroidConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val appId: String by project

        project.extensions.configure(CommonExtension::class.java) {
            val modulePath = project.path
                .drop(1)
                .split(':', '-')
                .joinToString(".")

            namespace = "$appId.$modulePath"
            compileSdk = project.libs.versions.compileAndroidSdk.get().toInt()
            defaultConfig.minSdk = project.libs.versions.minAndroidSdk.get().toInt()

            val javaVersion = JavaVersion.toVersion(project.libs.versions.jvmTarget.get())
            compileOptions.sourceCompatibility = javaVersion
            compileOptions.targetCompatibility = javaVersion

            packaging.resources {
                excludes.add("META-INF/*.kotlin_module")
                excludes.add("META-INF/AL2.0")
                excludes.add("META-INF/LICENSE.md")
                excludes.add("META-INF/LICENSE-notice.md")
                excludes.add("META-INF/LGPL2.1")
                excludes.add("**/*.kotlin_metadata")
            }

            project.tasks.withType<KotlinCompile>().configureEach {
                compilerOptions {
                    // Due to https://youtrack.jetbrains.com/issue/CMP-8498/KLIB-name-conflict-with-AndroidX-libraries
                    allWarningsAsErrors.set(false)
                    freeCompilerArgs.addAll(
                        "-Xcontext-parameters",
                        "-Xexplicit-backing-fields",
                        "-Xannotation-default-target=param-property"
                    )
                    optIn.addAll(
                        "kotlin.contracts.ExperimentalContracts",
                        "kotlinx.coroutines.ExperimentalCoroutinesApi",
                        "kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi",
                        "kotlinx.coroutines.FlowPreview"
                    )
                    jvmTarget.set(project.libs.versions.jvmTarget.map(JvmTarget::fromTarget))
                }
            }
        }

        project.dependencies {
            implementation(project.libs.bundles.android)
            implementation(project.libs.bundles.kmp)
            testImplementation(project.libs.bundles.test)

            if (project.path != ":core:common") {
                implementation(project(":core:common"))
            }
        }
    }
}
