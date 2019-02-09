package nz.co.jedsimson.lgp.lib

import nz.co.jedsimson.lgp.core.environment.Environment
import nz.co.jedsimson.lgp.core.program.instructions.BranchOperation
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.instructions.RegisterIndex
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramGenerator
import nz.co.jedsimson.lgp.core.evolution.operators.choice
import nz.co.jedsimson.lgp.core.evolution.operators.randInt
import nz.co.jedsimson.lgp.core.program.registers.RegisterType
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output

/**
 * A ``ProgramGenerator`` implementation that provides effective ``BaseProgram`` instances.
 *
 * All programs generated will have an entirely effective set of instructions.
 *
 * @property sentinelTrueValue A value that should be considered as boolean "true".
 * @property outputRegisterIndices A collection of indices that should be considered as the program output registers.
 * @property outputResolver A function that can be used to resolve the programs register contents to an [Output].
 */
class EffectiveProgramGenerator<TProgram, TOutput : Output<TProgram>>(
        environment: Environment<TProgram, TOutput>,
        val sentinelTrueValue: TProgram,
        val outputRegisterIndices: List<RegisterIndex>,
        val outputResolver: (BaseProgram<TProgram, TOutput>) -> TOutput
) : ProgramGenerator<TProgram, TOutput>(
    environment,
    instructionGenerator = EffectiveProgramInstructionGenerator(environment)
) {

    private val random = this.environment.randomState

    override fun generateProgram(): Program<TProgram, TOutput> {
        val length = this.random.randInt(
            this.environment.configuration.initialMinimumProgramLength,
            this.environment.configuration.initialMaximumProgramLength
        )

        val branchesUsed = this.environment.operations.any { op -> op is BranchOperation<TProgram> }
        val branchInitialisationRate = this.environment.configuration.branchInitialisationRate
        val output = this.outputRegisterIndices.first()

        val instructions = mutableListOf<Instruction<TProgram>>()
        val effectiveRegisters = mutableListOf(output)

        instructions.add(
            (this.instructionGenerator as EffectiveProgramInstructionGenerator<TProgram, TOutput>).generateInstruction(
                effectiveRegisters
            )
        )

        // Construct effective instructions
        for (i in 2..length) {
            // If the previously added instruction was a branch, then it's output register will
            // not be in the effective register set, so we don't attempt to delete it.
            if (instructions.first().operation !is BranchOperation<TProgram>) {
                effectiveRegisters.remove(instructions.first().destination)
            }

            // Add any operand registers that are not already known to be
            // effective and are calculation registers.
            instructions.first().operands.filter { operand ->
                operand !in effectiveRegisters &&
                (this.environment.registerSet.registerType(operand) == RegisterType.Calculation)
            }.forEach { operand ->
                effectiveRegisters.add(operand)
            }

            if (effectiveRegisters.isEmpty()) {
                effectiveRegisters.add(output)
            }

            val instruction = when {
                branchesUsed && random.nextDouble() < branchInitialisationRate -> {
                    this.instructionGenerator.next().first { instruction ->
                        instruction.operation is BranchOperation<TProgram>
                    }
                }
                else -> {
                    // Get a random instruction and make it effective by using one of the registers marked as effective.
                    val instr = this.instructionGenerator.generateInstruction(effectiveRegisters)

                    instr.destination = random.choice(effectiveRegisters)

                    instr
                }
            }

            // We always push the instruction to the front as we are somewhat working backwards.
            instructions.add(0, instruction)
        }

        // Each program gets its own copy of the register set
        val program = BaseProgram(
            instructions = instructions.toList(),
            registerSet = this.environment.registerSet.copy(),
            outputRegisterIndices = this.outputRegisterIndices,
            sentinelTrueValue = this.sentinelTrueValue,
            outputResolver = this.outputResolver
        )

        program.effectiveInstructions = instructions.toMutableList()

        return program
    }

    override val information = ModuleInformation(
            description = "A ProgramGenerator implementation that provides effective BaseProgram instances."
    )
}
