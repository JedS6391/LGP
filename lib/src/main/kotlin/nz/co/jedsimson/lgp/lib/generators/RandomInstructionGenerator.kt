package nz.co.jedsimson.lgp.lib.generators

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.choice
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.instructions.InstructionGenerator
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.program.registers.*
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.lib.base.BaseInstruction

/**
 * Built-in offering of the [InstructionGenerator] interface.
 *
 * The generator provides an endless stream of randomly generated instructions
 * that are constructed from the pool of available operations and registers.
 */
class RandomInstructionGenerator<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>
) :
    InstructionGenerator<TProgram, TOutput, TTarget>(environment) {

    private val random = this.environment.randomState
    private val operationPool: List<Operation<TProgram>> = environment.operations
    private val registers: RegisterSet<TProgram> = this.environment.registerSet.copy()
    private val registerGenerator: RandomRegisterGenerator<TProgram>

    init {
        this.registerGenerator = RandomRegisterGenerator(this.environment.randomState, this.registers)
    }

    /**
     * Generates a new, completely random instruction.
     *
     * The instruction will be a [BaseInstruction] instance, and the [InstructionGenerator.next] method can be called
     * to use this function as a generator.
     */
    override fun generateInstruction(): Instruction<TProgram> {
        // Choose a random output register
        val output = this.registerGenerator.getRandomInputAndCalculationRegisters(1).first()

        // Choose a random operator
        val operation = random.choice(this.operationPool)

        // Determine whether to use a constant register
        val shouldUseConstant = random.nextFloat().toDouble() < this.environment.configuration.constantsRate

        if (shouldUseConstant) {
            // This instruction should use a constant register, first get the constant register
            val constRegister = this.registerGenerator.next(RegisterType.Constant).first().index

            // Then choose either a calculation or input register. We use arity - 1 because whatever the arity of
            // the operation, we've already chosen one of the arguments registers as a constant register.
            val nonConstRegister = this.registerGenerator.getRandomInputAndCalculationRegisters(
                operation.arity.number - 1
            )

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
            val inputs = this.registerGenerator.getRandomInputAndCalculationRegisters(operation.arity.number)

            return BaseInstruction(operation, output, inputs)
        }
    }

    override val information = ModuleInformation (
        description = "A simple instruction generator."
    )
}
