package lgp.lib

import lgp.core.evolution.instructions.*
import lgp.core.evolution.registers.Arguments
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.ModuleInformation

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
        val representation = StringBuilder()

        if (this.operation is BranchOperation<T>) {
            representation.append("if (")
        } else {
            representation.append("r[")
            representation.append(this.destination)
            representation.append("] = ")
        }

        // TODO: Sanity check length of registers
        if (this.operation.arity === BaseArity.Unary) {
            representation.append(this.operation.representation)
            representation.append("(r[")
            representation.append(this.operands[0])
            representation.append("])")
        } else if (this.operation.arity === BaseArity.Binary) {
            representation.append("r[")
            representation.append(this.operands[0])
            representation.append("]")
            representation.append(this.operation.representation)
            representation.append("r[")
            representation.append(this.operands[1])
            representation.append("]")
        }

        if (this.operation is BranchOperation<T>)
            representation.append(")")

        return representation.toString()
    }

    override val information = ModuleInformation(
        description = "A simple instruction that puts the output of an operation on" +
                      " operand registers into a destination register."
    )
}
