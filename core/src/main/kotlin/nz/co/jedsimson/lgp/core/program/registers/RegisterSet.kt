package nz.co.jedsimson.lgp.core.program.registers

import nz.co.jedsimson.lgp.core.environment.DefaultValueProvider
import nz.co.jedsimson.lgp.core.environment.dataset.Feature
import nz.co.jedsimson.lgp.core.environment.dataset.Sample

/**
 * Represents a collection of [Register]s.
 *
 * The collection is broken into three separate ranges, for the three different
 * types of registers that are available to an LGP program. In a diagram form, the
 * ranges look something like:
 *
 * ```
 *     +-------+-------------+----------+
 *     | Input | Calculation | Constant |
 *     +-------+-------------+----------+
 * ```
 *
 * Input registers are used to store the value of features in an instance from some
 * data set. These input registers will have their value loaded from an instance before
 * evaluating an LGP program on some fitness case (an instance). The number of input registers
 * should be specified to match the number of features in each data set instance that the LGP
 * environment is performing evolution on.
 *
 * Calculation registers are freely available registers that can be used in an LGP program.
 * They are initialised with some default value when the register set is initialised. The
 * default value can be controlled by passing a specific [DefaultValueProvider].
 *
 * Constant registers are used to store a fixed set of constants, that are read-only to an
 * LGP program. The values of the registers will be loaded from constants specified during
 * initialisation of the register set.
 *
 * @param TData The type of the value that each register contains.
 */
interface RegisterSet<TData> {

    /**
     * Number of [RegisterType.Input] registers.
     */
    val inputRegisters: IntRange

    /**
     * Number of [RegisterType.Calculation] registers.
     */
    val calculationRegisters: IntRange

    /**
     * Number of [RegisterType.Constant] registers.
     */
    val constantRegisters: IntRange

    /**
     * The total number of registers in this register set.
     */
    val count: Int

    /**
     * Get the [Register] with the given index.
     *
     * @param index The index of the desired register.
     * @returns The [Register] instance that has the index specified.
     * @throws RegisterReadException When [index] is out-of-bounds of the register set.
     */
    fun register(index: Int): Register<TData>

    /**
     * Applies the given [modifier] to the value of the [Register] at [index].
     *
     * @param index The register to modify the value of.
     * @param modifier A function that modifies the value in the register.
     * @throws RegisterReadException When [index] is out-of-bounds of the register set.
     */
    fun apply(index: Int, modifier: (TData) -> TData)

    /**
     * Get the value the of the register at the given index.
     *
     * @param index The register to read from.
     * @returns The value of the register at the given index.
     * @throws RegisterReadException When [index] is out-of-bounds of the register set.
     */
    operator fun get(index: Int): TData

    /**
     * Sets the value of the register at the given index.
     *
     * @param index The register to write to.
     * @param value The value to write to the register.
     * @throws RegisterAccessException When the index refers to a constant register.
     */
    operator fun set(index: Int, value: TData)

    /**
     * Forcefully sets the value or the register with the given [index].
     *
     * This method has similar functionality to the [RegisterSet::set] method but does not
     * check the type of the register -- allowing constant registers to be overwritten without exception.
     *
     * @param index The register to write to.
     * @param value The value to write to the register.
     */
    fun overwrite(index: Int, value: TData)

    /**
     * Writes an instance from some data set to the register sets input registers.
     *
     * NOTE: The number of input registers of the set must match the number of features in the instance.
     *
     * @param data An instance to write to the input registers.
     * @throws RegisterWriteRangeException When the number of features does not match the number of input registers.
     */
    fun writeInstance(data: Sample<TData>)

    /**
     * Determines the type of the register with the given [index].
     *
     * @param index The index of the register whose type is desired.
     * @returns A [RegisterType] mapping for the given register.
     */
    fun registerType(index: Int): RegisterType

    /**
     * Resets the input and calculation registers to their default values.
     */
    fun reset()

    /**
     * Creates a clone of this register set.
     *
     * @returns A clone of this register set.
     */
    fun copy(): RegisterSet<TData>
}

/**
 * An implementation of [RegisterSet] that uses an array as the register store.
 *
 * @param TData The type of the value that each register contains.
 */
class ArrayRegisterSet<TData> : RegisterSet<TData> {

    override val inputRegisters: IntRange
    override val calculationRegisters: IntRange
    override val constantRegisters: IntRange
    override val count: Int get() = this.registers.size

    // Register set backing store
    private val registers: Array<Register<TData>>
    private val totalRegisters: Int

    // Keep a track of the original constants the register set is initialised with.
    private val constants: List<TData>

    // Used for initialising calculation registers.
    private val defaultValueProvider: DefaultValueProvider<TData>

