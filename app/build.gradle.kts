import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.baselineprofile")
}

// Plugin google-services hanya diterapkan bila berkas konfigurasinya
// ada (app/google-services.json, dari Firebase Console) - tanpanya
// aplikasi tetap ter-build persis seperti sebelumnya dan Mode Online
// tidak aktif. Pola yang sama seperti KasBulanan dan Apotek Lestari.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// AGP 9 has built-in Kotlin support, so the org.jetbrains.kotlin.android
// plugin is no longer applied; the JVM target moves to this block.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "id.kasir.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.kasir.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    // Kunci tanda tangan TETAP untuk semua build dari repo ini (keystore
    // kasir.p12 di root repo). Tanpa ini, setiap build GitHub Actions
    // memakai kunci debug acak baru, sehingga APK baru tidak bisa dipasang
    // menimpa yang lama dan aplikasi harus di-uninstall dulu setiap update.
    signingConfigs {
        create("ci") {
            storeFile = rootProject.file("kasir.p12")
            storePassword = "kasir"
            keyAlias = "kasir"
            keyPassword = "kasir"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("ci")
            // R8 aktif juga di build debug: APK yang dipasang sehari-hari
            // sudah teruji melewati minify + shrink resources.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            signingConfig = signingConfigs.getByName("ci")
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    // Applies the shipped baseline profile at install time on API < 31 and
    // provides the profile installer the profile tooling expects.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.activity:activity-compose:1.12.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")

    // Database lokal (Room) — data rental tersimpan di HP, tanpa server
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.8.5")

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3:1.5.0-beta02")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
    // org.json asli untuk tes unit BackupHelper (di perangkat, org.json bawaan Android).
    testImplementation("org.json:json:20240303")

    // Mode Online (Firebase) - pola yang sama seperti KasBulanan:
    // autentikasi + Firestore untuk cermin awan per akun. Semua
    // pemakaian dijaga FirebaseKit supaya aplikasi tetap jalan penuh
    // tanpa berkas konfigurasi (mode lokal saja).
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}
