package nz.co.jedsimson.lgp.test.environment

import nz.co.jedsimson.lgp.core.environment.config.Configuration
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object ConfigurationFeature : Spek({
    Feature("Configuration") {

        Scenario("At least one feature must be provided") {
            val configuration = Configuration()

            Given("A configuration with no features") {
                configuration.numFeatures = 0
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }

            Given("A configuration with negative one features") {
                configuration.numFeatures = -1
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }
        }

        Scenario("Constants must be provided when constant rate is set") {
            val configuration = Configuration().apply {
                numFeatures = 1
            }

            Given("A configuration with a constant rate of 0.5") {
                configuration.constantsRate = 0.5
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }
        }

        Scenario("Constants must be provided when constant rate is set") {
            val configuration = Configuration().apply {
                numFeatures = 1
            }

            Given("A configuration with a constant rate of 0.5 but no constants") {
                configuration.constants = listOf()
                configuration.constantsRate = 0.5
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }
        }

        Scenario("Constant rate should be positive") {
            val configuration = Configuration().apply {
                numFeatures = 1
                constants = listOf("1")
            }

            Given("A configuration with a constant rate of -0.5") {
                configuration.constantsRate = -0.5
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }
        }

        Scenario("Program lengths should all be positive") {
            val configuration = Configuration().apply {
                numFeatures = 1
                constants = listOf("1")
                constantsRate = 0.1
            }

            Given("All program lengths set to negative values") {
                configuration.initialMinimumProgramLength = -1
                configuration.initialMaximumProgramLength = -1
                configuration.minimumProgramLength = -1
                configuration.maximumProgramLength = -1
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }
        }

        Scenario("At least one operation should be specified") {
            val configuration = Configuration().apply {
                numFeatures = 1
                constants = listOf("1")
                constantsRate = 0.1
                initialMinimumProgramLength = 1
                initialMaximumProgramLength = 1
                minimumProgramLength = 1
                maximumProgramLength = 1
            }

            Given("A configuration with no operations") {
                configuration.operations = listOf()
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }
        }

        Scenario("Population size and number of generations must be positive") {
            val configuration = Configuration().apply {
                numFeatures = 1
                constants = listOf("1")
                constantsRate = 0.1
                initialMinimumProgramLength = 1
                initialMaximumProgramLength = 1
                minimumProgramLength = 1
                maximumProgramLength = 1
                operations = listOf("Test")
            }

            Given("A configuration with negative population size AND number of generations") {
                configuration.populationSize = -1
                configuration.generations = -1
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }

            Given("A configuration with negative population size") {
                configuration.populationSize = -1
                configuration.generations = 1
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }

            Given("A configuration with negative number of generations") {
                configuration.populationSize = 1
                configuration.generations = -1
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }
        }

        Scenario("At least one run should be requested") {
            val configuration = Configuration().apply {
                numFeatures = 1
                constants = listOf("1")
                constantsRate = 0.1
                initialMinimumProgramLength = 1
                initialMaximumProgramLength = 1
                minimumProgramLength = 1
                maximumProgramLength = 1
                operations = listOf("Test")
                populationSize = 1
                generations = 1
            }

            Given("A configuration with no runs") {
                configuration.numberOfRuns = 0
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }

            Given("A configuration with a negative number of runs") {
                configuration.numberOfRuns = -1
            }

            Then("The configuration is not valid") {
                assert(!configuration.isValid().isValid) { "Configuration was valid but should have been invalid" }
            }
        }
    }
})