package lgp.lib

import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
import lgp.core.evolution.instructions.BranchOperation
import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.instructions.RegisterIndex
import lgp.core.evolution.population.Program
import lgp.core.evolution.population.ProgramGenerator
import lgp.core.evolution.population.choice
import lgp.core.evolution.population.randInt
import lgp.core.evolution.registers.RegisterType
import lgp.core.evolution.registers.copy
import lgp.core.modules.ModuleInformation
import lgp.lib.BaseInstructionGenerator
import lgp.lib.BaseProgram
import java.util.*

/**
 * @suppress
 */
class BaseProgramGenerator<T>(environment: Environment<T>, val sentinelTrueValue: T)
    : ProgramGenerator<T>(environment, instructionGenerator = environment.registeredModule(CoreModuleType.InstructionGenerator)) {

    private val random = Random()

    override fun generateProgram(): Program<T> {
        val length = this.random.randInt(this.environment.config.initialMinimumProgramLength,
                                     this.environment.config.initialMaximumProgramLength)

        val branchesUsed = this.environment.operations.any { op -> op is BranchOperation<T> }
        val output = this.environment.registerSet.calculationRegisters.start

        val instructions = mutableListOf<Instruction<T>>()
        // TODO: Use set instead of list.
        val effectiveRegisters = mutableListOf<RegisterIndex>()

        // Add first instruction
        instructions.add(this.instructionGenerator.next().first())

        // Construct effective instructions
        for (i in 2..length) {
            if (instructions.first().operation !is BranchOperation<T>) {
                effectiveRegisters.remove(instructions.first().destination)
            }

            // Add any operand registers that are not already known to be
            // effective and are calculation registers.
            instructions.first().operands.filter { operand ->
                operand !in effectiveRegisters &&
                this.environment.registerSet.registerType(operand) == RegisterType.Calculation
            }.forEach { operand -> effectiveRegisters.add(operand) }

            if (effectiveRegisters.isEmpty()) {
                effectiveRegisters.add(output)
            }

            if (branchesUsed && random.nextGaussian() < this.environment.config.branchInitialisationRate) {
                val instr = this.instructionGenerator.next().first { instruction ->
                    instruction.operation is BranchOperation<T>
                }

                assert(instr.operation is BranchOperation<T>)

                instructions.add(0, instr)
            } else {
                // Get a random instruction and make it effective by
                // using one of the registers marked as effective.
                val instr = this.instructionGenerator.next().first()

                instr.destination = random.choice(effectiveRegisters)

                // And add it to the program
                instructions.add(0, instr)
            }
        }

        // Each program gets its own copy of the register set
        val program = BaseProgram(instructions.toList(), this.environment.registerSet.copy(), this.sentinelTrueValue)

        return program
    }

    override val information = ModuleInformation(
        description = "A simple program generator."
    )
}