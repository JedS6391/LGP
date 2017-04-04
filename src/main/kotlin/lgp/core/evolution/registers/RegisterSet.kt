package lgp.core.evolution.registers

import lgp.core.environment.DefaultValueProvider
import lgp.core.environment.dataset.Attribute
import lgp.core.environment.dataset.Instance

enum class RegisterType {
    Input,
    Calculation,
    Constant,
    Unknown
}

class RegisterAccessException(message: String) : Exception(message)

class RegisterWriteRangeException(message: String) : Exception(message)

class RegisterSet<T> {

    val totalRegisters: Int

    // Bounds for different register types
    private val inputRegisters: IntRange
    private val calculationRegisters: IntRange
    private val constantRegisters: IntRange

    private val inputRegistersCount: Int
    private val calculationRegistersCount: Int
    private val  constantRegistersCount: Int

    private val defaultValueProvider: DefaultValueProvider<T>

    private var registers: MutableList<Register<T>>

    val count: Int get() = this.registers.size


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

        this.defaultValueProvider = defaultValueProvider

        // Use an array list in the hope that a fixed size data structure that is not added to
        // will give us speed improvements when accessing registers.
        this.registers = ArrayList(this.totalRegisters)

        // Make sure every register slot has a default value.
        this.clear()

        // Initialise the constant registers
        this.writeConstants(constants)
    }

    private fun clear() {
        for (r in 0 .. this.totalRegisters - 1) {
            this.registers.add(r, Register(this.defaultValueProvider.value, r))
        }
    }

    private fun registerType(index: Int): RegisterType {
        return when (index) {
            in this.inputRegisters -> RegisterType.Input
            in this.calculationRegisters -> RegisterType.Calculation
            in this.constantRegisters -> RegisterType.Constant
            else -> RegisterType.Unknown // Hopefully we'll never get here but we need an exhaustive case.
        }
    }

    fun read(index: Int): Register<T> {
        return this.registers[index]
    }

    fun write(index: Int, value: T) {
        val type = this.registerType(index)

        when (type) {
            RegisterType.Constant -> throw RegisterAccessException("Can't write to constant register.")
            else -> {
                this.overwrite(index, value)
            }
        }
    }

    private fun overwrite(index: Int, value: T) {
        this.registers[index] = Register(value, index)
    }

    fun writeInstance(data: Instance<T>) {
        // The number of attributes must match the number of input registers
        assert(this.inputRegistersCount == data.attributes.size)

        this.writeRange(
                // Use `attributes()` to filter out class attribute
                data.attributes().map(Attribute<T>::value),
                this.inputRegisters
        )
    }

    private fun writeConstants(constants: List<T>) {
        this.writeRange(constants, this.constantRegisters)
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