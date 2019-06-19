package nz.co.jedsimson.lgp.test.program

import nz.co.jedsimson.lgp.core.program.instructions.ArityException
import nz.co.jedsimson.lgp.core.program.instructions.BinaryOperation
import nz.co.jedsimson.lgp.core.program.instructions.UnaryOperation
import nz.co.jedsimson.lgp.core.program.registers.Argument
import nz.co.jedsimson.lgp.core.program.registers.Arguments
import nz.co.jedsimson.lgp.test.mocks.Identity
import nz.co.jedsimson.lgp.test.mocks.Zero
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object OperationFeature : Spek({

    Feature("Operations") {
        Scenario("Unary operations can be executed with a valid argument set") {
            val rawArguments = listOf(1.0)
            var operation: UnaryOperation<Double>? = null
            var result: Double? = null

            Given("A unary operation") {
                operation = Identity()
            }

            When("The operation is executed with a single argument") {
                val arguments = Arguments(rawArguments.map { a -> Argument(a) })
                result = operation!!.execute(arguments)
            }

            Then("The operation execution is successful") {
                assert(result != null) { "Result was null" }
                assert(result == 1.0) { "Result was not expected" }
            }
        }

        Scenario("Unary operation throws when executed with too few arguments") {
            val rawArguments = listOf<Double>()
            var operation: UnaryOperation<Double>? = null
            var result: Double? = null
            var exception: Exception? = null

            Given("A unary operation") {
                operation = Identity()
            }

            When("The operation is executed with zero arguments") {
                val arguments = Arguments(rawArguments.map { a -> Argument(a) })

                try {
                    result = operation!!.execute(arguments)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The operation execution is not successful") {
                assert(result == null) { "Result was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is ArityException) { "Exception was not the correct type" }
            }
        }

        Scenario("Unary operation throws when executed with too many arguments") {
            val rawArguments = listOf(1.0, 2.0)
            var operation: UnaryOperation<Double>? = null
            var result: Double? = null
            var exception: Exception? = null

            Given("A unary operation") {
                operation = Identity()
            }

            When("The operation is executed with two arguments") {
                val arguments = Arguments(rawArguments.map { a -> Argument(a) })

                try {
                    result = operation!!.execute(arguments)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The operation execution is not successful") {
                assert(result == null) { "Result was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is ArityException) { "Exception was not the correct type" }
            }
        }

        Scenario("Binary operations can be executed with a valid argument set") {
            val rawArguments = listOf(1.0, 2.0)
            var operation: BinaryOperation<Double>? = null
            var result: Double? = null

            Given("A binary operation") {
                operation = Zero()
            }

            When("The operation is executed with two arguments") {
                val arguments = Arguments(rawArguments.map { a -> Argument(a) })
                result = operation!!.execute(arguments)
            }

            Then("The operation execution is successful") {
                assert(result != null) { "Result was null" }
                assert(result == 0.0) { "Result was not expected" }
            }
        }

        Scenario("Binary operation throws when executed with too few arguments") {
            val rawArguments = listOf(1.0)
            var operation: BinaryOperation<Double>? = null
            var result: Double? = null
            var exception: Exception? = null

            Given("A binary operation") {
                operation = Zero()
            }

            When("The operation is executed with one argument") {
                val arguments = Arguments(rawArguments.map { a -> Argument(a) })

                try {
                    result = operation!!.execute(arguments)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The operation execution is not successful") {
                assert(result == null) { "Result was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is ArityException) { "Exception was not the correct type" }
            }
        }

        Scenario("Binary operation throws when executed with too many arguments") {
            val rawArguments = listOf(1.0, 2.0, 3.0)
            var operation: BinaryOperation<Double>? = null
            var result: Double? = null
            var exception: Exception? = null

            Given("A binary operation") {
                operation = Zero()
            }

            When("The operation is executed with three argument") {
                val arguments = Arguments(rawArguments.map { a -> Argument(a) })

                try {
                    result = operation!!.execute(arguments)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The operation execution is not successful") {
                assert(result == null) { "Result was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is ArityException) { "Exception was not the correct type" }
            }
        }
    }
})