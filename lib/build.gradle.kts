plugins {
    val kotlinVersion = "1.5.31"

    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version kotlinVersion

    // Documentation generation
    id("org.jetbrains.dokka") version (kotlinVersion)
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
    val group = "nz.co.jedsimson.lgp"
    val version = "5.3"

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
        from(javadoc)
    }

    dokkaHtml.configure {
        outputDirectory.set(rootProject.rootDir.resolve("docs/api/html/lib"))
    }

    dokkaJavadoc.configure {
        outputDirectory.set(rootProject.rootDir.resolve("docs/api/javadoc/lib"))
    }
}

//// Signing and deployment
//apply plugin: 'maven'
//
//// We don't want local builds to have to depend on the signing process, so we disable
//// signing if the signing GPG key ID is not present.
//if (System.getenv('GPG_KEY_ID')) {
//    apply plugin: 'signing'
//
//    signing {
//        required { gradle.taskGraph.hasTask("uploadArchives") }
//        sign configurations.archives
//        setRequired { required true }
//    }
//}

//// Build, sign, and upload
//uploadArchives {
//    repositories {
//        mavenDeployer {
//
//            // Sign POM
//            beforeDeployment {
//                MavenDeployment deployment -> signing.signPom(deployment)
//            }
//
//            // Destination
//            repository(url: "https://oss.sonatype.org/service/local/staging/deploy/maven2/") {
//                authentication(userName: System.getenv('SONATYPE_USERNAME'), password: System.getenv('SONATYPE_PASSWORD'))
//            }
//            snapshotRepository(url: "https://oss.sonatype.org/content/repositories/snapshots/") {
//                authentication(userName: System.getenv('SONATYPE_USERNAME'), password: System.getenv('SONATYPE_PASSWORD'))
//            }
//
//            // Add required metadata to POM
//            pom.project {
//                name 'LGP-lib'
//                packaging 'jar'
//                description 'A library of implementations for core LGP framework components.'
//                url 'https://github.com/JedS6391/LGP-lib'
//
//                scm {
//                    connection 'scm:git:git://github.com/JedS6391/LGP-lib.git'
//                    developerConnection 'scm:git:ssh://github.com/JedS6391/LGP-lib.git'
//                    url 'http://github.com/JedS6391/LGP-lib/tree/master'
//                }
//
//                licenses {
//                    license {
//                        name 'MIT License'
//                        url 'https://github.com/JedS6391/LGP-lib/blob/master/LICENSE'
//                    }
//                }
//
//                developers {
//                    developer {
//                        id 'jedsimson'
//                        name 'Jed Simson'
//                        email 'jed.simson@gmail.com'
//                        organization 'jedsimson'
//                        organizationUrl 'https://jedsimson.co.nz'
//                    }
//                }
//            }
//        }
//    }
//}
