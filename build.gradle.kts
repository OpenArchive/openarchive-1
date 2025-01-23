plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10-RC2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10-RC2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" apply false
    id("com.google.devtools.ksp") version "2.1.10-RC2-1.0.29" apply false
}

configurations.configureEach {
    resolutionStrategy {
        force("com.android.support:support-v4:1.0.0")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
