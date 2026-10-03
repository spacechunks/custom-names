plugins {
    alias(libs.plugins.sonatype.central.portal.publisher)
    id("maven-publish")
}

dependencies {
    implementation(libs.minestom)
    api(project(":custom-names-api"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

centralPortal {
    name = project.name

    username = System.getenv("SONATYPE_USERNAME")
    password = System.getenv("SONATYPE_PASSWORD")

    pom {
        name.set("Custom Names")
        description.set("An API that allows you to register custom names on top of entities. These entities are fully client side, and corretly synced between players.")
        url.set("https://github.com/spacechunks/custom-names")

        developers {
            developer {
                id.set("fllipeis")
                email.set("p.eistrach@gmail.com")
            }
        }
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            url.set("https://github.com/spacechunks/custom-names.git")
            connection.set("scm:git:github.com:spacechunks/custom-names.git")
        }
    }
}

signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    } else {
        useGpgCmd()
    }
    sign(publishing.publications)
}