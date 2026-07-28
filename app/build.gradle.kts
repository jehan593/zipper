plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zipper.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zipper.app"
        minSdk = 26
        targetSdk = 35
        // Overridable via -PappVersionCode=/-PappVersionName= — CI (.github/workflows/build-apk.yml)
        // passes github.run_number so every push gets a strictly increasing versionCode (required
        // for update checkers like Obtainium to see a new build) and its own tag/release rather
        // than overwriting one shared release. Local/default builds fall back to a static version.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0"

        // Real phones are arm64-v8a (current) or armeabi-v7a (older/budget) — x86/x86_64 only
        // matter for emulators, which aren't a distribution target for a sideloaded APK. Drops
        // the emulator-only native library variants from every build.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        getByName("debug") {
            // Committed keystore (not the AGP-generated one) so every build — local or CI —
            // signs with the same key. Without this, a fresh CI runner would auto-generate a new
            // ~/.android/debug.keystore each run, giving every release a different signature and
            // breaking in-place updates (Obtainium/Android reject cross-signature installs as a
            // version conflict) even though versionCode keeps increasing.
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Same committed keystore as the debug signingConfig above — this is the variant CI
            // and local builds actually ship (see build-apk.yml), so it needs the same pinned key
            // for update checkers (Obtainium) and Android itself to accept it as an in-place
            // update rather than a signature conflict.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // kotlin-stdlib/kotlinx-coroutines metadata — no reflection, no debug-probe API used.
            excludes += "kotlin/**"
            excludes += "DebugProbesKt.bin"
            // commons-compress/commons-io/junrar all ship these plain-text metadata files under
            // the same META-INF path; without excluding them the merger fails with a duplicate-path
            // build error the moment more than one of these libraries is on the classpath together.
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/versions/9/OSGI-INF/**"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    // SAF folder/tree traversal (DocumentFile) — every source/destination the user picks is a
    // content:// Uri, never a raw filesystem path, so this is used everywhere instead of java.io.File.
    implementation(libs.androidx.documentfile)

    // ZIP create+extract with AES password support.
    implementation(libs.zip4j)
    // 7z and TAR/TAR.GZ create+extract. 7z's default entry codec is LZMA2, which commons-compress
    // only supports when org.tukaani:xz is also on the classpath (declared optional upstream).
    implementation(libs.commons.compress)
    implementation(libs.xz)
    // RAR extraction only — no free/open-source library can *write* RAR (proprietary format).
    // Ships under a custom "UnRAR license" rather than Apache/MIT, unlike every other dependency
    // here: free to use for extraction, but forbids using it to build a RAR-compatible archiver.
    implementation(libs.junrar)
}
