plugins {
    alias(libs.plugins.android).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.ksp).apply(false)
    alias(libs.plugins.googleGmsGoogleServices) apply false
}

tasks.register<Delete>("clean") {
    delete {
        rootProject.buildDir
    }
}



