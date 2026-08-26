import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.xiaotimel.sls.report"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(libs.aliyun.log.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson)

    compileOnly(libs.firebase.analytics.ktx)
}

val prop = Properties().apply {
    load(FileInputStream(File(".", "gradle.properties")))
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        register<MavenPublication>("release") {
            groupId = "com.xiaotimel.sls.report"
            artifactId = "sls"
            version = "0.0.2"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}