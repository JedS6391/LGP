package lgp.core.evolution.fitness

import lgp.core.environment.Environment
import lgp.core.environment.dataset.Dataset
import lgp.core.evolution.population.Program

/**
 * An evaluation of a program on a set of fitness cases.
 *
 * @param fitness The fitness of the program as determined by the fitness function on the cases given by a fitness context.
 */
data class Evaluation<T>(val fitness: Double, val individual: Program<T>)

/**
 * Provides a way to evaluate the fitness of a program.
 *
 * @param TData The type of the program being evaluated.
 */
class FitnessEvaluator<TData> {
    /**
     * Performs an evaluation on the program given with the specified environment by building a fitness context.
     *
     * The fitness is evaluated using the data set given.
     *
     * @param program The program to determine an evaluation for.
     * @param environment An environment that was used to build the program given.
     * @returns An evaluation of the program.
     */
    fun evaluate(
            program: Program<TData>,
            dataset: Dataset<TData>,
            environment: Environment<TData>
    ): Evaluation<TData> {

        // Build a fitness context for this program
        val context = FitnessContext(
                program = program,
                fitnessCases = dataset.inputs.zip(dataset.outputs).map { (features, target) ->
                    FitnessCase(features, target)
                },
                fitnessFunction = environment.fitnessFunction // User specified fitness function
        )

        return Evaluation(
                fitness = context.fitness(),
                individual = program
        )
    }
}