package nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.EffectiveCalculationRegisterResolvers
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategy
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategyFactory
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program

private enum class MacroMutationType {
    Insertion,
    Deletion
}

/**
 * Responsible for creating [MutationStrategy] instances to be used to perform macro-mutations.
 *
 * @param environment An environment that the mutation is occurring in.
 * @param insertionRate The rate with which instructions should be inserted.
 * @param deletionRate The rate with which instructions should be deleted.
 */
internal class MacroMutationStrategyFactory<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    private val environment: EnvironmentFacade<TProgram, TOutput, TTarget>,
    private val insertionRate: Double,      // p_ins
    private val deletionRate: Double        // p_del
) : MutationStrategyFactory<TProgram, TOutput, TTarget>() {

    private val random = this.environment.randomState
    private val minimumProgramLength = this.environment.configuration.minimumProgramLength
    private val maximumProgramLength = this.environment.configuration.maximumProgramLength

    override fun getStrategyForIndividual(
        individual: Program<TProgram, TOutput>
    ): MutationStrategy<TProgram, TOutput, TTarget> {
        val programLength = individual.instructions.size

        //  Randomly select macro mutation type (insertion or deletion)
        // with some probability (as defined by p_ins/p_del).
        val mutationType = if (random.nextDouble() < this.insertionRate) {
            MacroMutationType.Insertion
        } else {
            MacroMutationType.Deletion
        }

        if (programLength < maximumProgramLength &&
                (mutationType == MacroMutationType.Insertion || programLength == minimumProgramLength)) {
            // We only insert instructions when we haven't hit the maximum AND we need to insert (either
            // because that is the selected mutation type or we have a minimum length program.
            return MacroMutationStrategies.MacroMutationInsertionStrategy(this.environment, EffectiveCalculationRegisterResolvers::baseResolver)
        }
        else if (programLength > minimumProgramLength &&
                (mutationType == MacroMutationType.Deletion || programLength == maximumProgramLength)) {
            // Similar criteria as for insert, only the lengths are switched.
            return MacroMutationStrategies.MacroMutationDeletionStrategy(this.environment)
        }

        // This shouldn't happen.
        throw IllegalArgumentException(
            "The provided individual is not suitable for any of the built-in macro-mutation strategies."
        )
    }
}