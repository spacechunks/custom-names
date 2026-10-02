plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.paperweight.userdev)
}

dependencies {
    compileOnly(rootProject.libs.paper.api)
    implementation(rootProject.libs.reflection.remapper)
    api(project(":custom-names-api"))
    paperweight.paperDevBundle(libs.versions.paper.api)
}

tasks {
    runServer {
        minecraftVersion("26.3")
    }
}