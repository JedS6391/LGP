package lgp.core.environment.dataset

/**
 * An attribute from some data set.
 *
 * @param T the type of the attribute.
 * @property name the name of the this attribute in the data set.
 * @property value the value of this attribute in the data set.
 */
data class Attribute<out T>(val name: String, val value: T)

/**
 * An instance in a [Dataset].
 *
 * [Instance]s are made up of a collection of [Attribute]s.
 *
 * @param T the type of the [Attribute]s that make up this instance.
 * @property attributes the collection of [Attribute]s that this instance represents.
 */
open class Instance<out T>(val attributes: List<Attribute<T>>) {
    // By default, the last attribute is the class attribute. We don't let consumers
    // of instances change the class attribute, it can only be changed globally at the
    // dataset level.
    internal var classAttributeIndex: Int = this.attributes.size - 1

    /**
     * Gets the class [Attribute] for this instance.
     *
     * @returns The class attribute.
     */
    fun classAttribute(): Attribute<T> {
        return this.attributes[classAttributeIndex]
    }

    /**
     * Gets the non-class attributes belonging to this instance.
     *
     * @returns The non-class attributes.
     */
    fun attributes(filterClassAttribute: Boolean = true): List<Attribute<T>> {
        return this.attributes.filterIndexed { index, _ ->
            // Filter the class attribute when the flag is set
            // !(a ∧ b) == (!a ∨ !b) by De Morgan's laws
            ((index != this.classAttributeIndex) || !filterClassAttribute)
        }
    }
}

/**
 * A basic data set made up of a collection of [Instance]s.
 *
 * @param T the type of the attributes that the instances in this data set represents.
 * @property instances the instances belonging to this dataset.
 */
abstract class Dataset<out T>(val instances: List<Instance<T>>) {

    private var classIndex: Int = 0

    /**
     * Sets the index of the class [Attribute] for each [Instance].
     *
     * @param index An index such that 0 <= idx < len(instances).
     */
    fun setClassAttribute(index: Int) {
        this.classIndex = index

        for (instance in this.instances) {
            instance.classAttributeIndex = this.classIndex
        }
    }

    /**
     * The number of instances in this data set.
     *
     * @returns The number of instances in this data set.
     */
    fun numInstances(): Int {
        return this.instances.size
    }

    /**
     * The number of attributes in each instance in the data set.
     *
     * @returns The number of attributes in each instance in the data set.
     */
    fun numAttributes(): Int {
        return when {
            // Protect for the case when there are no instances (and hence no attributes).
            this.numInstances() == 0 -> 0
            else -> this.instances[0].attributes.size
        }
    }
}