plugins {
    java
    checkstyle
}

checkstyle {
    toolVersion = "13.2.0"
    configFile = rootProject.file("config").resolve("checkstyle.xml")
}
