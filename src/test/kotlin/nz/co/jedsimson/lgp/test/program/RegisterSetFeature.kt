package nz.co.jedsimson.lgp.test.program

import nz.co.jedsimson.lgp.core.environment.DefaultValueProviders
import nz.co.jedsimson.lgp.core.environment.dataset.Feature
import nz.co.jedsimson.lgp.core.environment.dataset.Sample
import nz.co.jedsimson.lgp.core.program.registers.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

object RegisterSetFeatureFactoryFeature : Spek({

    Feature("Register set operations") {
        Scenario("Building a valid register set") {
            lateinit var registerSet: RegisterSet<Double>

            Given("A register set with with 2 input registers, 2 calculation registers, and 2 constant registers and a default value of 1.0") {
                registerSet = RegisterSet(
                    inputRegisters = 2,
                    calculationRegisters = 2,
                    constants = listOf(1.0, 2.0),
                    defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)
                )
            }

            Then("There are 6 registers in total, with the correct number of each type") {
                assert(registerSet.count == 6) { "Unexpected number of registers (${registerSet.count})" }
                assert(registerSet.inputRegisters.count() == 2) { "Unexpected number of input registers"}
                assert(registerSet.calculationRegisters.count() == 2) { "Unexpected number of calculation registers" }
                assert(registerSet.constantRegisters.count() == 2) { "Unexpected number of constant registers" }
            }

            Then("Each register has the correct type") {
                registerSet.validateRegistersInRangeMatchType(registerSet.inputRegisters, RegisterType.Input)
                registerSet.validateRegistersInRangeMatchType(registerSet.calculationRegisters, RegisterType.Calculation)
                registerSet.validateRegistersInRangeMatchType(registerSet.constantRegisters, RegisterType.Constant)
            }

            Then("Input and calculation registers are initialised with the correct default values") {
                val inputRegisters = registerSet.getAllRegistersOfType(RegisterType.Input)
                val calculationRegisters = registerSet.getAllRegistersOfType(RegisterType.Calculation)

                assert(inputRegisters.all { r -> r.value == 1.0 }) { "Input registers not initialised with expected default values" }
                assert(calculationRegisters.all { r -> r.value == 1.0 }) { "Calculation registers not initialised with expected default values" }
            }

            Then("Constant registers are initialised with the specified values") {
                val constantRegisters = registerSet.getAllRegistersOfType(RegisterType.Constant)

                assert(constantRegisters[0].value == 1.0) { "First constant register did not have the expected value" }
                assert(constantRegisters[1].value == 2.0) { "Second constant register did not have the expected value" }
            }
        }

        Scenario("Building an invalid register set") {
            var registerSet: RegisterSet<Double>? = null
            var exception: Exception? = null

            Given("A register set with with -2 input registers") {
                try {
                    registerSet = RegisterSet(
                            inputRegisters = -2,
                            calculationRegisters = 0,
                            constants = listOf(),
                            defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)
                    )
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The register set is not created") {
                assert(registerSet == null) { "Register set was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is RegisterSetInitialisationException) { "Exception is not the correct type" }
            }

            Given("A register set with with -2 calculation registers") {
                try {
                    registerSet = RegisterSet(
                            inputRegisters = 0,
                            calculationRegisters = -2,
                            constants = listOf(),
                            defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)
                    )
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The register set is not created") {
                assert(registerSet == null) { "Register set was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is RegisterSetInitialisationException) { "Exception is not the correct type" }
            }
        }

        Scenario("Writing to registers") {
            var registerSet: RegisterSet<Double>? = null
            var oldValue: Double? = null
            var exception: Exception? = null
            val validFeatureList = listOf(
                Feature("f1", 4.0),
                Feature("f2", 5.0)
            )
            val invalidFeatureList = listOf(
                    Feature("f1", 4.0),
                    Feature("f2", 5.0),
                    Feature("f3", 6.0)
            )
            var oldRegisters: List<Register<Double>>? = null

            Given("A register set with with 2 input registers, 2 calculation registers, and 2 constant registers and a default value of 1.0") {
                registerSet = RegisterSet(
                    inputRegisters = 2,
                    calculationRegisters = 2,
                    constants = listOf(1.0, 2.0),
                    defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)
                )
            }

            When("The register at index zero is set to 2.0") {
                oldValue = registerSet!![0]
                registerSet!![0] = 2.0
            }

            Then("The register at index zero is updated to 2.0") {
                assert(oldValue == 1.0) { "Old value was not expected" }
                assert(registerSet!![0] == 2.0) { "New value was not expected" }
            }

            When("The register at index two is set to 2.0") {
                oldValue = registerSet!![2]
                registerSet!![2] = 2.0
            }

            Then("The register at index two is updated to 2.0") {
                assert(oldValue == 1.0) { "Old value was not expected" }
                assert(registerSet!![2] == 2.0) { "New value was not expected" }
            }

            When("The register at index four is set to 2.0") {
                try {
                    oldValue = registerSet!![4]
                    registerSet!![4] = 2.0
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The register at index four is not updated to 2.0 and a register access exception is given") {
                assert(oldValue == 1.0) { "Old value was not expected" }
                assert(registerSet!![4] == 1.0) { "New value was not expected" }
                assert(exception != null) { "Exception was null" }
                assert(exception is RegisterAccessException) { "Exception was not the correct type" }
            }

            When("The register at index four is overwritten with the value 2.0") {
                oldValue = registerSet!![4]
                registerSet!!.overwrite(4, 2.0)
            }

            Then("The register at index four is updated to 2.0") {
                assert(oldValue == 1.0) { "Old value was not expected" }
                assert(registerSet!![4] == 2.0) { "New value was not expected" }
            }

            When("An instance with 2 features is written to the register set") {
                registerSet!!.writeInstance(Sample(validFeatureList))
            }

            Then("The instance is correctly written to the input registers") {
                val inputRegisters = registerSet!!.getAllRegistersOfType(RegisterType.Input)

                assert(inputRegisters[0].value == validFeatureList[0].value)
                assert(inputRegisters[1].value == validFeatureList[1].value)
            }

            When("An instance with 3 features is written to the register set") {
                oldRegisters = registerSet!!.getAllRegistersOfType(RegisterType.Input)

                try {
                    registerSet!!.writeInstance(Sample(invalidFeatureList))
                 }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The instance is not written to the input registers and an register write exception is given") {
                val inputRegisters = registerSet!!.getAllRegistersOfType(RegisterType.Input)

                for ((actual, expected) in inputRegisters.zip(oldRegisters!!)) {
                    assert(actual.value == expected.value) { "Current register value does not match old register value" }
                }

                assert(exception != null) { "Exception was null" }
                assert(exception is RegisterWriteRangeException) { "Exception was not the correct type" }
            }
        }
    }
})

// A few helpers to ease testing
fun <T> RegisterSet<T>.validateRegistersInRangeMatchType(range: IntRange, type: RegisterType) {
    for (r in range) {
        assert(this.registerType(r) == type) { "Register did not match expected type (expected = $type, actual = ${this.registerType(r)})" }
    }
}

fun <T> RegisterSet<T>.getAllRegistersOfType(type: RegisterType): List<Register<T>> {
    return (0 until this.count).map { r ->
        Pair(this.register(r), this.registerType(r))
    }.filter { (_, t) ->
        t == type
    }.map { (r, _) -> r }
}