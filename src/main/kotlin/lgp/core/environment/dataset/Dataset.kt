package lgp.core.environment.dataset

/**
 * An attribute of an instance in some data set.
 *
 * @param T The type of the attribute.
 * @property name The name of the this attribute in the data set.
 * @property value The value of this attribute in the data set.
 */
data class Attribute<out T>(val name: String, val value: T)

/**
 * An instance in a [Dataset].
 *
 * [Instance]s are made up of a collection of [Attribute]s.
 *
 * **NOTE:** Similarly as for [Dataset]s, custom implementations of instances can be
 * defined but some functionality is marked so that it is not able to be overridden (see
 * any functions marked `final`).
 *
 *
 * @param TAttribute The type of the [Attribute]s that make up this instance.
 * @property attributes A collection of [Attribute]s that this instance represents.
 * @see [Dataset]
 */
open class Instance<out TAttribute>(val attributes: List<Attribute<TAttribute>>) {
    // By default, the last attribute is the class attribute. We don't let consumers
    // of instances change the class attribute, it can only be changed globally at the
    // data set level.
    internal var classAttributeIndex: Int = this.attributes.size - 1

    /**
     * Gets the class [Attribute] for this instance.
     *
     * @returns The class attribute.
     */
    final fun classAttribute(): Attribute<TAttribute> {
        return this.attributes[classAttributeIndex]
    }

    /**
     * Gets the non-class attributes belonging to this instance.
     *
     * @returns The non-class attributes.
     */
    final fun attributes(filterClassAttribute: Boolean = true): List<Attribute<TAttribute>> {
        return this.attributes.filterIndexed { index, _ ->
            // Filter the class attribute when the flag is set [!(a ∧ b) == (!a ∨ !b) by De Morgan's laws]
            ((index != this.classAttributeIndex) || !filterClassAttribute)
        }
    }
}

/**
 * A basic data set made up of a collection of [Instance]s.
 *
 * An abstract implementation is provided in order to permit the freedom to have differing
 * implementation details per [DatasetLoader].
 *
 * Despite this liberal approach, there are some parts of the implementation that are rigid:
 *
 * - Any extending classes need to pass a list of instances to the constructor appropriately; clearly this
 * can be customised to the intended use case.
 * - The shape of the data set is specified as a collection of instances and functions for specifying the
 * class attribute of these instances, as well as information about the number of attributes/instances are
 * provided which can not be modified (see the functions marked `final`).
 *
 * While the behaviour for these functions could be modified, care would need to be taken.
 *
 * @param TAttribute The type of the attributes that the instances in this data set represents.
 * @property instances The instances belonging to this data set.
 */
abstract class Dataset<out TAttribute>(val instances: List<Instance<TAttribute>>) {

    // The index of the attribute that is used as the class attribute for each instance.
    // Note: By default this is set to be the last attribute.
    private var classIndex: Int = this.numAttributes() - 1

    /**
     * Sets the index of the class [Attribute] for each [Instance].
     *
     * @param index An index such that 0 <= idx < len(instances).
     */
    final fun setClassAttribute(index: Int) {
        this.classIndex = index

        // Mark each instances class attribute
        for (instance in this.instances) {
            instance.classAttributeIndex = this.classIndex
        }
    }

    /**
     * The number of instances in this data set.
     *
     * @returns The number of instances in this data set.
     */
    final fun numInstances(): Int {
        return this.instances.size
    }

    /**
     * The number of attributes in each instance in the data set.
     *
     * @returns The number of attributes in each instance in the data set.
     */
    final fun numAttributes(): Int {
        return when {
            // Protect for the case when there are no instances (and hence no attributes).
            this.numInstances() == 0 -> 0
            else -> this.instances[0].attributes.size
        }
    }
}