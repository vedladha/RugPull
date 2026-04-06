plugins {
    java
    jacoco
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}


tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required = true
        csv.required = true
        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
    }
}
