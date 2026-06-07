plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.expensetracker.feature.ai"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "HF_CLIENT_ID",
            "\"${project.findProperty("HF_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "HF_REDIRECT_URI",
            "\"${project.findProperty("HF_REDIRECT_URI") ?: "expensetracker://auth/huggingface"}\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:finance"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.google.ai.edge.litertlm.android)
    implementation(libs.tensorflow.lite.gpu)

    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit4)
}
