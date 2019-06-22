package nz.co.jedsimson.lgp.core.program


/**
 * Represents the output of a [Program].
 *
 * @param TData The type of the program output.
 */
interface Output<TData>

/**
 * A collection of built-in [Output] implementations.
 */
object Outputs {
    /**
     * Used for [Program]s with a single output value.
     *
     * @property value The single output value of a program.
     */
    class Single<TData>(val value: TData) : Output<TData>

    /**
     * Used for [Program]s with multiple output values.
     *
     * @property values The output values of a program.
     */
    class Multiple<TData>(val values: List<TData>): Output<TData>
}