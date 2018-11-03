package lgp.core.evolution.fitness

import lgp.core.environment.CoreModuleType
import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.program.Output
import lgp.core.program.Program

/**
 * An evaluation of a program on a set of fitness cases.
 *
 * @param fitness The fitness of the program as determined by the fitness function on the cases given by a fitness context.
 */
data class Evaluation<TProgram, TOutput : Output<TProgram>>(
    val fitness: Double,
    val individual: Program<TProgram, TOutput>
)

/**
 * Provides a way to evaluate the fitness of a program.
 *
 * This is done by using the [FitnessContext] that is register with the [Environment].
 *
 * @param TData The type of the program being evaluated.
 */
class FitnessEvaluator<TData, TOutput : Output<TData>> {

    /**
     * Performs an evaluation on [program] with the specified [environment] through a fitness context.
     *
     * The fitness is evaluated on [dataset].
     *
     * @param program The program to determine an evaluation for.
     * @param environment An environment that was used to build the program given.
     * @returns An evaluation of the program.
     */
    fun evaluate(
        program: Program<TData, TOutput>,
        dataset: Dataset<TData>,
        environment: Environment<TData, TOutput>
    ): Evaluation<TData, TOutput> {

        // Request access to a fitness context implementation.
        val context: FitnessContext<TData, TOutput> = environment.registeredModule(CoreModuleType.FitnessContext)

        // Use the context to evaluate this programs fitness
        val fitness = context.fitness(
                program = program,
                fitnessCases = dataset.inputs.zip(dataset.outputs).map { (features, target) ->
                    FitnessCase(features, target)
                }
        )

        return Evaluation(
                fitness = fitness,
                individual = program
        )
    }
}