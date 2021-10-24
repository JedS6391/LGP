package nz.co.jedsimson.lgp.core.environment.dataset


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