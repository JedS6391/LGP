package nz.co.jedsimson.lgp.test.environment

import nz.co.jedsimson.lgp.core.environment.DefaultValueProvider
import nz.co.jedsimson.lgp.core.environment.DefaultValueProviders
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object DefaultValueProviderFeature : Spek({
    Feature("Retreiving default register values") {
        Scenario("Constant value provider") {
            lateinit var constantValueProvider: DefaultValueProvider<Double>
            var value: Double? = null

            Given("A constant value provider for the value 1.0") {
                constantValueProvider = DefaultValueProviders.constantValueProvider(1.0)
            }

            When("A value is retrieved") {
                value = constantValueProvider.value
            }

            Then("The value should be 1.0") {
                assert(value == 1.0)
            }
        }

        Scenario("Random double value provider") {
            lateinit var randomValueProvider: DefaultValueProvider<Double>
            var value: Double? = null

            Given("A random double value provider with seed 0") {
                randomValueProvider = DefaultValueProviders.randomDoubleValueProvider(java.util.Random(0))
            }

            When("A value is retrieved") {
                value = randomValueProvider.value
            }

            Then("The value should match the next randomly generated number for seed 0") {
                val expected = java.util.Random(0).nextDouble()

                assert(expected == value) {
                    "Values did not match (expected: $expected, was: $value)"
                }
            }
        }

        Scenario("Lambda value provider") {
            lateinit var lambdaValueProvider: DefaultValueProvider<Double>
            var values: List<Double>? = null
            val valueHelper = object {
                private var nextValue = 0.0

                fun next(): Double {
                    nextValue += 1.0

                    return nextValue
                }
            }


            Given("A lambda value provider which starts at one and increments by one on each call") {
                lambdaValueProvider = DefaultValueProviders.lambdaValueProvider {
                    valueHelper.next()
                }
            }

            When("5 values are retrieved") {
                values = (0 until 5).map {
                    lambdaValueProvider.value
                }
            }

            Then("The values list should be [\"1.0\", \"2.0\", \"3.0\", \"4.0\", \"5.0\"]") {
                assert(values == listOf(1.0, 2.0, 3.0, 4.0, 5.0))
            }
        }
    }
})