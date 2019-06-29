package nz.co.jedsimson.lgp.test.mocks

import nz.co.jedsimson.lgp.core.environment.DefaultValueProviders
import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.*
import nz.co.jedsimson.lgp.core.program.instructions.*
import nz.co.jedsimson.lgp.core.program.registers.Arguments
import nz.co.jedsimson.lgp.core.program.registers.ArrayRegisterSet
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex
import java.util.*

class MockInstruction(
    override var destination: RegisterIndex,
    override var operands: MutableList<RegisterIndex>,
    override var operation: Operation<Double>
) : Instruction<Double>()
{
    override fun execute(registers: RegisterSet<Double>) {
        val arguments = Arguments(this.operands.map { idx ->
            registers.register(idx).toArgument()
        })

        registers[this.destination] = this.operation.execute(arguments)
    }

    override fun copy(): Instruction<Double> {
        TODO("not implemented")
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")

}

class MockInstructionGenerator<TOutput : Output<Double>, TTarget : Target<Double>>(
    environment: EnvironmentFacade<Double, TOutput, TTarget>
) : InstructionGenerator<Double, TOutput, TTarget>(environment)
{
    private val random = Random()

    override fun generateInstruction(): Instruction<Double> {
        val arity = if (random.nextBoolean()) BaseArity.Unary else BaseArity.Binary

        val operation = when (arity) {
            BaseArity.Unary -> Identity()
            BaseArity.Binary -> Zero()
        }

        val operands = when (arity) {
            BaseArity.Unary -> mutableListOf(1)
            BaseArity.Binary -> mutableListOf(0, 1)
        }

        return MockInstruction(destination = 0, operands = operands, operation = operation)
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")

}

class MockSingleOutputProgram(
    override var instructions: MutableList<Instruction<Double>>,
    override val registers: RegisterSet<Double>,
    override val outputRegisterIndices: List<RegisterIndex>
) : Program<Double, Outputs.Single<Double>>()
{
    override fun output(): Outputs.Single<Double> {
        val output = this.registers[outputRegisterIndices.first()]

        return Outputs.Single(output)
    }

    override fun execute() {
        for (instruction in this.instructions) {
            instruction.execute(this.registers)
        }
    }

    override fun copy(): Program<Double, Outputs.Single<Double>> {
        TODO("not implemented")
    }

    override fun findEffectiveProgram() {
        TODO("not implemented")
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")
}

class MockSingleOutputProgramGenerator(
    environment: EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>
) : ProgramGenerator<Double, Outputs.Single<Double>, Targets.Single<Double>>(
    environment,
    MockInstructionGenerator(environment)
)
{
    override fun generateProgram(): Program<Double, Outputs.Single<Double>> {
        val instructions = this.instructionGenerator.next().take(2).toMutableList()
        val registers = ArrayRegisterSet(
                inputRegisters = 2,
                calculationRegisters = 0,
                constants = listOf(),
                defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)
        )

        return MockSingleOutputProgram(instructions, registers, listOf(0))
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")

}

class MockMultipleOutputProgram(
    override var instructions: MutableList<Instruction<Double>>,
    override val registers: RegisterSet<Double>,
    override val outputRegisterIndices: List<RegisterIndex>
) : Program<Double, Outputs.Multiple<Double>>()
{
    override fun output(): Outputs.Multiple<Double> {
        val outputs = this.outputRegisterIndices.map { idx -> this.registers[idx] }

        return Outputs.Multiple(outputs)
    }

    override fun execute() {
        for (instruction in this.instructions) {
            instruction.execute(this.registers)
        }
    }

    override fun copy(): Program<Double, Outputs.Multiple<Double>> {
        TODO("not implemented")
    }

    override fun findEffectiveProgram() {
        TODO("not implemented")
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")
}

class MockMultipleOutputProgramGenerator(
        environment: EnvironmentFacade<Double, Outputs.Multiple<Double>, Targets.Multiple<Double>>
) : ProgramGenerator<Double, Outputs.Multiple<Double>, Targets.Multiple<Double>>(
        environment,
        MockInstructionGenerator(environment)
)
{
    override fun generateProgram(): Program<Double, Outputs.Multiple<Double>> {
        val instructions = this.instructionGenerator.next().take(2).toMutableList()
        val registers = ArrayRegisterSet(
            inputRegisters = 2,
            calculationRegisters = 0,
            constants = listOf(),
            defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)
        )

        return MockMultipleOutputProgram(instructions, registers, listOf(0, 1))
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")

}

class MockSingleOutputProgramTranslator : ProgramTranslator<Double, Outputs.Single<Double>>() {

    override fun translate(program: Program<Double, Outputs.Single<Double>>): String {
        return "MockSingleOutputProgram"
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")

}