package lgp.core.evolution.fitness

import lgp.core.environment.dataset.Instance
import lgp.core.evolution.population.Program

// Maybe wrap with a data class instead?
typealias FitnessCase<T> = Instance<T>

/**
 * A mapping of program to fitness cases using a given fitness function.
 *
 * Used to evaluate the fitness of a program on a collection of fitness cases.
 */
class FitnessContext<T>(
        /**
         * A collection of fitness cases to evaluate the program on.
         *
         * Fitness cases are just instances in a dataset which will be loaded into
         * the program as a set of input registers, and the output of the program
         * compared to the output attribute of the fitness case by the fitness
         * function of the given context.
         */
        private val fitnessCases: List<FitnessCase<T>>,

        /**
         * A program that the fitness cases are evaluated on.
         */
        private val program: Program<T>,

        // TODO: Is this function type enough?
        /**
         * A function that tests the given program on the given fitness cases.
         */
        private val fitnessFunction: FitnessFunction<T>
)
{

    /**
     * Returns the fitness as determined by this context.
     *
     * @returns A double value as returned by the fitness function.
     */
    fun fitness(): Double {
        return this.fitnessFunction(this.program, this.fitnessCases)
    }
}
