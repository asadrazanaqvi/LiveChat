
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.compose.compiler) // Add Compose Compiler plugin
}

android {
    namespace = "com.example.livechatdemo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.livechatdemo"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        android {
            namespace = "com.example.livechatdemo"
            compileSdk = 34

            defaultConfig {
                applicationId = "com.example.livechatdemo"
                minSdk = 27
                targetSdk = 34
                versionCode = 1
                versionName = "1.0"

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables {
                    useSupportLibrary = true
                }
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
                sourceCompatibility = JavaVersion.VERSION_17 // Updated to Java 17 for Kotlin 2.0
                targetCompatibility = JavaVersion.VERSION_17
            }

            kotlinOptions {
                jvmTarget = "17" // Updated to match Java 17
            }

            buildFeatures {
                compose = true
            }

            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.16" // Latest for Kotlin 2.0.20
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }

        dependencies {
            // Core
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.activity.compose)
            implementation("com.google.android.material:material:1.11.0")
            implementation("org.java-websocket:Java-WebSocket:1.5.6")
            implementation("androidx.work:work-runtime-ktx:2.9.0")

            // Jetpack Compose
            implementation(platform(libs.androidx.compose.bom))
            implementation(libs.androidx.ui)
            implementation(libs.androidx.ui.graphics)
            implementation(libs.androidx.ui.tooling.preview)
            implementation(libs.androidx.material3)
            implementation(libs.androidx.navigation.compose) // For navigation

            // Coroutines and Flow
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.android)

            // Hilt
            implementation(libs.hilt.android)
            kapt(libs.hilt.compiler)
            implementation(libs.androidx.hilt.navigation.compose)

            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
            kapt(libs.androidx.room.compiler)

            // WebSocket (OkHttp)
            implementation(libs.okhttp)

            // Jetpack Security
            implementation(libs.androidx.security.crypto)

            // Testing
            testImplementation(libs.junit)
            testImplementation(libs.mockk)
            testImplementation(libs.kotlinx.coroutines.test)
            testImplementation(libs.androidx.arch.core.testing)
            androidTestImplementation(libs.androidx.junit)
            androidTestImplementation(libs.androidx.espresso.core)
            androidTestImplementation(platform(libs.androidx.compose.bom))
            androidTestImplementation(libs.androidx.ui.test.junit4)
            debugImplementation(libs.androidx.ui.tooling)
            debugImplementation(libs.androidx.ui.test.manifest)
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation(libs.androidx.hilt.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}