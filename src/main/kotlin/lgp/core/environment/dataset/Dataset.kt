package lgp.core.environment.dataset

/**
 * An attribute from some data set.
 *
 * @property name the name of the this attribute in the data set.
 * @property value the value of this attribute in the data set.
 */
class Attribute<out T>(val name: String, val value: T)

/**
 * A generic data set.
 *
 * Generally a data set would be made up of some collection of attributes,
 * but it is up to the implementation to decide on how they are represented
 * internally.
 *
 * TODO: Probably good to make this interface more robust so that implementations
 * can convert from a dataset to fitness cases, etc.
 */
interface Dataset<out T>