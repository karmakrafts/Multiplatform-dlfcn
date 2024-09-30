plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    mingwX64()
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    applyDefaultHierarchyTemplate()
    sourceSets {
        nativeMain {
            dependencies {
                implementation(project(":multiplatform-dlfcn"))
                implementation(libs.kotest.engine)
            }
        }
    }
}