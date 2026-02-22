plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "rpc_currency"
gradle.rootProject {
    group = "edu.wisc.t32"
    version = "1.0.0-SNAPSHOT"
}

include("app")
