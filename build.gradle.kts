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

    `java-library`
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
    val version = "5.1"

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

    artifacts {
        archives(sourcesJar)
    }
}