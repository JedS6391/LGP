package nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.choice
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.events.Diagnostics
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.EffectiveCalculationRegisterResolver
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategy
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.registers.RandomRegisterGenerator
import nz.co.jedsimson.lgp.core.program.registers.RegisterType

/**
 * A collection of [MutationStrategy] implementations for performing micro-mutations.
 */
internal object MicroMutationStrategies {

    /**
     * A [MutationStrategy] that will modify the registers of a random instruction in a [Program]
     *
     * The modification will either be:
     *   - Replace the instructions destination register with another register
     *   - Replace one of the instructions operand registers with another register
     *
     * @param environment An environment that the mutation is occurring in.
     * @property registerGenerator A generator that can be used to decide on random registers.
     * @property effectiveCalculationRegisterResolver A function that can be used to resolve effective calculation registers.
     */
    internal class RegisterMicroMutationStrategy<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
        environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
        private val registerGenerator: RandomRegisterGenerator<TProgram>,
        private val effectiveCalculationRegisterResolver: EffectiveCalculationRegisterResolver<TProgram, TOutput>
    ) : MutationStrategy<TProgram, TOutput, TTarget>(environment) {

        private val constantsRate = this.environment.configuration.constantsRate
        private val random = this.environment.randomState

        override fun mutate(individual: Program<TProgram, TOutput>) {
            Diagnostics.debug("RegisterMicroMutation-start", mapOf(
                "individual" to individual
            ))

            val instruction = this.random.choice(individual.effectiveInstructions)
            val registerPositions = mutableListOf(instruction.destination) + instruction.operands

            // (a) Randomly select a register position destination | operand
            val register = this.random.choice(registerPositions)

            if (register == instruction.destination) {
                // (b) If destination register then select a different (effective)
                // destination register (applying Algorithm 3.1)
                val instructionPosition = individual.instructions.indexOf(instruction)

                // Use our shortcut version of Algorithm 3.1.
                val effectiveRegisters = this.effectiveCalculationRegisterResolver(individual, instructionPosition)

                instruction.destination = if (effectiveRegisters.isNotEmpty()) {
                    this.random.choice(effectiveRegisters)
                } else {
                    instruction.destination
                }
            } else {
                // (c) If operand register then select a different constant | register with probability
                // p_const | 1 - p_const
                val operand = instruction.operands.indexOf(register)

                val replacementRegister = if (this.random.nextDouble() < this.constantsRate) {
                    this.registerGenerator.next(RegisterType.Constant).first()
                } else {
                    this.registerGenerator.next(
                        a = RegisterType.Input,
                        b = RegisterType.Calculation,
                        predicate = { this.random.nextDouble() < 0.5 }
                    ).first()
                }

                instruction.operands[operand] = replacementRegister.index
            }

            Diagnostics.debug("RegisterMicroMutation-end", mapOf(
                "individual" to individual
            ))
        }
    }

    /**
     * A [MutationStrategy] that will modify the operation of a random instruction in a [Program].
     *
     * The modification involves replacing the operation with another random operation.
     *
     * @param environment An environment that the mutation is occurring in.
     * @property registerGenerator A generator that can be used to decide on random registers.
     */
    internal class OperatorMicroMutationStrategy<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
        environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
        private val registerGenerator: RandomRegisterGenerator<TProgram>
    ) : MutationStrategy<TProgram, TOutput, TTarget>(environment) {

        private val random = this.environment.randomState
        private val constantsRate = this.environment.configuration.constantsRate
        private val operations = this.environment.operations

        override fun mutate(individual: Program<TProgram, TOutput>) {
            Diagnostics.debug("OperatorMicroMutation-start", mapOf(
                "individual" to individual
            ))

            val instruction = this.random.choice(individual.effectiveInstructions)

            // 4. If operator mutation then select a different instruction operation randomly
            val operation = this.random.choice(this.operations)

            // Assure that the arity of the new operation matches with the number of operands the instruction has.
            // If the arity of the operations is the same, then nothing needs to be done.
            if (instruction.operands.size > operation.arity.number) {
                // If we're going to a reduced arity instruction, we can just truncate the operands
                instruction.operands = instruction.operands.slice(0 until operation.arity.number).toMutableList()
            } else if (instruction.operands.size < operation.arity.number) {
                // Otherwise, if we're increasing the arity, just add random input
                // and calculation registers until the arity is met.
                while (instruction.operands.size < operation.arity.number) {
                    val register = if (this.random.nextDouble() < constantsRate) {
                        this.registerGenerator.next(RegisterType.Constant).first()
                    } else {
                        this.registerGenerator.next(
                            a = RegisterType.Input,
                            b = RegisterType.Calculation,
                            predicate = { this.random.nextDouble() < 0.5 }
                        ).first()
                    }

                    instruction.operands.add(register.index)
                }
            }

            instruction.operation = operation

            Diagnostics.debug("OperatorMicroMutation-end", mapOf(
                "individual" to individual
            ))
        }
    }

    /**
     * A [MutationStrategy] that will modify the constant of a random instruction in a [Program]
     *
     * The modification will determine all the instructions with a constant register and modify the constant
     * of the first instruction with the given [constantMutationFunction].
     *
     * @param environment An environment that the mutation is occurring in.
     * @property constantMutationFunction A function that can mutate values in the domain of [TProgram].
     */
    internal class ConstantMicroMutationStrategy<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
        environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
        private val constantMutationFunction: ConstantMutationFunction<TProgram>
    ) : MutationStrategy<TProgram, TOutput, TTarget>(environment) {

        private val random = this.environment.randomState

        override fun mutate(individual: Program<TProgram, TOutput>) {
            Diagnostics.debug("ConstantMicroMutation-start", mapOf(
                "individual" to individual
            ))

            // 5. If constant mutation then
            // (a) Randomly select an (effective) instruction with a constant c.
            // Unfortunately the way of searching for an instruction that uses a constant is not
            // particularly elegant, requiring a random search of the entire program.
            var instr = this.random.choice(individual.effectiveInstructions)

            var constantRegisters = instr.operands.filter { operand ->
                individual.registers.registerType(operand) == RegisterType.Constant
            }

            // Arbitrarily limit the search to avoid entering a potentially infinite search.
            var limit = 0

            while (constantRegisters.isEmpty() && limit++ < individual.effectiveInstructions.size) {
                instr = this.random.choice(individual.effectiveInstructions)

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
                // TODO: Should we pick a random constant register here?
                val register = constantRegisters.first()
                val oldValue = individual.registers[register]

                // Compute a new value using the current constant register value.
                val newValue = this.constantMutationFunction(oldValue)

                // Because each individual has its own register set, we can
                // just overwrite the constant register value.
                // TODO: Perhaps better to keep original constants for diversity?
                individual.registers.overwrite(register, newValue)
            }

            Diagnostics.debug("ConstantMicroMutation-end", mapOf(
                "individual" to individual
            ))
        }
    }
}