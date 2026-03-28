plugins {
    jacoco
    checkstyle
    application
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
