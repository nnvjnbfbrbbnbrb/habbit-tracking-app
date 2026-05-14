import java.util.Properties
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
}

val habitProLocalProps = Properties().apply {
    val lp = rootProject.layout.projectDirectory.file("local.properties").asFile
    if (lp.exists()) lp.inputStream().use { load(it) }
}

fun habitProBuildConfigString(raw: String): String {
    val escaped = raw.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

android {
    namespace = "com.ansangha.craxxjxbdbf"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ansangha.craxxjxbdbf"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val habitBase = (habitProLocalProps.getProperty("habitpro.api.baseUrl") ?: "").trim()
        val habitToken = (habitProLocalProps.getProperty("habitpro.api.bearerToken") ?: "").trim()
        buildConfigField("String", "HABITPRO_API_BASE_URL", habitProBuildConfigString(habitBase))
        buildConfigField("String", "HABITPRO_API_BEARER_TOKEN", habitProBuildConfigString(habitToken))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // View-based UI (needed for fragments/layouts)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // Gson for JSON conversion
    implementation(libs.gson)

    // Hilt Navigation Compose
    implementation(libs.androidx.hilt.navigation.compose)

    // ViewModel for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Lottie animations
    implementation(libs.lottie.compose)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Accompanist permissions
    implementation(libs.accompanist.permissions)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // SplashScreen API
    implementation(libs.androidx.core.splashscreen)

    // Background routines (inexact periodic; battery-friendly)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    // OkHttp (used by ApiManager.java + Retrofit)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)

    implementation(libs.play.services.location)

    // Haptic feedback using compatible version - removed temporarily to fix build
    // implementation(libs.androidx.core.haptics)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// AGP 9 + android.builtInKotlin=false: Kotlin main outputs live under tmp/kotlin-classes but are omitted from the
// default `bundleDebugClassesToRuntimeJar` classpath used by JVM unit tests.
afterEvaluate {
    tasks.named<Test>("testDebugUnitTest").configure {
        dependsOn(tasks.named("compileDebugKotlin"))
        classpath += files(layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
    }
}