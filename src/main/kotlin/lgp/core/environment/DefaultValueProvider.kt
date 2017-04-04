package lgp.core.environment

/**
 * An implementation will be able to provide default values to some consumer.
 *
 * @param TValue The type of the values that will be provided.
 */
interface DefaultValueProvider<out TValue> {
    /**
     * A value that will be provided to any consumers.
     */
    val value: TValue
}