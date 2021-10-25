package nz.co.jedsimson.lgp.core.evolution.fitness

import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.program.Outputs
import kotlin.math.abs
import kotlin.math.sqrt

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
    val MAE: SingleOutputFitnessFunction<Double> = object : SingleOutputFitnessFunction<Double>() {

        override fun fitness(outputs: List<Outputs.Single<Double>>, cases: List<FitnessCase<Double, Targets.Single<Double>>>): Double {
            val fitness = cases.zip(outputs).map { (case, actual) ->
                val expected = case.target.value

                abs(actual.value - expected)
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
    val SSE: SingleOutputFitnessFunction<Double> = object : SingleOutputFitnessFunction<Double>() {

        override fun fitness(outputs: List<Outputs.Single<Double>>, cases: List<FitnessCase<Double, Targets.Single<Double>>>): Double {
            val fitness = cases.zip(outputs).map { (case, actual) ->
                val expected = case.target.value

                Math.pow((actual.value - expected), 2.0)
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
    val MSE: SingleOutputFitnessFunction<Double> = object : SingleOutputFitnessFunction<Double>() {

        override fun fitness(outputs: List<Outputs.Single<Double>>, cases: List<FitnessCase<Double, Targets.Single<Double>>>): Double {
            val fitness = cases.zip(outputs).map { (case, actual) ->
                val expected = case.target.value

                Math.pow((actual.value - expected), 2.0)
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
    val RMSE: SingleOutputFitnessFunction<Double> = object : SingleOutputFitnessFunction<Double>() {

        override fun fitness(outputs: List<Outputs.Single<Double>>, cases: List<FitnessCase<Double, Targets.Single<Double>>>): Double {
            val fitness = cases.zip(outputs).map { (case, actual) ->
                val expected = case.target.value

                Math.pow((actual.value - expected), 2.0)
            }.sum()

            val mse = ((1.0 / cases.size.toDouble()) * fitness)

            return when {
                fitness.isFinite() -> sqrt(mse)
                else               -> UNDEFINED_FITNESS
            }
        }
    }

    /**
     * Classification error fitness function implementation.
     *
     * @suppress
     */
    private class ClassificationError(val mappingFunction: (Double) -> Double) : SingleOutputFitnessFunction<Double>() {

        override fun fitness(outputs: List<Outputs.Single<Double>>, cases: List<FitnessCase<Double, Targets.Single<Double>>>): Double {
            return cases.zip(outputs).map { (case, output) ->
                val expected = case.target.value
                val actual = this.mappingFunction(output.value)

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
    val CE: (mappingFunction: (Double) -> Double) -> SingleOutputFitnessFunction<Double> = { mappingFunction ->
        ClassificationError(mappingFunction)
    }

    /**
     * Threshold Classification error fitness function implementation.
     *
     * @suppress
     */
    private class ThresholdClassificationError(val threshold: Double) : SingleOutputFitnessFunction<Double>() {

        override fun fitness(outputs: List<Outputs.Single<Double>>, cases: List<FitnessCase<Double, Targets.Single<Double>>>): Double {
            return cases.zip(outputs).filter { (case, output) ->
                val expected = case.target.value

                // Program is correct when the distance between the actual
                // and expected values is within some threshold.
                abs(output.value - expected) > this.threshold
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
    val thresholdCE: (threshold: Double) -> SingleOutputFitnessFunction<Double> = { threshold ->
        ThresholdClassificationError(threshold)
    }
}
