plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.chsbuffer.installer.xp"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.chsbuffer.installer.xp"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
