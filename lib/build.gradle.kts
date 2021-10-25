plugins {
    val kotlinVersion = "1.5.31"

    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version kotlinVersion

    // Documentation generation
    id("org.jetbrains.dokka") version (kotlinVersion)

    id("java")

    // Artifact publishing
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    // Use the Kotlin JDK 8 standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation(project(":core"))

    // For YAML parsing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.0")
}

tasks {
    val mainArtifactName = "${rootProject.name}-${project.name}"
    val group = project.group.toString()
    val version = project.version.toString()

    // Main library JAR.
    jar {
        archiveBaseName.set(mainArtifactName)

        manifest {
            attributes["Implementation-Title"] = mainArtifactName
            attributes["Implementation-Version"] = version
        }
    }

    // JAR for distribution of source files.
    val sourcesJar by creating(Jar::class) {
        archiveBaseName.set(mainArtifactName)
        archiveClassifier.set("sources")

        manifest {
            attributes["Implementation-Title"] = mainArtifactName
            attributes["Implementation-Version"] = version
        }

        from(sourceSets.main.get().allSource)
    }

    // JAR for distribution of JavaDoc.
    val javaDocsJar by creating(Jar::class) {
        archiveBaseName.set(mainArtifactName)
        archiveClassifier.set("javadoc")
        from(dokkaJavadoc)
    }

    dokkaHtml.configure {
        outputDirectory.set(rootProject.rootDir.resolve("docs/api/html/lib"))
    }

    dokkaJavadoc.configure {
        outputDirectory.set(rootProject.rootDir.resolve("docs/api/javadoc/lib"))
    }

    signing {
        // Sign archives when the required environment variables are available.
        val signingEnvironmentVariableNames = listOf(
                "ORG_GRADLE_PROJECT_signingKeyId",
                "ORG_GRADLE_PROJECT_signingKey",
                "ORG_GRADLE_PROJECT_signingPassword"
        )

        setRequired({
            signingEnvironmentVariableNames.all { System.getenv(it) != null  } &&
                gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
        })

        val signingKeyId: String? by project
        val signingKey: String? by project
        val signingPassword: String? by project

        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

        sign(configurations.archives.get())
        sign(publishing.publications)
    }

    publishing {
        publications {
            create<MavenPublication>("LGP-lib") {
                setGroupId(group)
                setVersion(version)

                from(project.components["java"])
                artifact(sourcesJar)
                artifact(javaDocsJar)

                pom {
                    packaging = "jar"
                    name.set("LGP-lib")
                    description.set("A library of implementations for core LGP framework components.")
                    url.set("https://github.com/JedS6391/LGP")

                    scm {
                        connection.set("scm:git:git://github.com/JedS6391/LGP.git")
                        developerConnection.set("scm:git:ssh://github.com/JedS6391/LGP.git")
                        url.set("http://github.com/JedS6391/LGP/tree/master")
                    }

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/JedS6391/LGP/blob/master/LICENSE")
                        }
                    }

                    developers {
                        developer {
                            id.set("jedsimson")
                            name.set("Jed Simson")
                            email.set("jed.simson@gmail.com")
                            organization.set("jedsimson")
                            organizationUrl.set("https://www.jedsimson.co.nz")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

                authentication {
                    credentials.username = System.getenv("SONATYPE_USERNAME")
                    credentials.password = System.getenv("SONATYPE_PASSWORD")
                }
            }
        }
    }
}