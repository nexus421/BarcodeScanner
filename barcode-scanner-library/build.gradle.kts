plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        targetSdk = 36

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    publishing {
        singleVariant("release") {
            // Optional: mit Sources und Javadoc
            withSourcesJar()
        }
    }

    namespace = "bayern.kickner.barcode_scanner_library"
}

dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.cardview:cardview:1.0.0")

    api("com.google.mlkit:barcode-scanning:17.3.0")
    //api == share this dependency. implementation == dependency is only for this module (here: the library) -> But we may need the BarcodeScannerOptions. So use api here
    implementation("androidx.camera:camera-camera2:1.5.2")
    implementation("androidx.camera:camera-lifecycle:1.5.2")
    implementation("androidx.camera:camera-view:1.5.2")
    implementation("androidx.camera:camera-core:1.5.2")
    implementation("androidx.camera:camera-extensions:1.5.2")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "nexus421Maven"
                url = uri("https://maven.kickner.bayern/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "bayern.kickner"
                artifactId = "BarcodeScanner"
                version = "2.2.3"
                from(components["release"])
            }
        }
    }
}