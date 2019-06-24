package nz.co.jedsimson.lgp.test.environment

import nz.co.jedsimson.lgp.core.environment.ComponentLoadException
import nz.co.jedsimson.lgp.core.environment.operations.DefaultOperationLoader
import nz.co.jedsimson.lgp.core.environment.operations.InvalidOperationSpecificationException
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.instructions.BinaryOperation
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.program.instructions.RegisterIndex
import nz.co.jedsimson.lgp.core.program.instructions.UnaryOperation
import nz.co.jedsimson.lgp.core.program.registers.Argument
import nz.co.jedsimson.lgp.core.program.registers.Arguments
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object OperationLoaderFeature : Spek({
    Feature("Loading Operation instances from a list") {
        Scenario("Load double-typed operations from valid list") {
            lateinit var operationsRaw: List<String>
            var operationsLoaded: List<Operation<Double>>? = null
            lateinit var operationLoader: DefaultOperationLoader<Double>

            Given("The list of operations [\"nz.co.jedsimson.lgp.test.environment.Addition\", \"nz.co.jedsimson.lgp.test.environment.Subtraction\"]") {
                operationsRaw = listOf(
                    "nz.co.jedsimson.lgp.test.environment.Addition",
                    "nz.co.jedsimson.lgp.test.environment.Subtraction"
                )
            }

            And("A DefaultOperationLoader for the operations list") {
                operationLoader = DefaultOperationLoader(operationsRaw)
            }

            When("The operations are loaded") {
                operationsLoaded = operationLoader.load()
            }

            Then("The operations are loaded successfully") {
                assert(
                    operationsLoaded != null &&
                    operationsLoaded!!.size == operationsRaw.size
                ) {
                    "Loaded operations is null or the number of operations loaded is not correct"
                }

                val firstOperation = operationsLoaded!!.first()
                val lastOperation = operationsLoaded!!.last()

                assert(firstOperation is Addition && lastOperation is Subtraction) {
                    "One of the operations loaded (or both) is not the correct type"
                }
            }
        }

        Scenario("Load double-typed operations from invalid list") {
            lateinit var operationsRaw: List<String>
            var operationsLoaded: List<Operation<Double>>? = null
            lateinit var operationLoader: DefaultOperationLoader<Double>
            var exception: Exception? = null

            Given("The list of operations [\"nz.co.jedsimson.lgp.test.environment.NotValid1\", \"nz.co.jedsimson.lgp.test.environment.NotValid2\"]") {
                operationsRaw = listOf("nz.co.jedsimson.lgp.test.environment.NotValid1", "nz.co.jedsimson.lgp.test.environment.NotValid2")
            }

            And("A DefaultOperationLoader for the operations list") {
                operationLoader = DefaultOperationLoader(operationsRaw)
            }

            When("The operations are loaded") {
                try {
                    operationsLoaded = operationLoader.load()
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("environment successfully") {
                assert(operationsLoaded == null) { "Loaded operations is not null" }
                assert(exception != null) { "Exception is null"}
                assert(exception is ComponentLoadException) { "Exception is not of correct type" }
                assert(exception!!.cause is InvalidOperationSpecificationException) { "Inner exception is not of correct type "}
            }
        }

        Scenario("Load non-module type from list") {
            lateinit var operationsRaw: List<String>
            var operationsLoaded: List<Operation<Double>>? = null
            lateinit var operationLoader: DefaultOperationLoader<Double>
            var exception: Exception? = null

            Given("The list of operations [\"nz.co.jedsimson.lgp.test.environment.NonModuleSubclass\"]") {
                operationsRaw = listOf("nz.co.jedsimson.lgp.test.environment.NonModuleSubclass")
            }

            And("A DefaultOperationLoader for the operations list") {
                operationLoader = DefaultOperationLoader(operationsRaw)
            }

            When("The operations are loaded") {
                try {
                    operationsLoaded = operationLoader.load()
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The operations are not loaded successfully") {
                assert(operationsLoaded == null) { "Loaded operations is not null" }
                assert(exception != null) { "Exception is null"}
                assert(exception is ComponentLoadException) { "Exception is not of correct type" }
                assert(exception!!.cause is InvalidOperationSpecificationException) { "Inner exception is not of correct type" }
                assert(exception!!.cause!!.message.equals("${NonModuleSubclass::class.qualifiedName} is not a valid Module.")) {
                    "Exception message not expected (${exception!!.cause!!.message})"
                }
            }
        }

        Scenario("Load non-operation type from list") {
            lateinit var operationsRaw: List<String>
            var operationsLoaded: List<Operation<Double>>? = null
            lateinit var operationLoader: DefaultOperationLoader<Double>
            var exception: Exception? = null

            Given("The list of operations [\"nz.co.jedsimson.lgp.test.environment.NonOperationSubclass\"]") {
                operationsRaw = listOf("nz.co.jedsimson.lgp.test.environment.NonOperationSubclass")
            }

            And("A DefaultOperationLoader for the operations list") {
                operationLoader = DefaultOperationLoader(operationsRaw)
            }

            When("The operations are loaded") {
                try {
                    operationsLoaded = operationLoader.load()
                } catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The operations are not loaded successfully") {
                assert(operationsLoaded == null) { "Loaded operations is not null" }
                assert(exception != null) { "Exception is null"}
                assert(exception is ComponentLoadException) { "Exception is not of correct type" }
                assert(exception!!.cause is InvalidOperationSpecificationException) { "Inner exception is not of correct type" }
                // TODO: This isn't the most reliable as it will break if the message changes...
                assert(exception!!.cause!!.message.equals("${NonOperationSubclass::class.qualifiedName} is not a valid Operation.")) {
                    "Exception message not expected (${exception!!.cause!!.message})"
                }
            }
        }

        Scenario("Load double-typed operations from valid list using builder") {
            lateinit var operationsRaw: List<String>
            var operationsLoaded: List<Operation<Double>>? = null
            lateinit var operationLoader: DefaultOperationLoader<Double>
            lateinit var operationLoaderBuilder: DefaultOperationLoader.Builder<Double>

            Given("The list of operations [\"nz.co.jedsimson.lgp.test.environment.Addition\", \"nz.co.jedsimson.lgp.test.environment.Subtraction\"]") {
                operationsRaw = listOf("nz.co.jedsimson.lgp.test.environment.Addition", "nz.co.jedsimson.lgp.test.environment.Subtraction")
            }

            And("A DefaultOperationLoader.Builder for the operations list") {
                operationLoaderBuilder = DefaultOperationLoader
                    .Builder<Double>()
                    .operations(operationsRaw)
            }

            When("The DefaultOperationLoader is built") {
                operationLoader = operationLoaderBuilder.build()
            }

            And("The operations are loaded") {
                operationsLoaded = operationLoader.load()
            }

            Then("The operations are loaded successfully") {
                assert(
                    operationsLoaded != null &&
                            operationsLoaded!!.size == operationsRaw.size
                ) { "Loaded operations is null or the number of operations loaded is not correct" }

                val firstOperation = operationsLoaded!!.first()
                val lastOperation = operationsLoaded!!.last()

                assert(firstOperation is Addition && lastOperation is Subtraction) {
                    "One of the operations loaded (or both) is not the correct type"
                }
            }
        }

        Scenario("Load example string-typed operation from valid list") {
            lateinit var operationsRaw: List<String>
            var operationsLoaded: List<Operation<String>>? = null
            lateinit var operationLoader: DefaultOperationLoader<String>

            Given("The list of operations [\"nz.co.jedsimson.lgp.test.environment.StringIdentity\"]") {
                operationsRaw = listOf("nz.co.jedsimson.lgp.test.environment.StringIdentity")
            }

            And("A DefaultOperationLoader for the operations list") {
                operationLoader = DefaultOperationLoader(operationsRaw)
            }

            When("The operations are loaded") {
                operationsLoaded = operationLoader.load()
            }

            Then("The operations are loaded successfully") {
                assert(
              operationsLoaded != null &&
                    operationsLoaded!!.size == operationsRaw.size
                ) {
                    "Loaded operations is null or the number of operations loaded is not correct"
                }

                val operation = operationsLoaded!!.first()

                assert(operation is StringIdentity) {
                    "The operation loaded is not the correct type"
                }

                val arguments = Arguments(listOf(Argument("test")))
                val result = operation.execute(arguments)

                assert(result == "test") {
                    "Operation did not return the expected value (was: $result, expected: test)"
                }
            }
        }
    }
})

/**
 * @suppress
 */
internal class NonModuleSubclass

/**
 * @suppress
 */
internal class NonOperationSubclass : Module {
    override val information: ModuleInformation
        get() = ModuleInformation("A class that is a Module but not an Operation")
}

/**
 * @suppress
 */
internal class StringIdentity : UnaryOperation<String>(Companion::execute) {
    companion object {
        fun execute(args: Arguments<String>): String {
            return args.get(0)
        }
    }

    override val representation = "identity"

    override val information = ModuleInformation(
        description = "An identity function for strings."
    )

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "identity"
    }
}

// Mimics the implementations in LGP-lib
internal class Addition : BinaryOperation<Double>(Companion::add) {

    companion object {
        fun add(args: Arguments<Double>): Double {
            return args.get(0) + args.get(1)
        }
    }

    override val representation = " + "

    override val information = ModuleInformation(
            description = "An operation for performing the addition function on two Double arguments."
    )

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "r[$destination] = r[${ operands[0] }] + r[${ operands[1] }]"
    }
}

internal class Subtraction : BinaryOperation<Double>(
        function = { args: Arguments<Double> ->
            args.get(0) - args.get(1)
        }
) {
    override val representation = " - "

    override val information = ModuleInformation(
            description = "An operation for performing the subtraction function on two Double arguments."
    )

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        return "r[$destination] = r[${ operands[0] }] - r[${ operands[1] }]"
    }
}