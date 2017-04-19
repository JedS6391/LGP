package lgp.core.evolution.fitness

import lgp.core.evolution.population.Program

/**
 * Computes the fitness of an individual program on a set of input-output examples.
 *
 * The fitness value should always be a simple double but the way in which the value
 * is determined can be customised depending on the type of program/fitness cases.
 */
typealias FitnessFunction<T> = (Program<T>, List<FitnessCase<T>>) -> Double

/**
 * A collection of standard fitness functions.
 *
 * Custom fitness functions can be defined by creating any function with a type of [FitnessFunction].
 */
object FitnessFunctions {

    /**
     * Mean-Squared Error fitness function for programs that operate on doubles.
     *
     * Evaluates a program on a set of fitness cases using mean-squared error
     * on the actual and expected values. The actual value is whatever is in
     * register 0 after program execution.
     */
    @JvmStatic
    fun MSE(): FitnessFunction<Double> = { program, cases ->
        var fitness = 0.0

        for (case in cases) {
            // Make sure the registers are in a default state
            program.registers.reset()

            // Load the case
            program.registers.writeInstance(case)

            program.execute()

            // Just use the first register as output
            val actual: Double = program.registers.read(0)
            val expected: Double = case.classAttribute().value

            fitness += Math.pow((actual - expected), 2.0)
        }

        ((1.0 / cases.size.toDouble()) * fitness)
    }

    /**
     * Classification Error fitness function for programs that operate on doubles.
     *
     * Evaluates a program on a set of fitness cases using classification error
     * on the actual and expected class values. The actual value is whatever is in
     * register 0 after program execution mapped using the mapping function given.
     *
     * @param mappingFunction A function that maps the continuous program output to a discrete class value.
     */
    @JvmStatic
    fun CE(mappingFunction: (Double) -> Double): FitnessFunction<Double> {
        val ce: FitnessFunction<Double> = { program, cases ->
            var fitness = 0.0

            for (case in cases) {
                // TODO: Seem to repeat this logic. Perhaps it could be a property of fitness functions?
                // Make sure the registers are in a default state
                program.registers.reset()

                // Load the case
                program.registers.writeInstance(case)

                program.execute()

                // Just use the first register as output, mapping it to the domain of class values.
                val actual: Double = mappingFunction(program.registers.read(0))
                val expected: Double = case.classAttribute().value

                fitness += if (actual != expected) 1 else 0
            }

            fitness
        }

        return ce
    }
}
