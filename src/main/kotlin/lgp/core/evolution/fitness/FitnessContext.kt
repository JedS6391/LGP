package lgp.core.evolution.fitness

import lgp.core.environment.dataset.Instance
import lgp.core.evolution.population.Program

// Maybe wrap with a data class instead?
typealias FitnessCase<T> = Instance<T>

/**
 * A mapping of program to fitness cases using a given fitness function.
 *
 * When a context is asked for the fitness of the program it encapsulates,
 * it will execute the program on each of the fitness cases, and collect the
 * program outputs.
 *
 * These program outputs are feed into a fitness function to determine a fitness
 * metric for that program in the given context.
 *
 * @property fitnessCases A collection of fitness cases to evaluate the program on.
 * @property program A program to evaluate.
 * @property fitnessFunction A function to determine the fitness of a program by its outputs.
 */
class FitnessContext<T>(
        /*
         * Fitness cases are just instances in a dataset which will be loaded into
         * the program as a set of input registers, and the output of the program
         * compared to the output attribute of the fitness case by the fitness
         * function of the given context.
         */
        private val fitnessCases: List<FitnessCase<T>>,
        private val program: Program<T>,

        // TODO: Is this function type enough?
        private val fitnessFunction: FitnessFunction<T>
)
{

    /**
     * Returns the fitness as determined by this context.
     *
     * @returns A double value as returned by the fitness function.
     */
    fun fitness(): Double {
        // Collect the results of the program for each fitness case.
        val outputs: List<T> = this.fitnessCases.map { case ->
            // Make sure the registers are in a default state
            this.program.registers.reset()

            // Load the case
            this.program.registers.writeInstance(case)

            // Run the program...
            this.program.execute()

            // ... and gather a result from register zero
            // TODO: Make this configurable
            // TODO: How to handle multiple outputs?
            this.program.registers.read(0)
        }

        return this.fitnessFunction(outputs, this.fitnessCases)
    }
}
