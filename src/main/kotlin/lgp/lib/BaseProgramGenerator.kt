package lgp.lib

import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
import lgp.core.evolution.fitness.Output
import lgp.core.program.instructions.BranchOperation
import lgp.core.program.instructions.Instruction
import lgp.core.program.instructions.RegisterIndex
import lgp.core.program.Program
import lgp.core.program.ProgramGenerator
import lgp.core.evolution.operators.choice
import lgp.core.evolution.operators.randInt
import lgp.core.program.registers.RegisterType
import lgp.core.modules.ModuleInformation

/**
 * A ``ProgramGenerator`` implementation that provides effective ``BaseProgram`` instances.
 *
 * This generator only creates programs which are "effective".
 */
class BaseProgramGenerator<TProgram, TOutput : Output<TProgram>>(
        environment: Environment<TProgram, TOutput>,
        val sentinelTrueValue: TProgram,
        val outputRegisterIndices: List<RegisterIndex>,
        val outputResolver: (BaseProgram<TProgram, TOutput>) -> TOutput
) : ProgramGenerator<TProgram, TOutput>(
        environment,
        instructionGenerator = environment.registeredModule(CoreModuleType.InstructionGenerator)
) {

    private val random = this.environment.randomState

    override fun generateProgram(): Program<TProgram, TOutput> {
        val length = this.random.randInt(
                this.environment.configuration.initialMinimumProgramLength,
                this.environment.configuration.initialMaximumProgramLength
        )

        val branchesUsed = this.environment.operations.any { op -> op is BranchOperation<TProgram> }
        val output = this.environment.registerSet.calculationRegisters.start

        val instructions = mutableListOf<Instruction<TProgram>>()
        // TODO: Use set instead of list.
        val effectiveRegisters = mutableListOf<RegisterIndex>()

        // Add first instruction
        instructions.add(this.instructionGenerator.generateInstruction())

        // Construct effective instructions
        for (i in 2..length) {
            if (instructions.first().operation !is BranchOperation<TProgram>) {
                effectiveRegisters.remove(instructions.first().destination)
            }

            // Add any operand registers that are not already known to be
            // effective and are calculation registers.
            instructions.first().operands.filter { operand ->
                operand !in effectiveRegisters &&
                        (this.environment.registerSet.registerType(operand) == RegisterType.Calculation)
            }.forEach { operand -> effectiveRegisters.add(operand) }

            if (effectiveRegisters.isEmpty()) {
                effectiveRegisters.add(output)
            }

            if (branchesUsed && random.nextDouble() < this.environment.configuration.branchInitialisationRate) {
                val instr = this.instructionGenerator.next().first { instruction ->
                    instruction.operation is BranchOperation<TProgram>
                }

                assert(instr.operation is BranchOperation<TProgram>)

                instructions.add(0, instr)
            } else {
                // Get a random instruction and make it effective by
                // using one of the registers marked as effective.
                val instr = this.instructionGenerator.generateInstruction()

                instr.destination = random.choice(effectiveRegisters)

                // And add it to the program
                instructions.add(0, instr)
            }
        }

        // Each program gets its own copy of the register set
        return BaseProgram(
            instructions = instructions.toList(),
            registerSet = this.environment.registerSet.copy(),
            outputRegisterIndices = this.outputRegisterIndices,
            sentinelTrueValue = this.sentinelTrueValue,
            outputResolver = this.outputResolver
        )
    }

    override val information = ModuleInformation(
        description = "A simple program generator."
    )
}
