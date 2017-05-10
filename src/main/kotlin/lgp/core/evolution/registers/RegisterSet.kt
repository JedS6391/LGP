package lgp.core.evolution.registers

import lgp.core.environment.DefaultValueProvider
import lgp.core.environment.dataset.Attribute
import lgp.core.environment.dataset.Instance

/**
 * Represents the type of a register.
 */
enum class RegisterType {
    /**
     * A register that contains input values from a data set instance.
     */
    Input,

    /**
     * A register that can be used for calculations and will be loaded
     * with some default value (given by a [DefaultValueProvider]).
     */
    Calculation,

    /**
     * A register that is read-only and will be initialised with values
     * from a set of constants in the LGP environment.
     */
    Constant,

    /**
     * A register that does not fall in some known range will have unknown
     * type. This type primarily serves as a default case when mapping register
     * index ranges to register types.
     */
    Unknown
}

/**
 * Thrown when a write operation is attempted on a [RegisterType.Constant] register.
 *
 * @param message A message accompanying the exception.
 */
class RegisterAccessException(message: String) : Exception(message)

/**
 * Thrown when the number of values being written to a particular register range,
 * does not match the size of the range.
 *
 * @param message A message accompanying the exception.
 */
class RegisterWriteRangeException(message: String) : Exception(message)

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
 * Input registers are used to store the value of attributes in an instance from some
 * data set. These input registers will have their value loaded from an instance before
 * evaluating an LGP program on some fitness case (an instance). The number of input registers
 * should be specified to match the number of attributes in each data set instance that the LGP
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
 * @param T The type of the value that each register contains.
 */
class RegisterSet<T> {
    val totalRegisters: Int

    // Bounds for different register types
    val inputRegisters: IntRange
    val calculationRegisters: IntRange
    val constantRegisters: IntRange

    private val inputRegistersCount: Int
    private val calculationRegistersCount: Int
    private val  constantRegistersCount: Int

    private val defaultValueProvider: DefaultValueProvider<T>

    val registers: MutableList<Register<T>>

    /**
     * The total number of registers in this register set.
     */
    val count: Int get() = this.registers.size

    val constants: List<T>

    /**
     * Creates a new [RegisterSet] with the specified parameters.
     *
     * @param inputRegisters Number of registers reserved for input values from a data set instance.
     * @param calculationRegisters Number of registers reserved for calculations (i.e. free registers).
     * @param constants A collection of constants to be stored in read-only constant registers.
     * @param defaultValueProvider An implementation that can provide default values for the calculation registers.
     */
    constructor(inputRegisters: Int,
                calculationRegisters: Int,
                constants: List<T>,
                defaultValueProvider: DefaultValueProvider<T>) {

        // TODO: Busy constructor... could possibly be stream-lined.
        val constantRegisters = constants.size

        this.totalRegisters = inputRegisters + calculationRegisters + constantRegisters

        this.inputRegisters = 0 .. (inputRegisters - 1)
        this.calculationRegisters = inputRegisters .. ((inputRegisters + calculationRegisters) - 1)
        this.constantRegisters = (inputRegisters + calculationRegisters) .. this.totalRegisters - 1

        this.inputRegistersCount = inputRegisters
        this.calculationRegistersCount = calculationRegisters
        this.constantRegistersCount = constantRegisters

        this.constants = constants

        this.defaultValueProvider = defaultValueProvider

        this.registers = mutableListOf()

        // Make sure every register slot has a default value.
        this.initialise()

        // Initialise the constant registers
        this.writeConstants()
    }

    internal constructor(source: RegisterSet<T>) {
        this.totalRegisters = source.totalRegisters

        this.inputRegisters = source.inputRegisters
        this.calculationRegisters = source.calculationRegisters
        this.constantRegisters = source.constantRegisters

        this.inputRegistersCount = source.inputRegistersCount
        this.calculationRegistersCount = source.calculationRegistersCount
        this.constantRegistersCount = source.constantRegistersCount

        this.defaultValueProvider = source.defaultValueProvider
        this.constants = source.constants
        this.registers = mutableListOf()

        for (register in source.registers) {
            this.registers.add(register.index, Register(register))
        }
    }

    fun register(index: Int): Register<T> {
        return this.registers[index]
    }

    /**
     * Get the value the of the register at the given index.
     *
     * @param index The register to read from.
     * @returns The value of the register at the given index.
     */
    fun read(index: Int): T {
        return this.registers[index].value
    }

    /**
     * Sets the value of the register at the given index.
     *
     * @param index The register to write to.
     * @param value The value to write to the register.
     * @throws RegisterAccessException When the index refers to a constant register.
     */
    fun write(index: Int, value: T) {
        val type = this.registerType(index)

        when (type) {
            RegisterType.Constant -> throw RegisterAccessException("Can't write to constant register.")
            else -> {
                this.overwrite(index, value)
            }
        }
    }

    /**
     * Writes an instance from some data set to the register sets input registers.
     *
     * NOTE: The number of input registers of the set must match the number of attributes in the instance.
     *
     * @param data An instance to write to the input registers.
     */
    fun writeInstance(data: Instance<T>) {
        // The number of attributes must match the number of input registers
        assert(this.inputRegistersCount == data.attributes.size)

        this.writeRange(
                // Use `attributes()` to filter out class attribute
                data.attributes().map(Attribute<T>::value),
                this.inputRegisters
        )
    }

    fun registerType(index: Int): RegisterType {
        return when (index) {
            in this.inputRegisters -> RegisterType.Input
            in this.calculationRegisters -> RegisterType.Calculation
            in this.constantRegisters -> RegisterType.Constant
            else -> RegisterType.Unknown // Hopefully we'll never get here but we need an exhaustive case.
        }
    }

    fun reset() {
        // A reset is similar to a clear operation except that it only sets the value of the input and calculation
        // registers to default values.
        for (r in this.inputRegisters) {
            this.registers[r] = Register(this.defaultValueProvider.value, r)
        }

        for (r in this.calculationRegisters) {
            this.registers[r] = Register(this.defaultValueProvider.value, r)
        }

        //this.writeConstants()
    }

    private fun initialise() {
        for (r in 0 .. this.totalRegisters - 1) {
            this.registers.add(r, Register(this.defaultValueProvider.value, r))
        }
    }

    fun overwrite(index: Int, value: T) {
        this.registers[index] = Register(value, index)
    }

    private fun writeConstants() {
        this.writeRange(this.constants, this.constantRegisters)
    }

    private fun writeRange(coll: List<T>, range: IntRange) {
        val size = (range.endInclusive - range.start) + 1

        if (size != coll.size) {
            throw RegisterWriteRangeException("${size} != ${coll.size}")
        }

        for ((idx, value) in range.zip(coll)) {
            this.overwrite(idx, value)
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()

        for (register in this.registers) {
            sb.append(register.toString())
            sb.append(if (register.index in this.constantRegisters) " (CONSTANT)" else "")
            sb.append('\n')
        }

        return sb.toString()
    }
}

fun <T> RegisterSet<T>.copy(): RegisterSet<T> {
    return RegisterSet(this)
}

