plugins {
    java
}

val rootEnv = rootProject.extensions.getByName("env") as co.uzzu.dotenv.gradle.DotEnvRoot

tasks.test {
    environment(rootEnv.allVariables())
}

tasks.withType<JavaExec> {
    environment(rootEnv.allVariables())

}
