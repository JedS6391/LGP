package nz.co.jedsimson.lgp.lib

import nz.co.jedsimson.lgp.core.environment.Environment
import nz.co.jedsimson.lgp.core.evolution.operators.choice
import nz.co.jedsimson.lgp.core.program.registers.*
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.instructions.*

internal class EffectiveProgramInstructionGenerator<TProgram, TOutput : Output<TProgram>> : InstructionGenerator<TProgram, TOutput> {

    private val random = this.environment.randomState
    private val operationPool: List<Operation<TProgram>>
    private val registers: RegisterSet<TProgram>
    private val registerGenerator: RandomRegisterGenerator<TProgram>

    constructor(environment: Environment<TProgram, TOutput>) : super(environment) {
        this.operationPool = environment.operations
        this.registers = this.environment.registerSet.copy()
        this.registerGenerator = RandomRegisterGenerator(this.environment.randomState, this.registers)
    }

    /**
     * Not implemented -- this implementation should only be used internally by [EffectiveProgramGenerator].
     */
    override fun generateInstruction(): Instruction<TProgram> {
        throw NotImplementedError()
    }

    /**
     * Generates an effective instruction.
     *
     * @param effectiveRegisters The current set of effective registers.
     * @param branch Determines whether the generated instruction should be a branch instruction.
     */
    fun generateInstruction(effectiveRegisters: List<RegisterIndex>, branch: Boolean = false): Instruction<TProgram> {
        // If there are no effective registers, then default to the first calculation register.
        val outputs = when {
            effectiveRegisters.isEmpty() -> listOf(this.registers.calculationRegisters.first)
            else -> effectiveRegisters
        }

        // Pick a random operation for this instruction.
        val operation = when {
            branch -> {
                this.random.choice(this.operationPool.filter { operation ->
                    operation is BranchOperation<TProgram>
                })
            }
            else -> {
                this.random.choice(this.operationPool)
            }
        }

        val shouldUseConstant = random.nextFloat().toDouble() < this.environment.configuration.constantsRate
        // Pick a random (but effective) register.
        val output = this.random.choice(outputs)

        if (shouldUseConstant) {
            // This instruction should use a constant register, first get the constant register
            val constRegister = this.registerGenerator.next(RegisterType.Constant).first().index

            // Then choose either a calculation or input register. We use arity - 1 because whatever the arity of
            // the operation, we've already chosen delegated one of the arguments registers as a constant register.
            val nonConstRegisters = this.registerGenerator.getRandomInputAndCalculationRegisters(
                operation.arity.number - 1
            )

            // We don't always want the constant register to be in the same position in an instruction.
            val inputs = when {
                this.random.nextFloat() < 0.5 -> {
                    listOf(constRegister) + nonConstRegisters
                }
                else -> {
                    nonConstRegisters + listOf(constRegister)
                }
            }

            return BaseInstruction(operation, output, inputs.toMutableList())
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
