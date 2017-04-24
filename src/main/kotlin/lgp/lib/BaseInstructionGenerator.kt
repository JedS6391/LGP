package lgp.lib

import lgp.core.environment.Environment
import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.instructions.InstructionGenerator
import lgp.core.evolution.instructions.Operation
import lgp.core.evolution.registers.*
import lgp.core.modules.ModuleInformation

import java.util.Random
import kotlin.coroutines.experimental.buildSequence

/**
 * @suppress
 */
class BaseInstructionGenerator<T> : InstructionGenerator<T> {

    private val rg: Random
    val operationPool: List<Operation<T>>
    private val registers: RegisterSet<T>
    private val registerGenerator: RandomRegisterGenerator<T>

    constructor(environment: Environment<T>) : super(environment) {
        this.rg = Random()
        this.operationPool = environment.operations
        this.registers = this.environment.registerSet.copy()
        this.registerGenerator = RandomRegisterGenerator(this.registers)
    }

    override fun next(): Sequence<Instruction<T>> = buildSequence {
        while (true) {
            yield(this@BaseInstructionGenerator.generateRandomInstruction())
        }
    }

    private fun generateRandomInstruction(): Instruction<T> {
        // Choose a random output register
        val outputIndex = this.getRandomInputAndCalculationRegisters(1).first().index

        // Choose a random operator
        val operatorIndex = rg.nextInt(this.operationPool.size)
        val operation = this.operationPool[operatorIndex]

        // Determine whether to use a constant register
        val shouldUseConstant = rg.nextFloat().toDouble() < this.environment.config.constantsRate

        if (shouldUseConstant) {
            // This instruction should use a constant register, first get the constant register
            val constRegister = this.registerGenerator.next(RegisterType.Constant).first()

            // Then choose either a calculation or input register. We use arity - 1 because whatever the arity of
            // the operation, we've already chosen one of the arguments registers as a constant register.
            val nonConstRegister = this.getRandomInputAndCalculationRegisters(operation.arity.number - 1)

            val inputIndices = mutableListOf<Int>()

            // We don't always want the constant register to be in the same position in an instruction.
            val prob = this.rg.nextFloat()

            when {
                prob < 0.5 -> {
                    inputIndices.add(constRegister.index)
                    inputIndices.addAll(nonConstRegister.map(Register<T>::index))
                }
                else -> {
                    inputIndices.addAll(nonConstRegister.map(Register<T>::index))
                    inputIndices.add(constRegister.index)
                }
            }

            return BaseInstruction(operation, outputIndex, inputIndices)
        } else {
            // Choose some random input registers depending on the arity of the operation
            val inputs = this.getRandomInputAndCalculationRegisters(operation.arity.number)

            return BaseInstruction(operation, outputIndex, inputs.map(Register<T>::index).toMutableList())
        }
    }

    private fun getRandomInputAndCalculationRegisters(count: Int): List<Register<T>> {
        val inputs = this.registerGenerator.next(
                a = RegisterType.Calculation,
                b = RegisterType.Input,
                // 50% probability of an input or calculation register
                predicate = { this.rg.nextDouble() < 0.5 }
        ).take(count).toList()

        return inputs
    }

    override val information = ModuleInformation (
        description = "A simple instruction generator."
    )
}
