package lgp.lib

import lgp.core.environment.Environment
import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.instructions.InstructionGenerator
import lgp.core.evolution.instructions.Operation
import lgp.core.evolution.instructions.RegisterIndex
import lgp.core.evolution.population.choice
import lgp.core.evolution.registers.*
import lgp.core.modules.ModuleInformation

import java.util.Random
import kotlin.coroutines.experimental.buildSequence

/**
 * Built-in offering of the ``InstructionGenerator`` interface.
 *
 * The generator provides an endless stream of randomly generated instructions
 * that are constructed from the pool of available operations and registers.
 */
class BaseInstructionGenerator<T> : InstructionGenerator<T> {

    private val random = this.environment.randomState
    val operationPool: List<Operation<T>>
    private val registers: RegisterSet<T>
    private val registerGenerator: RandomRegisterGenerator<T>

    constructor(environment: Environment<T>) : super(environment) {
        this.operationPool = environment.operations
        this.registers = this.environment.registerSet.copy()
        this.registerGenerator = RandomRegisterGenerator(this.environment.randomState, this.registers)
    }

    /**
     * Generator function that provides a sequence of randomly generated instructions.
     *
     * This sequence can be consumed in a loop or a fixed number of instructions can be requested.
     */
    override fun next(): Sequence<Instruction<T>> = buildSequence {
        while (true) {
            yield(this@BaseInstructionGenerator.generateRandomInstruction())
        }
    }

    private fun generateRandomInstruction(): Instruction<T> {
        // Choose a random output register
        val output = this.getRandomInputAndCalculationRegisters(1).first()

        // Choose a random operator
        val operation = random.choice(this.operationPool)

        // Determine whether to use a constant register
        val shouldUseConstant = random.nextFloat().toDouble() < this.environment.config.constantsRate

        if (shouldUseConstant) {
            // This instruction should use a constant register, first get the constant register
            val constRegister = this.registerGenerator.next(RegisterType.Constant).first().index

            // Then choose either a calculation or input register. We use arity - 1 because whatever the arity of
            // the operation, we've already chosen one of the arguments registers as a constant register.
            val nonConstRegister = this.getRandomInputAndCalculationRegisters(operation.arity.number - 1)

            val inputs = mutableListOf<RegisterIndex>()

            // We don't always want the constant register to be in the same position in an instruction.
            val prob = this.random.nextFloat()

            when {
                prob < 0.5 -> {
                    inputs.add(constRegister)
                    inputs.addAll(nonConstRegister)
                }
                else -> {
                    inputs.addAll(nonConstRegister)
                    inputs.add(constRegister)
                }
            }

            return BaseInstruction(operation, output, inputs)
        } else {
            // Choose some random input registers depending on the arity of the operation
            val inputs = this.getRandomInputAndCalculationRegisters(operation.arity.number)

            return BaseInstruction(operation, output, inputs)
        }
    }

    private fun getRandomInputAndCalculationRegisters(count: Int): MutableList<RegisterIndex> {
        val inputs = this.registerGenerator.next(
                a = RegisterType.Calculation,
                b = RegisterType.Input,
                // 50% probability of an input or calculation register
                predicate = { this.random.nextDouble() < 0.5 }
        ).take(count).map(Register<T>::index)

        return inputs.toMutableList()
    }

    override val information = ModuleInformation (
        description = "A simple instruction generator."
    )
}
