package lgp.lib

import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
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
class BaseProgramGenerator<T>(
        environment: Environment<T>,
        val sentinelTrueValue: T,
        val outputRegisterIndex: RegisterIndex
) : ProgramGenerator<T>(
        environment,
        instructionGenerator = environment.registeredModule(CoreModuleType.InstructionGenerator)
) {

    private val random = this.environment.randomState

    override fun generateProgram(): Program<T> {
        val length = this.random.randInt(
                this.environment.configuration.initialMinimumProgramLength,
                this.environment.configuration.initialMaximumProgramLength
        )

        val branchesUsed = this.environment.operations.any { op -> op is BranchOperation<T> }
        val output = this.environment.registerSet.calculationRegisters.start

        val instructions = mutableListOf<Instruction<T>>()
        // TODO: Use set instead of list.
        val effectiveRegisters = mutableListOf<RegisterIndex>()

        // Add first instruction
        instructions.add(this.instructionGenerator.generateInstruction())

        // Construct effective instructions
        for (i in 2..length) {
            if (instructions.first().operation !is BranchOperation<T>) {
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
                    instruction.operation is BranchOperation<T>
                }

                assert(instr.operation is BranchOperation<T>)

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
                outputRegisterIndex = this.outputRegisterIndex,
                sentinelTrueValue = this.sentinelTrueValue
        )
    }

    override val information = ModuleInformation(
        description = "A simple program generator."
    )
}