plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
    // The AnchorScanner port is suspending, so its tests drive it with runTest.
    testImplementation(libs.kotlinx.coroutines.test)
}
