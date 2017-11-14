package lgp.core.evolution.fitness

/**
 * Provides the functionality to compute the fitness of an individual program on a set of input-output examples.
 *
 * The fitness value should always be a simple double but the way in which the value
 * is determined can be customised depending on the type of program/fitness cases.
 *
 * A fitness function is really just a function that maps a set of program outputs
 * with a set of examples in some way. It is encapsulated in a class to make the interaction
 * with it slightly nicer and more straightforward (especially when used from Java).
 *
 * An implementation of the class can be directly queried for fitness using the `()` operator.
 */
abstract class FitnessFunction<in T> {

    /**
     * Computes the fitness based on the given [outputs] and [cases].
     *
     * @param outputs A set of predicted program outputs.
     * @param cases A set of expected outputs.
     * @return A double value that represents the error measure between the predicted/expected outputs.
     */
    abstract fun fitness(outputs: List<T>, cases: List<FitnessCase<T>>): Double

    /**
     * Allows [fitness] to be called directly using `()` syntax (e.g. `fitnessFunctionInstance(outputs, cases)`).
     */
    operator fun invoke(outputs: List<T>, cases: List<FitnessCase<T>>): Double {
        return this.fitness(outputs, cases)
    }
}

/**
 * A collection of standard fitness functions.
 *
 * Custom fitness functions can be defined by creating any function with a type of [FitnessFunction].
 */
object FitnessFunctions {

    /**
     * Large constant value for fitness in the cases when either a program is
     * un-evaluated or the fitness computation overflows.
     */
    const val UNDEFINED_FITNESS: Double = 10e9

    /**
     * Mean absolute error fitness function.
     *
     * Simply sums up the absolute differences between the outputs and takes the mean.
     */
    @JvmStatic
    val MAE: FitnessFunction<Double> = object : FitnessFunction<Double>() {

        override fun fitness(outputs: List<Double>, cases: List<FitnessCase<Double>>): Double {
            val fitness = cases.zip(outputs).map { (case, actual) ->
                val expected = case.target

                Math.abs(actual - expected)
            }.sum()

            return when {
                fitness.isFinite() -> fitness / outputs.size.toDouble()
                else               -> UNDEFINED_FITNESS
            }
        }
    }

    /**
     * Sum-of-squared errors fitness function.
     *
     * Simply calculates the sum of the squared error between the actual and expected outputs.
     */
    @JvmStatic
    val SSE: FitnessFunction<Double> = object : FitnessFunction<Double>() {

        override fun fitness(outputs: List<Double>, cases: List<FitnessCase<Double>>): Double {
            val fitness = cases.zip(outputs).map { (case, actual) ->
                val expected = case.target

                Math.pow((actual - expected), 2.0)
            }.sum()

            return when {
                fitness.isFinite() -> fitness
                else               -> UNDEFINED_FITNESS
            }
        }
    }

    /**
     * Mean-Squared Error fitness function for programs that operate on doubles.
     *
     * Calculates the sum of squared errors and then takes the mean.
     */
    @JvmStatic
    val MSE: FitnessFunction<Double> = object : FitnessFunction<Double>() {

        override fun fitness(outputs: List<Double>, cases: List<FitnessCase<Double>>): Double {
            val fitness = cases.zip(outputs).map { (case, actual) ->
                val expected = case.target

                Math.pow((actual - expected), 2.0)
            }.sum()

            return when {
                fitness.isFinite() -> ((1.0 / cases.size.toDouble()) * fitness)
                else               -> UNDEFINED_FITNESS
            }
        }
    }

    /**
     * Root Mean-Squared Error fitness function for programs that operate on doubles.
     *
     * Essentially operates by computing the MSE, then taking the square-root of the result.
     */
    @JvmStatic
    val RMSE: FitnessFunction<Double> = object : FitnessFunction<Double>() {

        override fun fitness(outputs: List<Double>, cases: List<FitnessCase<Double>>): Double {
            val fitness = cases.zip(outputs).map { (case, actual) ->
                val expected = case.target

                Math.pow((actual - expected), 2.0)
            }.sum()

            val mse = ((1.0 / cases.size.toDouble()) * fitness)

            return when {
                fitness.isFinite() -> Math.sqrt(mse)
                else               -> UNDEFINED_FITNESS
            }
        }
    }

    /**
     * Classification error fitness function implementation.
     *
     * @suppress
     */
    private class ClassificationError(val mappingFunction: (Double) -> Double) : FitnessFunction<Double>() {

        override fun fitness(outputs: List<Double>, cases: List<FitnessCase<Double>>): Double {
            return cases.zip(outputs).map { (case, output) ->
                    val expected = case.target
                    val actual = this.mappingFunction(output)

                    if (actual != expected) 1.0 else 0.0
                }.sum()
        }
    }

    /**
     * Classification Error fitness function for programs that operate on doubles.
     *
     * This function requires a function that maps the continuous program output to a discrete class value,
     * as specified by the [mappingFunction] parameter.
     */
    @JvmStatic
    val CE: (mappingFunction: (Double) -> Double) -> FitnessFunction<Double> = { mappingFunction ->
        ClassificationError(mappingFunction)
    }

    /**
     * Threshold Classification error fitness function implementation.
     *
     * @suppress
     */
    private class ThresholdClassificationError(val threshold: Double) : FitnessFunction<Double>() {

        override fun fitness(outputs: List<Double>, cases: List<FitnessCase<Double>>): Double {
            return cases.zip(outputs).filter { (case, output) ->
                val expected = case.target
                val actual = output

                // Program is correct when the distance between the actual
                // and expected values is within some threshold.
                Math.abs(actual - expected) > this.threshold
            }.count().toDouble()
        }
    }

    /**
     * Threshold Classification Error fitness function.
     *
     * This fitness function works by counting the number of outputs which have
     * an error difference greater than a given threshold. Error differences which fall
     * beneath the threshold are considered to be "good" solutions.
     *
     * The field expects to be passed a threshold for differentiating good/bad solutions,
     * specified through the [threshold] parameter.
     */
    @JvmStatic
    val thresholdCE: (threshold: Double) -> FitnessFunction<Double> = { threshold ->
        ThresholdClassificationError(threshold)
    }
}
