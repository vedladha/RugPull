plugins {
    `java-library`
    id("environment-preset")
    id("checkstyle-preset")
    id("jacoco-preset")
    alias(libs.plugins.shadow)
}

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

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    enabled = true

    archiveClassifier = "bundle"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

val copyJars by tasks.registering(Copy::class) {
    from(tasks.jar)
    from(tasks.shadowJar)
    into(rootProject.layout.buildDirectory.dir("libs"))

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.build {
    dependsOn(tasks.check)
    dependsOn(tasks.test)
    finalizedBy(copyJars)
}
