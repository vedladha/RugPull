plugins {
    jacoco
    checkstyle
    application
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

jacoco {
    toolVersion = "0.8.14"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = main
}

checkstyle {
    toolVersion = "13.2.0"
    configFile = rootProject.file("config").resolve("checkstyle.xml")
}

tasks.named<Test>("test") {
    environment(env.allVariables())
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.named<JavaExec>("run") {
    environment(env.allVariables())
}

tasks.named<JavaExec>("runShadow") {
    environment(env.allVariables())
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required = true
        csv.required = true
        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
    }
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
