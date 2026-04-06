dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Point this to your root project's TOML file
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
