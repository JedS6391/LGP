package nz.co.jedsimson.lgp.test.evolution

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.environment.logging.LoggerProvider
import nz.co.jedsimson.lgp.core.evolution.operators.selection.BinaryTournamentSelection
import nz.co.jedsimson.lgp.core.evolution.operators.selection.IndividualSelector
import nz.co.jedsimson.lgp.core.evolution.operators.selection.TournamentSelection
import nz.co.jedsimson.lgp.core.evolution.operators.selection.tournament
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import kotlin.random.Random

object SelectionOperatorFeature : Spek({
    Feature("Binary tournament selection operator") {
        Scenario("Binary tournament selection operator with an invalid tournament size") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            var binaryTournamentSelection: BinaryTournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var exception: Exception? = null

            whenever(mockEnvironment.loggerProvider).thenReturn(LoggerProvider())

            When("A binary tournament selection operator is created with an invalid tournament size") {
                try {
                    binaryTournamentSelection = BinaryTournamentSelection(
                        mockEnvironment,
                        tournamentSize = -1
                    )
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The binary tournament selection operator is not created") {
                assert(binaryTournamentSelection == null) { "Binary tournament selection operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }
        }

        Scenario("Binary tournament selection fails when sample size is smaller than population") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockPopulation = mutableListOf<Program<Double, Outputs.Single<Double>>>(
                mock(),
                mock(),
                mock()
            )
            var binaryTournamentSelection: BinaryTournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var selected: List<Program<Double, Outputs.Single<Double>>>? = null
            var exception: Exception? = null

            whenever(mockEnvironment.loggerProvider).thenReturn(LoggerProvider())

            When("A binary tournament selection operator is created with a tournament size of 2") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)

                binaryTournamentSelection = BinaryTournamentSelection(
                    mockEnvironment,
                    tournamentSize = 2
                )
            }

            And("A tournament is performed on a population with 4 individuals") {
                try {
                    selected = binaryTournamentSelection?.select(mockPopulation)
                } catch (ex: Exception) {
                    exception  = ex
                }
            }

            Then("Tournament selection fails due to the sample size being smaller than the population") {
                assert(selected == null) { "Selected was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }
        }

        Scenario("Binary tournament selection returns two individuals from the population") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockPopulation = mutableListOf<Program<Double, Outputs.Single<Double>>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            var binaryTournamentSelection: BinaryTournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var selected: List<Program<Double, Outputs.Single<Double>>>? = null

            whenever(mockEnvironment.loggerProvider).thenReturn(LoggerProvider())

            When("A binary tournament selection operator is created with a tournament size of 2") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                mockPopulation.forEach { individual ->
                    whenever(individual.copy()).thenReturn(individual)
                }

                binaryTournamentSelection = BinaryTournamentSelection(
                    mockEnvironment,
                    tournamentSize = 2
                )
            }

            And("A tournament is performed on a population with 6 individuals") {
                selected = binaryTournamentSelection?.select(mockPopulation)
            }

            Then("Two individuals are selected from the population") {
                assert(selected != null) { "Selected was null" }
                assert(selected?.size == 2) { "Unexpected number of selected individuals" }
            }

        }

        Scenario("Binary tournament selection removes the two winners from the population") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockPopulation = mutableListOf<Program<Double, Outputs.Single<Double>>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            var binaryTournamentSelection: BinaryTournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var selected: List<Program<Double, Outputs.Single<Double>>>? = null
            val originalPopulationSize: Int = mockPopulation.size

            whenever(mockEnvironment.loggerProvider).thenReturn(LoggerProvider())

            When("A binary tournament selection operator is created with a tournament size of 2") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                mockPopulation.forEach { individual ->
                    whenever(individual.copy()).thenReturn(individual)
                }

                binaryTournamentSelection = BinaryTournamentSelection(
                    mockEnvironment,
                    tournamentSize = 2
                )
            }

            And("A tournament is performed on a population with 6 individuals") {
                selected = binaryTournamentSelection?.select(mockPopulation)
            }

            Then("Two individuals are selected and removed from the population") {
                assert(selected != null) { "Selected was null" }
                assert(selected?.size == 2) { "Unexpected number of selected individuals" }
                assert(mockPopulation.size == originalPopulationSize - selected!!.size) { "Winners were not removed from the population" }

                selected!!.forEach { individual -> assert(individual !in mockPopulation) { "Winner was found in the population but should have been removed" } }
            }

        }
    }

    Feature("Tournament selection operator") {
        Scenario("Tournament selection operator with an invalid tournament size") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            var tournamentSelection: TournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var exception: Exception? = null

            whenever(mockEnvironment.loggerProvider).thenReturn(LoggerProvider())

            When("A tournament selection operator is created with an invalid tournament size") {
                try {
                    tournamentSelection = TournamentSelection(
                        mockEnvironment,
                        tournamentSize = -1,
                        numberOfOffspring = 0
                    )
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The tournament selection operator is not created") {
                assert(tournamentSelection == null) { "Tournament selection operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }
        }

        Scenario("Tournament selection operator with number of offspring greater than or equal to population size") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                populationSize = 5
            }
            var tournamentSelection: TournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var exception: Exception? = null

            When("A tournament selection operator is created with number of offspring greater than the population size") {
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)

                try {
                    tournamentSelection = TournamentSelection(
                        mockEnvironment,
                        tournamentSize = 2,
                        numberOfOffspring = 6
                    )
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The tournament selection operator is not created") {
                assert(tournamentSelection == null) { "Tournament selection operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }

            When("A tournament selection operator is created with number of offspring equal to the population size") {
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)

                exception = null

                try {
                    tournamentSelection = TournamentSelection(
                        mockEnvironment,
                        tournamentSize = 2,
                        numberOfOffspring = 5
                    )
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The tournament selection operator is not created") {
                assert(tournamentSelection == null) { "Tournament selection operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }
        }

        Scenario("Tournament selection returns four individuals from the population when number of offspring is 2 (even population size)") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                populationSize = 6
            }
            val mockPopulation = mutableListOf<Program<Double, Outputs.Single<Double>>>(
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                    mock()
            )
            var tournamentSelection: TournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var selected: List<Program<Double, Outputs.Single<Double>>>? = null

            When("A tournament selection operator is created with a tournament size of 2 and number of offspring is 2") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)
                mockPopulation.forEach { individual ->
                    whenever(individual.copy()).thenReturn(individual)
                }

                tournamentSelection = TournamentSelection(
                    mockEnvironment,
                    tournamentSize = 2,
                    numberOfOffspring = 2
                )
            }

            And("A tournament is performed on a population with 6 individuals") {
                selected = tournamentSelection?.select(mockPopulation)
            }

            Then("Four individuals are selected from the population") {
                assert(selected != null) { "Selected was null" }
                assert(selected?.size == 4) { "Unexpected number of selected individuals" }
            }

        }

        Scenario("Tournament selection returns four individuals from the population when number of offspring is 2 (odd population size)") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                populationSize = 5
            }
            val mockPopulation = mutableListOf<Program<Double, Outputs.Single<Double>>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            var tournamentSelection: TournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var selected: List<Program<Double, Outputs.Single<Double>>>? = null

            When("A tournament selection operator is created with a tournament size of 2 and number of offspring is 2") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)
                mockPopulation.forEach { individual ->
                    whenever(individual.copy()).thenReturn(individual)
                }

                tournamentSelection = TournamentSelection(
                    mockEnvironment,
                    tournamentSize = 2,
                    numberOfOffspring = 2
                )
            }

            And("A tournament is performed on a population with 6 individuals") {
                selected = tournamentSelection?.select(mockPopulation)
            }

            Then("Four individuals are selected from the population") {
                assert(selected != null) { "Selected was null" }
                assert(selected?.size == 4) { "Unexpected number of selected individuals" }
            }

        }

        Scenario("Tournament selection does not remove winners from the population when requested") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                populationSize = 6
            }
            val mockPopulation = mutableListOf<Program<Double, Outputs.Single<Double>>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            var tournamentSelection: TournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var selected: List<Program<Double, Outputs.Single<Double>>>? = null
            val originalPopulationSize = mockPopulation.size

            When("A tournament selection operator is created with a tournament size of 2 and number of offspring is 2") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)
                mockPopulation.forEach { individual ->
                    whenever(individual.copy()).thenReturn(individual)
                }

                tournamentSelection = TournamentSelection(
                    mockEnvironment,
                    tournamentSize = 2,
                    numberOfOffspring = 2,
                    removeWinnersFromPopulation = false
                )
            }

            And("A tournament is performed on a population with 6 individuals") {
                selected = tournamentSelection?.select(mockPopulation)
            }

            Then("Four individuals are selected from the population but not removed from the population") {
                assert(selected != null) { "Selected was null" }
                assert(selected?.size == 4) { "Unexpected number of selected individuals" }
                assert(mockPopulation.size == originalPopulationSize) { "Population size changed but should have remained the same" }

                selected!!.forEach { individual -> assert(individual in mockPopulation) { "Winner was not found in population but should have been" } }
            }

        }

        Scenario("Tournament selection does remove winners from the population when requested") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                populationSize = 6
            }
            val mockPopulation = mutableListOf<Program<Double, Outputs.Single<Double>>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            var tournamentSelection: TournamentSelection<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var selected: List<Program<Double, Outputs.Single<Double>>>? = null
            val originalPopulationSize = mockPopulation.size

            When("A tournament selection operator is created with a tournament size of 2 and number of offspring is 2") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)
                mockPopulation.forEach { individual ->
                    whenever(individual.copy()).thenReturn(individual)
                }

                tournamentSelection = TournamentSelection(
                    mockEnvironment,
                    tournamentSize = 2,
                    numberOfOffspring = 2
                )
            }

            And("A tournament is performed on a population with 6 individuals") {
                selected = tournamentSelection?.select(mockPopulation)
            }

            Then("Four individuals are selected from the population but not removed from the population") {
                assert(selected != null) { "Selected was null" }
                assert(selected?.size == 4) { "Unexpected number of selected individuals" }
                assert(mockPopulation.size == originalPopulationSize - selected!!.size) { "Population size remained the same but should have changed" }

                selected!!.forEach { individual -> assert(individual !in mockPopulation) { "Winner was found in population but should not have been" } }
            }

        }
    }

    Feature("Tournament selection algorithm") {
        Scenario("A tournament cannot be performed on an empty population") {
            var population: MutableList<Program<Double, Outputs.Single<Double>>>? = null
            var selected: Program<Double, Outputs.Single<Double>>? = null
            var exception: Exception? = null

            Given("An empty population") {
                population = mutableListOf()
            }

            When("Tournament selection is performed") {
                try {
                    selected = tournament(
                        population!!,
                        List<Program<Double, Outputs.Single<Double>>>::first,
                        tournamentSize = 2
                    )
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("Tournament selection fails") {
                assert(selected == null) { "Selected was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }
        }

        Scenario("The correct number of tournaments are performed") {
            val population: MutableList<Program<Double, Outputs.Single<Double>>> = mutableListOf(
                mock(),
                mock(),
                mock(),
                mock()
            )
            var selected: Program<Double, Outputs.Single<Double>>? = null
            val selector = mock<IndividualSelector<Double, Outputs.Single<Double>>>()

            When("Tournament selection is performed with a tournament size of 2") {
                whenever(selector.invoke(population)).thenReturn(population.first())

                selected = tournament(
                    population,
                    selector,
                    tournamentSize = 2
                )
            }

            Then("Tournament selection succeeds and 2 tournaments are performed") {
                assert(selected != null) { "Selected was null" }
                verify(selector, times(2)).invoke(population)
            }
        }

        Scenario("An individual with a lower fitness value is chosen in a tournament") {
            val population: MutableList<Program<Double, Outputs.Single<Double>>> = mutableListOf(
                mock(),
                mock(),
                mock(),
                mock()
            )
            var selected: Program<Double, Outputs.Single<Double>>? = null
            var selectorInvocations = 0
            val selector: IndividualSelector<Double, Outputs.Single<Double>> = { individuals ->
                selectorInvocations += 1

                when (selectorInvocations) {
                    1    -> individuals.first()
                    else -> individuals.last()
                }
            }
            val expectedWinner = population.last()

            When("Tournament selection is performed with a tournament size of 2") {
                whenever(population.first().fitness).thenReturn(1.0)
                whenever(population.last().fitness).thenReturn(0.5)

                selected = tournament(
                    population,
                    selector,
                    tournamentSize = 2
                )
            }

            Then("Tournament selection succeeds and the more fit individual is chosen") {
                assert(selected != null) { "Selected was null" }

                assert(selected != population.first()) { "Did not except first individual to win selection" }
                assert(selected == expectedWinner) { "Winning individual was not expected" }
            }
        }

        Scenario("Winning individual remains in the population when requested") {
            val population: MutableList<Program<Double, Outputs.Single<Double>>> = mutableListOf(
                mock(),
                mock(),
                mock(),
                mock()
            )
            var selected: Program<Double, Outputs.Single<Double>>? = null
            val selector = mock<IndividualSelector<Double, Outputs.Single<Double>>>()

            When("Tournament selection is performed with a tournament size of 2") {
                whenever(selector.invoke(population)).thenReturn(population.first())

                selected = tournament(
                    population,
                    selector,
                    tournamentSize = 2,
                    removeWinnerFromPopulation = false
                )
            }

            Then("Tournament selection succeeds and the winner remains in the population") {
                assert(selected != null) { "Selected was null" }
                assert(selected in population) { "Winner was removed from population but should not have been" }
            }
        }

        Scenario("Winning individual is removed from the population when requested") {
            val population: MutableList<Program<Double, Outputs.Single<Double>>> = mutableListOf(
                mock(),
                mock(),
                mock(),
                mock()
            )
            var selected: Program<Double, Outputs.Single<Double>>? = null
            val selector = mock<IndividualSelector<Double, Outputs.Single<Double>>>()

            When("Tournament selection is performed with a tournament size of 2") {
                whenever(selector.invoke(population)).thenReturn(population.first())

                selected = tournament(
                    population,
                    selector,
                    tournamentSize = 2,
                    removeWinnerFromPopulation = true
                )
            }

            Then("Tournament selection succeeds and the winner is removed from the population") {
                assert(selected != null) { "Selected was null" }
                assert(selected !in population) { "Winner was removed from population but should not have been" }
            }
        }
    }
})