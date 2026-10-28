import java.util.Properties
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.android.build.api.variant.impl.VariantOutputImpl

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 读取签名配置：如果 keystore.properties 存在就用正式证书，否则退回调试证书
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().also { props ->
    if (keystorePropsFile.exists()) {
        keystorePropsFile.reader().use { props.load(it) }
    }
}
val hasReleaseSigning = keystorePropsFile.exists() &&
        keystoreProps.getProperty("storeFile") != null

// ---- 版本号 ----
// version.properties 里维护 versionName 和 versionCode；
// 若 versionCode 写成 auto，则用「年 月 日 × 10 + 当日序号」自动生成，适合频繁出包。
val versionFile = rootProject.file("version.properties")
val versionProps = Properties().also { props ->
    if (versionFile.exists()) {
        versionFile.reader().use { props.load(it) }
    }
}
val appVersionName = versionProps.getProperty("versionName", "1.0.0")
val appVersionCode: Int = run {
    val raw = versionProps.getProperty("versionCode", "auto").trim()
    if (raw.equals("auto", ignoreCase = true)) {
        // 例：2026-10-24 => 20261024，数值单调递增且直观
        LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
    } else {
        raw.toIntOrNull() ?: 1
    }
}

android {
    namespace = "com.pxy.depositbook"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pxy.depositbook"
        minSdk = 26
        targetSdk = 36
        // 版本号统一从 version.properties 读取，改一处即可
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 有正式证书用正式证书，没有则退回 debug 证书，保证随时能出包
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
            )
        }
    }
    // 按 ABI 拆包，可进一步减小体积
    splits {
        abi {
            isEnable = false
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // WorkManager (到期提醒)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // 银行 logo SVG 渲染（离线 assets + coil-svg）
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

androidComponents {
    onVariants { variant ->
        val vName = appVersionName
        variant.outputs.forEach { output ->
            val outputImpl = output as VariantOutputImpl
            val buildType = variant.buildType
            val fileName = if (buildType == "release") {
                "deposit-book-v${vName}.apk"
            } else {
                "deposit-book-v${vName}-debug.apk"
            }
            outputImpl.outputFileName = fileName
        }
    }
}
