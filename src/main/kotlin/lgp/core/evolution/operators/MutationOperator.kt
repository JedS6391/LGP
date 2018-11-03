package lgp.core.evolution.operators

import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
import lgp.core.evolution.fitness.Output
import lgp.core.program.instructions.InstructionGenerator
import lgp.core.program.instructions.RegisterIndex
import lgp.core.program.registers.RandomRegisterGenerator
import lgp.core.program.registers.RegisterType
import lgp.core.modules.Module
import lgp.core.modules.ModuleInformation
import lgp.core.program.Program
import java.util.*

/**
 * A search operator used during evolution to mutate an individual from a population.
 *
 * The individual is mutated in place, that is a call to [MutationOperator.mutate] will directly
 * modify the given individual.
 *
 * @param TProgram The type of programs being mutated.
 * @property environment The environment evolution is being performed within.
 */
abstract class MutationOperator<TProgram, TOutput : Output<TProgram>>(val environment: Environment<TProgram, TOutput>) : Module {
    /**
     * Mutates the individual given using some mutation method.
     */
    abstract fun mutate(individual: Program<TProgram, TOutput>)
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
class MacroMutationOperator<TProgram, TOutput : Output<TProgram>>(
        environment: Environment<TProgram, TOutput>,
        val insertionRate: Double,      // p_ins
        val deletionRate: Double        // p_del
) : MutationOperator<TProgram, TOutput>(environment) {

    init {
        // Give a nasty runtime message if we get invalid parameters.
        assert((insertionRate + deletionRate) == 1.0)
    }

    private val minimumProgramLength = this.environment.configuration.minimumProgramLength
    private val maximumProgramLength = this.environment.configuration.maximumProgramLength
    private val random = this.environment.randomState
    private val instructionGenerator = this.environment.registeredModule<InstructionGenerator<TProgram, TOutput>>(
            CoreModuleType.InstructionGenerator
    )

    /**
     * Performs a single, effective macro mutation to the individual given.
     */
    override fun mutate(individual: Program<TProgram, TOutput>) {
        // Make sure the individuals effective program is found before mutating, since
        // we need it to perform effective mutations.
        individual.findEffectiveProgram()

        val programLength = individual.instructions.size

        // 1. Randomly select macro mutation type insertion | deletion with probability
        // p_ins | p_del and with p_ins + p_del = 1
        val mutationType = if (random.nextDouble() < this.insertionRate) {
            MacroMutationType.Insertion
        } else {
            MacroMutationType.Deletion
        }

        // 2. Randomly select an instruction at a position i (mutation point) in program gp.
        val mutationPoint = random.randInt(0, programLength - 1)

        // 3. If len(gp) < l_max and (insertion or len(gp) = l_min) then
        if (programLength < maximumProgramLength &&
                (mutationType == MacroMutationType.Insertion || programLength == minimumProgramLength)) {

            // We can avoid running algorithm 3.1 like in the literature by
            // just searching for effective calculation registers and making
            // sure we choose an effective register for our mutation
            val effectiveRegisters = findEffectiveCalculationRegisters(individual, mutationPoint)
            val instruction = this.instructionGenerator.generateInstruction()

            // Can only perform a mutation if there is an effective register to choose from.
            if (effectiveRegisters.isNotEmpty()) {
                instruction.destination = random.choice(effectiveRegisters)

                individual.instructions.add(mutationPoint, instruction)
            }
        }
        else if (programLength > minimumProgramLength &&
                    (mutationType == MacroMutationType.Deletion || programLength == maximumProgramLength)) {

            // 4.
            // (a) Select an effective instruction i (if existent)
            if (individual.effectiveInstructions.isNotEmpty()) {
                val instruction = random.choice(individual.effectiveInstructions)

                // (b) Delete instruction i
                individual.instructions.remove(instruction)
            }
        }
    }

    override val information = ModuleInformation("Algorithm 6.1 ((effective) instruction mutation).")
}

/**
 * A function that can be used to mutate a constant value.
 *
 * Used by the [MicroMutationOperator] implementation.
 */
typealias ConstantMutationFunction<T> = (T) -> T

/**
 * A collection of [ConstantMutationFunction] implementations for use by a [MicroMutationOperator].
 */
object ConstantMutationFunctions {

    /**
     * A [ConstantMutationFunction] which simply returns the original constant value unchanged.
     */
    @JvmStatic
    fun <T> identity(): ConstantMutationFunction<T> {
        return { v -> v }
    }

