package nz.co.jedsimson.lgp.test.mocks

import nz.co.jedsimson.lgp.core.environment.DefaultValueProviders
import nz.co.jedsimson.lgp.core.environment.EnvironmentDefinition
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramGenerator
import nz.co.jedsimson.lgp.core.program.instructions.*
import nz.co.jedsimson.lgp.core.program.registers.Arguments
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import java.util.*

class Identity : UnaryOperation<Double>({ arguments -> arguments.get(0) }) {
    override val representation: String
        get() = TODO("not implemented")

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        TODO("not implemented")
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")
}

class Zero : BinaryOperation<Double>({ arguments -> 0.0 }) {
    override val representation: String
        get() = TODO("not implemented")

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        TODO("not implemented")
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")
}

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

class MockInstructionGenerator(
    environment: EnvironmentDefinition<Double, Outputs.Single<Double>>
) : InstructionGenerator<Double, Outputs.Single<Double>>(environment)
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

class MockProgram(
    instructions: List<Instruction<Double>>,
    registers: RegisterSet<Double>,
    outputRegisterIndices: List<RegisterIndex>
) : Program<Double, Outputs.Single<Double>>(
    instructions.toMutableList(),
    registers,
    outputRegisterIndices
)
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

class MockProgramGenerator(
    environment: EnvironmentDefinition<Double, Outputs.Single<Double>>
) : ProgramGenerator<Double, Outputs.Single<Double>>(
    environment,
    MockInstructionGenerator(environment)
)
{
    override fun generateProgram(): Program<Double, Outputs.Single<Double>> {
        val instructions = this.instructionGenerator.next().take(2).toList()
        val registers = RegisterSet(
                inputRegisters = 2,
                calculationRegisters = 0,
                constants = listOf(),
                defaultValueProvider = DefaultValueProviders.constantValueProvider(1.0)
        )

        return MockProgram(instructions, registers, listOf(0))
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")

}