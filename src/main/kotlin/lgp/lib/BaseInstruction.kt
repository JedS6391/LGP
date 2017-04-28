package lgp.lib

import lgp.core.evolution.instructions.*
import lgp.core.evolution.registers.Arguments
import lgp.core.evolution.registers.Register
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.ModuleInformation

/**
 * @suppress
 */
class BaseInstruction<T>(
        override var operation: Operation<T>,
        override var destination: RegisterIndex,
        override var operands: MutableList<RegisterIndex>
) : Instruction<T>() {

    override fun execute(registers: RegisterSet<T>) {
        val arguments = Arguments(
                this.operands.map { idx ->
                    registers.register(idx).toArgument()
                }
        )

        registers.write(this.destination, this.operation.execute(arguments))
    }

    override fun copy(): Instruction<T> {
        return BaseInstruction(
                operation = this.operation,
                destination = this.destination,
                operands = this.operands.toMutableList()
        )
    }

    override fun toString(): String {
        val representation = StringBuilder()

        representation.append("r[")
        representation.append(this.destination)
        representation.append("] = ")

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

        return representation.toString()
    }

    override val information = ModuleInformation(
        description = "A simple instruction that puts the output of an operation on" +
                      " operand registers into a destination register."
    )
}
