package nz.co.jedsimson.lgp.lib.generators

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.randInt
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramGenerator
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex
import nz.co.jedsimson.lgp.lib.base.BaseProgram
import nz.co.jedsimson.lgp.lib.operations.BranchOperation

/**
 * A ``ProgramGenerator`` implementation that provides random ``BaseProgram`` instances.
 *
 * @property sentinelTrueValue A value that should be considered as boolean "true".
 * @property outputRegisterIndices A collection of indices that should be considered as the program output registers.
 * @property outputResolver A function that can be used to resolve the programs register contents to an [Output].
 */
class RandomProgramGenerator<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    val sentinelTrueValue: TProgram,
    val outputRegisterIndices: List<RegisterIndex>,
    val outputResolver: (BaseProgram<TProgram, TOutput>) -> TOutput
) : ProgramGenerator<TProgram, TOutput, TTarget>(
    environment,
    instructionGenerator = environment.moduleFactory.instance(CoreModuleType.InstructionGenerator)
) {
    private val random = this.environment.randomState

    override fun generateProgram(): Program<TProgram, TOutput> {
        val length = this.random.randInt(
            this.environment.configuration.initialMinimumProgramLength,
            this.environment.configuration.initialMaximumProgramLength
        )

        val instructions = mutableListOf<Instruction<TProgram>>()

        val branchesUsed = this.environment.operations.any { op -> op is BranchOperation<TProgram> }
        val branchInitialisationRate = this.environment.configuration.branchInitialisationRate

        for (i in 1..length) {
            val instruction = when {
                branchesUsed && random.nextDouble() < branchInitialisationRate -> {
                    this.instructionGenerator.next().first { instruction ->
                        instruction.operation is BranchOperation<TProgram>
                    }
                }
                else -> {
                    this.instructionGenerator.generateInstruction()
                }
            }

            instructions.add(instruction)
        }

        // Each program gets its own copy of the register set
        return BaseProgram(
            instructions = instructions,
            registers = this.environment.registerSet.copy(),
            outputRegisterIndices = this.outputRegisterIndices,
            sentinelTrueValue = this.sentinelTrueValue,
            outputResolver = this.outputResolver
        )
    }

    override val information = ModuleInformation(
        description = "A ProgramGenerator implementation that provides random BaseProgram instances."
    )
}
