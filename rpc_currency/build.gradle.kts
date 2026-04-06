plugins {
    java
    `jacoco-report-aggregation`
    alias(libs.plugins.dotenv) apply true
}

repositories {
    mavenCentral()
}

dependencies {
    jacocoAggregation(project(":app"))
    jacocoAggregation(project(":api"))
}

tasks.jar {
    enabled = false
}

tasks.testCodeCoverageReport {
    reports {
        html.required = true
        xml.required = true
        csv.required = true
    }
}