    /**
     * A [ConstantMutationFunction] which returns the original constant with a small amount of gaussian noise added.
     *
     * @param randomState The system random number generator.
     */
    @JvmStatic
    fun randomGaussianNoise(randomState: Random): ConstantMutationFunction<Double> {
        return { v ->
            v + (randomState.nextGaussian() * 1)
        }
    }
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
 * @property constantMutationFunc A function that can mutate values in the domain of [TProgram].
 */
class MicroMutationOperator<TProgram, TOutput : Output<TProgram>>(
        environment: Environment<TProgram, TOutput>,
        val registerMutationRate: Double,
        val operatorMutationRate: Double,
        val constantMutationFunc: ConstantMutationFunction<TProgram>
) : MutationOperator<TProgram, TOutput>(environment) {

    private val constantsRate = this.environment.configuration.constantsRate
    private val operations = this.environment.operations
    private val random = this.environment.randomState

    /**
     * Performs a single, effective micro mutation to the individual given.
     */
    override fun mutate(individual: Program<TProgram, TOutput>) {
        // 1. Randomly select an (effective) instruction from program gp.
        individual.findEffectiveProgram()

        // Can't do anything if there are no effective instructions
        if (individual.effectiveInstructions.isEmpty()) {
            return
        }

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

        val registerGenerator = RandomRegisterGenerator(this.environment.randomState, individual.registers)

        when (mutationType) {
            // 3. If register mutation then
            MicroMutationType.Register -> {
                val registerPositions = mutableListOf(instruction.destination)
                registerPositions.addAll(instruction.operands)

                // (a) Randomly select a register position destination | operand
                val register = random.choice(registerPositions)

                if (register == instruction.destination) {
                    // (b) If destination register then select a different (effective)
                    // destination register (applying Algorithm 3.1)
                    val instructionPosition = individual.instructions.indexOf(instruction)

                    // Use our shortcut version of Algorithm 3.1.
                    val effectiveRegisters = findEffectiveCalculationRegisters(individual, instructionPosition)

                    instruction.destination = if (effectiveRegisters.isNotEmpty()) {
                        random.choice(effectiveRegisters)
                    } else {
                        instruction.destination
                    }
                } else {
                    // (c) If operand register then select a different constant | register with probability
                    // p_const | 1 - p_const
                    val operand = instruction.operands.indexOf(register)

                    val replacementRegister = if (random.nextDouble() < constantsRate) {
                        registerGenerator.next(RegisterType.Constant).first()
                    } else {
                        registerGenerator.next(
                            a = RegisterType.Input,
                            b = RegisterType.Calculation,
                            predicate = { random.nextDouble() < 0.5 }
                        ).first()
                    }

                    instruction.operands[operand] = replacementRegister.index
                }
            }
            MicroMutationType.Operator -> {
                // 4. If operator mutation then select a different instruction operation randomly
                val operation = random.choice(this.operations)

                // Assure that the arity of the new operation matches with the number of operands the instruction has.
                // If the arity of the operations is the same, then nothing needs to be done.
                if (instruction.operands.size > operation.arity.number) {
                    // If we're going to a reduced arity instruction, we can just truncate the operands
                    instruction.operands = instruction.operands.slice(0 until operation.arity.number)
                } else if (instruction.operands.size < operation.arity.number) {
                    // Otherwise, if we're increasing the arity, just add random input
                    // and calculation registers until the arity is met.
                    while (instruction.operands.size < operation.arity.number) {
                        val register = if (random.nextDouble() < constantsRate) {
                            registerGenerator.next(RegisterType.Constant).first()
                        } else {
                            registerGenerator.next(
                                a = RegisterType.Input,
                                b = RegisterType.Calculation,
                                predicate = { random.nextDouble() < 0.5 }
                            ).first()
                        }

                        instruction.operands.add(register.index)
                    }
                }

                instruction.operation = operation
            }
            else -> {
                // 5. If constant mutation then
                // (a) Randomly select an (effective) instruction with a constant c.
                // Unfortunately the way of searching for an instruction that uses a constant is not
                // particularly elegant, requiring a random search of the entire program.
                var instr = random.choice(individual.effectiveInstructions)

                var constantRegisters = instr.operands.filter { operand ->
                    individual.registers.registerType(operand) == RegisterType.Constant
                }

                // Arbitrarily limit the search to avoid entering a potentially infinite search.
                var limit = 0

                while (constantRegisters.isEmpty() && limit++ < individual.effectiveInstructions.size) {
                    instr = random.choice(individual.effectiveInstructions)

                    constantRegisters = instr.operands.filter { operand ->
                        individual.registers.registerType(operand) == RegisterType.Constant
                    }
                }

                // Only do this if we found an instruction with a constant before giving up.
                if (limit < individual.effectiveInstructions.size) {
                    // (b) Change constant c through a standard deviation σ_const
                    // from the current value: c := c + N(0, σ_const)
                    // NOTE: We allow for different types of mutation of values
                    // depending on the type, but in general a standard deviation
                    // should be used.

                    // Use the first operand that refers to a constant register.
                    val register = constantRegisters.first()
                    val oldValue = individual.registers[register]

                    // Compute a new value using the current constant register value.
                    val newValue = this.constantMutationFunc(oldValue)

                    // Because each individual has its own register set, we can
                    // just overwrite the constant register value.
                    // TODO: Perhaps better to keep original constants for diversity?
                    individual.registers.overwrite(register, newValue)
                }
            }
        }
    }

    override val information = ModuleInformation("Algorithm 6.2 ((effective) micro mutation).")
}

internal fun <TProgram, TOutput : Output<TProgram>> findEffectiveCalculationRegisters(
    individual: Program<TProgram, TOutput>,
    stopPoint: Int
): List<RegisterIndex> {
    val effectiveRegisters = individual.outputRegisterIndices.toMutableList()
    // Only instructions up until to the stop point should be searched.
    val instructions = individual.instructions.reversed().filterIndexed { idx, _ -> idx < stopPoint }

    instructions.forEach { instruction ->
        if (instruction.destination in effectiveRegisters) {
            effectiveRegisters.remove(instruction.destination)

            instruction.operands.forEach { operand ->
                val type = individual.registers.registerType(operand)
                val isCalculation = type == RegisterType.Calculation || type == RegisterType.Input

                if (operand !in effectiveRegisters && isCalculation) {
                    effectiveRegisters.add(operand)
                }
            }
        }
    }

    return effectiveRegisters
}
