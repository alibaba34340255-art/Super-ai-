// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.set("kotlinVersion", "1.9.20")
}
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version extra["kotlinVersion"] as String apply false
    id("org.jetbrains.kotlin.plugin.serialization") version extra["kotlinVersion"] as String apply false
}
