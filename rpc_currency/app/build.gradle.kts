plugins {
    application
    id("environment-preset")
    id("checkstyle-preset")
    id("jacoco-preset")
    alias(libs.plugins.shadow)
}

val main = "edu.wisc.t32.Main"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.grpc)
    implementation(libs.gson)
    implementation(libs.hedera.hashgraph)
    implementation(libs.slf4j)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = main
}

tasks.test {
    useJUnitPlatform()
}

val copyJars by tasks.registering(Copy::class) {
    from(tasks.jar)
    from(tasks.shadowJar)
    into(rootProject.layout.buildDirectory.dir("libs"))

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = main
        attributes["Description"] = $$"This is an application JAR for $RPC"
    }
}

tasks.shadowJar {
    enabled = true

    archiveClassifier = "bundle"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.check)
    dependsOn(tasks.named<Test>("test"))
    finalizedBy(copyJars)
}
