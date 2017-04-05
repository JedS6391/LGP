package lgp.lib

import lgp.core.evolution.instructions.Arity
import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.instructions.Operation
import lgp.core.evolution.registers.Arguments
import lgp.core.evolution.registers.Register
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.ModuleInformation

class BaseInstruction<T>(
        override val operation: Operation<T>,
        private val destination: Register<T>,
        private val operands: List<Register<T>>
) : Instruction<T>() {

    override fun execute(registers: RegisterSet<T>) {
        val arguments = Arguments(this.operands.map(Register<T>::toArgument))

        this.destination.value = this.operation.execute(arguments)
    }

    override fun toString(): String {
        val representation = StringBuilder()

        representation.append("r[")
        representation.append(this.destination.index)
        representation.append("] = ")

        // TODO: Sanity check length of registers
        if (this.operation.arity === Arity.Unary) {
            representation.append(this.operation.representation)
            representation.append("(r[")
            representation.append(this.operands[0].index)
            representation.append("])")
        } else if (this.operation.arity === Arity.Binary) {
            representation.append("r[")
            representation.append(this.operands[0].index)
            representation.append("]")
            representation.append(this.operation.representation)
            representation.append("r[")
            representation.append(this.operands[1].index)
            representation.append("]")
        }

        return representation.toString()
    }

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A simple instruction that puts the output of an operation on operand registers into a destination register."
    }
}
