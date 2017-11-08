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

    // Large value for fitness when either a program is un-evaluated or
    // the fitness computation overflows.
    const val UNDEFINED_FITNESS: Double = 10e9

    /**
     * Mean absolute error fitness function.
     *
     * Simply sums up the absolute differences between the outputs and takes the mean.
     */
    @JvmStatic
    fun MAE(): FitnessFunction<Double> = { outputs, cases ->
        val fitness = cases.zip(outputs).map { (case, actual) ->
            val expected = case.target

            Math.abs(actual - expected)
        }.sum()

        when {
            fitness.isFinite() -> fitness / outputs.size.toDouble()
            else               -> UNDEFINED_FITNESS
        }
    }

    /**
     * Sum-of-squared errors fitness function.
     *
     * Simply calculates the sum of the squared error between the actual and expected outputs.
     */
    @JvmStatic
    fun SSE(): FitnessFunction<Double> = { outputs, cases ->
        val fitness = cases.zip(outputs).map { (case, actual) ->
            val expected = case.target

            Math.pow((actual - expected), 2.0)
        }.sum()

        when {
            fitness.isFinite() -> fitness
            else               -> UNDEFINED_FITNESS
        }
    }

    /**
     * Mean-Squared Error fitness function for programs that operate on doubles.
     *
     * Calculates the sum of squared errors and then takes the mean.
     */
    @JvmStatic
    fun MSE(): FitnessFunction<Double> = { outputs, cases ->
        val fitness = cases.zip(outputs).map { (case, actual) ->
            val expected = case.target

            Math.pow((actual - expected), 2.0)
        }.sum()

        when {
            fitness.isFinite() -> ((1.0 / cases.size.toDouble()) * fitness)
            else               -> UNDEFINED_FITNESS
        }
    }

    /**
     * Root Mean-Squared Error fitness function for programs that operate on doubles.
     *
     * Essentially operates by computing the MSE, then taking the square-root of the result.
     */
    @JvmStatic
    fun RMSE(): FitnessFunction<Double> = { outputs, cases ->
        val fitness = cases.zip(outputs).map { (case, actual) ->
            val expected = case.target

            Math.pow((actual - expected), 2.0)
        }.sum()

        val mse = ((1.0 / cases.size.toDouble()) * fitness)

        when {
            fitness.isFinite() -> Math.sqrt(mse)
            else               -> UNDEFINED_FITNESS
        }
    }

    /**
     * Classification Error fitness function for programs that operate on doubles.
     *
     * @param mappingFunction A function that maps the continuous program output to a discrete class value.
     */
    @JvmStatic
    fun CE(mappingFunction: (Double) -> Double): FitnessFunction<Double> {
        val ce: FitnessFunction<Double> = { outputs, cases ->
            cases.zip(outputs).map { (case, output) ->
                val expected = case.target
                val actual = mappingFunction(output)

                if (actual != expected) 1.0 else 0.0
            }.sum()
        }

        return ce
    }

    /**
     * Threshold Classification Error fitness function.
     *
     * This fitness function works by counting the number of outputs which have
     * an error difference greater than [threshold]. Error differences which fall
     * beneath [threshold] are considered to be "good" solutions.
     *
     * @param threshold The threshold for differentiating good/bad solutions.
     */
    @JvmStatic
    fun thresholdCE(threshold: Double): FitnessFunction<Double> {
        val ce: FitnessFunction<Double> = { outputs, cases ->
            cases.zip(outputs).filter { (case, output) ->
                val expected = case.target
                val actual = output

                // Program is correct when the distance between the actual
                // and expected values is within some threshold.
                Math.abs(actual - expected) > threshold
            }.count().toDouble()
        }

        return ce
    }
}
