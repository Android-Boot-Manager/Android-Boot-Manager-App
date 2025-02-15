// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.8.0" apply false
    val kotlinVersion = "2.0.0"
    id("org.jetbrains.kotlin.android")  version kotlinVersion apply false
    id("org.jetbrains.kotlin.plugin.compose")  version kotlinVersion apply false
    id("com.mikepenz.aboutlibraries.plugin") version "11.2.1" apply false
}