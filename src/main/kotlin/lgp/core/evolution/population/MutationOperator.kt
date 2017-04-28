package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.environment.RegisteredModuleType
import lgp.core.evolution.instructions.InstructionGenerator
import lgp.core.evolution.instructions.RegisterIndex
import lgp.core.evolution.registers.RandomRegisterGenerator
import lgp.core.evolution.registers.Register
import lgp.core.evolution.registers.RegisterType
import lgp.core.modules.Module
import lgp.core.modules.ModuleInformation
import java.util.*

abstract class MutationOperator<T>(val environment: Environment<T>) : Module {
    abstract fun mutate(individual: Program<T>)
}

enum class MacroMutationType {
    Insertion,
    Deletion
}

// Algorithm 6.1 ((effective) instruction mutation)
class MacroMutationOperator<T>(
        environment: Environment<T>,
        val insertionRate: Double,      // p_ins
        val deletionRate: Double        // p_del
) : MutationOperator<T>(environment) {

    val minimumProgramLength = this.environment.config.minimumProgramLength
    val maximumProgramLength = this.environment.config.maximumProgramLength
    val instructionGenerator = this.environment.registeredModule<InstructionGenerator<T>>(RegisteredModuleType.InstructionGenerator)
    val random = Random()

    override fun mutate(individual: Program<T>) {
        individual.findEffectiveProgram()

        val programLength = individual.instructions.size

        // 1. Randomly select macro mutation type insertion | deletion with probability
        // p_ins | p_del and with p_ins + p_del = 1
        val mutationType = if (random.nextGaussian() < this.insertionRate) MacroMutationType.Insertion
                           else MacroMutationType.Deletion

        // 2. Randomly select an instruction at a position i (mutation point) in program gp.
        val i = random.randInt(0, programLength - 1)

        // 3. If len(gp) < l_max and (insertion or len(gp) = l_min) then
        if (programLength < maximumProgramLength &&
                (mutationType == MacroMutationType.Insertion || programLength == minimumProgramLength)) {

            // We can avoid running algorithm 3.1 like in the literature by
            // just searching for effective calculation registers and making
            // sure we choose an effective register for our mutation
            val effectiveRegisters = findEffectiveCalculationRegisters(individual, i)
            val instruction = this.instructionGenerator.next().take(1).first()

            if (effectiveRegisters.isNotEmpty()) {
                instruction.destination = random.choice(effectiveRegisters)

                individual.instructions.add(i, instruction)
            }
        }
        else if (programLength > minimumProgramLength &&
                    (mutationType == MacroMutationType.Deletion || programLength == maximumProgramLength)) {
            // 4.
            // (a) Select an effective instruction i (if existent)
            if (individual.effectiveInstructions.size > 0) {
                val instruction = random.choice(individual.effectiveInstructions)

                // (b) Delete instruction i
                individual.instructions.remove(instruction)
            }
        }

    }

    override val information = ModuleInformation("Algorithm 6.1 ((effective) instruction mutation).")
}

enum class MicroMutationType {
    Register,
    Operator,
    Constant
}

