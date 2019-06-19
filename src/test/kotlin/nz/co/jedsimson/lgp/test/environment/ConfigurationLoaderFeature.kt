package nz.co.jedsimson.lgp.test.environment

import com.google.gson.JsonSyntaxException
import nz.co.jedsimson.lgp.core.environment.ComponentLoadException
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.config.JsonConfigurationLoader
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object ConfigurationLoaderFeature : Spek({
    val testConfigurationFilePath = this.javaClass.classLoader.getResource("test-configuration.json").file
    val testConfigurationInvalidFilePath = this.javaClass.classLoader.getResource("test-configuration-invalid.json").file

    val expectedConfiguration = Configuration().apply {
        initialMinimumProgramLength = 20
        initialMaximumProgramLength = 40
        minimumProgramLength = 20
        maximumProgramLength = 400
        operations = listOf(
            "nz.co.jedsimson.lgp.lib.operations.Addition",
            "nz.co.jedsimson.lgp.lib.operations.Subtraction"
        )
        constantsRate = 0.2
        constants = listOf(
            "0.0", "0.1", "1.0"
        )
        numCalculationRegisters = 5
        populationSize = 500
        numFeatures = 2
        crossoverRate = 0.2
        microMutationRate = 0.2
        macroMutationRate = 0.8
        generations = 100
        numOffspring = 10
        branchInitialisationRate = 0.1
        stoppingCriterion = 0.0001
        numberOfRuns = 2
    }

    Feature("Loading configuration from a JSON file") {
        Scenario("Load configuration from valid file") {
            lateinit var configurationLoader: JsonConfigurationLoader
            var configuration: Configuration? = null

            Given("A JsonConfigurationLoader for the file test-configuration.json") {
                configurationLoader = JsonConfigurationLoader(testConfigurationFilePath)
            }

            When("The configuration is loaded") {
                configuration = configurationLoader.load()
            }

            Then("The configuration should be loaded successfully") {
                assert(configuration != null) { "Configuration is null" }
            }

            And("The configuration is valid") {
                val validity = configuration!!.isValid()

                assert(validity.isValid) { "Configuration is not valid" }
            }

            And("The configuration is loaded correctly") {
                assert(configuration == expectedConfiguration) { "Configuration loaded does not match expected"}
            }
        }

        Scenario("Load configuration from invalid file") {
            lateinit var configurationLoader: JsonConfigurationLoader
            var configuration: Configuration? = null
            var exception: Exception? = null

            Given("A JsonConfigurationLoader for the file test-configuration-invalid.json") {
                configurationLoader = JsonConfigurationLoader(testConfigurationInvalidFilePath)
            }

            When("The configuration is loaded") {
                try {
                    configuration = configurationLoader.load()
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The configuration should not be loaded successfully") {
                assert(configuration == null) { "Configuration is not null" }
                assert(exception != null) { "Exception is null"}
                assert(exception is ComponentLoadException) { "Exception is not of correct type" }
                assert(exception!!.cause is JsonSyntaxException) { "Inner exception is not of correct type "}
            }
        }

        Scenario("Load configuration from valid file using builder") {
            lateinit var configurationLoaderBuilder: JsonConfigurationLoader.Builder
            lateinit var configurationLoader: JsonConfigurationLoader
            var configuration: Configuration? = null

            Given("A JsonConfigurationLoader.Builder for the file test-configuration.json") {
                configurationLoaderBuilder = JsonConfigurationLoader
                    .Builder()
                    .filename(testConfigurationFilePath)

                assert(configurationLoaderBuilder.filename == testConfigurationFilePath)
            }

            And("The JsonConfigurationLoader is built") {
                // The compiler guarantees that configurationLoader is not null.
                configurationLoader = configurationLoaderBuilder.build()
            }

            When("The configuration is loaded") {
                configuration = configurationLoader.load()
            }

            Then("The configuration should be loaded successfully") {
                assert(configuration != null) { "Configuration is null" }
            }

            And("The configuration is valid") {
                val validity = configuration!!.isValid()

                assert(validity.isValid) { "Configuration is not valid" }
            }

            And("The configuration is loaded correctly") {
                assert(configuration == expectedConfiguration) { "Configuration loaded does not match expected"}
            }
        }
    }
})