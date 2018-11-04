package lgp.core.environment.dataset

/**
 * A feature of a sample in some data set.
 *
 * @param TData The type of the feature.
 * @property name The name of the this feature in the data set.
 * @property value The value of this feature in the data set.
 */
open class Feature<out TData>(val name: String, val value: TData) {

    override fun toString(): String {
        return "Feature(name = ${this.name}, value = ${this.value})"
    }
}

class NominalFeature<out TData>(name: String, value: TData, val labels: List<String>)
    : Feature<TData>(name, value) {

    override fun toString(): String {
        return "NominalFeature(name = ${this.name}, value = ${this.value}, labels = ${this.labels})"
    }
}

/**
 * A sample in a [Dataset] made up of a collection of [Feature]s.
 *
 * @param TFeature The type of the [Feature]s that make up this sample.
 * @property features A collection of [Feature]s that this sample represents.
 * @see [Dataset]
 */
class Sample<out TData>(val features: List<Feature<TData>>) {

    fun feature(name: String): Feature<TData> {
        return this.features.first { feature ->
            feature.name == name
        }
    }

    override fun toString(): String {
        return "Sample(features = ${this.features})"
    }
}

/**
 * A target output in a [Dataset].
 *
 * An interface is provided to cater for scenarios with differing output requirements (e.g. single vs multiple).
 *
 * @param TData The type of the value(s) this target represents.
 */
interface Target<out TData> {

    /**
     * How many output values this target represents.
     */
    val size: Int
}

/**
 * A collection of built-in [Target] implementations
 */
object Targets {

    /**
     * Represents a target with a single output value.
     *
     * @property value The output value this target represents.
     */
    class Single<TData>(val value: TData) : Target<TData> {
        override val size = 1
    }

    /**
     * Represents a target with multiple output values.
     *
     * @property values The output values this target represents.
     */
    class Multiple<TData>(val values: List<TData>): Target<TData> {
        override val size = values.size
    }
}

class InvalidNumberOfSamplesException(message: String) : Exception(message)

/**
 * A basic data set composed of a vector of input [Sample]s and a collection of output [Target]s.
 *
 * **NOTE:** The type of the inputs and outputs is constrained to be the same.
 *
 * @param TData The type of the features in the input vector and the type of the outputs.
 * @property inputs Vector of inputs with the shape [numSamples, numFeatures]
 * @property outputs Vector that describes target values for each sample in the input vector, with shape [numSamples].
 */
class Dataset<out TData>(
    val inputs: List<Sample<TData>>,
    val outputs: List<Target<TData>>
) {
    /**
     * The number of features of each sample in the input data set.
     *
     * @returns The number of features of each sample.
     */
    fun numFeatures(): Int {
        return when {
            // Generally won't be true, but we'll protect against the exception anyway.
            this.inputs.isEmpty() -> 0
            else                  -> this.inputs.first().features.size
        }
    }

    /**
     * The number of samples in the input data set.
     *
     * @returns The number of samples in this data set.
     */
    fun numSamples(): Int {
        return this.inputs.size
    }

    /**
     * The number of outputs in the data set, which is always one for this type of data set.
     *
     * @returns The number of outputs in this data set.
     */
    fun numTargets(): Int {
        return when {
            this.outputs.isEmpty() -> 0
            else                   -> this.outputs.first().size
        }
    }
}