// Algorithm 6.2 ((effective) micro mutation)
class MicroMutationOperator<T>(
        environment: Environment<T>,
        val registerMutationRate: Double,
        val operatorMutationRate: Double,
        val constantMutationFunc: (T) -> T
) : MutationOperator<T>(environment) {

    val constantsRate = this.environment.config.constantsRate
    val operations = this.environment.operations
    val random = Random()

    override fun mutate(individual: Program<T>) {
        // 1. Randomly select an (effective) instruction from program gp.
        individual.findEffectiveProgram()

        // Can't do anything if there are no effective instructions
        if (individual.effectiveInstructions.size == 0)
            return

        val instruction = random.choice(individual.effectiveInstructions)

        // 2. Randomly select mutation type register | operator | constant with
        // probability p_regmut | p_opermut | p_constmut and with p_regmut +
        // p_opermut + p_constmut = 1.
        // Assumption: p_constmut = 1 - (p_regmut + p_opermut).
        val p = random.nextGaussian()
        val mutationType = when {
            (p < registerMutationRate) -> {
                MicroMutationType.Register
            }
            (registerMutationRate <= p && p <= (registerMutationRate + operatorMutationRate)) -> {
                MicroMutationType.Operator
            }
            else -> {
                MicroMutationType.Constant
            }
        }

        val registerGenerator = RandomRegisterGenerator(individual.registers)

        when (mutationType) {
            // 3. If register mutation then
            MicroMutationType.Register -> {
                val choices = mutableListOf(instruction.destination)
                choices.addAll(instruction.operands)

                // (a) Randomly select a register position destination | operand
                val reg = random.choice(choices)

                if (reg == instruction.destination) {
                    // (b) If destination register then select a different (effective)
                    // destination register (applying Algorithm 3.1)
                    val i = individual.instructions.indexOf(instruction)

                    // Use our shortcut version of Algorithm 3.1.
                    val effectiveRegisters = findEffectiveCalculationRegisters(individual, i)

                    instruction.destination = if (effectiveRegisters.isNotEmpty()) random.choice(effectiveRegisters)
                                              else instruction.destination
                } else {
                    // (c) If operand register then select a different constant | register with probability
                    // p_const | 1 - p_const
                    val idx = instruction.operands.indexOf(reg)

                    if (random.nextGaussian() < constantsRate) {
                        instruction.operands[idx] = registerGenerator.next(RegisterType.Constant).first().index
                    } else {
                        instruction.operands[idx] = registerGenerator.next(
                                a = RegisterType.Input,
                                b = RegisterType.Calculation,
                                predicate = { random.nextDouble() < 0.5 }
                        ).first().index
                    }
                }
            }
            MicroMutationType.Operator -> {
                // 4. If operator mutation then select a different instruction operation randomly
                val op = random.choice(this.operations)

                // Assure that the arity of the new operation matches with
                // the number of operands the instruction has.
                // If we're going to a reduced arity instruction, we can just truncate the operands
                if (instruction.operands.size > op.arity.number) {
                    instruction.operands = instruction.operands.slice(0..op.arity.number - 1)
                } else if (instruction.operands.size < op.arity.number) {
                    // Otherwise, if we're increasing the arity, just add random input
                    // and calculation registers until the arity is met.
                    while (instruction.operands.size < op.arity.number) {
                        val reg = if (random.nextGaussian() < constantsRate) {
                            registerGenerator.next(RegisterType.Constant).first()
                        } else {
                            registerGenerator.next(
                                    a = RegisterType.Input,
                                    b = RegisterType.Calculation,
                                    predicate = { random.nextDouble() < 0.5 }
                            ).first()
                        }

                        instruction.operands.add(reg.index)
                    }
                }

                instruction.operation = op
            }
            else -> {
                // 5. If constant mutation then
                // (a) Randomly select an (effective) instruction with a constant c.
                // TODO: Refactor this.
                var instr = random.choice(individual.effectiveInstructions)
                var constantRegisters = instr.operands.filter { operand ->
                    individual.registers.registerType(operand) == RegisterType.Constant
                }

                var limit  = 0

                while (constantRegisters.isEmpty() && limit++ < individual.effectiveInstructions.size) {
                    instr = random.choice(individual.effectiveInstructions)

                    constantRegisters = instr.operands.filter { operand ->
                        individual.registers.registerType(operand) == RegisterType.Constant
                    }
                }

                // Only do this if we found an instruction with a constant
                if (limit < individual.effectiveInstructions.size) {
                    // (b) Change constant c through a standard deviation σ_const
                    // from the current value: c := c + N(0, σ_const)
                    // NOTE: We allow for different types of mutation of values
                    // depending on the type, but in general a standard deviation
                    // should be used.

                    // Use the first operand that refers to a constant register.
                    val reg = constantRegisters.first()
                    val oldValue = individual.registers.read(reg)

                    // Compute a new value using the current constant register value.
                    val newValue = this.constantMutationFunc(oldValue)

                    // Because each individual has its own register set, we can
                    // just overwrite the constant register value.
                    // TODO: Perhaps better to keep original constants for diversity?
                    individual.registers.overwrite(reg, newValue)
                }
            }
        }
    }

    override val information = ModuleInformation("Algorithm 6.2 ((effective) micro mutation).")
}

internal fun <T> findEffectiveCalculationRegisters(individual: Program<T>, stopPoint: Int): List<RegisterIndex> {
    val effectiveRegisters = mutableListOf(individual.outputRegisterIndex)

    for ((i, instruction) in individual.instructions.reversed().withIndex()) {
        if (i == stopPoint)
            break

        if (instruction.destination in effectiveRegisters) {
            effectiveRegisters.remove(instruction.destination)

            for (operand in instruction.operands) {
                val isCalculation = individual.registers.registerType(operand) == RegisterType.Calculation

                if (operand !in effectiveRegisters && isCalculation) {
                    effectiveRegisters.add(operand)
                }
            }
        }
    }

    return effectiveRegisters
}