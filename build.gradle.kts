buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://plugins.gradle.org/m2/") {
            content {
                includeGroupByRegex("com\\.google.*")
                includeGroup("com.squareup")
                includeGroupByRegex("commons-.*")
                includeModule("org.jdom", "jdom2")
                includeModule("org.ow2", "ow2")
                includeGroup("org.ow2.asm")
                includeGroupByRegex("org\\.jetbrains.*")
                includeGroup("org.slf4j")
                includeModule("org.bitbucket.b_c", "jose4j")
                includeModule("org.checkerframework", "checker-qual")
                includeGroup("net.java.dev.jna")
                includeModule("net.java", "jvnet-parent")
                includeModule("javax.annotation", "javax.annotation-api")
                includeGroupByRegex("org\\.apache.*")
                includeGroupByRegex("com\\.sun.*")
                includeModule("xerces", "xercesImpl")
                includeModule("xml-apis", "xml-apis")
                includeGroup("org.bouncycastle")
                includeGroupByRegex("net\\.sf.*")
                includeModule("javax.inject", "javax.inject")
                includeModule("org.tensorflow", "tensorflow-lite-metadata")
                includeModule("org.json", "json")
                includeGroup("io.grpc")
                includeGroup("io.netty")
                includeModule("io.perfmark", "perfmark-api")
                includeModule("org.codehaus.mojo", "animal-sniffer-annotations")
                includeGroup("org.glassfish.jaxb")
                includeGroupByRegex("jakarta.*")
                includeModule("org.jvnet.staxex", "stax-ex")
                includeGroup("com.testdroid")
                includeModule("log4j", "log4j")
                includeModule("com.fasterxml", "oss-parent")
                includeGroupByRegex("com\\.fasterxml\\.jackson.*")
                includeModule("org.sonatype.oss", "oss-parent")
                includeModule("org.eclipse.ee4j", "project")
                includeGroup("org.codehaus.mojo")
            }
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.1.0")
    }
}


configurations.configureEach {
    resolutionStrategy {
        force("com.android.support:support-v4:1.0.0")
    }
}


allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://raw.githubusercontent.com/guardianproject/gpmaven/master") {
            content {
                includeModule("org.proofmode", "android-libproofmode")
            }
        }

        maven("https://jitpack.io") {
            content {
                includeModule("com.github.esafirm", "android-image-picker")
                includeModule("com.github.derlio", "audio-waveform")
                includeModule("com.github.abdularis", "circularimageview")
                includeModule("com.github.guardianproject", "sardine-android")
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}