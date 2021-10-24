package nz.co.jedsimson.lgp.test.evolution

import com.nhaarman.mockitokotlin2.*
import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover.CrossoverPoint
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover.CrossoverPointProvider
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover.LinearCrossover
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.linearCrossover.SegmentProvider
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import kotlin.random.Random

object RecombinationOperatorFeature : Spek({
    Feature("Linear crossover operator") {
        Scenario("Linear crossover operator with an invalid maximum segment length") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                minimumProgramLength = 5
                maximumProgramLength = 10
            }
            var linearCrossover: LinearCrossover<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var exception: Exception? = null

            When("A linear crossover operator is created with an invalid maximum segment length") {
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)

                try {
                    linearCrossover = LinearCrossover(
                            mockEnvironment,
                            maximumSegmentLength = 0,
                            maximumCrossoverDistance = 1,
                            maximumSegmentLengthDifference = 1
                    )
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The linear crossover operator is not created") {
                assert(linearCrossover == null) { "Linear crossover operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }
        }

        Scenario("Linear crossover operator fails if mother program length is outside of valid range") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                minimumProgramLength = 5
                maximumProgramLength = 10
            }
            val mockMother = mock<Program<Double, Outputs.Single<Double>>>()
            val mockFather = mock<Program<Double, Outputs.Single<Double>>>()
            var linearCrossover: LinearCrossover<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var exception: Exception? = null

            When("A linear crossover operator is created") {
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                whenever(mockMother.instructions).thenReturn(mutableListOf())

                linearCrossover = LinearCrossover(
                        mockEnvironment,
                        maximumSegmentLength = 1,
                        maximumCrossoverDistance = 1,
                        maximumSegmentLengthDifference = 1
                )
            }

            And("Linear crossover is performed") {
                try {
                    linearCrossover?.combine(mockMother, mockFather)
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("Linear crossover fails") {
                assert(linearCrossover != null) { "Linear crossover operator was null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }
        }

        Scenario("Linear crossover operator fails if father program length is outside of valid range") {
            val mockEnvironment = mock<EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                minimumProgramLength = 5
                maximumProgramLength = 10
            }
            val mockMother = mock<Program<Double, Outputs.Single<Double>>>()
            val mockFather = mock<Program<Double, Outputs.Single<Double>>>()
            var linearCrossover: LinearCrossover<Double, Outputs.Single<Double>, Targets.Single<Double>>? = null
            var exception: Exception? = null

            When("A linear crossover operator is created") {
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                whenever(mockMother.instructions).thenReturn(mutableListOf(
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                    mock()
                ))
                whenever(mockFather.instructions).thenReturn(mutableListOf())

                linearCrossover = LinearCrossover(
                        mockEnvironment,
                        maximumSegmentLength = 1,
                        maximumCrossoverDistance = 1,
                        maximumSegmentLengthDifference = 1
                )
            }

            And("Linear crossover is performed") {
                try {
                    linearCrossover?.combine(mockMother, mockFather)
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("Linear crossover fails") {
                assert(linearCrossover != null) { "Linear crossover operator was null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }
        }

        // TODO: High-level linear crossover operator calls different components and produces a result
        //Scenario("") {}
    }

    Feature("Crossover point provider") {

        listOf(
            CrossoverPointProviderTestCase(
                firstCrossoverPoint = 4,
                secondCrossoverPoint = 2,
                maximumCrossoverDistance = 2,
                firstIndividualSize = 10,
                shouldBeConsideredValid = true
            ),
            CrossoverPointProviderTestCase(
                firstCrossoverPoint = 2,
                secondCrossoverPoint = 4,
                maximumCrossoverDistance = 2,
                firstIndividualSize = 10,
                shouldBeConsideredValid = true
            ),
            CrossoverPointProviderTestCase(
                firstCrossoverPoint = 3,
                secondCrossoverPoint = 4,
                maximumCrossoverDistance = 2,
                firstIndividualSize = 10,
                shouldBeConsideredValid = true
            ),
            CrossoverPointProviderTestCase(
                firstCrossoverPoint = 2,
                secondCrossoverPoint = 5,
                maximumCrossoverDistance = 2,
                firstIndividualSize = 10,
                shouldBeConsideredValid = false
            ),
            CrossoverPointProviderTestCase(
                firstCrossoverPoint = 2,
                secondCrossoverPoint = 5,
                maximumCrossoverDistance = 0,
                firstIndividualSize = 10,
                shouldBeConsideredValid = false
            ),
            CrossoverPointProviderTestCase(
                firstCrossoverPoint = 2,
                secondCrossoverPoint = 5,
                maximumCrossoverDistance = 5,
                firstIndividualSize = 3,
                shouldBeConsideredValid = false
            )
        ).forEachIndexed { idx, testCase ->
            Scenario("Crossover points must satisfy the maximum crossover distance constraint to be considered valid (scenario $idx)") {
                var firstCrossoverPoint: CrossoverPoint? = null
                var secondCrossoverPoint: CrossoverPoint? = null
                var maximumCrossoverDistance: Int? = null

                Given("A first crossover point of ${testCase.firstCrossoverPoint}") {
                    firstCrossoverPoint = testCase.firstCrossoverPoint
                }

                And("A second crossover point of ${testCase.secondCrossoverPoint}") {
                    secondCrossoverPoint = testCase.secondCrossoverPoint
                }

                And("A maximum crossover distance of ${testCase.maximumCrossoverDistance}") {
                    maximumCrossoverDistance = testCase.maximumCrossoverDistance
                }

                Then("The crossover points are ${if (testCase.shouldBeConsideredValid) "valid" else "invalid"}") {
                    val crossoverPointProvider = CrossoverPointProvider<Double>(maximumCrossoverDistance!!, Random.Default)

                    val shouldBeConsideredValid = testCase.shouldBeConsideredValid

                    assert(crossoverPointProvider.crossoverPointsAreValid(
                        firstCrossoverPoint!!,
                        secondCrossoverPoint!!,
                        testCase.firstIndividualSize
                    ) == shouldBeConsideredValid) {
                        "Crossover points (expected valid = $shouldBeConsideredValid)"
                    }
                }
            }
        }

        Scenario("No search is performed if the initial crossover points are valid") {
            val firstIndividual = mutableListOf<Instruction<Double>>(
                mock(),
                mock(),
                mock(),
                mock()
            )
            val secondIndividual = mutableListOf<Instruction<Double>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            val mockRandom = mock<Random>()
            var crossoverPoints: Pair<CrossoverPoint, CrossoverPoint>? = null
            val crossoverPointProvider = CrossoverPointProvider<Double>(10, mockRandom)
            val crossoverPointProviderSpy = spy(crossoverPointProvider)

            When("The initial crossover points are valid") {
                whenever(mockRandom.nextInt(firstIndividual.size)).thenReturn(2)
                whenever(mockRandom.nextInt(secondIndividual.size)).thenReturn(4)

                crossoverPoints = crossoverPointProviderSpy.determineCrossoverPoints(firstIndividual, secondIndividual)
            }

            Then("No search is performed") {
                verify(mockRandom, times(1)).nextInt(firstIndividual.size)
                verify(mockRandom, times(1)).nextInt(secondIndividual.size)
                verify(crossoverPointProviderSpy, times(1)).crossoverPointsAreValid(any(), any(), any())

                assert(crossoverPoints != null) { "Crossover points were null" }

                val (firstCrossoverPoint, secondCrossoverPoint) = crossoverPoints!!

                assert(firstCrossoverPoint == 2) { "First crossover point not expected" }
                assert(secondCrossoverPoint == 4) { "Second crossover point not expected" }
            }
        }

        Scenario("A search is performed until the crossover points are valid") {
            val firstIndividual = mutableListOf<Instruction<Double>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            val secondIndividual = mutableListOf<Instruction<Double>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            val mockRandom = mock<Random>()
            var crossoverPoints: Pair<CrossoverPoint, CrossoverPoint>? = null
            val crossoverPointProvider = CrossoverPointProvider<Double>(4, mockRandom)
            val crossoverPointProviderSpy = spy(crossoverPointProvider)

            When("The initial crossover points are not valid") {
                whenever(mockRandom.nextInt(any())).thenReturn(
                    // The first two pairs don't satisfy the crossover point validation,
                    // so the last pair should be chosen.
                    // firstCrossoverPoint, secondCrossoverPoint
                    6, 1,
                    3, 8,
                    2, 1
                )


                crossoverPoints = crossoverPointProviderSpy.determineCrossoverPoints(firstIndividual, secondIndividual)
            }

            Then("A search is performed until the crossover points") {
                verify(mockRandom, times(6)).nextInt(any())
                verify(crossoverPointProviderSpy, times(3)).crossoverPointsAreValid(any(), any(), any())

                assert(crossoverPoints != null) { "Crossover points were null" }

                val (firstCrossoverPoint, secondCrossoverPoint) = crossoverPoints!!

                assert(firstCrossoverPoint == 2) { "First crossover point not expected" }
                assert(secondCrossoverPoint == 1) { "Second crossover point not expected" }
            }
        }

        Scenario("The search terminates if no valid crossover points can be found after the max number of iterations") {
            val firstIndividual = mutableListOf<Instruction<Double>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            val secondIndividual = mutableListOf<Instruction<Double>>(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock()
            )
            val mockRandom = mock<Random>()
            var crossoverPoints: Pair<CrossoverPoint, CrossoverPoint>? = null
            val crossoverPointProvider = CrossoverPointProvider<Double>(4, mockRandom)
            val crossoverPointProviderSpy = spy(crossoverPointProvider)

            When("The initial crossover points are not valid") {
                whenever(mockRandom.nextInt(firstIndividual.size)).thenReturn(6)
                whenever(mockRandom.nextInt(secondIndividual.size)).thenReturn(1)

                crossoverPoints = crossoverPointProviderSpy.determineCrossoverPoints(firstIndividual, secondIndividual)
            }

            Then("A search is performed until the crossover points, but gives up after max number of iterations are exceeded") {
                // We except 21 calls - 1 for the initial crossover points and then 20 iterations in the search.
                verify(mockRandom, times(21)).nextInt(firstIndividual.size)
                verify(mockRandom, times(21)).nextInt(secondIndividual.size)

                // Only 20 calls to the validation routine should be made (i.e. one for each iteration)
                verify(crossoverPointProviderSpy, times(20)).crossoverPointsAreValid(any(), any(), any())

                assert(crossoverPoints == null) { "Crossover points were not null" }
            }
        }
    }


    Feature("Segment provider") {

        listOf(
            SegmentProviderTestCase(
                firstSegmentSize = 3,
                secondSegmentSize = 5,
                maximumSegmentLengthDifference = 2,
                shouldBeConsideredValid = true
            ),
            SegmentProviderTestCase(
                firstSegmentSize = 5,
                secondSegmentSize = 3,
                maximumSegmentLengthDifference = 2,
                shouldBeConsideredValid = false
            ),
            SegmentProviderTestCase(
                firstSegmentSize = 3,
                secondSegmentSize = 5,
                maximumSegmentLengthDifference = 1,
                shouldBeConsideredValid = false
            )
        ).forEachIndexed { idx, testCase ->
            Scenario("Segment sizes must satisfy the maximum segment length difference constraint to be considered valid (scenario $idx)") {
                var firstSegmentSize: Int? = null
                var secondSegmentSize: Int? = null
                var maximumSegmentLengthDifference: Int? = null

                Given("A first segment size of ${testCase.firstSegmentSize}") {
                    firstSegmentSize = testCase.firstSegmentSize
                }

                Given("A second segment size of ${testCase.secondSegmentSize}") {
                    secondSegmentSize = testCase.secondSegmentSize
                }

                Given("A maximum segment length difference of ${testCase.maximumSegmentLengthDifference}") {
                    maximumSegmentLengthDifference = testCase.maximumSegmentLengthDifference
                }

                Then("The segment size are ${if (testCase.shouldBeConsideredValid) "valid" else "invalid"}") {
                    val segmentProvider = SegmentProvider<Double>(10, maximumSegmentLengthDifference!!, 5, 10, Random.Default)

                    val shouldBeConsideredValid = testCase.shouldBeConsideredValid

                    assert(segmentProvider.segmentSizesAreValid(
                        firstSegmentSize!!,
                        secondSegmentSize!!
                    ) == shouldBeConsideredValid) {
                        "Crossover points (expected valid = $shouldBeConsideredValid)"
                    }
                }
            }
        }


    }
})

private data class CrossoverPointProviderTestCase(
    val firstCrossoverPoint: CrossoverPoint,
    val secondCrossoverPoint: CrossoverPoint,
    val maximumCrossoverDistance: Int,
    val firstIndividualSize: Int,
    val shouldBeConsideredValid: Boolean
)

private data class SegmentProviderTestCase(
    val firstSegmentSize: Int,
    val secondSegmentSize: Int,
    val maximumSegmentLengthDifference: Int,
    val shouldBeConsideredValid: Boolean
)