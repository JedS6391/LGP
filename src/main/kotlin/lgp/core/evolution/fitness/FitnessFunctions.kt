package lgp.core.evolution.fitness

/**
 * Computes the fitness of an individual program on a set of input-output examples.
 *
 * The fitness value should always be a simple double but the way in which the value
 * is determined can be customised depending on the type of program/fitness cases.
 *
 * A fitness function is really just a function that maps a set of program outputs
 * with a set of examples in some way.
 */
typealias FitnessFunction<T> = (List<T>, List<FitnessCase<T>>) -> Double

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
    fun MSE(): FitnessFunction<Double> = { outputs, cases ->
        val fitness = cases.zip(outputs).map { (case, actual) ->
            val expected = case.classAttribute().value

            Math.pow((actual - expected), 2.0)
        }.sum()

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
        val ce: FitnessFunction<Double> = { outputs, cases ->
            cases.zip(outputs).map { (case, output) ->
                val expected = case.classAttribute().value
                val actual = mappingFunction(output)

                if (actual != expected) 1.0 else 0.0
            }.sum()
        }

        return ce
    }
}
