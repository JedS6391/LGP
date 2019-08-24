package nz.co.jedsimson.lgp.test.environment

import nz.co.jedsimson.lgp.core.environment.dataset.SequenceGenerator
import nz.co.jedsimson.lgp.core.environment.dataset.UniformlyDistributedGenerator
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import java.util.*

class ValueGeneratorFeature : Spek({
    Feature("Generating a sequence of values") {
        Scenario("Generating the sequence [0:10:1] excluding the last value") {
            lateinit var sequenceGenerator: SequenceGenerator
            var sequence: Sequence<Double>? = null

            Given("A SequenceGenerator") {
                sequenceGenerator = SequenceGenerator()
            }

            When("A sequence with start=0, end=10, step=1, inclusive=false is generated") {
                sequence = sequenceGenerator.generate(
                    start = 0.0,
                    end = 10.0,
                    step = 1.0,
                    inclusive = false
                )
            }

            Then("The sequence should contain the values 0.0 - 9.0") {
                assert(sequence != null) { "Sequence was null" }
                assert(sequence!!.toList() == listOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)) {
                    "Sequence did not contain the expected values (${sequence!!.toList()})"
                }
            }
        }

        Scenario("Generating the sequence [0:10:1] including the last value") {
            lateinit var sequenceGenerator: SequenceGenerator
            var sequence: Sequence<Double>? = null

            Given("A SequenceGenerator") {
                sequenceGenerator = SequenceGenerator()
            }

            When("A sequence with start=0, end=10, step=1, inclusive=true is generated") {
                sequence = sequenceGenerator.generate(
                    start = 0.0,
                    end = 10.0,
                    step = 1.0,
                    inclusive = true
                )
            }

            Then("The sequence should contain the values 0.0 - 10.0") {
                assert(sequence != null) { "Sequence was null" }
                assert(sequence!!.toList() == listOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)) {
                    "Sequence did not contain the expected values"
                }
            }
        }

        Scenario("Generating the sequence [0:3:0.5] excluding the last value") {
            lateinit var sequenceGenerator: SequenceGenerator
            var sequence: Sequence<Double>? = null

            Given("A SequenceGenerator") {
                sequenceGenerator = SequenceGenerator()
            }

            When("A sequence with start=0, end=3, step=0.5, inclusive=false is generated") {
                sequence = sequenceGenerator.generate(
                    start = 0.0,
                    end = 3.0,
                    step = 0.5,
                    inclusive = false
                )
            }

            Then("The sequence should contain the values 0.0, 0.5, 1.0, 1.5, 2.0, 2.5") {
                assert(sequence != null) { "Sequence was null" }
                assert(sequence!!.toList() == listOf(0.0, 0.5, 1.0, 1.5, 2.0, 2.5)) {
                    "Sequence did not contain the expected values"
                }
            }
        }

        Scenario("Generating the sequence [0:3:0.5] including the last value") {
            lateinit var sequenceGenerator: SequenceGenerator
            var sequence: Sequence<Double>? = null

            Given("A SequenceGenerator") {
                sequenceGenerator = SequenceGenerator()
            }

            When("A sequence with start=0, end=3, step=0.5, inclusive=true is generated") {
                sequence = sequenceGenerator.generate(
                    start = 0.0,
                    end = 3.0,
                    step = 0.5,
                    inclusive = true
                )
            }

            Then("The sequence should contain the values 0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0") {
                assert(sequence != null) { "Sequence was null" }
                assert(sequence!!.toList() == listOf(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0)) {
                    "Sequence did not contain the expected values"
                }
            }
        }
    }

    Feature("Generating a number of uniformly distributed values") {
        Scenario("Generating 5 values bounded to the range 0.0 - 1.0") {
            val expectedRandomGenerator = java.util.Random(0)
            lateinit var generator: UniformlyDistributedGenerator
            var sequence: Sequence<Double>? = null

            Given("A UniformlyDistributedGenerator with a random seed of 0") {
                generator = UniformlyDistributedGenerator(Random(0))
            }

            When("A sequence of 5 values in the range 0.0 - 1.0 is generated") {
                sequence = generator.generate(
                    n = 5,
                    start = 0.0,
                    end = 1.0
                )
            }

            Then("The sequence should contain 5 values in the range 0.0 - 1.0") {
                assert(sequence != null) { "Sequence was null" }

                val list = sequence!!.toList()
                val expectedList = (0 until 5).map { expectedRandomGenerator.nextDouble() }

                assert(list.size == 5) { "Sequence size did not match expected (${list.size})" }
                assert(list.all { v -> v in 0.0..1.0 }) { "A value in the sequence was not within the expected bounds" }
                assert(list == expectedList) { "Sequence did not contain the expected values" }
            }
        }
    }
})