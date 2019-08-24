package nz.co.jedsimson.lgp.core.evolution.fitness

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Dataset
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program

/**
 * An evaluation of a program on a set of fitness cases.
 *
 * @property fitness The fitness of the program as determined by the fitness function on the cases given by a fitness context.
 * @property individual The program that this evaluation is related to.
 */
data class Evaluation<TProgram, TOutput : Output<TProgram>>(
    val fitness: Double,
    val individual: Program<TProgram, TOutput>
)

/**
 * Provides a way to evaluate the fitness of a program.
 *
 * This is done by using the [FitnessContext] that is register with the [EnvironmentFacade].
 *
 * @param TData The type of the program being evaluated.
 * @param TOutput The type of output of the program being evaluated.
 * @param TTarget The type of target for fitness cases being evaluated against.
 * @property environment Provides access to the current environment.
 * @constructor Creates a new [FitnessEvaluator] with the given [environment].
 */
class FitnessEvaluator<TData, TOutput : Output<TData>, TTarget : Target<TData>>(
    private val environment: EnvironmentFacade<TData, TOutput, TTarget>
) {

    /**
     * Performs an evaluation on [program] with the specified [environment] against the given [dataset].
     *
     * The [FitnessEvaluator] acts as a mediator between something that wants to know the fitness of a [Program]
     * and the actual [FitnessContext] that fitness is calculated in.
     *
     * @param program The program to determine an evaluation for.
     * @returns An evaluation of the program.
     */
    fun evaluate(
        program: Program<TData, TOutput>,
        dataset: Dataset<TData, TTarget>
    ): Evaluation<TData, TOutput> {

        // Request access to a fitness context implementation.
        val context = this.environment.moduleFactory.instance<FitnessContext<TData, TOutput, TTarget>>(
            CoreModuleType.FitnessContext
        )

        val fitnessCases = dataset.inputs.zip(dataset.outputs).map { (features, target) ->
            FitnessCase(features, target)
        }

        // Use the context to evaluate this programs fitness
        val fitness = context.fitness(
            program = program,
            fitnessCases = fitnessCases
        )

        return Evaluation(fitness, program)
    }
}