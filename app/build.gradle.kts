import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

fun getCalVer(): Pair<Int, String> {
    val date = Date()
    val year = SimpleDateFormat("yy").format(date).toInt()
    val month = SimpleDateFormat("MM").format(date).toInt()
    val day = SimpleDateFormat("dd").format(date).toInt()
    val sequence = 0 // This can be incremented for multiple builds on the same day
    val versionCode = (year * 1000000) + (month * 10000) + (day * 100) + sequence
    val versionName = "v$year.$month.$day.$sequence"
    return Pair(versionCode, versionName)
}

android {
    namespace = "id.kjlogistik.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.kjlogistik.app"
        minSdk = 24
        targetSdk = 36
        val (calVerCode, calVerName) = getCalVer()
        versionCode = calVerCode
        versionName = calVerName

        resValue("string", "app_version_name", versionName!!)

        val githubToken = localProperties.getProperty("GITHUB_TOKEN") ?: ""
        resValue("string", "github_pat", githubToken)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
//        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Retrofit for API Calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)

    // Android Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx.v280)

    // Hilt Dependency Injection
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)

    // QR Scanner
    implementation(libs.play.services.code.scanner)

    // Room Database
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Jetpack Compose Integration
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Chucker for debugging network calls
    implementation("com.github.chuckerteam.chucker:library:4.2.0")

    implementation("androidx.compose.material:material-icons-extended")
}