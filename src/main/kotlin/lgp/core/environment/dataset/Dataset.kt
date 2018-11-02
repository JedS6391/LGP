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
        return this.features.first { feature ->
            feature.name == name
        }
    }

    override fun toString(): String {
        return "Sample(features = ${this.features})"
    }
}

interface Target<out TTarget> {
    public val size: Int
}

object Targets {
    class Single<TData>(val value: TData) : Target<TData> {
        override val size = 1
    }

    class Multiple<TData>(val values: List<TData>): Target<TData> {
        override val size = values.size
    }
}

class InvalidNumberOfSamplesException(message: String) : Exception(message)

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
    fun numOutputs(): Int {
        return when {
            this.outputs.isEmpty() -> 0
            else                   -> this.outputs.first().size
        }
    }
}