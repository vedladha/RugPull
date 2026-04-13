plugins {
    java
    jacoco
    checkstyle
    alias(libs.plugins.spring.boot.framework)
    alias(libs.plugins.spring.dependency.management)
}

jacoco {
    toolVersion = "0.8.14"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("libs/api-bundle.jar"))
    implementation(libs.spring.boot.webmvc)
    implementation(libs.spring.boot.data)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.actuator)

    implementation(libs.jwt.api)
    runtimeOnly(libs.jwt.impl)
    runtimeOnly(libs.jwt.jackson)

    runtimeOnly(libs.mysql.connector)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.webmvc.test)
    testImplementation(libs.spring.boot.security.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)
    
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly(libs.junit.platform)
}

checkstyle {
    toolVersion = "13.2.0"
    configFile = rootProject.file("config/checkstyle.xml")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))

    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}
