package lgp.core.environment.dataset

/**
 * A feature of a sample in some data set.
 *
 * @param TFeature The type of the feature.
 * @property name The name of the this feature in the data set.
 * @property value The value of this feature in the data set.
 */
open class Feature<out TFeature>(val name: String, val value: TFeature) {

    override fun toString(): String {
        return "Feature(name = ${this.name}, value = ${this.value})"
    }
}

class NominalFeature<out TFeature>(name: String, value: TFeature, val labels: List<String>)
    : Feature<TFeature>(name, value) {

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
class Sample<out TFeature>(val features: List<Feature<TFeature>>) {

    fun feature(name: String): Feature<TFeature> {
        return this.features.filter { feature ->
            feature.name == name
        }.first()
    }

    override fun toString(): String {
        return "Sample(features = ${this.features})"
    }
}

class InvalidNumberOfSamplesException(message: String) : Exception(message)

/**
 * A basic data set composed of a vector of input [Sample]s and a collection of outputs.
 *
 * **NOTE:** The type of the inputs and outputs is constrained to be the same.
 *
 * @param TData The type of the features in the input vector and the type of the outputs.
 * @property inputs Vector of inputs with the shape [numSamples, numFeatures]
 * @property outputs Vector that describes target values for each sample in the input vector, with shape [numSamples].
 */
open class Dataset<out TData>(
        val inputs: List<Sample<TData>>,
        val outputs: List<TData>
) {

    init {
        // Ensure that the number of samples in the input vector
        // matches the number of output samples given.
        if (inputs.size != outputs.size)
            throw InvalidNumberOfSamplesException(
                    "Number of samples in the input and output vectors should be equal " +
                            "(inputs = ${inputs.size}, outputs = ${outputs.size})"
            )
    }

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
}