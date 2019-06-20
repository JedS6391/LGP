package nz.co.jedsimson.lgp.test.evolution

import nz.co.jedsimson.lgp.core.environment.EnvironmentDefinition
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.test.mocks.MockEnvironment
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

// These tests also cover module container
object MutationOperatorFeature : Spek({

    Feature("Macro mutations") {
        Scenario("Inserting a random instruction") {
            var environment: EnvironmentDefinition<Double, Outputs.Single<Double>>? = null

            Given("A macro mutation operator") {
                environment = MockEnvironment()
            }

            When("A module is loaded") {
            }

            Then("No module should be loaded") {
            }
        }
    }
})