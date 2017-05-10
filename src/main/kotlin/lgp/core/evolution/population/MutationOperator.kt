package lgp.core.evolution.population

import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
import lgp.core.evolution.instructions.BranchOperation
import lgp.core.evolution.instructions.InstructionGenerator
import lgp.core.evolution.instructions.RegisterIndex
import lgp.core.evolution.registers.RandomRegisterGenerator
import lgp.core.evolution.registers.RegisterType
import lgp.core.modules.Module
import lgp.core.modules.ModuleInformation
import java.util.*

/**
 * A search operator used during evolution to mutate an individual from a population.
 *
 * The individual is mutated in place, that is a call to [MutationOperator.mutate] will directly
 * modify the given individual.
 *
 * @param T The type of programs being mutated.
 * @property environment The environment evolution is being performed within.
 */
abstract class MutationOperator<T>(val environment: Environment<T>) : Module {
    /**
     * Mutates the individual given using some mutation method.
     */
    abstract fun mutate(individual: Program<T>)
}

// Used internally to make the macro/micro mutation code a bit clearer.
private enum class MacroMutationType {
    Insertion,
    Deletion
}

private enum class MicroMutationType {
    Register,
    Operator,
    Constant
}

/**
 * A [MutationOperator] implementation that performs effective macro mutations.
 *
 * For more information, see Algorithm 6.1 from Linear Genetic Programming (Brameier, M., Banzhaf, W. 2001).
 *
 * Note that [insertionRate] + [deletionRate] should be equal to 1.
 *
 * @property insertionRate The probability with which instructions should be inserted.
 * @property deletionRate The probability with which instructions should be deleted.
 */
class MacroMutationOperator<T>(
        environment: Environment<T>,
        val insertionRate: Double,      // p_ins
        val deletionRate: Double        // p_del
) : MutationOperator<T>(environment) {

    init {
        // Give a nasty runtime message if we get invalid parameters.
        assert((insertionRate + deletionRate) == 1.0)
    }

    private val minimumProgramLength = this.environment.config.minimumProgramLength
    private val maximumProgramLength = this.environment.config.maximumProgramLength
    private val random = Random()
    private val instructionGenerator = this.environment.registeredModule<InstructionGenerator<T>>(
            CoreModuleType.InstructionGenerator
    )

    /**
     * Performs a single, effective macro mutation to the individual given.
     */
    override fun mutate(individual: Program<T>) {
        // Make sure the individuals effective program is found before mutating, since
        // we need it to perform effective mutations.
        individual.findEffectiveProgram()

        val programLength = individual.instructions.size

        // 1. Randomly select macro mutation type insertion | deletion with probability
        // p_ins | p_del and with p_ins + p_del = 1
        val mutationType = if (random.nextDouble() < this.insertionRate) MacroMutationType.Insertion
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

            // Can only perform a mutation if there is an effective register to choose from.
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

/**
 * A [MutationOperator] implementation that performs effective micro mutations.
 *
 * For more information, see Algorithm 6.2 from Linear Genetic Programming (Brameier, M., Banzhaf, W. 2001).
 *
 * Note that the constant mutation rate is 1 - ([registerMutationRate] - [operatorMutationRate]), which should
 * be taken into account when choosing values for these parameters.
 *
 * @property registerMutationRate The rate with which registers should be mutated.
 * @property operatorMutationRate The rate with which operates should be mutated.
 * @property constantMutationFunc A function that can mutate values in the domain of [T].
 */
class MicroMutationOperator<T>(
        environment: Environment<T>,
        val registerMutationRate: Double,
        val operatorMutationRate: Double,
        val constantMutationFunc: (T) -> T
) : MutationOperator<T>(environment) {

    private val constantsRate = this.environment.config.constantsRate
    private val operations = this.environment.operations
    private val random = Random()

    /**
     * Performs a single, effective micro mutation to the individual given.
     */
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
        val p = random.nextDouble()
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

                    if (random.nextDouble() < constantsRate) {
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
                        val reg = if (random.nextDouble() < constantsRate) {
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