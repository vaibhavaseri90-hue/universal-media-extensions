plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

buildscript {
    extra.apply {
        set("extVersionCode", 1)
        set("extName", "Public Domain Cinema")
        set("pkgNameSuffix", "en.publicdomaincinema")
        set("extClass", ".PublicDomainCinema")
        // Uses only the API level documented as compatible with Tadami lib 1.5+.
        set("apiVersion", "1.5")
    }
}

apply(from = "$rootDir/common.gradle")
