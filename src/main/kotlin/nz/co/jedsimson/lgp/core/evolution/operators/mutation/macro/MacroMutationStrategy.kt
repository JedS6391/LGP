package nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.choice
import nz.co.jedsimson.lgp.core.environment.randInt
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.EffectiveCalculationRegisterResolver
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategy
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.InstructionGenerator

/**
 * A collection of [MutationStrategy] implementations for performing macro-mutations.
 */
internal object MacroMutationStrategies {

    /**
     * A [MutationStrategy] that can insert random instructions into a [Program].
     *
     * @param environment An environment that the mutation is occurring in.
     */
    internal class MacroMutationInsertionStrategy<TProgram, TOutput : Output<TProgram>>(
            private val environment: EnvironmentFacade<TProgram, TOutput>,
            private val effectiveCalculationRegisterResolver: EffectiveCalculationRegisterResolver<TProgram, TOutput>
    ) : MutationStrategy<TProgram, TOutput>() {

        private val random = this.environment.randomState
        private val instructionGenerator = this.environment.moduleFactory.instance<InstructionGenerator<TProgram, TOutput>>(
            CoreModuleType.InstructionGenerator
        )

        override fun mutate(individual: Program<TProgram, TOutput>) {
            val programLength = individual.instructions.size

            // 2. Randomly select an instruction at a position i (mutation point) in program gp.
            val mutationPoint = random.randInt(0, programLength - 1)

            // We can avoid running algorithm 3.1 like in the literature by
            // just searching for effective calculation registers and making
            // sure we choose an effective register for our mutation
            val effectiveRegisters = this.effectiveCalculationRegisterResolver(individual, mutationPoint)

            // Can only perform a mutation if there is an effective register to choose from.
            if (effectiveRegisters.isNotEmpty()) {
                val instruction = this.instructionGenerator.generateInstruction()
                val destinationRegister = random.choice(effectiveRegisters)

                instruction.destination = destinationRegister

                individual.instructions.add(mutationPoint, instruction)
            }
        }
    }

    /**
     * A [MutationStrategy] that can delete random instructions from a [Program].
     *
     * @param environment An environment that the mutation is occurring in.
     */
    internal class MacroMutationDeletionStrategy<TProgram, TOutput : Output<TProgram>>(
        private val environment: EnvironmentFacade<TProgram, TOutput>
    ) : MutationStrategy<TProgram, TOutput>() {

        private val random = this.environment.randomState

        override fun mutate(individual: Program<TProgram, TOutput>) {
            // 4.
            // (a) Select an effective instruction i (if existent)
            if (individual.effectiveInstructions.isNotEmpty()) {
                val instruction = random.choice(individual.effectiveInstructions)

                // (b) Delete instruction i
                individual.instructions.remove(instruction)
            }
        }
    }
}

