import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    val kotlinVersion = "1.5.31"

    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version kotlinVersion

    // Documentation generation
    id("org.jetbrains.dokka") version (kotlinVersion)
    
    // Test report generation 
    id("jacoco")

    // Artifact publishing
    `maven-publish`
    signing
}

repositories { 
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Kotlin co-routine support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.1")

    // Used for JSON configuration file loading
    implementation("com.google.code.gson:gson:2.8.8")

    // Used for CSV dataset file loading
    implementation("com.opencsv:opencsv:5.5.2")

    // Tests
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:2.0.17")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:2.0.17")

    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

    testCompileOnly("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testCompileOnly("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

java {                                      
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    val group = "nz.co.jedsimson.lgp"
    val version = "5.2"

    jar {
        manifest {
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = version
        }
    }

    // JAR for distribution of source files.
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")

        from(sourceSets.main.get().allSource)
    }

    // JAR for distribution of project + dependency binaries (i.e. fat JAR).
    val coreJar by creating(Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveBaseName.set("${project.name}-core")

        manifest {
            attributes["Implementation-Title"] = project.name
            attributes["Implementation-Version"] = version
        }

        from(configurations.runtimeClasspath
                .get()
                .map { if (it.isDirectory) it else zipTree(it) })

        val sourcesMain = sourceSets.main.get()

        from(sourcesMain.output)
    }

    val javaDocsJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from(dokkaJavadoc)
    }

    test {   
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
        }
    }

    dokkaHtml.configure {
        outputDirectory.set(projectDir.resolve("docs/api/html"))

        dokkaSourceSets {
            named("main") {
                reportUndocumented.set(true)

                includes.from(
                    "src/main/kotlin/nz/co/jedsimson/lgp/core/environment/README.md",
                    "src/main/kotlin/nz/co/jedsimson/lgp/core/evolution/README.md",
                    "src/main/kotlin/nz/co/jedsimson/lgp/core/modules/README.md",
                    "src/main/kotlin/nz/co/jedsimson/lgp/core/program/README.md"
                )
            }
        }
    }

    dokkaJavadoc.configure {
        outputDirectory.set(projectDir.resolve("docs/api/javadoc"))
    }

    jacocoTestReport {
        dependsOn(test)

        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    signing {
        setRequired({ System.getenv("GPG_KEY_ID") != null && gradle.taskGraph.hasTask("uploadArchives") })
        sign(configurations.archives.get())
    }

    publishing {
        publications {
            create<MavenPublication>("LGP") {
                setGroupId(group)
                setVersion(version)

                artifact(jar)
                artifact(sourcesJar)
                artifact(javaDocsJar)

                pom {
                    name.set("LGP")
                    description.set("A robust Linear Genetic Programming implementation on the JVM using Kotlin.")
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
                             organizationUrl.set("https://jedsimson.co.nz")
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

    artifacts {
        archives(sourcesJar)
        archives(javaDocsJar)
    }
}