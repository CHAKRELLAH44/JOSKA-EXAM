// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.2")  // Version cohérente avec vos plugins
        classpath("com.google.gms:google-services:4.4.2")  // Ajout spécifique demandé
    }
}

plugins {
    id("com.android.application") version "8.9.2" apply false
    id("com.android.library") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // Le plugin google-services est déjà dans le buildscript
}

