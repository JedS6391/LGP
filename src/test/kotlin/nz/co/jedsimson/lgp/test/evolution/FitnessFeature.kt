package nz.co.jedsimson.lgp.test.evolution

import com.nhaarman.mockitokotlin2.mock
import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessContexts
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object FitnessFeature : Spek({

    Feature("Fitness context") {

        Scenario("Single output fitness context") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            //val mockProgram = mock< Program<Double, Outputs.Single<Double>>()

            Given("") {
                val s = FitnessContexts.SingleOutputFitnessContext(mockEnvironment)
            }
        }

    }
})