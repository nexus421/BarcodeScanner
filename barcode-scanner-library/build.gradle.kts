plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    compileSdk = 37

    defaultConfig {
        minSdk = 23

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

    publishing {
        singleVariant("release") {
            // Optional: mit Sources und Javadoc
            withSourcesJar()
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    namespace = "bayern.kickner.barcode_scanner_library"
}

dependencies {

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.cardview:cardview:1.0.0")

    api("com.google.mlkit:barcode-scanning:17.3.0")
    //api == share this dependency. implementation == dependency is only for this module (here: the library) -> But we may need the BarcodeScannerOptions. So use api here
    implementation("androidx.camera:camera-camera2:1.6.1")
    implementation("androidx.camera:camera-lifecycle:1.6.1")
    implementation("androidx.camera:camera-view:1.6.1")
    implementation("androidx.camera:camera-core:1.6.1")
    implementation("androidx.camera:camera-extensions:1.6.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.23.0")
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
                version = "3.0.0"
                from(components["release"])
            }
        }
    }
}