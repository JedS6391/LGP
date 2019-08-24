package nz.co.jedsimson.lgp.test.environment

import nz.co.jedsimson.lgp.core.environment.ComponentLoadException
import nz.co.jedsimson.lgp.core.environment.constants.DoubleConstantLoader
import nz.co.jedsimson.lgp.core.environment.constants.GenericConstantLoader
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import java.lang.NumberFormatException

object ConstantLoaderFeature : Spek({

    Feature("Loading constants using a custom parse function") {
        Scenario("Load double constants from valid list (generic constant loader)") {
            lateinit var constantsRaw: List<String>
            var constantsParsed: List<Double>? = null
            lateinit var constantLoader: GenericConstantLoader<Double>

            Given("The list of constants [\"0.0\", \"0.1\", \"1.0\"]") {
                constantsRaw = listOf("0.0", "0.1", "1.0")
            }

            And("A GenericConstantLoader with the parse function String::toDouble") {
                constantLoader = GenericConstantLoader(constantsRaw, String::toDouble)
            }

            When("The constants are loaded") {
                constantsParsed = constantLoader.load()
            }

            Then("The constants are loaded successfully") {
                assert(constantsParsed != null) { "Parsed constant list is null" }
                assert(constantsParsed == listOf(0.0, 0.1, 1.0)) { "Parsed constant list does not match expected" }
            }
        }

        Scenario("Load double constants from valid list (double constant loader") {
            lateinit var constantsRaw: List<String>
            var constantsParsed: List<Double>? = null
            lateinit var constantLoader: DoubleConstantLoader

            Given("The list of constants [\"0.0\", \"0.1\", \"1.0\"]") {
                constantsRaw = listOf("0.0", "0.1", "1.0")
            }

            And("A DoubleConstantLoader") {
                constantLoader = DoubleConstantLoader(constantsRaw)
            }

            When("The constants are loaded") {
                constantsParsed = constantLoader.load()
            }

            Then("The constants are loaded successfully") {
                assert(constantsParsed != null) { "Parsed constant list is null" }
                assert(constantsParsed == listOf(0.0, 0.1, 1.0)) { "Parsed constant list does not match expected" }
            }
        }

        Scenario("Load constants from invalid list") {
            lateinit var constantsRaw: List<String>
            var constantsParsed: List<Double>? = null
            lateinit var constantLoader: GenericConstantLoader<Double>
            var exception: Exception? = null

            Given("The list of constants [\"0.0\", \"0.1\", \"test\"]") {
                constantsRaw = listOf("0.0", "0.1", "test")
            }

            And("A GenericConstantLoader with the parse function String::toDouble") {
                constantLoader = GenericConstantLoader(constantsRaw, String::toDouble)
            }

            When("The constants are loaded") {
                try {
                    constantsParsed = constantLoader.load()
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The constants are not loaded successfully") {
                assert(constantsParsed == null) { "Parsed constants list is not null" }
                assert(exception != null) { "Exception is null" }
                assert(exception is ComponentLoadException) { "Exception is not of correct type" }
                assert(exception!!.cause is NumberFormatException) { "Inner exception is not of correct type "}
            }
        }

        Scenario("Load double constants from valid list using builder") {
            lateinit var constantsRaw: List<String>
            var constantsParsed: List<Double>? = null
            lateinit var constantLoader: GenericConstantLoader<Double>
            lateinit var constantLoaderBuilder: GenericConstantLoader.Builder<Double>

            Given("The list of constants [\"0.0\", \"0.1\", \"1.0\"]") {
                constantsRaw = listOf("0.0", "0.1", "1.0")
            }

            And("A GenericConstantLoader.Builder for the constants with the parse function String::toDouble") {
                constantLoaderBuilder = GenericConstantLoader
                    .Builder<Double>()
                    .constants(constantsRaw)
                    .parseFunction(String::toDouble)
            }

            When("The GenericConstantLoader is built") {
                constantLoader = constantLoaderBuilder.build()
            }

            When("The constants are loaded") {
                constantsParsed = constantLoader.load()
            }

            Then("The constants are loaded successfully") {
                assert(constantsParsed != null)
                assert(constantsParsed == listOf(0.0, 0.1, 1.0))
            }
        }

        Scenario("Load double constants from valid list with custom parse function") {
            lateinit var constantsRaw: List<String>
            var constantsParsed: List<Double>? = null
            lateinit var constantLoader: GenericConstantLoader<Double>

            Given("The list of constants [\"0.0\", \"0.1\", \"1.0\"]") {
                constantsRaw = listOf("0.0", "0.1", "1.0")
            }

            And("A GenericConstantLoader with a custom parse function that adds one to each constant") {
                constantLoader = GenericConstantLoader(constantsRaw) { constant ->
                    constant.toDouble() + 1.0
                }
            }

            When("The constants are loaded") {
                constantsParsed = constantLoader.load()
            }

            Then("The constants are loaded successfully as [\"1.0\", \"1.1\", \"2.0\"]") {
                assert(constantsParsed != null) { "Parsed constant list is null" }
                assert(constantsParsed == listOf(1.0, 1.1, 2.0)) { "Parsed constant list does not match expected" }
            }
        }
    }
})