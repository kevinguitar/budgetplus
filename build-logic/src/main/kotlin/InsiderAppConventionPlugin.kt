import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import common.libs
import common.localProperty
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import java.util.*

class InsiderAppConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply(plugin = project.libs.plugins.android.application.get().pluginId)
        project.apply(plugin = project.libs.plugins.google.services.get().pluginId)
        project.apply(plugin = project.libs.plugins.firebase.crashlytics.get().pluginId)
        project.apply(plugin = project.libs.plugins.kotlin.serialization.get().pluginId)
        project.apply<KotlinAndroidConventionPlugin>()

        val appId: String by project

        val localProperties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        } else {
            throw GradleException("local.properties file not found in root project.")
        }

        project.extensions.configure<ApplicationExtension> {
            defaultConfig {
                applicationId = "$appId.insider"
                targetSdk = project.libs.versions.compileAndroidSdk.get().toInt()
                versionName = "1.0.0"
                versionCode = 1

                buildConfigField(
                    type = "String",
                    name = "GOOGLE_API_KEY",
                    value = localProperties.getProperty("GOOGLE_API_KEY", "\"\"")
                )

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables {
                    useSupportLibrary = true
                }

                androidResources {
                    @Suppress("UnstableApiUsage")
                    localeFilters.add("zh-rTW")
                }
            }

            signingConfigs {
                named("debug") {
                    storeFile = project.rootProject.file("misc/debug.keystore")
                }
                create("release") {
                    storeFile = project.localProperty("KEYSTORE_PATH")?.let { project.rootProject.file(it) }
                    storePassword = project.localProperty("KEYSTORE_PASSWORD")
                    keyAlias = project.localProperty("KEYSTORE_ALIAS")
                    keyPassword = project.localProperty("KEYSTORE_PASSWORD")
                }
            }

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    signingConfig = signingConfigs.getByName("release")
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                    configure<CrashlyticsExtension> {
                        mappingFileUploadEnabled = true
                        nativeSymbolUploadEnabled = true
                    }
                }
            }

            buildFeatures {
                buildConfig = true
            }

            packaging {
                resources {
                    // To make google translation SDK work :/
                    excludes.add("META-INF/DEPENDENCIES")
                    excludes.add("META-INF/INDEX.LIST")
                }
            }

            lint {
                checkDependencies = true
            }
        }
    }
}