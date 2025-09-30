import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"

}

android {
    namespace = "com.kairos.ast"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.kairos.ast"
        minSdk = 27
        targetSdk = 36
        versionCode = (project.findProperty("KAIROS_VERSION_CODE") as String?)?.toInt() ?: 1
        versionName = project.findProperty("KAIROS_VERSION_NAME") as String? ?: "0.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProperties = gradleLocalProperties(rootDir, providers)

        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("SUPABASE_KEY")}\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${localProperties.getProperty("GOOGLE_MAPS_API_KEY")}\"")
        buildConfigField ("String", "WHATSAPP_NUMBER", "\"${localProperties.getProperty("WHATSAPP_NUMBER")}\"")



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
    // Habilitar ViewBinding
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val appName = "KairosAst"
            val versionName = project.findProperty("KAIROS_VERSION_NAME") as String? ?: "0.0.5"
            val buildType = variant.buildType

            val newApkName = "${appName}-v${versionName}-${buildType}.apk"
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName?.set(newApkName)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")


    // Asegura que todas las librerías de Ktor usen la misma versión
    implementation(platform("io.ktor:ktor-bom:3.3.0"))

    // Ktor core + Android engine
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-android")

    // Content negotiation + JSON
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    // Supabase-KT (la versión más reciente)
    implementation("io.github.jan-tennert.supabase:supabase-kt:3.2.3")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.2.3")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.3")
    implementation("io.github.jan-tennert.supabase:storage-kt-android:3.2.3")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.3")


    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")



}