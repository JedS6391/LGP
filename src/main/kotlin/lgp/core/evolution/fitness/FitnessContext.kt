package lgp.core.evolution.fitness

import lgp.core.environment.dataset.Sample
import lgp.core.evolution.population.Program

/**
 * A case to evaluate a programs fitness on.
 *
 * @param TData The type of data the features and target value represent.
 * @property features A sampling of features from a data set.
 * @property target The target value for this cases set of features.
 */
data class FitnessCase<out TData>(val features: Sample<TData>, val target: TData)

/**
 * A mapping of program to fitness cases using a given fitness function.
 *
 * When a context is requested for the fitness of the program it encapsulates,
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
class FitnessContext<TData>(
        /*
         * Fitness cases are just samples in a data set which will be loaded into
         * the program as a set of input registers, and the output of the program
         * compared to the output attribute of the fitness case by the fitness
         * function of the given context.
         */
        private val fitnessCases: List<FitnessCase<TData>>,
        private val program: Program<TData>,
        private val fitnessFunction: FitnessFunction<TData>
)
{

    /**
     * Returns the fitness as determined by this context.
     *
     * @returns A double value as returned by the fitness function.
     */
    fun fitness(): Double {
        // Make sure the programs effective instructions have been found
        this.program.findEffectiveProgram()

        // Collect the results of the program for each fitness case.
        val outputs = this.fitnessCases.map { case ->
            // Make sure the registers are in a default state
            this.program.registers.reset()

            // Load the case
            this.program.registers.writeInstance(case.features)

            // Run the program...
            this.program.execute()

            // ... and gather a result from register zero
            // TODO: Make this configurable
            // TODO: How to handle multiple outputs?
            this.program.registers.read(this.program.outputRegisterIndex)
        }

        // Copy the fitness to the program for later accesses
        this.program.fitness = this.fitnessFunction(outputs, this.fitnessCases)

        return this.program.fitness
    }
}
