package lgp.lib

import lgp.core.program.instructions.*
import lgp.core.program.registers.Arguments
import lgp.core.program.registers.RegisterSet
import lgp.core.modules.ModuleInformation
import lgp.lib.operations.*

/**
 * A built-in offering of the ``Instruction`` type.
 *
 * This instruction is simple in its representation and execution:
 *     - It consists of a single operation that operates on a set of operand registers
 *       and stores the result in a single destination register.
 *     - It provides the ability to be exported as a C-style instruction (e.g. "r[1] = r[1] + r[2]")
 */
class BaseInstruction<T>(
        override var operation: Operation<T>,
        override var destination: RegisterIndex,
        override var operands: MutableList<RegisterIndex>
) : Instruction<T>() {

    /**
     * Applies [operation] to the [operands] and stores the result in [destination].
     */
    override fun execute(registers: RegisterSet<T>) {
        val arguments = Arguments(
                this.operands.map { idx ->
                    registers.register(idx).toArgument()
                }
        )

        registers[this.destination] = this.operation.execute(arguments)
    }

    /**
     * Creates a new ``BaseInstruction`` instance that is a clone of this instruction.
     */
    override fun copy(): Instruction<T> {
        return BaseInstruction(
                operation = this.operation,
                destination = this.destination,
                operands = this.operands.toMutableList()
        )
    }

    /**
     * Provides a C-style representation of this instruction.
     */
    override fun toString(): String {
        return this.operation.toString(this.operands, this.destination)
    }

    override val information = ModuleInformation(
        description = "A simple instruction that puts the output of an operation on" +
                      " operand registers into a destination register."
    )
}
