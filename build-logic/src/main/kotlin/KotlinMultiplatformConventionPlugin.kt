import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import common.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val appId: String by project
        val modulePath = project.path
            .drop(1)
            .split(':', '-')
            .joinToString(".")

        project.apply(plugin = project.libs.plugins.kotlin.multiplatform.get().pluginId)
        project.apply(plugin = project.libs.plugins.android.kotlin.multiplatform.library.get().pluginId)
        project.apply(plugin = project.libs.plugins.kotlin.serialization.get().pluginId)

        project.extensions.configure<KotlinMultiplatformExtension> {
            extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                namespace = "$appId.$modulePath"
                compileSdk = project.libs.versions.compileAndroidSdk.get().toInt()
                minSdk = project.libs.versions.minAndroidSdk.get().toInt()
                compilerOptions {
                    jvmTarget.set(project.libs.versions.jvmTarget.map(JvmTarget::fromTarget))
                }
                //TODO: After this all resources can live in core:common
                androidResources.enable = true
                packaging.resources {
                    excludes.add("META-INF/*.kotlin_module")
                    excludes.add("META-INF/AL2.0")
                    excludes.add("META-INF/LICENSE.md")
                    excludes.add("META-INF/LICENSE-notice.md")
                    excludes.add("META-INF/LGPL2.1")
                    excludes.add("**/*.kotlin_metadata")
                }
            }

            applyDefaultHierarchyTemplate()

            listOf(
                iosArm64(),
                iosSimulatorArm64()
            ).forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = modulePath.replaceFirstChar { it.uppercase() }
                    freeCompilerArgs += listOf("-Xbinary=bundleId=$appId.$modulePath")
                    isStatic = true
                }
            }

            jvmToolchain(project.libs.versions.jvmTarget.get().toInt())

            sourceSets.apply {
                commonMain.dependencies {
                    implementation(project.libs.bundles.kmp)
                    api(project.libs.datastore)
                    api(project.libs.datastore.preferences)

                    if (project.path != ":core:common") {
                        implementation(project(":core:common"))
                    }
                }

                androidMain.dependencies {
                    implementation(project.libs.bundles.android)
                }

                androidUnitTest.dependencies {
                    implementation(kotlin("test"))
                    implementation(project.libs.bundles.test)

                    if (project.path != ":core:unit-test") {
                        implementation(project(":core:unit-test"))
                    }
                }

                named { it.lowercase().startsWith("ios") }.configureEach {
                    languageSettings {
                        optIn("kotlinx.cinterop.ExperimentalForeignApi")
                    }
                }
            }

            compilerOptions {
                // Due to https://youtrack.jetbrains.com/issue/CMP-8498/KLIB-name-conflict-with-AndroidX-libraries
                allWarningsAsErrors.set(false)
                freeCompilerArgs.addAll(
                    "-Xcontext-parameters",
                    "-Xexplicit-backing-fields",
                    "-Xannotation-default-target=param-property",
                    "-Xexpect-actual-classes"
                )
                optIn.addAll(
                    "kotlin.contracts.ExperimentalContracts",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi",
                    "kotlinx.coroutines.FlowPreview"
                )
            }
        }
    }
}