    /**
     * Creates a new [ArrayRegisterSet] with the specified parameters.
     *
     * @param inputRegisters Number of registers reserved for input values from a data set instance.
     * @param calculationRegisters Number of registers reserved for calculations (i.e. free registers).
     * @param constants A collection of constants to be stored in read-only constant registers.
     * @param defaultValueProvider An implementation that can provide default values for the calculation registers.
     */
    constructor(
        inputRegisters: Int,
        calculationRegisters: Int,
        constants: List<TData>,
        defaultValueProvider: DefaultValueProvider<TData>
    ) {
        if (inputRegisters < 0 || calculationRegisters < 0) {
            throw RegisterSetInitialisationException("inputRegisters and calculationRegisters must be positive.")
        }

        val constantRegisters = constants.size

        this.totalRegisters = inputRegisters + calculationRegisters + constantRegisters

        this.inputRegisters = 0 until inputRegisters
        this.calculationRegisters = inputRegisters until (inputRegisters + calculationRegisters)
        this.constantRegisters = (inputRegisters + calculationRegisters) until this.totalRegisters

        this.constants = constants
        this.defaultValueProvider = defaultValueProvider

        // Make sure every register slot has a default value.
        this.registers = this.initialise()

        // Initialise the constant registers
        this.writeConstants()
    }

    /**
     * Creates a new [ArrayRegisterSet] based on the [source] [ArrayRegisterSet].
     *
     * Essentially, this constructor will create a clone of [source] but with its own [Register]
     * instances, so that modifications don't effect the original.
     *
     * @param source A [RegisterSet] instance to clone.
     */
    internal constructor(source: ArrayRegisterSet<TData>) {
        this.totalRegisters = source.totalRegisters
        this.inputRegisters = source.inputRegisters
        this.calculationRegisters = source.calculationRegisters
        this.constantRegisters = source.constantRegisters
        this.defaultValueProvider = source.defaultValueProvider
        this.constants = source.constants
        // We have to make sure we map to new Register instances because otherwise
        // the modifying the register set copy will effect the original.
        this.registers = source.registers.map { r -> Register(r) }.toTypedArray()
    }

    override fun register(index: Int): Register<TData> {
        try {
            return this.registers[index]
        }
        catch (outOfBounds: ArrayIndexOutOfBoundsException) {
            throw RegisterReadException("No register exists with the index $index in the register set.")
        }
    }

    override fun apply(index: Int, modifier: (TData) -> TData) {
        val current = this[index]

        this.overwrite(index, modifier(current))
    }

    override operator fun get(index: Int): TData {
        try {
            return this.registers[index].value
        }
        catch (outOfBounds: ArrayIndexOutOfBoundsException) {
            throw RegisterReadException("No register exists with the index $index in the register set.")
        }
    }

    override operator fun set(index: Int, value: TData) {
        val type = this.registerType(index)

        // Constant registers can't be overwritten.
        when (type) {
            RegisterType.Constant -> throw RegisterAccessException("Can't write to constant register.")
            else -> {
                this.overwrite(index, value)
            }
        }
    }

    override fun overwrite(index: Int, value: TData) {
        this.registers[index] = Register(value, index)
    }

    override fun writeInstance(data: Sample<TData>) {
        when {
            (this.inputRegisters.count() != data.features.size) -> {
                throw RegisterWriteRangeException("The number of features must match the number of input registers.")
            }
            else -> {
                this.writeRange(
                    data.features.map(Feature<TData>::value),
                    this.inputRegisters
                )
            }
        }
    }

    override fun registerType(index: Int): RegisterType {
        return when (index) {
            in this.inputRegisters -> RegisterType.Input
            in this.calculationRegisters -> RegisterType.Calculation
            in this.constantRegisters -> RegisterType.Constant
            else -> RegisterType.Unknown // Hopefully we'll never get here but we need an exhaustive case.
        }
    }

    override fun reset() {
        val registersToReset = listOf(this.inputRegisters, this.calculationRegisters)

        registersToReset.flatten().forEach { r ->
            this.registers[r] = Register(this.defaultValueProvider.value, r)
        }
    }

    override fun copy(): RegisterSet<TData> {
        return ArrayRegisterSet(this)
    }

    /**
     * Initialises all registers with a default value using [defaultValueProvider].
     */
    private fun initialise(): Array<Register<TData>> {
        return Array(this.totalRegisters) { index ->
            Register(this.defaultValueProvider.value, index)
        }
    }

    /**
     * Writes the initial constant values to their registers, overwriting whatever is currently stored.
     */
    private fun writeConstants() {
        this.writeRange(this.constants, this.constantRegisters)
    }

    /**
     * Writes [source] to the registers with indices given by [range].
     *
     * @param source A collections of value to write to the specified indices.
     * @param range A collection of indices to write [source] values to.
     * @throws RegisterWriteRangeException When the number of values does not equal the number of indices.
     */
    private fun writeRange(source: List<TData>, range: IntRange) {
        val size = (range.endInclusive - range.start) + 1

        // This should be asserted by callers, but we do it here as a sanity check.
        when {
            (size != source.size) -> throw RegisterWriteRangeException("$size != ${source.size}")
            else -> {
                range.zip(source).forEach { (idx, value) ->
                    this.overwrite(idx, value)
                }
            }
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()

        this.registers.forEach { register ->
            sb.append(register.toString())
            sb.append(if (register.index in this.constantRegisters) " (CONSTANT)" else "")
            sb.append('\n')
        }

        return sb.toString()
    }
}

